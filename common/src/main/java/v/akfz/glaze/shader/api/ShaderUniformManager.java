package v.akfz.glaze.shader.api;

import org.joml.*;
import org.lwjgl.system.MemoryStack;
import v.akfz.glaze.shader.util.UniformBuffer;

import java.nio.FloatBuffer;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;

public final class ShaderUniformManager {
    private final IShaderProgram program;

    public ShaderUniformManager(IShaderProgram program) {
        this.program = program;
    }

    private int prepare(String name) {
        program.checkActive();
        return program.getUniformLocation(name);
    }

    public void bindUbo(String blockName, UniformBuffer ubo) {
        program.checkActive();
        int blockIndex = glGetUniformBlockIndex(program.getProgramID(), blockName);
        if (blockIndex != -1) {
            glUniformBlockBinding(program.getProgramID(), blockIndex, ubo.getBindingPoint());
        }
    }

    public void set(String name, boolean value) {
        set(name, value ? 1 : 0);
    }

    public void set(String name, int value) {
        int loc = prepare(name);
        if (loc != -1) glUniform1i(loc, value);
    }

    public void set(String name, float value) {
        int loc = prepare(name);
        if (loc != -1) glUniform1f(loc, value);
    }

    public void set(String name, Vector2f vec) {
        int loc = prepare(name);
        if (loc != -1) glUniform2f(loc, vec.x, vec.y);
    }

    public void set(String name, Vector3f vec) {
        int loc = prepare(name);
        if (loc != -1) glUniform3f(loc, vec.x, vec.y, vec.z);
    }

    public void set(String name, Vector4f vec) {
        int loc = prepare(name);
        if (loc != -1) glUniform4f(loc, vec.x, vec.y, vec.z, vec.w);
    }

    public void set(String name, Matrix3f matrix) {
        int loc = prepare(name);
        if (loc != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(9);
                matrix.get(buffer);
                glUniformMatrix3fv(loc, false, buffer);
            }
        }
    }

    public void set(String name, Matrix4f matrix) {
        int loc = prepare(name);
        if (loc != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(16);
                matrix.get(buffer);
                glUniformMatrix4fv(loc, false, buffer);
            }
        }
    }

    public void set(String name, float[] values) {
        int loc = prepare(name);
        if (loc == -1) return;
        switch (values.length) {
            case 1 -> glUniform1fv(loc, values);
            case 2 -> glUniform2fv(loc, values);
            case 3 -> glUniform3fv(loc, values);
            case 4 -> glUniform4fv(loc, values);
            default -> throw new IllegalArgumentException("Unsupported float array length: " + values.length);
        }
    }

    public void set(String name, int[] values) {
        int loc = prepare(name);
        if (loc == -1) return;
        switch (values.length) {
            case 1 -> glUniform1iv(loc, values);
            case 2 -> glUniform2iv(loc, values);
            case 3 -> glUniform3iv(loc, values);
            case 4 -> glUniform4iv(loc, values);
            default -> throw new IllegalArgumentException("Unsupported int array length: " + values.length);
        }
    }

    public void setTexture(String name, int textureUnit) {
        set(name, textureUnit);
    }

    public void setTexture(String name, int textureUnit, int textureId) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureId);
        set(name, textureUnit);
        glActiveTexture(GL_TEXTURE0);
    }

    public void bindUniformBlock(int bindingPoint, String blockName) {
        program.checkActive();
        int blockIndex = glGetUniformBlockIndex(program.getProgramID(), blockName);
        if (blockIndex != -1) {
            glUniformBlockBinding(program.getProgramID(), blockIndex, bindingPoint);
        }
    }

    public void setMultiple(Map<String, UniformValue> uniforms) {
        uniforms.forEach((name, value) -> value.apply(this, name));
    }

    @FunctionalInterface
    public interface UniformValue {
        void apply(ShaderUniformManager manager, String name);
    }

    public static UniformValue of(boolean v) { return (m, n) -> m.set(n, v); }
    public static UniformValue of(int v)     { return (m, n) -> m.set(n, v); }
    public static UniformValue of(float v)   { return (m, n) -> m.set(n, v); }
    public static UniformValue of(Vector2f v){ return (m, n) -> m.set(n, v); }
    public static UniformValue of(Vector3f v){ return (m, n) -> m.set(n, v); }
    public static UniformValue of(Vector4f v){ return (m, n) -> m.set(n, v); }
    public static UniformValue of(Matrix4f v){ return (m, n) -> m.set(n, v); }
    public static UniformValue tex(int unit) { return (m, n) -> m.setTexture(n, unit); }
}