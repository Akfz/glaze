package v.akfz.glaze.impl.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import v.akfz.glaze.Glaze;
import v.akfz.glazelib.shader.api.IShaderProgram;
import v.akfz.glazelib.shader.api.ShaderUniformManager;
import v.akfz.glazelib.util.PingPongBuffer;
import v.akfz.glazelib.util.QuadMesh;

import java.util.ArrayList;
import java.util.List;

public class PostProcessRenderer {
	public static final PostProcessRenderer INSTANCE = new PostProcessRenderer();

	public boolean renderGlobal = true;

	private final List<ResourceLocation> pendingShaders = new ArrayList<>();
	private final List<PostProcessShader> shaders = new ArrayList<>();

	private QuadMesh quadMesh;
	private PingPongBuffer pingPong;

	private int cachedWidth = -1;
	private int cachedHeight = -1;
	private boolean meshInitialized = false;
	private boolean initialized = false;

	private PostProcessRenderer() {}

	public void render(RenderTarget mainBuffer) {
		if (mainBuffer == null) return;
		if (!Glaze.postProcess) return;

		checkInit();
		if (shaders.isEmpty()) return;

		int width = mainBuffer.width;
		int height = mainBuffer.height;
		if (width <= 0 || height <= 0) return;

		ensureResources(width, height);

		ShaderInstance previousShader = RenderSystem.getShader();
		boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

		GlStateManager._disableDepthTest();
		GlStateManager._disableBlend();
		GlStateManager._disableCull();
		if (scissorEnabled) GL11.glDisable(GL11.GL_SCISSOR_TEST);

		blit(mainBuffer, pingPong.front());
		pingPong.front().copyDepthFrom(mainBuffer);

		int depthTextureId = pingPong.front().getDepthTextureId();
		float nearPlane = 0.05f;
		float farPlane = Minecraft.getInstance().options.renderDistance().get() * 16.0f;

		for (IShaderProgram shader : shaders) {
			if (!shader.isValid()) continue;

			RenderTarget input = pingPong.front();
			RenderTarget output = pingPong.back();

			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, output.frameBufferId);
			GL11.glViewport(0, 0, width, height);
			GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

			final int inputColor = input.getColorTextureId();
			final int depthId = depthTextureId;

			shader.use(() -> {
				ShaderUniformManager um = shader.getUniformManager();

				RenderSystem.activeTexture(GL13.GL_TEXTURE0);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, inputColor);
				um.setTexture("uTexture", 0);

				if (depthId != 0) {
					RenderSystem.activeTexture(GL13.GL_TEXTURE1);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthId);
					um.setTexture("uDepth", 1);
				}

				um.set("uNear", nearPlane);
				um.set("uFar", farPlane);
				um.set("uResolution", new org.joml.Vector2f(width, height));
				um.set("uTime", (float) (System.currentTimeMillis() % 100000L) / 1000.0f);

				quadMesh.render();
			});

			pingPong.swap();
		}

		RenderTarget finalResult = pingPong.front();
		if (finalResult != mainBuffer) {
			blit(finalResult, mainBuffer);
		}

		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainBuffer.frameBufferId);
		GlStateManager._enableDepthTest();
		GlStateManager._enableBlend();
		GlStateManager._enableCull();
		if (scissorEnabled) GL11.glEnable(GL11.GL_SCISSOR_TEST);

		RenderSystem.activeTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		RenderSystem.activeTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

		if (previousShader != null) {
			RenderSystem.setShader(() -> previousShader);
			previousShader.apply();
		} else {
			RenderSystem.setShader(() -> null);
			GlStateManager._glUseProgram(0);
		}
	}

	private void ensureResources(int w, int h) {
		if (cachedWidth == w && cachedHeight == h && pingPong != null) return;

		if (!meshInitialized) {
			if (quadMesh == null) quadMesh = new QuadMesh();
			quadMesh.init();
			meshInitialized = true;
		}

		if (pingPong != null) pingPong.destroy();

		pingPong = new PingPongBuffer(true);
		pingPong.ensureSize(w, h);

		cachedWidth = w;
		cachedHeight = h;
	}

	private void blit(RenderTarget source, RenderTarget target) {
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
		GL30.glBlitFramebuffer(
				0, 0, source.width, source.height,
				0, 0, target.width, target.height,
				GL11.GL_COLOR_BUFFER_BIT,
				GL11.GL_NEAREST
		);
	}

	public void cleanup(boolean cleanShaders) {
		if (pingPong != null) {
			pingPong.destroy();
			pingPong = null;
		}
		if (quadMesh != null && meshInitialized) {
			quadMesh.destroy();
			meshInitialized = false;
		}
		if (cleanShaders) {
			shaders.forEach(PostProcessShader::cleanup);
			shaders.clear();
			pendingShaders.clear();
		}
		initialized = false;
		cachedWidth = -1;
		cachedHeight = -1;
	}

	private void checkInit() {
		if (initialized || pendingShaders.isEmpty()) return;

		shaders.forEach(PostProcessShader::cleanup);
		shaders.clear();

		for (ResourceLocation fragment : pendingShaders) {
			try {
				shaders.add(new PostProcessShader(fragment));
			} catch (Exception e) {
				System.err.println("[Glaze] Failed to compile shader: " + fragment);
				e.printStackTrace();
			}
		}
		initialized = true;
	}

	public void addShader(ResourceLocation fragmentShader) {
		if (!pendingShaders.contains(fragmentShader)) {
			pendingShaders.add(fragmentShader);
			initialized = false;
		}
	}

	public void addShader(PostProcessShader shader) {
		shaders.add(shader);
	}

	public void removeShader(PostProcessShader sh) {
		shaders.remove(sh);
	}

	public void removeShader(ResourceLocation fragmentShaderPath) {
		if (pendingShaders.remove(fragmentShaderPath)) initialized = false;
		shaders.removeIf(shader -> {
			ResourceLocation loc = shader.getFragmentLocation();
			return loc != null && loc.equals(fragmentShaderPath);
		});
	}

	public List<PostProcessShader> getShaders() {
		return new ArrayList<>(shaders);
	}

	public void reload() {
		initialized = false;
		cachedWidth = -1;
		cachedHeight = -1;
	}
}