package v.akfz.glaze.addictivelight.data.material;

import v.akfz.aslib.util.json.JsonData;
import java.util.Map;

public class MaterialsConfig implements JsonData {
    public Map<String, BlockMaterial> blocks;
    public Map<String, EntityMaterial> entities;
}