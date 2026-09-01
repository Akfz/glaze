package v.akfz.glazelib.shader.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import v.akfz.glazelib.shader.api.IShaderProgram;
import v.akfz.glazelib.shader.api.ShaderUniformManager;
import v.akfz.glazelib.util.gl.GLPossibilities;
import java.nio.IntBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.opengl.GL43;

import static org.lwjgl.opengl.GL30.glGetIntegeri_v;
import static org.lwjgl.opengl.GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT;
import static org.lwjgl.opengl.GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE;
import static org.lwjgl.opengl.GL20.*;

public class ComputeShaderProgram implements IShaderProgram {
	private int programId;
	private final ResourceLocation computeLocation;
	private final String directSource;
	private final Map<String, Integer> uniformLocationCache = new ConcurrentHashMap<>();
	public ShaderUniformManager uniformManager;
	private boolean isInUse = false;

	private int maxWorkGroupX = -1, maxWorkGroupY = -1, maxWorkGroupZ = -1;
	private int maxWorkGroupCountX = -1, maxWorkGroupCountY = -1, maxWorkGroupCountZ = -1;

	public ComputeShaderProgram(ResourceLocation computeLocation) {
		if (!GLPossibilities.supportsComputeShaders()) {
			throw new UnsupportedOperationException("Compute shaders are not supported on this GPU. Requires OpenGL 4.3+ or GL_ARB_compute_shader.");
		}
		this.computeLocation = computeLocation;
		this.directSource = null;
		this.init();
	}

	public ComputeShaderProgram(String directSource) {
		if (!GLPossibilities.supportsComputeShaders()) {
			throw new UnsupportedOperationException("Compute shaders are not supported on this GPU.");
		}
		this.computeLocation = null;
		this.directSource = directSource;
		this.init();
	}

	private void init() {
		if (this.programId > 0) this.cleanup();
		this.programId = glCreateProgram();

		try {
			String source = (this.directSource != null) ? this.directSource : this.loadShaderSource(this.computeLocation);
			int computeId = this.compileComputeShader(source);

			glAttachShader(this.programId, computeId);
			glLinkProgram(this.programId);
			this.checkProgramLinkStatus(this.programId);

			glDetachShader(this.programId, computeId);
			glDeleteShader(computeId);

			this.uniformManager = new ShaderUniformManager(this);

			glValidateProgram(this.programId);
			this.checkProgramValidateStatus(this.programId);
			this.queryGpuLimits();
		} catch (Exception e) {
			throw new RuntimeException("Failed to link compute shader program: " + this.getProgramName(), e);
		}
	}

	private int compileComputeShader(String source) {
		int shaderId = glCreateShader(GL43.GL_COMPUTE_SHADER);
		glShaderSource(shaderId, source);
		glCompileShader(shaderId);

		if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
			String log = glGetShaderInfoLog(shaderId);
			glDeleteShader(shaderId);
			throw new RuntimeException("Compile error in Compute shader (" + this.getProgramName() + "):\n" + log);
		}
		return shaderId;
	}

	private void queryGpuLimits() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer buf = stack.mallocInt(1);

			glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0, buf);
			this.maxWorkGroupX = buf.get(0);
			glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1, buf);
			this.maxWorkGroupY = buf.get(0);
			glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2, buf);
			this.maxWorkGroupZ = buf.get(0);

			glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0, buf);
			this.maxWorkGroupCountX = buf.get(0);
			glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1, buf);
			this.maxWorkGroupCountY = buf.get(0);
			glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2, buf);
			this.maxWorkGroupCountZ = buf.get(0);
		}
	}
	private String loadShaderSource(ResourceLocation location) {
		try {
			Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(location);
			if (resourceOptional.isEmpty()) throw new IOException("Compute shader resource not found: " + location);
			try (InputStream stream = resourceOptional.get().open()) {
				return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load compute shader source: " + location, e);
		}
	}

	@Override
	public void dispatch(int x, int y, int z) {
		dispatch(x, y, z, GL43.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_TEXTURE_UPDATE_BARRIER_BIT);
	}

	public void dispatch(int x, int y, int z, int barrierBits) {
		checkActive();
		if (x <= 0 || y <= 0 || z <= 0) {
			throw new IllegalArgumentException("Dispatch dimensions must be > 0, got: " + x + "x" + y + "x" + z);
		}
		if (x > maxWorkGroupCountX || y > maxWorkGroupCountY || z > maxWorkGroupCountZ) {
			throw new IllegalArgumentException("Dispatch exceeds GPU limits. Max: " + maxWorkGroupCountX + "x" + maxWorkGroupCountY + "x" + maxWorkGroupCountZ + ", got: " + x + "x" + y + "x" + z);
		}

		GL43.glDispatchCompute(x, y, z);
		if (barrierBits != 0) {
			GL43.glMemoryBarrier(barrierBits);
		}
	}

	public void dispatchForSize(int width, int height, int localSizeX, int localSizeY) {
		int groupsX = (width + localSizeX - 1) / localSizeX;
		int groupsY = (height + localSizeY - 1) / localSizeY;
		dispatch(groupsX, groupsY, 1);
	}

	public int getMaxWorkGroupSizeX() { return maxWorkGroupX; }
	public int getMaxWorkGroupSizeY() { return maxWorkGroupY; }
	public int getMaxWorkGroupSizeZ() { return maxWorkGroupZ; }
	public int getMaxWorkGroupCountX() { return maxWorkGroupCountX; }
	public int getMaxWorkGroupCountY() { return maxWorkGroupCountY; }
	public int getMaxWorkGroupCountZ() { return maxWorkGroupCountZ; }

	public void reload() { this.cleanup(); this.init(); }
	public void use() { glUseProgram(this.programId); this.isInUse = true; }

	public void use(Runnable action) {
		this.use();
		try { action.run(); } finally { this.stop(); }
	}

	public void stop() { glUseProgram(0); this.isInUse = false; }

	public void cleanup() {
		if (this.programId != 0) {
			glDeleteProgram(this.programId);
			this.programId = 0;
		}
		this.uniformLocationCache.clear();
		this.isInUse = false;
	}

	@Override
	public int getUniformLocation(String name) {
		if (this.programId <= 0) return -1;
		return this.uniformLocationCache.computeIfAbsent(name, key -> glGetUniformLocation(this.programId, key));
	}

	@Override
	public void checkActive() {
		if (!this.isInUse) throw new IllegalStateException("Compute shader program '" + this.getProgramName() + "' is not active!");
	}

	@Override
	public ShaderUniformManager getUniformManager() { return this.uniformManager; }

	@Override
	public int getProgramID() { return this.programId; }

	@Override
	public String getProgramName() {
		if (this.computeLocation == null) return "DynamicComputeShader";
		String path = this.computeLocation.getPath();
		int lastSlash = path.lastIndexOf('/');
		return lastSlash != -1 ? path.substring(lastSlash + 1) : path;
	}

	public ResourceLocation getComputeLocation() { return this.computeLocation; }

	@Override
	public boolean isValid() { return this.programId > 0 && glIsProgram(this.programId); }

	private void checkProgramLinkStatus(int id) {
		if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
			throw new RuntimeException("Compute program linkage error: " + glGetProgramInfoLog(id));
		}
	}

	private void checkProgramValidateStatus(int id) {
		if (glGetProgrami(id, GL_VALIDATE_STATUS) == GL_FALSE) {
			System.err.println("Compute program validation warning: " + glGetProgramInfoLog(id));
		}
	}
}