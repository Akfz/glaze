package v.akfz.glaze.impl.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import v.akfz.glaze.Glaze;
import v.akfz.glaze.shader.impl.ShaderProgram;
import v.akfz.glaze.shader.util.QuadMesh;

import java.util.ArrayList;
import java.util.List;

public class PostProcessRenderer {
	public static final PostProcessRenderer INSTANCE = new PostProcessRenderer();

	private PostProcessRenderer() {}

	public boolean renderGlobal = true; //false - работает на мир только, true - на все.

	private final List<ResourceLocation> pendingShaders = new ArrayList<>();
	private final List<PostProcessShader> shaders = new ArrayList<>();
	private final QuadMesh quadMesh = new QuadMesh();

	private RenderTarget pingBuffer;
	private RenderTarget pongBuffer;
	private boolean initialized = false;
	private boolean meshInitialized = false;

	public void render(RenderTarget mainBuffer) {
		if (mainBuffer == null) return;
		checkInit();

		if (Glaze.postProcess || shaders.isEmpty()) return;
		ShaderInstance previousShader = RenderSystem.getShader();

		int width = mainBuffer.width;
		int height = mainBuffer.height;
		ensureResources(width, height);

		boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
		if (scissorEnabled) {
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
		}

		GlStateManager._disableDepthTest();
		GlStateManager._disableBlend();
		GlStateManager._disableCull();

		blit(mainBuffer, pingBuffer);
		pingBuffer.copyDepthFrom(mainBuffer);

		int originalDepthTextureId = pingBuffer.getDepthTextureId();
		RenderTarget currentInput = pingBuffer;
		RenderTarget currentOutput = pongBuffer;

		for (ShaderProgram shader : shaders) {
			if (!shader.isValid()) return;

			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentOutput.frameBufferId);
			GL11.glViewport(0, 0, width, height);

			GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

			RenderTarget finalCurrentInput = currentInput;
			shader.use(() -> {
				RenderSystem.activeTexture(GL13.GL_TEXTURE0);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, finalCurrentInput.getColorTextureId());
				shader.uniformManager.set("uTexture", 0);

				RenderSystem.activeTexture(GL13.GL_TEXTURE1);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, originalDepthTextureId);
				shader.uniformManager.set("uDepth", 1);

				shader.uniformManager.set("uNear", 0.05f);
				float farPlane = (float) (Minecraft.getInstance().options.renderDistance().get() * 16);
				shader.uniformManager.set("uFar", farPlane);

				quadMesh.render();
			});

			currentInput = currentOutput;
			currentOutput = (currentInput == pingBuffer) ? pongBuffer : pingBuffer;
		}
		if (currentInput != mainBuffer) {
			blit(currentInput, mainBuffer);
		}

		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainBuffer.frameBufferId);
		GlStateManager._enableDepthTest();
		GlStateManager._enableBlend();
		GlStateManager._enableCull();

		if (scissorEnabled) {
			GL11.glEnable(GL11.GL_SCISSOR_TEST);
		}

		resetTextures();

		if (previousShader != null) {
			RenderSystem.setShader(() -> previousShader);
			previousShader.apply();
		} else {
			RenderSystem.setShader(() -> null);
			GlStateManager._glUseProgram(0);
		}
	}

	private void ensureResources(int w, int h) {
		if (!meshInitialized) {
			quadMesh.init();
			meshInitialized = true;
		}

		if (pingBuffer == null || pingBuffer.width != w || pingBuffer.height != h) {
			if (pingBuffer != null) pingBuffer.destroyBuffers();
			if (pongBuffer != null) pongBuffer.destroyBuffers();

			pingBuffer = new TextureTarget(w, h, true, Minecraft.ON_OSX);
			pongBuffer = new TextureTarget(w, h, true, Minecraft.ON_OSX);
		}
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

	private void resetTextures() {
		RenderSystem.activeTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

		RenderSystem.activeTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	public void cleanup(boolean cleanShaders) {
		if (pingBuffer != null) {
			pingBuffer.destroyBuffers();
			pingBuffer = null;
		}
		if (pongBuffer != null) {
			pongBuffer.destroyBuffers();
			pongBuffer = null;
		}
		if (meshInitialized) {
			quadMesh.destroy();
			meshInitialized = false;
		}
		if (cleanShaders) {
			shaders.forEach(ShaderProgram::cleanup);
			shaders.clear();
			pendingShaders.clear();
		}
		this.initialized = false;
	}

	private void checkInit() {
		if (!initialized && !pendingShaders.isEmpty()) {
			shaders.forEach(PostProcessShader::cleanup);
			shaders.clear();

			for (ResourceLocation fragment : pendingShaders) {
				try {
					shaders.add(new PostProcessShader(fragment));
				} catch (Exception e) {
					System.err.println("Не удалось скомпилировать шейдер: " + fragment);
					e.printStackTrace();
				}
			}
			this.initialized = true;
		}
	}

	public void addShader(ResourceLocation fragmentShader) {
		if (!pendingShaders.contains(fragmentShader)) {
			pendingShaders.add(fragmentShader);
			this.initialized = false;
		}
	}

	public void addShader(PostProcessShader shader) {
		shaders.add(shader);
	}

	public void removeShader(PostProcessShader sh) {
		shaders.remove(sh);
	}

	public void removeShader(ResourceLocation fragmentShaderPath) {
		if (pendingShaders.remove(fragmentShaderPath)) {
			this.initialized = false;
		}
		shaders.removeIf(shader -> shader.getFragmentLocation().equals(fragmentShaderPath));
	}

	public List<PostProcessShader> getShaders() {
		return new ArrayList<>(shaders);
	}

	public void reload() {
		this.initialized = false;
	}

}
