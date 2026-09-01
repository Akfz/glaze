package v.akfz.glazelib.shader.api;

import v.akfz.glazelib.util.gl.GLPossibilities;

public final class ShaderPreprocessor {
	private ShaderPreprocessor() {}

	public static String process(String source, String shaderType) {
		String trimmed = source.trim();
		if (!trimmed.startsWith("#version")) {
			return "#version " + GLPossibilities.getGLSLVersionString() + "\n" + source;
		}
		return validateVersion(source, shaderType);
	}

	public static String process(String source) {
		return process(source, "unknown");
	}

	private static String validateVersion(String source, String shaderType) {
		int maxVersion = GLPossibilities.getMaxGLSLVersion();

		int newlineIndex = source.indexOf('\n');
		if (newlineIndex == -1) newlineIndex = source.length();

		String firstLine = source.substring(0, newlineIndex).trim();
		String[] parts = firstLine.split("\\s+");

		if (parts.length < 2) {
			return source;
		}

		int requestedVersion;
		try {
			requestedVersion = Integer.parseInt(parts[1]);
		} catch (NumberFormatException e) {
			return source;
		}

		if (requestedVersion > maxVersion) {
			throw new RuntimeException(String.format(
					"Shader compilation impossible: requested GLSL %d, but GPU supports max %d. (Type: %s)",
					requestedVersion, maxVersion, shaderType
			));
		}

		if ("compute".equals(shaderType) && requestedVersion < 430) {
			throw new RuntimeException("Compute shader requires GLSL 430+, but requested " + requestedVersion + ".");
		}

		if ("geometry".equals(shaderType) && requestedVersion < 150) {
			throw new RuntimeException("Geometry shader requires GLSL 150+, but requested " + requestedVersion + ".");
		}

		return source;
	}
}