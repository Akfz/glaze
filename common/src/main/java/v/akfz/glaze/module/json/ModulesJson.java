package v.akfz.glaze.module.json;

import v.akfz.aslib.util.GlobalUtils;
import v.akfz.aslib.util.json.JsonFile;

import java.nio.file.Path;

public class ModulesJson implements JsonFile<ModulesData> {
    public ModulesData data = new ModulesData();
    @Override
    public ModulesData data() {
        return data;
    }

    @Override
    public Path getPath() {
        return GlobalUtils.getAsLibCFGPath().resolve("glaze").resolve("modules.json");
    }
}
