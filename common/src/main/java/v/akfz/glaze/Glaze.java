package v.akfz.glaze;

import net.minecraft.resources.ResourceLocation;
import v.akfz.db.generator.GenerateInitializer;
import v.akfz.glaze.impl.post.PostProcessRenderer;

@GenerateInitializer(modId = "glz")
public class Glaze {
	public static boolean postProcess = true;
	public static boolean addictiveLight = false;

	public void init() {
		PostProcessRenderer.INSTANCE.addShader(new ResourceLocation("glaze", "shader/test/postprocess/pptest.glsl"));
	}
}
