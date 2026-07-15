package v.akfz.glaze.module;

import v.akfz.aslib.util.json.GsonHelper;
import v.akfz.glaze.module.json.ModulesData;
import v.akfz.glaze.module.json.ModulesJson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class RenderModuleManager {
    public static final RenderModuleManager INSTANCE = new RenderModuleManager();

    private final List<RenderModule> registeredModules = new ArrayList<>();
    private boolean systemLocked = false;

    private RenderModuleManager(){}

    private ModulesData lastData;

    public ModulesData getData() {
        return lastData;
    }

    public void updateData() {
        ModulesJson modulesJson = new ModulesJson();
        ModulesData json = GsonHelper.read(modulesJson.getPath(), ModulesData.class);
        boolean needsSave = false;

        if (json == null) {
            this.lastData = generateData().data;
            return;
        }

        if (json.moduleBooleanMap == null) {
            json.moduleBooleanMap = new HashMap<>();
            needsSave = true;
        }

        this.lastData = json;

        for (RenderModule module : registeredModules) {
            String id = module.getModuleId();

            if (!json.moduleBooleanMap.containsKey(id)) {
                json.moduleBooleanMap.put(id, module.isEnabled());
                needsSave = true;
            } else {
                boolean shouldBeEnabled = json.moduleBooleanMap.get(id);
                boolean isCurrentlyEnabled = module.isEnabled();

                if (shouldBeEnabled && !isCurrentlyEnabled) {
                    module.onEnable();
                } else if (!shouldBeEnabled && isCurrentlyEnabled) {
                    module.onDisable();
                }
            }
        }

        if (needsSave) {
            modulesJson.data = json;
            GsonHelper.write(modulesJson);
        }
    }

    public ModulesJson generateData() {
        ModulesJson json = new ModulesJson();
        registeredModules.forEach(module -> json.data.moduleBooleanMap.put(module.getModuleId(), module.isEnabled()));
        GsonHelper.write(json);
        return json;
    }

    @SuppressWarnings("unchecked")
    public <T extends RenderModule> T getByID(String id) {
        return (T) registeredModules.stream()
                .filter(m -> m.getModuleId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean haveModule(String id) {
        return registeredModules.stream().anyMatch(m -> m.getModuleId().equals(id));
    }

    public boolean haveModule(RenderModule module) {
        return haveModule(module.getModuleId());
    }

    public void registerModule(RenderModule module) {
        if (systemLocked) {
            throw new IllegalStateException("Glaze Module Manager has been closed");
        }
        if (haveModule(module)) {
            throw new IllegalStateException("Module with ID '" + module.getModuleId() + "' already exists");
        }

        registeredModules.add(module);
    }

    public void unregisterModule(RenderModule module) {
        unregisterModule(module.getModuleId());
    }

    public void unregisterModule(String id) {
        if (systemLocked) {
            throw new IllegalStateException("Glaze Module Manager has been closed");
        }
        if (!haveModule(id)) {
            throw new IllegalStateException("Module with ID '" + id + "' does not exist");
        }

        registeredModules.removeIf(module -> module.getModuleId().equals(id));
    }

    public void lock() {
        this.systemLocked = true;
    }

    public List<RenderModule> getRegisteredModules() {
        return new ArrayList<>(registeredModules);
    }
}