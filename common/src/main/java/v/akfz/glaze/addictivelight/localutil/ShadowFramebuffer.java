package v.akfz.glaze.addictivelight.localutil;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import v.akfz.glaze.addictivelight.data.manager.DataManager;

import java.nio.FloatBuffer;

public class ShadowFramebuffer extends RenderTarget {

    @Getter private final boolean hasColor;
    @Getter private int colorBufferId = 0;
    @Getter private int allocatedLayers = 0;

    public ShadowFramebuffer(int width, int height, boolean hasColor) {
        super(false);
        this.hasColor = hasColor;
        RenderSystem.assertOnRenderThreadOrInit();
        this.resize(width, height, Minecraft.ON_OSX);
    }

    @Override
    public void createBuffers(int width, int height, boolean getError) {
        RenderSystem.assertOnRenderThreadOrInit();
        this.viewWidth = width;
        this.viewHeight = height;
        this.width = width;
        this.height = height;

        this.frameBufferId = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);

        this.depthBufferId = GL11.glGenTextures();
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.depthBufferId);

        int maxLayers = DataManager.INSTANCE.getSettingsData().maxLights;
        maxLayers = Math.min(32, maxLayers);

        long estimatedBytes = (long) this.width * this.height * 4L * maxLayers;
        if (estimatedBytes > 1024L * 1024L * 1024L) {
            maxLayers = (int) (1024L * 1024L * 1024L / ((long) this.width * this.height * 4L));
        }

        if (maxLayers >= 6) {
            maxLayers = (maxLayers / 6) * 6;
        } else {
            maxLayers = 6;
        }
        this.allocatedLayers = maxLayers;

        GL30.glTexImage3D(
                GL30.GL_TEXTURE_2D_ARRAY,
                0,
                GL30.GL_DEPTH_COMPONENT32F,
                this.width,
                this.height,
                maxLayers,
                0,
                GL11.GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT,
                (java.nio.ByteBuffer) null
        );

        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer borderColor = stack.floats(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glTexParameterfv(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_BORDER_COLOR, borderColor);
        }

        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL30.GL_TEXTURE_COMPARE_MODE, GL30.GL_COMPARE_REF_TO_TEXTURE);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL30.GL_TEXTURE_COMPARE_FUNC, GL11.GL_LEQUAL);

        GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, this.depthBufferId, 0, 0);

        if (hasColor) {
            this.colorBufferId = GL11.glGenTextures();
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.colorBufferId);

            GL30.glTexImage3D(
                    GL30.GL_TEXTURE_2D_ARRAY,
                    0,
                    GL11.GL_RGBA8,
                    this.width,
                    this.height,
                    maxLayers,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    (java.nio.ByteBuffer) null
            );

            GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, this.colorBufferId, 0, 0);
        }

        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);

        this.checkStatus();
        this.clearAllLayers();

        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void bindLayer(int layer) {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
        GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, this.depthBufferId, 0, layer);

        if (hasColor) {
            GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, this.colorBufferId, 0, layer);
            GL20.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            GL20.glDrawBuffer(GL11.GL_NONE);
        }
        GL11.glViewport(0, 0, this.width, this.height);
    }

    public void clearLayer(int layer) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
        GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, this.depthBufferId, 0, layer);
        GlStateManager._depthMask(true);
        GL11.glClearDepth(1.0D);

        if (hasColor) {
            GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, this.colorBufferId, 0, layer);
            GL11.glColorMask(true, true, true, true);
            GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            GL20.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
            GL20.glDrawBuffer(GL11.GL_NONE);
        } else {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        }
    }

    public void unbind() {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void clearAllLayers() {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
        GlStateManager._depthMask(true);
        GL11.glClearDepth(1.0D);

        if (hasColor) {
            GL11.glColorMask(true, true, true, true);
            GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            GL20.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            GL11.glDrawBuffer(GL11.GL_NONE);
            GL11.glReadBuffer(GL11.GL_NONE);
        }

        for (int i = 0; i < allocatedLayers; i++) {
            GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, this.depthBufferId, 0, i);
            if (hasColor) {
                GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, this.colorBufferId, 0, i);
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
            } else {
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            }
        }

        if (hasColor) {
            GL20.glDrawBuffer(GL11.GL_NONE);
        }

        GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, this.depthBufferId, 0, 0);
        if (hasColor) {
            GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, this.colorBufferId, 0, 0);
        }
    }

    public void bindDepthArray(int textureUnit) {
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + textureUnit);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.depthBufferId);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
    }

    @Override
    public void clear(boolean getError) {
    }

    @Override
    public void destroyBuffers() {
        RenderSystem.assertOnRenderThreadOrInit();
        if (this.colorBufferId != 0) {
            GL11.glDeleteTextures(this.colorBufferId);
            this.colorBufferId = 0;
        }
        super.destroyBuffers();
    }
}