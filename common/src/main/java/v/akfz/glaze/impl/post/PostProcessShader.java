package v.akfz.glaze.impl.post;

import net.minecraft.resources.ResourceLocation;
import v.akfz.glaze.shader.impl.ShaderProgram;

public class PostProcessShader extends ShaderProgram {
	public PostProcessShader(ResourceLocation fragmentLocation) {
		super(new ResourceLocation("glaze", "shader/vertex.glsl"),fragmentLocation);
	}
}
