package v.akfz.glazelib.util;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.BufferUtils.createFloatBuffer;
import static org.lwjgl.BufferUtils.createIntBuffer;

public class QuadMesh {

    private static final float[] VERTEX_DATA = {
            -1f, -1f,  0f,  0f,
            1f, -1f,  1f,  0f,
            1f,  1f,  1f,  1f,
            -1f,  1f,  0f,  1f
    };

    private static final int[] INDEX_DATA = {
            0, 1, 2,
            2, 3, 0
    };

    private int vaoId, vboId, eboId;

    public void init() {
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = createFloatBuffer(VERTEX_DATA.length);
        vertexBuffer.put(VERTEX_DATA).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        IntBuffer indexBuffer = createIntBuffer(INDEX_DATA.length);
        indexBuffer.put(INDEX_DATA).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

        int stride = 4 * Float.BYTES;

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2 * Float.BYTES);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        GL30.glBindVertexArray(0);
    }

    public void render() {
        if (vaoId == 0) return;
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES, INDEX_DATA.length, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    public void destroy() {
        if (vaoId != 0) GL30.glDeleteVertexArrays(vaoId);
        if (vboId != 0) GL15.glDeleteBuffers(vboId);
        if (eboId != 0) GL15.glDeleteBuffers(eboId);
        vaoId = 0;
    }
}