package v.akfz.glazelib.util.gl;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLCapabilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GLPossibilities {
	private static GLCapabilities caps;
	private static boolean checked = false;

	private static String versionString;
	private static int majorVersion = -1;
	private static int minorVersion = -1;
	private static String vendor;
	private static String renderer;

	private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

	private static void ensureChecked() {
		if (checked) return;
		checked = true;

		caps = GL.getCapabilities();
		if (caps == null) {
			return;
		}

		try {
			versionString = GL11.glGetString(GL11.GL_VERSION);
			vendor = GL11.glGetString(GL11.GL_VENDOR);
			renderer = GL11.glGetString(GL11.GL_RENDERER);

			if (versionString != null) {
				Matcher m = VERSION_PATTERN.matcher(versionString);
				if (m.find()) {
					majorVersion = Integer.parseInt(m.group(1));
					minorVersion = Integer.parseInt(m.group(2));
				}
			}
		} catch (Exception e) {
			majorVersion = -1;
			minorVersion = -1;
		}
	}

	public static String getActualVersion() {
		ensureChecked();
		return versionString;
	}

	public static int getMajorVersion() {
		ensureChecked();
		return majorVersion;
	}

	public static int getMinorVersion() {
		ensureChecked();
		return minorVersion;
	}

	public static String getVendor() {
		ensureChecked();
		return vendor;
	}

	public static String getRenderer() {
		ensureChecked();
		return renderer;
	}

	public static boolean supportsOpenGL(int major, int minor) {
		ensureChecked();
		if (majorVersion < 0) return false;
		if (majorVersion > major) return true;
		if (majorVersion == major) return minorVersion >= minor;
		return false;
	}

	public static int getMaxGLSLVersion() {
		ensureChecked();
		if (majorVersion < 0) return 330;

		if (majorVersion >= 4) {
			return majorVersion * 100 + minorVersion * 10;
		}
		if (majorVersion == 3 && minorVersion >= 3) {
			return 330;
		}
		if (majorVersion == 3 && minorVersion >= 2) {
			return 150;
		}
		return 330;
	}

	public static String getGLSLVersionString() {
		return getMaxGLSLVersion() + " core";
	}

	public static boolean isOpenGL30() { return supportsOpenGL(3, 0); }
	public static boolean isOpenGL31() { return supportsOpenGL(3, 1); }
	public static boolean isOpenGL32() { return supportsOpenGL(3, 2); }
	public static boolean isOpenGL33() { return supportsOpenGL(3, 3); }
	public static boolean isOpenGL40() { return supportsOpenGL(4, 0); }
	public static boolean isOpenGL41() { return supportsOpenGL(4, 1); }
	public static boolean isOpenGL42() { return supportsOpenGL(4, 2); }
	public static boolean isOpenGL43() { return supportsOpenGL(4, 3); }
	public static boolean isOpenGL44() { return supportsOpenGL(4, 4); }
	public static boolean isOpenGL45() { return supportsOpenGL(4, 5); }
	public static boolean isOpenGL46() { return supportsOpenGL(4, 6); }

	public static boolean supportsComputeShaders() {
		ensureChecked();
		return caps != null && (caps.OpenGL43 || caps.GL_ARB_compute_shader);
	}

	public static boolean supportsSSBO() {
		ensureChecked();
		return caps != null && (caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object);
	}

	public static boolean supportsImageLoadStore() {
		ensureChecked();
		return caps != null && (caps.OpenGL42 || caps.GL_ARB_shader_image_load_store);
	}

	public static boolean supportsDebugOutput() {
		ensureChecked();
		return caps != null && (caps.OpenGL43 || caps.GL_KHR_debug || caps.GL_ARB_debug_output);
	}

	public static boolean supportsFloatFramebuffer() {
		ensureChecked();
		return caps != null && (caps.OpenGL30 || caps.GL_ARB_color_buffer_float);
	}

	public static boolean supportsBufferStorage() {
		ensureChecked();
		return caps != null && (caps.OpenGL44 || caps.GL_ARB_buffer_storage);
	}

	public static boolean supportsMultiDrawIndirect() {
		ensureChecked();
		return caps != null && (caps.OpenGL43 || caps.GL_ARB_multi_draw_indirect);
	}

	public static boolean supportsTessellation() {
		ensureChecked();
		return caps != null && (caps.OpenGL40 || caps.GL_ARB_tessellation_shader);
	}

	public static boolean supportsSeparateBlend() {
		ensureChecked();
		return caps != null && (caps.OpenGL40 || caps.GL_ARB_draw_buffers_blend);
	}

	public static boolean supportsSeparateShaderObjects() {
		ensureChecked();
		return caps != null && (caps.OpenGL41 || caps.GL_ARB_separate_shader_objects);
	}

	public static boolean supportsShaderImageSize() {
		ensureChecked();
		return caps != null && (caps.OpenGL42 || caps.GL_ARB_shader_image_size);
	}

	public static boolean supportsDSA() {
		ensureChecked();
		return caps != null && (caps.OpenGL45 || caps.GL_ARB_direct_state_access);
	}

	public static boolean supportsSPIRV() {
		ensureChecked();
		return caps != null && (caps.OpenGL46 || caps.GL_ARB_gl_spirv);
	}

	public static boolean supportsGeometryShaders() {
		ensureChecked();
		return caps != null && (caps.OpenGL32 || caps.GL_ARB_geometry_shader4);
	}

	public static boolean supportsMultisampleTextures() {
		ensureChecked();
		return caps != null && (caps.OpenGL32 || caps.GL_ARB_texture_multisample);
	}

	public static boolean supportsFramebufferObjects() {
		ensureChecked();
		return caps != null && (caps.OpenGL30 || caps.GL_ARB_framebuffer_object);
	}

	public static boolean supportsUBO() {
		ensureChecked();
		return caps != null && (caps.OpenGL31 || caps.GL_ARB_uniform_buffer_object);
	}

	public static boolean supportsFloatTextures() {
		ensureChecked();
		return caps != null && (caps.OpenGL30 || caps.GL_ARB_texture_float);
	}

	public static boolean supportsES3Compatibility() {
		ensureChecked();
		return caps != null && caps.OpenGL43;
	}

	public static int getMaxTextureUnits() {
		ensureChecked();
		if (caps == null) return 0;
		return GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
	}

	public static int getMaxTextureSize() {
		ensureChecked();
		if (caps == null) return 0;
		return GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
	}

	public static int getMaxColorAttachments() {
		ensureChecked();
		if (caps == null) return 0;
		return org.lwjgl.opengl.GL30.glGetInteger(org.lwjgl.opengl.GL30.GL_MAX_COLOR_ATTACHMENTS);
	}

	public static int getMaxUniformBlockSize() {
		ensureChecked();
		if (caps == null || !supportsUBO()) return 0;
		return org.lwjgl.opengl.GL31.glGetInteger(org.lwjgl.opengl.GL31.GL_MAX_UNIFORM_BLOCK_SIZE);
	}

	public static int getMaxSSBOSize() {
		ensureChecked();
		if (caps == null || !supportsSSBO()) return 0;
		return org.lwjgl.opengl.GL43.glGetInteger(org.lwjgl.opengl.GL43.GL_MAX_SHADER_STORAGE_BLOCK_SIZE);
	}

	public static void dumpInfo() {
		ensureChecked();
		System.out.println("=== OpenGL Capabilities ===");
		System.out.println("Version : " + versionString);
		System.out.println("Vendor  : " + vendor);
		System.out.println("Renderer: " + renderer);
		System.out.println("OpenGL 3.0+: " + isOpenGL30());
		System.out.println("OpenGL 3.3+: " + isOpenGL33());
		System.out.println("OpenGL 4.0+: " + isOpenGL40());
		System.out.println("OpenGL 4.3+: " + isOpenGL43());
		System.out.println("OpenGL 4.4+: " + isOpenGL44());
		System.out.println("OpenGL 4.5+: " + isOpenGL45());
		System.out.println("OpenGL 4.6+: " + isOpenGL46());
		System.out.println("Compute Shaders  : " + supportsComputeShaders());
		System.out.println("SSBO             : " + supportsSSBO());
		System.out.println("Image Load/Store : " + supportsImageLoadStore());
		System.out.println("Debug Output     : " + supportsDebugOutput());
		System.out.println("Float Framebuffer: " + supportsFloatFramebuffer());
		System.out.println("Buffer Storage   : " + supportsBufferStorage());
		System.out.println("DSA              : " + supportsDSA());
		System.out.println("Max Texture Size : " + getMaxTextureSize());
		System.out.println("Max Texture Units: " + getMaxTextureUnits());
		System.out.println("Max Color Attach.: " + getMaxColorAttachments());
		System.out.println("=============================");
	}
}