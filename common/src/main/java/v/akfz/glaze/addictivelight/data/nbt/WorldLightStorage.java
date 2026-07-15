package v.akfz.glaze.addictivelight.data.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import v.akfz.glaze.addictivelight.data.manager.DataManager;

public class WorldLightStorage extends SavedData {
    private static final String FILE_NAME = "glaze_world_lights";

    public WorldLightStorage() {}

    public static WorldLightStorage load(CompoundTag tag) {
        WorldLightStorage storage = new WorldLightStorage();
        DataManager.INSTANCE.getLightManager().load(tag);
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        DataManager.INSTANCE.getLightManager().save(tag);
        DataManager.INSTANCE.getLightManager().setClean();
        return tag;
    }

    public static WorldLightStorage get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                WorldLightStorage::load,
                WorldLightStorage::new,
                FILE_NAME
        );
    }

    public static WorldLightStorage get(MinecraftServer server) {
        return get(server.overworld());
    }
}