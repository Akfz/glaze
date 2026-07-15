package v.akfz.glaze.module.json;

import v.akfz.aslib.util.json.JsonData;

import java.util.HashMap;
import java.util.Map;

public class ModulesData implements JsonData {
    public Map<String, Boolean> moduleBooleanMap = new HashMap<>(); // id, включен ли
}
