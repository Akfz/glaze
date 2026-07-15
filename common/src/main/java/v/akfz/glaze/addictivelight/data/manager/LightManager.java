package v.akfz.glaze.addictivelight.data.manager;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import v.akfz.glaze.addictivelight.data.light.LightSource;
import v.akfz.glaze.addictivelight.data.light.SimpleLightSource;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class LightManager {
    public static class LightGroup {
        private final List<LightSource<?>> allSources;
        private final Map<String, List<LightSource<?>>> groups;

        public boolean isDirty = false;

        public LightGroup() {
            allSources = Collections.synchronizedList(new ArrayList<>());
            groups = new java.util.concurrent.ConcurrentHashMap<>();
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