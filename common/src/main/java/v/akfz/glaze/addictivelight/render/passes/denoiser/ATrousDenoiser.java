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

public class ATrousDenoiser {
    private final ShaderProgram atrousShader = new ShaderProgram(
            new ResourceLocation("glze", "shader/vertex.glsl"),
            new ResourceLocation("glze", "shader/denoiser/atrous.glsl")
    );
    private HDRFramebuffer tempBuffer;

    public void render(Object... objects) {
        RenderTarget lightPassBuffer = (RenderTarget) objects[4];
        RenderTarget materialNormal = (RenderTarget) objects[7];
        QuadMesh quadMesh = (QuadMesh) objects[9];
        Matrix4f capturedView = (Matrix4f) objects[10];
        Matrix4f capturedProj = (Matrix4f) objects[11];

        Minecraft mc = Minecraft.getInstance();
        int w = lightPassBuffer.width;
        int h = lightPassBuffer.height;

        if (tempBuffer == null || tempBuffer.width != w || tempBuffer.height != h) {
            if (tempBuffer != null) tempBuffer.destroyBuffers();
            tempBuffer = new HDRFramebuffer(w, h, false);
        }

        RenderSystem.disableDepthTest();
        GL11.glDepthMask(false);

        RenderTarget input = lightPassBuffer;
        RenderTarget output = tempBuffer;
        SettingsData s = DataManager.INSTANCE.getSettingsData();

        for (int step = 0; step < 3; step++) {
            int stepSize = 1 << step;
            output.bindWrite(true);
            GL11.glViewport(0, 0, w, h);
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            atrousShader.use();

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, input.getColorTextureId());
            atrousShader.uniformManager.set("uLightTex", 0);

            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getMainRenderTarget().getDepthTextureId());
            atrousShader.uniformManager.set("uDepth", 1);

            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, materialNormal.getColorTextureId());
            atrousShader.uniformManager.set("uNormal", 2);

            atrousShader.uniformManager.set("uStepSize", stepSize);
            atrousShader.uniformManager.set("uDepthThreshold", s.atrousDepthThreshold);
            atrousShader.uniformManager.set("uNormalThreshold", s.atrousNormalThreshold);
            atrousShader.uniformManager.set("uLumaThreshold", s.atrousLumaThreshold);

            atrousShader.uniformManager.set("uNear", 0.05f);
            float farPlane = (float) (mc.options.renderDistance().get() * 16);
            atrousShader.uniformManager.set("uFar", farPlane);

            Matrix4f invView = new Matrix4f(capturedView).invert();
            Matrix4f invProj = new Matrix4f(capturedProj).invert();
            atrousShader.uniformManager.set("uInvProj", invProj);
            atrousShader.uniformManager.set("uInvView", invView);

            quadMesh.render();

            atrousShader.stop();

            RenderTarget temp = input;
            input = output;
            output = temp;
        }

        if (input != lightPassBuffer) {
            blit(input, lightPassBuffer);
        }

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
        atrousShader.cleanup();
        if (tempBuffer != null) {
            tempBuffer.destroyBuffers();
            tempBuffer = null;
        }
    }
}