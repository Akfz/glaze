package v.akfz.glaze;

import v.akfz.aslib.resourcepack.ModAssetsRegistrar;
import v.akfz.db.generator.GenerateInitializer;

@GenerateInitializer(modId = "glz")
public class Glaze {
	public static boolean postProcess = false;
	//public static boolean addictiveLight = true; if i add this, it will be custom render world pipeline, maybe on vulkan, or i really add this like addictive, idk

	public void init() {
		ModAssetsRegistrar.registerModAssets("glz");
	}
}
