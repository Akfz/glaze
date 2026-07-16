package v.akfz.glaze.addictivelight.data.manager;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import v.akfz.aslib.util.GlobalUtils;
import v.akfz.glaze.addictivelight.data.SettingsData;
import v.akfz.glaze.addictivelight.data.light.LightSource;
import v.akfz.glaze.addictivelight.data.light.LightType;
import v.akfz.glaze.addictivelight.data.light.SimpleLightSource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class LightManager {
    public static class LightGroup {
        private final List<LightSource<?>> allSources;
        private final Map<String, List<LightSource<?>>> groups;

        public boolean isDirty = false;

        public LightGroup() {
            allSources = Collections.synchronizedList(new ArrayList<>());
            groups = new ConcurrentHashMap<>();
        }

        public synchronized void add(String groupName, LightSource<?> source) {
            if (source == null) return;
            if (!allSources.contains(source)) {
                allSources.add(source);
            }
            groups.computeIfAbsent(groupName, k -> new ArrayList<>()).add(source);
            isDirty = true;
        }

        public synchronized void remove(LightSource<?> source) {
            if (allSources.remove(source)) {
                groups.values().forEach(list -> list.remove(source));
                groups.entrySet().removeIf(e -> e.getValue().isEmpty());
            }
            isDirty = true;
        }

        public synchronized List<LightSource<?>> getGroup(String name) {
            return new ArrayList<>(groups.getOrDefault(name, Collections.emptyList()));
        }

        public synchronized Set<String> getGroupNames() {
            return new HashSet<>(groups.keySet());
        }

        public synchronized void clear() {
            allSources.clear();
            groups.clear();
            isDirty = true;
        }

        public synchronized List<LightSource<?>> getAllSources() {
            return new ArrayList<>(allSources);
        }

        public synchronized Map<String, List<LightSource<?>>> getGroups() {
            return new HashMap<>(groups);
        }
    }

    @Getter private final LightGroup storage = new LightGroup();
    private boolean dirty = false;

    public synchronized boolean isDirty() {
        return dirty || storage.isDirty;
    }

    public synchronized String getGroupName(LightSource<?> source) {
        if (source == null) return null;
        for (Map.Entry<String, List<LightSource<?>>> entry : this.storage.getGroups().entrySet()) {
            if (entry.getValue().contains(source)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public synchronized void modifySource(Predicate<LightSource<?>> selector, Consumer<LightSource<?>> modifier) {
        List<LightSource<?>> group = getAllSources();
        for (LightSource<?> source : group) {
            if (selector.test(source)) {
                modifier.accept(source);
                this.dirty = true;
            }
        }
    }

    public synchronized void modifySource(String nameOfGroup, Predicate<LightSource<?>> selector, Consumer<LightSource<?>> modifier) {
        List<LightSource<?>> group = storage.getGroup(nameOfGroup);
        for (LightSource<?> source : group) {
            if (selector.test(source)) {
                modifier.accept(source);
                this.dirty = true;
            }
        }
    }

    public void savePrevPoses() {
        getAllSources().forEach(lightSource -> {
            lightSource.setPrevX(lightSource.getX());
            lightSource.setPrevY(lightSource.getY());
            lightSource.setPrevZ(lightSource.getZ());
        });
    }

    public void checkAndSetBlockLights() {
        if (GlobalUtils.isClientSide()) {
            ClientLightsHandler.run(this);
        }
    }

    private static class ClientLightsHandler {
        private static long lastUpdateTime = 0;
        private static BlockPos lastPlayerPos = null;

        private static void run(LightManager manager) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null && mc.player != null) {
                    long now = System.currentTimeMillis();
                    BlockPos currentPos = mc.player.blockPosition();

                    if (now - lastUpdateTime > 250 || lastPlayerPos == null || !currentPos.equals(lastPlayerPos)) {
                        manager.updateBlockLights(mc.level, mc.player);
                        lastUpdateTime = now;
                        lastPlayerPos = currentPos;
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private synchronized void updateBlockLights(Level level, Player player) {
        SettingsData settings = DataManager.INSTANCE.getSettingsData();
        Map<String, SettingsData.BlockLightSettings> customBlocks = settings.customLightBlocks;

        if (customBlocks == null || customBlocks.isEmpty()) {
            cleanBlockLights(null, level, Collections.emptyMap());
            return;
        }

        int radiusXZ = settings.materialXZRadius;
        int radiusY = settings.materialYRadius;

        BlockPos playerPos = player.blockPosition();
        Set<BlockPos> foundPositions = new HashSet<>();

        int startX = playerPos.getX() - radiusXZ;
        int endX = playerPos.getX() + radiusXZ;
        int startY = Math.max(level.getMinBuildHeight(), playerPos.getY() - radiusY);
        int endY = Math.min(level.getMaxBuildHeight(), playerPos.getY() + radiusY);
        int startZ = playerPos.getZ() - radiusXZ;
        int endZ = playerPos.getZ() + radiusXZ;

        Set<BlockPos> alreadySpawned = new HashSet<>();
        for (LightSource<?> light : storage.getGroup("CustomBlockLights")) {
            alreadySpawned.add(BlockPos.containing(light.getX(), light.getY(), light.getZ()));
        }

        int minChunkX = startX >> 4;
        int maxChunkX = endX >> 4;
        int minChunkZ = startZ >> 4;
        int maxChunkZ = endZ >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) {
                    continue;
                }

                int blockXStart = Math.max(startX, chunkX << 4);
                int blockXEnd = Math.min(endX, (chunkX << 4) + 15);
                int blockZStart = Math.max(startZ, chunkZ << 4);
                int blockZEnd = Math.min(endZ, (chunkZ << 4) + 15);

                for (int x = blockXStart; x <= blockXEnd; x++) {
                    for (int z = blockZStart; z <= blockZEnd; z++) {
                        for (int y = startY; y <= endY; y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            BlockState state = chunk.getBlockState(pos);
                            if (state.isAir()) continue;

                            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                            if (customBlocks.containsKey(blockId)) {
                                foundPositions.add(pos.immutable());

                                if (!alreadySpawned.contains(pos)) {
                                    SettingsData.BlockLightSettings template = customBlocks.get(blockId);
                                    if (template != null) {
                                        SimpleLightSource newLight = new SimpleLightSource();
                                        copyProperties(template, newLight);
                                        newLight.position(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                        newLight.save(false);
                                        storage.add("CustomBlockLights", newLight);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        cleanBlockLights(foundPositions, level, customBlocks);
    }

    private synchronized void cleanBlockLights(Set<BlockPos> validPositions, Level level, Map<String, SettingsData.BlockLightSettings> customBlocks) {
        List<LightSource<?>> blockLights = storage.getGroup("CustomBlockLights");
        for (LightSource<?> light : blockLights) {
            BlockPos pos = BlockPos.containing(light.getX(), light.getY(), light.getZ());
            boolean keep = false;
            if (validPositions != null && validPositions.contains(pos)) {
                BlockState state = level.getBlockState(pos);
                String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                if (customBlocks.containsKey(blockId)) {
                    keep = true;
                }
            }
            if (!keep) {
                storage.remove(light);
            }
        }
    }

    private void copyProperties(SettingsData.BlockLightSettings from, SimpleLightSource to) {
        to.active(true);
        to.dynamic(true);
        to.type(LightType.values()[from.type % LightType.values().length]);
        to.color(new v.akfz.aslib.render.color.Color(from.r, from.g, from.b));
        to.intensity(from.intensity);
        to.linear(from.linear);
        to.quadratic(from.quadratic);
        to.radius(from.radius);
        to.width(from.width);
        to.height(from.height);
        to.shadowSoftness(from.shadowSoftness);
        to.shadowBias(from.shadowBias);
        to.shadowsEnabled(from.shadowsEnabled);
        to.ignoreBlocks(false);
        to.volumetric(from.volumetric);
        to.volumetricStrength(from.volumetricStrength);
        to.mieG(from.mieG);
        to.fogDensity(from.fogDensity);
        to.fogAbsorption(from.fogAbsorption);
        to.falloffExponent(from.falloffExponent);
        to.sourceSize(from.sourceSize);
        to.shadowNear(from.shadowNear);
        to.shadowFar(from.shadowFar);
    }

    public synchronized List<LightSource<?>> getAllSources() {
        return storage.getAllSources();
    }

    public synchronized void setClean() {
        this.dirty = false;
        this.storage.isDirty = false;
    }

    public synchronized void setDirty() {
        this.dirty = true;
    }

    public synchronized void clean() {
        storage.clear();
        setClean();
    }

    public synchronized void removeSource(UUID uuid) {
        LightSource<?> target = null;
        for (LightSource<?> source : storage.getAllSources()) {
            if (source.getId().equals(uuid)) {
                target = source;
                break;
            }
        }
        if (target != null) {
            storage.remove(target);
            this.dirty = true;
        }
    }

    public synchronized CompoundTag save(CompoundTag nbt) {
        CompoundTag groupsTag = new CompoundTag();
        for (Map.Entry<String, List<LightSource<?>>> entry : storage.getGroups().entrySet()) {
            ListTag sourcesList = new ListTag();
            for (LightSource<?> source : entry.getValue()) {
                CompoundTag sourceTag = new CompoundTag();
                source.toNBT(sourceTag);
                sourcesList.add(sourceTag);
            }
            groupsTag.put(entry.getKey(), sourcesList);
        }
        nbt.put("LightGroups", groupsTag);
        return nbt;
    }

    public synchronized void load(CompoundTag nbt) {
        clean();
        if (!nbt.contains("LightGroups")) return;
        CompoundTag groupsTag = nbt.getCompound("LightGroups");
        for (String groupName : groupsTag.getAllKeys()) {
            ListTag sourcesList = groupsTag.getList(groupName, 10);
            for (int i = 0; i < sourcesList.size(); i++) {
                CompoundTag sourceTag = sourcesList.getCompound(i);

                String identifier = sourceTag.contains("TypeId") ? sourceTag.getString("TypeId") : sourceTag.getString("ClassType");
                LightSource<?> source = createLightInstance(identifier, sourceTag);
                if (source != null) {
                    storage.add(groupName, source);
                }
            }
        }
    }

    private LightSource<?> createLightInstance(String identifier, CompoundTag sourceTag) {
        if (identifier == null || identifier.isEmpty()) return null;

        LightSource<?> source = LightSource.create(identifier);
        if (source != null) {
            source.fromNBT(sourceTag);
            return source;
        }

        if (identifier.contains(".")) {
            try {
                Class<?> clazz = Class.forName(identifier);
                java.lang.reflect.Constructor<?> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                LightSource<?> legacySource = (LightSource<?>) ctor.newInstance();
                legacySource.fromNBT(sourceTag);
                return legacySource;
            } catch (Exception ignored) {
            }
        }

        try {
            return new SimpleLightSource().fromNBT(sourceTag);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}