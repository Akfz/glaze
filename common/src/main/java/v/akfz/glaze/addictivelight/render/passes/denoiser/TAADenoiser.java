package v.akfz.glaze.addictivelight.render.passes.denoiser;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import v.akfz.glaze.addictivelight.data.SettingsData;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.shader.impl.ShaderProgram;
import v.akfz.glaze.shader.util.HDRFramebuffer;
import v.akfz.glaze.shader.util.QuadMesh;

public class TAADenoiser {
    private final ShaderProgram taaShader = new ShaderProgram(
            new ResourceLocation("glze", "shader/vertex.glsl"),
            new ResourceLocation("glze", "shader/denoiser/taa.glsl")
    );
    private HDRFramebuffer historyBuffer;
    private HDRFramebuffer tempBuffer;

    public void render(Object... objects) {
        RenderTarget lightPassBuffer = (RenderTarget) objects[4];
        QuadMesh quadMesh = (QuadMesh) objects[9];

        Minecraft mc = Minecraft.getInstance();
        int w = lightPassBuffer.width;
        int h = lightPassBuffer.height;

        if (historyBuffer == null || historyBuffer.width != w || historyBuffer.height != h) {
            if (historyBuffer != null) historyBuffer.destroyBuffers();
            if (tempBuffer != null) tempBuffer.destroyBuffers();
            historyBuffer = new HDRFramebuffer(w, h, false);
            tempBuffer = new HDRFramebuffer(w, h, false);
            historyBuffer.clear(true);
        }

        RenderSystem.disableDepthTest();
        GL11.glDepthMask(false);

        tempBuffer.bindWrite(true);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        taaShader.use();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lightPassBuffer.getColorTextureId());
        taaShader.uniformManager.set("uCurrentTex", 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, historyBuffer.getColorTextureId());
        taaShader.uniformManager.set("uHistoryTex", 1);

        SettingsData s = DataManager.INSTANCE.getSettingsData();
        taaShader.uniformManager.set("uBlendFactor", s.taaBlendFactor);
        taaShader.uniformManager.set("uVarianceScale", s.taaVarianceScale);

        quadMesh.render();

        taaShader.stop();

        blit(tempBuffer, lightPassBuffer);
        blit(tempBuffer, historyBuffer);

        for (int i = 0; i <= 1; i++) {
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
        taaShader.cleanup();
        if (historyBuffer != null) {
            historyBuffer.destroyBuffers();
            historyBuffer = null;
        }
        if (tempBuffer != null) {
            tempBuffer.destroyBuffers();
            tempBuffer = null;
        }
    }
}