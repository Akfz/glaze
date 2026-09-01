package v.akfz.glazelib.shader.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import v.akfz.glazelib.shader.api.IShaderProgram;
import v.akfz.glazelib.shader.api.ShaderUniformManager;
import v.akfz.glazelib.util.gl.GLPossibilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL40.*;

public class TessellationShaderProgram implements IShaderProgram {
	private int programId;
	private final ResourceLocation vertexLocation;
	private final ResourceLocation tessControlLocation;
	private final ResourceLocation tessEvalLocation;
	private final ResourceLocation fragmentLocation;
	private final Map<String, Integer> uniformLocationCache = new ConcurrentHashMap<>();
	public ShaderUniformManager uniformManager;
	private boolean isInUse = false;

	public TessellationShaderProgram(ResourceLocation vertex, ResourceLocation tessControl, ResourceLocation tessEval, ResourceLocation fragment) {
		if (!GLPossibilities.supportsTessellation()) {
			throw new UnsupportedOperationException("Tessellation shaders are not supported on this GPU. Requires OpenGL 4.0+ or GL_ARB_tessellation_shader.");
		}
		this.vertexLocation = vertex;
		this.tessControlLocation = tessControl;
		this.tessEvalLocation = tessEval;
		this.fragmentLocation = fragment;
		this.init();
	}

	private void init() {
		if (this.programId > 0) this.cleanup();
		this.programId = glCreateProgram();
		List<Integer> compiledShaders = new ArrayList<>();

		try {
			compiledShaders.add(this.createShader(this.vertexLocation, GL_VERTEX_SHADER));
			compiledShaders.add(this.createShader(this.tessControlLocation, GL_TESS_CONTROL_SHADER));
			compiledShaders.add(this.createShader(this.tessEvalLocation, GL_TESS_EVALUATION_SHADER));
			compiledShaders.add(this.createShader(this.fragmentLocation, GL_FRAGMENT_SHADER));

			for (int shaderId : compiledShaders) {
				glAttachShader(this.programId, shaderId);
			}

			glBindAttribLocation(this.programId, 0, "aPos");
			glBindAttribLocation(this.programId, 1, "aTexCoord");
			glLinkProgram(this.programId);
			this.checkProgramLinkStatus(this.programId);

			for (int shaderId : compiledShaders) {
				glDetachShader(this.programId, shaderId);
				glDeleteShader(shaderId);
			}

			this.uniformManager = new ShaderUniformManager(this);
			glValidateProgram(this.programId);
			this.checkProgramValidateStatus(this.programId);
		} catch (Exception e) {
			for (int shaderId : compiledShaders) {
				if (shaderId != 0) glDeleteShader(shaderId);
			}
			throw new RuntimeException("Failed to link tessellation shader program: " + this.getProgramName(), e);
		}
	}

	private int createShader(ResourceLocation location, int type) {
		String source = this.loadShaderSource(location);
		int shaderId = glCreateShader(type);
		glShaderSource(shaderId, source);
		glCompileShader(shaderId);

		if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
			String log = glGetShaderInfoLog(shaderId);
			String typeName = type == GL_VERTEX_SHADER ? "Vertex" : (type == GL_TESS_CONTROL_SHADER ? "TessControl" : (type == GL_TESS_EVALUATION_SHADER ? "TessEval" : "Fragment"));
			glDeleteShader(shaderId);
			throw new RuntimeException("Compile error in " + typeName + " shader (" + location + "):\n" + log);
		}
		return shaderId;
	}

	private String loadShaderSource(ResourceLocation location) {
		try {
			Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(location);
			if (resourceOptional.isEmpty()) throw new IOException("Shader assets file not found: " + location);
			try (InputStream stream = resourceOptional.get().open()) {
				return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load shader source: " + location, e);
		}
	}

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
		if (!this.isInUse) throw new IllegalStateException("Tessellation shader program '" + this.getProgramName() + "' is not active!");
	}

	@Override
	public ShaderUniformManager getUniformManager() { return this.uniformManager; }

	@Override
	public int getProgramID() { return this.programId; }

	@Override
	public String getProgramName() {
		if (this.tessControlLocation == null) return "UnknownTessellation";
		String path = this.tessControlLocation.getPath();
		int lastSlash = path.lastIndexOf('/');
		return lastSlash != -1 ? path.substring(lastSlash + 1) : path;
	}

	@Override
	public boolean isValid() { return this.programId > 0 && glIsProgram(this.programId); }

	private void checkProgramLinkStatus(int id) {
		if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
			throw new RuntimeException("Tessellation program linkage error: " + glGetProgramInfoLog(id));
		}
	}

	private void checkProgramValidateStatus(int id) {
		if (glGetProgrami(id, GL_VALIDATE_STATUS) == GL_FALSE) {
			System.err.println("Tessellation program validation warning: " + glGetProgramInfoLog(id));
		}
	}
}