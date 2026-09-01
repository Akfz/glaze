package v.akfz.glaze;

import v.akfz.aslib.datagen.fabric.mod.FabricModJsonData;
import v.akfz.aslib.datagen.fabric.mod.GenerateFabricModJson;
import v.akfz.aslib.datagen.forge.modstoml.GenerateModsToml;
import v.akfz.aslib.datagen.forge.modstoml.ModsTomlData;
import v.akfz.aslib.datagen.forge.packmcmeta.GeneratePackMcmeta;
import v.akfz.aslib.datagen.forge.packmcmeta.PackMcmetaData;
import v.akfz.db.annotation.DontCompile;

@DontCompile
public class DataGen {
	public static void main(String[] args) {
		new GenerateFabricModJson(new FabricModJsonData()
				.entrypoint("v.akfz.glaze.Glaze_fabric")
				.depend("aslib", ">=1")
				.description(""))
				.run("common");
		new GenerateModsToml(new ModsTomlData()
				.dependency("aslib", true, ">=1"))
				.run("common");
		new GeneratePackMcmeta(new PackMcmetaData()).run("common");
	}
}
