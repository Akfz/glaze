package v.akfz.glazelib.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

public class HDRFramebuffer extends RenderTarget {

    private int[] colorAttachments;

    public HDRFramebuffer(int width, int height, boolean useDepth) {
        super(useDepth);
        RenderSystem.assertOnRenderThreadOrInit();
        this.resize(width, height, Minecraft.ON_OSX);
    }

    @Override
    public void createBuffers(int width, int height, boolean getError) {
        RenderSystem.assertOnRenderThreadOrInit();
        int maxSize = RenderSystem.maxSupportedTextureSize();

        if (width <= 0 || width > maxSize || height <= 0 || height > maxSize) {
            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + maxSize + ")");
        }

        this.viewWidth = width;
        this.viewHeight = height;
        this.width = width;
        this.height = height;

        this.releaseLocalAttachments();

        this.frameBufferId = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);

        if (this.useDepth) {
            this.depthBufferId = TextureUtil.generateTextureId();
            GlStateManager._bindTexture(this.depthBufferId);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, this.width, this.height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null);
            GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthBufferId, 0);
        }

        this.colorAttachments = new int[5];
        for (int i = 0; i < 5; i++) {
            int textureId = TextureUtil.generateTextureId();
            this.colorAttachments[i] = textureId;
            GlStateManager._bindTexture(textureId);

            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            int internalFormat;
            int pixelFormat;
            int pixelType;

            switch (i) {
                case 0:
                    internalFormat = GL30.GL_RGBA16F;
                    pixelFormat = GL11.GL_RGBA;
                    pixelType = GL11.GL_FLOAT;
                    break;
                case 1:
                    internalFormat = GL30.GL_RGBA16F;
                    pixelFormat = GL11.GL_RGBA;
                    pixelType = GL11.GL_FLOAT;
                    break;
                case 2:
                    internalFormat = GL11.GL_RGBA8;
                    pixelFormat = GL11.GL_RGBA;
                    pixelType = GL11.GL_UNSIGNED_BYTE;
                    break;
                case 3:
                    internalFormat = GL30.GL_RG16F;
                    pixelFormat = GL30.GL_RG;
                    pixelType = GL11.GL_FLOAT;
                    break;
                case 4:
                    internalFormat = GL30.GL_RGBA16F;
                    pixelFormat = GL11.GL_RGBA;
                    pixelType = GL11.GL_FLOAT;
                    break;
                default:
                    throw new IllegalStateException("Unexpected color attachment index: " + i);
            }

            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, this.width, this.height, 0, pixelFormat, pixelType, null);
            GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i, GL11.GL_TEXTURE_2D, textureId, 0);
        }

        this.colorTextureId = this.colorAttachments[0];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer drawBuffers = stack.mallocInt(5);
            for (int i = 0; i < 5; i++) {
                drawBuffers.put(GL30.GL_COLOR_ATTACHMENT0 + i);
            }
            drawBuffers.flip();
            GL20.glDrawBuffers(drawBuffers);
        }

        this.checkStatus();
        this.clear(getError);
        this.unbindRead();
    }

    @Override
    public void clear(boolean getError) {
        RenderSystem.assertOnRenderThreadOrInit();
        this.bindWrite(true);

        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        int clearMask = GL11.GL_COLOR_BUFFER_BIT;
        if (this.useDepth) {
            GL11.glClearDepth(1.0D);
            clearMask |= GL11.GL_DEPTH_BUFFER_BIT;
        }
        GL11.glClear(clearMask);

        if (getError) this.checkStatus();
    }

    private void releaseLocalAttachments() {
        if (this.colorAttachments != null) {
            for (int i = 1; i < this.colorAttachments.length; i++) {
                int id = this.colorAttachments[i];
                if (id > 0) {
                    TextureUtil.releaseTextureId(id);
                }
            }
            this.colorAttachments = null;
        }
    }

    @Override
    public void destroyBuffers() {
        RenderSystem.assertOnRenderThreadOrInit();
        this.releaseLocalAttachments();
        super.destroyBuffers();
    }

    public void bindAttachment(int index, int textureUnit) {
        if (this.colorAttachments != null && index >= 0 && index < this.colorAttachments.length) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + textureUnit);
            GlStateManager._bindTexture(this.colorAttachments[index]);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        }
    }

    public int[] getColorAttachments() {
        return this.colorAttachments;
    }
}