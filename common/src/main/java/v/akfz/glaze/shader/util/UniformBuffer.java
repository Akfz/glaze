package v.akfz.glaze.shader.util;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;

public class UniformBuffer {
    private int uboId = 0;
    private int bindingPoint = 0;

    public void init(int sizeInBytes, int bindingPoint) {
        if (this.uboId != 0) {
            cleanup();
        }
        this.bindingPoint = bindingPoint;

        RenderSystem.assertOnRenderThreadOrInit();

        this.uboId = GL30.glGenBuffers();
        GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.uboId);
        GL30.glBufferData(GL31.GL_UNIFORM_BUFFER, sizeInBytes, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, bindingPoint, this.uboId);
        GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public void update(FloatBuffer directBuffer) {
        if (this.uboId == 0) return;
        GL30.glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.uboId);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, directBuffer);
        GL31.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public int getUboId() {
        return this.uboId;
    }

    public int getBindingPoint() {
        return this.bindingPoint;
    }

    public void cleanup() {
        if (this.uboId != 0) {
            GL30.glDeleteBuffers(this.uboId);
            this.uboId = 0;
        }
    }
}