package v.akfz.glaze.addictivelight.render.passes.denoiser;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import v.akfz.glaze.addictivelight.data.SettingsData;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.shader.impl.ShaderProgram;
import v.akfz.glaze.shader.util.HDRFramebuffer;
import v.akfz.glaze.shader.util.QuadMesh;

public class SVGFDenoiser {
    private final ShaderProgram temporalShader = new ShaderProgram(
            new ResourceLocation("glze", "shader/vertex.glsl"),
            new ResourceLocation("glze", "shader/denoiser/taa.glsl")
    );
    private final ShaderProgram waveletShader = new ShaderProgram(
            new ResourceLocation("glze", "shader/vertex.glsl"),
            new ResourceLocation("glze", "shader/denoiser/svgf_atrous.glsl")
    );

    private HDRFramebuffer historyBuffer;
    private HDRFramebuffer tempBuffer1;
    private HDRFramebuffer tempBuffer2;

    public void render(Object... objects) {
        RenderTarget lightPassBuffer = (RenderTarget) objects[4];
        RenderTarget materialNormal = (RenderTarget) objects[7];
        QuadMesh quadMesh = (QuadMesh) objects[9];
        Matrix4f capturedView = (Matrix4f) objects[10];
        Matrix4f capturedProj = (Matrix4f) objects[11];

        Minecraft mc = Minecraft.getInstance();
        int w = lightPassBuffer.width;
        int h = lightPassBuffer.height;

        if (historyBuffer == null || historyBuffer.width != w || historyBuffer.height != h) {
            if (historyBuffer != null) historyBuffer.destroyBuffers();
            if (tempBuffer1 != null) tempBuffer1.destroyBuffers();
            if (tempBuffer2 != null) tempBuffer2.destroyBuffers();
            historyBuffer = new HDRFramebuffer(w, h, false);
            tempBuffer1 = new HDRFramebuffer(w, h, false);
            tempBuffer2 = new HDRFramebuffer(w, h, false);
            historyBuffer.clear(true);
        }

        RenderSystem.disableDepthTest();
        GL11.glDepthMask(false);

        tempBuffer1.bindWrite(true);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        SettingsData s = DataManager.INSTANCE.getSettingsData();

        temporalShader.use();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lightPassBuffer.getColorTextureId());
        temporalShader.uniformManager.set("uCurrentTex", 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, historyBuffer.getColorTextureId());
        temporalShader.uniformManager.set("uHistoryTex", 1);
        temporalShader.uniformManager.set("uBlendFactor", s.taaBlendFactor);
        quadMesh.render();
        temporalShader.stop();

        blit(tempBuffer1, historyBuffer);

        RenderTarget input = tempBuffer1;
        RenderTarget output = tempBuffer2;

        for (int step = 0; step < 2; step++) {
            int stepSize = 1 << step;
            output.bindWrite(true);
            GL11.glViewport(0, 0, w, h);
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            waveletShader.use();

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, input.getColorTextureId());
            waveletShader.uniformManager.set("uLightTex", 0);

            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getMainRenderTarget().getDepthTextureId());
            waveletShader.uniformManager.set("uDepth", 1);

            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, materialNormal.getColorTextureId());
            waveletShader.uniformManager.set("uNormal", 2);

            waveletShader.uniformManager.set("uStepSize", stepSize);
            waveletShader.uniformManager.set("uDepthThreshold", s.svgfDepthThreshold);
            waveletShader.uniformManager.set("uNormalThreshold", s.svgfNormalThreshold);
            waveletShader.uniformManager.set("uLumaThreshold", s.svgfLumaThreshold);

            waveletShader.uniformManager.set("uNear", 0.05f);
            float farPlane = (float) (mc.options.renderDistance().get() * 16);
            waveletShader.uniformManager.set("uFar", farPlane);

            Matrix4f invView = new Matrix4f(capturedView).invert();
            Matrix4f invProj = new Matrix4f(capturedProj).invert();
            waveletShader.uniformManager.set("uInvProj", invProj);
            waveletShader.uniformManager.set("uInvView", invView);

            quadMesh.render();
            waveletShader.stop();

            RenderTarget temp = input;
            input = output;
            output = temp;
        }

        blit(input, lightPassBuffer);

        for (int i = 0; i <= 2; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
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

    public void cleanup() {
        temporalShader.cleanup();
        waveletShader.cleanup();
        if (historyBuffer != null) {
            historyBuffer.destroyBuffers();
            historyBuffer = null;
        }
        if (tempBuffer1 != null) {
            tempBuffer1.destroyBuffers();
            tempBuffer1 = null;
        }
        if (tempBuffer2 != null) {
            tempBuffer2.destroyBuffers();
            tempBuffer2 = null;
        }
    }
}