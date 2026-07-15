package v.akfz.glaze.addictivelight.data.manager;

import lombok.Getter;
import v.akfz.aslib.util.GlobalUtils;
import v.akfz.aslib.util.json.GsonHelper;
import v.akfz.aslib.util.json.JsonData;
import v.akfz.aslib.util.json.JsonFile;
import v.akfz.glaze.addictivelight.data.SettingsData;
import v.akfz.glaze.addictivelight.data.block.VoxelGrid;

import java.nio.file.Path;

public class DataManager {
    private DataManager(){}

    public static final DataManager INSTANCE = new DataManager();

    private static final DataManager CLIENT_HOLDER = new DataManager();
    private static final DataManager SERVER_HOLDER = new DataManager();

    private static DataManager getActive() {
        if (GlobalUtils.isClientSide()) {
            String threadName = Thread.currentThread().getName();
            if (threadName.contains("Server")) {
                return SERVER_HOLDER;
            }
            return CLIENT_HOLDER;
        }
        return SERVER_HOLDER;
    }

    private SettingsData settingsData;
    private final LightManager lightManager = new LightManager();
    private VoxelGrid voxelGrid;

    public VoxelGrid getVoxelGrid() {
        DataManager active = getActive();
        if (active.voxelGrid != null) {
            if (active.voxelGrid.getGridXZ() != active.getSettingsData().materialXZRadius ||
                    active.voxelGrid.getGridY() != active.getSettingsData().materialYRadius) {
                active.voxelGrid.cleanup();
                active.voxelGrid = null;
            } else {
                return active.voxelGrid;
            }
        }
        return active.voxelGrid = new VoxelGrid(active.getSettingsData().materialXZRadius, active.getSettingsData().materialYRadius);
    }

    public LightManager getLightManager() {
        return getActive().lightManager;
    }

    public void updateSetData() {
        getActive().updateSetDataInternal();
    }

    private void updateSetDataInternal() {
        Path setPath = GlobalUtils.getAsLibCFGPath().resolve("glaze/light.json");
        this.settingsData = GsonHelper.read(setPath, SettingsData.class);
        if (this.settingsData == null) {
            this.settingsData = new SettingsData();
            GsonHelper.write(new JsonFile<JsonData>() {
                @Override
                public JsonData data() {
                    return settingsData;
                }

                @Override
                public Path getPath() {
                    return setPath;
                }
            });
        }
    }

    public SettingsData getSettingsData() {
        DataManager active = getActive();
        if (active.settingsData == null) {
            active.updateSetDataInternal();
        }
        return active.settingsData;
    }

    public void cleanup() {
        CLIENT_HOLDER.cleanupInternal();
        SERVER_HOLDER.cleanupInternal();
    }

    private void cleanupInternal() {
        this.lightManager.clean();
        if (this.voxelGrid != null) {
            this.voxelGrid.cleanup();
        }
        this.voxelGrid = null;
        this.settingsData = null;
    }
}