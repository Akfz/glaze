package v.akfz.glaze.shader.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

public class TextureBuffer {
    private int textureId = 0;
    private FloatBuffer buffer;
    private int width = 0;
    private int height = 0;

    public void init(int width, int height) {
        this.width = width;
        this.height = height;

        int requiredCapacity = width * height * 4;

        if (this.buffer == null || this.buffer.capacity() < requiredCapacity) {
            this.buffer = BufferUtils.createFloatBuffer(requiredCapacity);
        }

        if (this.textureId == 0) {
            this.textureId = GL11.glGenTextures();
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, 0L);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void resize(int newWidth, int newHeight) {
        if (this.width == newWidth && this.height == newHeight && this.textureId != 0 && this.buffer != null) {
            return;
        }
        init(newWidth, newHeight);
    }

    public void upload(FloatBuffer directBuffer) {
        if (this.textureId == 0) return;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, this.width, this.height, GL11.GL_RGBA, GL11.GL_FLOAT, directBuffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void upload(float[] data) {
        if (this.textureId == 0 || this.buffer == null) return;

        this.buffer.clear();
        int requiredSize = this.width * this.height * 4;
        int limit = Math.min(data.length, requiredSize);
        this.buffer.put(data, 0, limit);

        while (this.buffer.position() < requiredSize) {
            this.buffer.put(0.0f);
        }

        this.buffer.flip();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, this.width, this.height, GL11.GL_RGBA, GL11.GL_FLOAT, this.buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void bind(int textureUnit) {
        if (this.textureId != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);
        }
    }

    public void unbind(int textureUnit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public int getTextureId() {
        return this.textureId;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void cleanup() {
        if (this.textureId != 0) {
            GL11.glDeleteTextures(this.textureId);
            this.textureId = 0;
        }
        this.buffer = null;
        this.width = 0;
        this.height = 0;
    }
}