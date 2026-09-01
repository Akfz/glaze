package v.akfz.glazelib.shader.util;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import v.akfz.glazelib.util.gl.GLPossibilities;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class ShaderStorageBuffer {
	private int ssboId = 0;
	private int bindingPoint = 0;
	private int sizeInBytes = 0;

	public void init(int sizeInBytes, int bindingPoint) {
		if (!GLPossibilities.supportsSSBO()) {
			throw new UnsupportedOperationException(
					"SSBO is not supported on this GPU. Requires OpenGL 4.3+ or GL_ARB_shader_storage_buffer_object."
			);
		}

		if (this.ssboId != 0) {
			cleanup();
		}

		this.bindingPoint = bindingPoint;
		this.sizeInBytes = sizeInBytes;

		RenderSystem.assertOnRenderThreadOrInit();

		this.ssboId = GL30.glGenBuffers();
		GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.ssboId);
		GL30.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, sizeInBytes, GL15.GL_DYNAMIC_DRAW);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, bindingPoint, this.ssboId);
		GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
	}

	public void update(FloatBuffer directBuffer) {
		if (this.ssboId == 0) return;
		GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.ssboId);
		GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, directBuffer);
		GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
	}

	public void update(ByteBuffer directBuffer) {
		if (this.ssboId == 0) return;
		GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.ssboId);
		GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, directBuffer);
		GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
	}

	public void updateSubData(int offset, FloatBuffer data) {
		if (this.ssboId == 0) return;
		GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.ssboId);
		GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, offset, data);
		GL30.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
	}

	public int getSsboId() {
		return this.ssboId;
	}

	public int getBindingPoint() {
		return this.bindingPoint;
	}

	public int getSizeInBytes() {
		return this.sizeInBytes;
	}

	public void cleanup() {
		if (this.ssboId != 0) {
			GL30.glDeleteBuffers(this.ssboId);
			this.ssboId = 0;
		}
		this.sizeInBytes = 0;
	}
}