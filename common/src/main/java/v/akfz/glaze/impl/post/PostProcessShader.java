package v.akfz.glaze.impl.post;

import net.minecraft.resources.ResourceLocation;
import v.akfz.glazelib.shader.impl.ShaderProgram;

public class PostProcessShader extends ShaderProgram {
	private static final ResourceLocation DEFAULT_VERTEX =
			new ResourceLocation("glaze", "shader/vertex.glsl");

	public PostProcessShader(ResourceLocation fragmentLocation) {
		super(DEFAULT_VERTEX, fragmentLocation);
	}

	public PostProcessShader(String fragmentSource) {
		super(DEFAULT_VERTEX, fragmentSource);
	}
}