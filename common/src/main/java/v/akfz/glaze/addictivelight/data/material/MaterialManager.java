package v.akfz.glaze.addictivelight.data.material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import v.akfz.aslib.util.json.GsonHelper;
import v.akfz.glaze.addictivelight.data.SettingsData;
import v.akfz.glaze.addictivelight.data.manager.DataManager;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MaterialManager {
    private static final Map<ResourceLocation, BlockMaterial> BLOCK_REGISTRY = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EntityMaterial> ENTITY_REGISTRY = new ConcurrentHashMap<>();

    private static final BlockMaterial DEFAULT_BLOCK_MATERIAL = new BlockMaterial();
    private static final EntityMaterial DEFAULT_ENTITY_MATERIAL = new EntityMaterial();

    public static void registerBlock(ResourceLocation id, BlockMaterial material) {
        material.setRuntimeID(BLOCK_REGISTRY.size());
        BLOCK_REGISTRY.put(id, material);
    }

    public static void registerEntity(ResourceLocation id, EntityMaterial material) {
        ENTITY_REGISTRY.put(id, material);
    }

    public static BlockMaterial getBlockMaterial(BlockState state) {
        if (state.isAir()) {
            return DEFAULT_BLOCK_MATERIAL;
        }
        Block block = state.getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (blockId == null) {
            return DEFAULT_BLOCK_MATERIAL;
        }

        BlockMaterial registered = BLOCK_REGISTRY.get(blockId);
        if (registered != null) {
            return registered;
        }

        String blockIdStr = blockId.toString();
        SettingsData settings = DataManager.INSTANCE.getSettingsData();
        if (settings != null && settings.customLightBlocks != null && settings.customLightBlocks.containsKey(blockIdStr)) {
            SettingsData.BlockLightSettings config = settings.customLightBlocks.get(blockIdStr);
            if (config != null) {
                BlockMaterial customMat = new BlockMaterial()
                        .emissive(1.0f)
                        .roughness(0.5f)
                        .metallic(0.0f)
                        .castShadows(config.shadowsEnabled);

                customMat.setTintR(config.r);
                customMat.setTintG(config.g);
                customMat.setTintB(config.b);

                registerBlock(blockId, customMat);
                return customMat;
            }
        }

        return DEFAULT_BLOCK_MATERIAL;
    }

    public static EntityMaterial getEntityMaterial(Entity entity) {
        if (entity == null) {
            return DEFAULT_ENTITY_MATERIAL;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return ENTITY_REGISTRY.getOrDefault(entityId, DEFAULT_ENTITY_MATERIAL);
    }

    public static Map<ResourceLocation, BlockMaterial> getBlockRegistry() {
        return BLOCK_REGISTRY;
    }

    public static void loadConfig(Path path) {
        MaterialsConfig config = GsonHelper.read(path, MaterialsConfig.class);
        applyConfig(config);
    }

    public static void loadFromResources(ResourceManager manager, ResourceLocation location) {
        MaterialsConfig config = GsonHelper.read(location, MaterialsConfig.class, manager);
        applyConfig(config);
    }

    private static void applyConfig(MaterialsConfig config) {
        if (config != null) {
            BLOCK_REGISTRY.clear();
            ENTITY_REGISTRY.clear();

            if (config.blocks != null) {
                config.blocks.forEach((key, material) -> {
                    ResourceLocation rl = ResourceLocation.tryParse(key);
                    if (rl != null) {
                        registerBlock(rl, material);
                    }
                });
            }

            if (config.entities != null) {
                config.entities.forEach((key, material) -> {
                    ResourceLocation rl = ResourceLocation.tryParse(key);
                    if (rl != null) {
                        registerEntity(rl, material);
                    }
                });
            }
        }
    }
}