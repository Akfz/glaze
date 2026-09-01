package v.akfz.glazelib.util.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLUtil;

public class GLDebug {
	public static void enableDebug() {
		if (!GLPossibilities.supportsDebugOutput()) return;

		int flags = GL11.glGetInteger(GL43.GL_CONTEXT_FLAGS);
		if ((flags & GL43.GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
			System.out.println("GL Debug not available (context not created with debug flag).");
			return;
		}

		GLUtil.setupDebugMessageCallback();
		GL43.glEnable(GL43.GL_DEBUG_OUTPUT);
		GL43.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
	}
}
