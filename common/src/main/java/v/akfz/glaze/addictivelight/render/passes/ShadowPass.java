package v.akfz.glaze.addictivelight.render.passes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import v.akfz.glaze.addictivelight.data.light.LightSource;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.addictivelight.data.material.BlockMaterial;
import v.akfz.glaze.addictivelight.data.material.EntityMaterial;
import v.akfz.glaze.addictivelight.data.material.MaterialManager;
import v.akfz.glaze.addictivelight.localutil.ShadowBufferSourceBridge;
import v.akfz.glaze.addictivelight.localutil.ShadowFramebuffer;
import v.akfz.glaze.mixin.client.LevelRendererAccessor;

import java.util.*;

public class ShadowPass implements RenderPass {

    public static final Matrix4f[] lightSpaceMatrices = new Matrix4f[256];
    static {
        for (int i = 0; i < 256; i++) {
            lightSpaceMatrices[i] = new Matrix4f();
        }
    }
    private static final Vector3f[] UPS = {
            new Vector3f(0.0f, -1.0f, 0.0f),
            new Vector3f(0.0f, -1.0f, 0.0f),
            new Vector3f(0.0f, 0.0f, 1.0f),
            new Vector3f(0.0f, 0.0f, -1.0f),
            new Vector3f(0.0f, -1.0f, 0.0f),
            new Vector3f(0.0f, -1.0f, 0.0f)
    };

    private final BufferBuilder blockBuilder = new BufferBuilder(262144);
    private final BufferBuilder blockEntityBuilder = new BufferBuilder(262144);
    private final BufferBuilder entityBuilder = new BufferBuilder(262144);
    private final BufferBuilder particleBuilder = new BufferBuilder(262144);

    private static final Map<Class<?>, java.lang.reflect.Field> compositeStateFieldCache = new HashMap<>();
    private static final Map<Class<?>, java.lang.reflect.Field> textureStateFieldCache = new HashMap<>();
    private static final Map<Class<?>, java.lang.reflect.Field> resourceLocationFieldCache = new HashMap<>();

    private static ResourceLocation extractTexture(net.minecraft.client.renderer.RenderType type) {
        if (type == null) return null;
        Class<?> typeClass = type.getClass();
        String className = typeClass.getName();
        if (className.contains("CompositeRenderType") || typeClass.getSimpleName().equals("CompositeRenderType")) {
            try {
                java.lang.reflect.Field stateField = compositeStateFieldCache.computeIfAbsent(typeClass, clazz -> {
                    Class<?> superClazz = clazz;
                    while (superClazz != null && superClazz != Object.class) {
                        for (java.lang.reflect.Field f : superClazz.getDeclaredFields()) {
                            if (f.getType().getSimpleName().equals("CompositeState") || f.getType().getName().contains("CompositeState")) {
                                f.setAccessible(true);
                                return f;
                            }
                        }
                        superClazz = superClazz.getSuperclass();
                    }
                    return null;
                });

                if (stateField == null) return null;
                Object compositeState = stateField.get(type);
                if (compositeState == null) return null;

                java.lang.reflect.Field textureStateField = textureStateFieldCache.computeIfAbsent(compositeState.getClass(), clazz -> {
                    Class<?> superClazz = clazz;
                    while (superClazz != null && superClazz != Object.class) {
                        for (java.lang.reflect.Field f : superClazz.getDeclaredFields()) {
                            if (f.getType().getSimpleName().equals("TextureStateShard") || f.getType().getName().contains("TextureStateShard")) {
                                f.setAccessible(true);
                                return f;
                            }
                        }
                        superClazz = superClazz.getSuperclass();
                    }
                    return null;
                });

                if (textureStateField == null) return null;
                Object textureState = textureStateField.get(compositeState);
                if (textureState == null) return null;

                java.lang.reflect.Field resLocField = resourceLocationFieldCache.computeIfAbsent(textureState.getClass(), clazz -> {
                    Class<?> superClazz = clazz;
                    while (superClazz != null && superClazz != Object.class) {
                        for (java.lang.reflect.Field f : superClazz.getDeclaredFields()) {
                            if (f.getType() == Optional.class || f.getType() == ResourceLocation.class) {
                                f.setAccessible(true);
                                return f;
                            }
                        }
                        superClazz = superClazz.getSuperclass();
                    }
                    return null;
                });

                if (resLocField == null) return null;
                Object val = resLocField.get(textureState);
                if (val instanceof Optional<?> opt) {
                    return (ResourceLocation) opt.orElse(null);
                } else if (val instanceof ResourceLocation rl) {
                    return rl;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private MultiBufferSource.BufferSource createBufferSource(BufferBuilder fallbackBuilder, boolean isEntity) {
        return new ShadowBufferSourceBridge(fallbackBuilder, new HashMap<>()) {
            @Override
            public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(net.minecraft.client.renderer.RenderType type) {
                net.minecraft.client.renderer.RenderType targetType = type;
                String name = type.toString();
                if (name.contains("translucent") || name.contains("tripwire") || name.contains("water") || name.contains("transparent")) {
                    ResourceLocation tex = extractTexture(type);
                    if (tex != null) {
                        if (isEntity) {
                            targetType = net.minecraft.client.renderer.RenderType.entityCutout(tex);
                        } else {
                            targetType = net.minecraft.client.renderer.RenderType.cutout();
                        }
                    } else {
                        if (isEntity) {
                            targetType = net.minecraft.client.renderer.RenderType.entityCutout(new ResourceLocation("textures/atlas/blocks.png"));
                        } else {
                            targetType = net.minecraft.client.renderer.RenderType.cutout();
                        }
                    }
                }
                return super.getBuffer(targetType);
            }
        };
    }

    private final List<BlockPos> blocksToRender = new ArrayList<>();
    private final List<BlockEntity> blockEntitiesToRender = new ArrayList<>();
    private final List<Entity> entitiesToRender = new ArrayList<>();
    private final Set<BlockPos> uniqueBlockSet = new HashSet<>();

    @Override
    public void render(Object... objects) {
        ShadowFramebuffer blockFb = (ShadowFramebuffer) objects[0];
        ShadowFramebuffer blockEntityFb = (ShadowFramebuffer) objects[1];
        ShadowFramebuffer entityFb = (ShadowFramebuffer) objects[2];
        ShadowFramebuffer particleFb = (ShadowFramebuffer) objects[3];
        Vec3 camPos = (Vec3) objects[4];

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        this.blockBuilder.discard();
        this.blockEntityBuilder.discard();
        this.entityBuilder.discard();
        this.particleBuilder.discard();

        CameraType originalCameraType = mc.options.getCameraType();
        boolean wasFirstPerson = originalCameraType.isFirstPerson();
        if (wasFirstPerson) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        mc.gameRenderer.overlayTexture().setupOverlayColor();
        mc.gameRenderer.lightTexture().turnOnLightLayer();

        Matrix4f oldProj = null;
        Matrix4f oldModelView = null;
        boolean oldDepthTest = false;
        boolean oldDepthMask = false;
        boolean oldCullFace = false;
        boolean oldBlend = false;
        boolean[] oldColorMask = new boolean[4];
        boolean statesSaved = false;

        try {
            Collection<LightSource<?>> lights = DataManager.INSTANCE.getLightManager().getStorage().getAllSources();
            int maxLights = DataManager.INSTANCE.getSettingsData().maxLights;

            int maxAllowedLayers = Math.min(
                    Math.min(blockFb.getAllocatedLayers(), blockEntityFb.getAllocatedLayers()),
                    Math.min(entityFb.getAllocatedLayers(), particleFb.getAllocatedLayers())
            );
            if (maxAllowedLayers > 0) {
                maxLights = Math.min(maxLights, maxAllowedLayers);
            } else {
                maxLights = 0;
            }

            if (maxLights <= 0) return;

            oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
            oldModelView = new Matrix4f(RenderSystem.getModelViewMatrix());

            oldDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            oldCullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            oldBlend = GL11.glIsEnabled(GL11.GL_BLEND);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                java.nio.ByteBuffer colorMaskBuffer = stack.malloc(4);
                GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMaskBuffer);
                oldColorMask[0] = colorMaskBuffer.get(0) != 0;
                oldColorMask[1] = colorMaskBuffer.get(1) != 0;
                oldColorMask[2] = colorMaskBuffer.get(2) != 0;
                oldColorMask[3] = colorMaskBuffer.get(3) != 0;
            }
            statesSaved = true;

            GlStateManager._enableDepthTest();
            GlStateManager._depthMask(true);
            GlStateManager._disableBlend();
            GlStateManager._disableCull();
            GlStateManager._depthFunc(GL11.GL_LEQUAL);
            GL11.glColorMask(false, false, false, false);

            entityFb.clearAllLayers();
            particleFb.clearAllLayers();

            var blockRenderer = mc.getBlockRenderer();
            var entityRenderer = mc.getEntityRenderDispatcher();
            var blockEntityRenderer = mc.getBlockEntityRenderDispatcher();

            LevelRendererAccessor lra = (LevelRendererAccessor) mc.levelRenderer;
            Frustum frustum = lra.getCapturedFrustum() != null ? lra.getCapturedFrustum() : lra.getCullingFrustum();

            int layerIndex = 0;
            for (LightSource<?> light : lights) {
                if (layerIndex >= maxLights) break;
                if (!light.isActive() || !light.isShadowsEnabled()) continue;

                int layersNeeded = light.isOmnidirectional() ? 6 : 1;

                if (frustum != null) {
                    double r = light.getRadius();
                    AABB lightBounds = new AABB(
                            light.getX() - r, light.getY() - r, light.getZ() - r,
                            light.getX() + r, light.getY() + r, light.getZ() + r
                    );
                    if (!frustum.isVisible(lightBounds)) {
                        layerIndex += layersNeeded;
                        continue;
                    }
                }

                blocksToRender.clear();
                blockEntitiesToRender.clear();
                entitiesToRender.clear();
                uniqueBlockSet.clear();

                boolean renderStatic = light.isDirty() || light.isDynamic();

                boolean allChunksLoaded = gatherLightGeometry(mc, light, renderStatic);

                if (renderStatic) {
                    for (int d = 0; d < layersNeeded; d++) {
                        int currentLayer = layerIndex + d;
                        if (currentLayer >= maxLights) break;
                        blockFb.clearLayer(currentLayer);
                        blockEntityFb.clearLayer(currentLayer);
                    }
                }

                for (int direction = 0; direction < layersNeeded; direction++) {
                    int currentLayer = layerIndex + direction;
                    if (currentLayer >= maxLights) break;

                    renderLayerQueues(
                            light,
                            direction,
                            currentLayer,
                            renderStatic,
                            blockRenderer,
                            entityRenderer,
                            blockEntityRenderer,
                            blockFb,
                            blockEntityFb,
                            entityFb,
                            particleFb,
                            camPos
                    );
                }

                if (allChunksLoaded) {
                    light.setDirty(false);
                } else {
                    light.setDirty(true);
                }

                layerIndex += layersNeeded;
            }

        } finally {
            blockFb.unbind();
            GL30.glBindVertexArray(0);

            if (statesSaved) {
                RenderSystem.setProjectionMatrix(oldProj, RenderSystem.getVertexSorting());
                RenderSystem.getModelViewMatrix().set(oldModelView);
                RenderSystem.applyModelViewMatrix();

                if (oldDepthTest) GlStateManager._enableDepthTest(); else GlStateManager._disableDepthTest();
                GlStateManager._depthMask(oldDepthMask);
                if (oldBlend) GlStateManager._enableBlend(); else GlStateManager._disableBlend();
                if (oldCullFace) GlStateManager._enableCull(); else GlStateManager._disableCull();
                GL11.glColorMask(oldColorMask[0], oldColorMask[1], oldColorMask[2], oldColorMask[3]);
            }
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);

            mc.gameRenderer.lightTexture().turnOffLightLayer();
            mc.gameRenderer.overlayTexture().teardownOverlayColor();

            if (wasFirstPerson) {
                mc.options.setCameraType(originalCameraType);
            }
        }
    }

    @Override
    public void cleanup() {
        blocksToRender.clear();
        blockEntitiesToRender.clear();
        entitiesToRender.clear();
        uniqueBlockSet.clear();

        this.blockBuilder.discard();
        this.blockEntityBuilder.discard();
        this.entityBuilder.discard();
        this.particleBuilder.discard();
    }

    private boolean gatherLightGeometry(Minecraft mc, LightSource<?> light, boolean renderStatic) {
        double lightX = light.getX();
        double lightY = light.getY();
        double lightZ = light.getZ();
        boolean allChunksLoaded = true;

        if (renderStatic) {
            int rad = Math.min(32, (int) Math.ceil(light.getRadius()));
            double radSqr = rad * rad;
            BlockPos center = BlockPos.containing(lightX, lightY, lightZ);

            if (light.isDynamic()) {
                rad = 5;
                radSqr = rad * rad;

                int minX = center.getX() - rad;
                int maxX = center.getX() + rad;
                int minY = Math.max(mc.level.getMinBuildHeight(), center.getY() - rad);
                int maxY = Math.min(mc.level.getMaxBuildHeight(), center.getY() + rad);
                int minZ = center.getZ() - rad;
                int maxZ = center.getZ() + rad;

                for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
                    for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                        net.minecraft.world.level.chunk.LevelChunk chunk = mc.level.getChunkSource().getChunk(chunkX, chunkZ, false);
                        if (chunk == null) {
                            allChunksLoaded = false;
                            continue;
                        }

                        int startX = Math.max(minX, chunkX << 4);
                        int endX = Math.min(maxX, (chunkX << 4) + 15);
                        int startZ = Math.max(minZ, chunkZ << 4);
                        int endZ = Math.min(maxZ, (chunkZ << 4) + 15);

                        for (int y = minY; y <= maxY; y++) {
                            for (int z = startZ; z <= endZ; z++) {
                                for (int x = startX; x <= endX; x++) {
                                    if (x == center.getX() && y == center.getY() && z == center.getZ()) {
                                        continue;
                                    }

                                    double dx = x + 0.5 - lightX;
                                    double dy = y + 0.5 - lightY;
                                    double dz = z + 0.5 - lightZ;

                                    if (dx * dx + dy * dy + dz * dz > radSqr) {
                                        continue;
                                    }

                                    BlockPos pos = new BlockPos(x, y, z);
                                    BlockState state = chunk.getBlockState(pos);

                                    if (!state.isAir()) {
                                        BlockMaterial mat = MaterialManager.getBlockMaterial(state);
                                        if (!mat.isCastShadows() || mat.getOpacity() <= 0.0f) {
                                            continue;
                                        }

                                        uniqueBlockSet.add(pos);
                                        BlockEntity be = chunk.getBlockEntity(pos);
                                        if (be != null) {
                                            blockEntitiesToRender.add(be);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (light.getCachedShadowBlocks().isEmpty() || light.isDirty()) {
                    light.getCachedShadowBlocks().clear();
                    light.getCachedShadowBlockEntities().clear();

                    int minX = center.getX() - rad;
                    int maxX = center.getX() + rad;
                    int minY = Math.max(mc.level.getMinBuildHeight(), center.getY() - rad);
                    int maxY = Math.min(mc.level.getMaxBuildHeight(), center.getY() + rad);
                    int minZ = center.getZ() - rad;
                    int maxZ = center.getZ() + rad;

                    for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
                        for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                            net.minecraft.world.level.chunk.LevelChunk chunk = mc.level.getChunkSource().getChunk(chunkX, chunkZ, false);
                            if (chunk == null) {
                                allChunksLoaded = false;
                                continue;
                            }

                            int startX = Math.max(minX, chunkX << 4);
                            int endX = Math.min(maxX, (chunkX << 4) + 15);
                            int startZ = Math.max(minZ, chunkZ << 4);
                            int endZ = Math.min(maxZ, (chunkZ << 4) + 15);

                            for (int y = minY; y <= maxY; y++) {
                                for (int z = startZ; z <= endZ; z++) {
                                    for (int x = startX; x <= endX; x++) {
                                        if (x == center.getX() && y == center.getY() && z == center.getZ()) {
                                            continue;
                                        }

                                        double dx = x + 0.5 - lightX;
                                        double dy = y + 0.5 - lightY;
                                        double dz = z + 0.5 - lightZ;

                                        if (dx * dx + dy * dy + dz * dz > radSqr) {
                                            continue;
                                        }

                                        BlockPos pos = new BlockPos(x, y, z);
                                        BlockState state = chunk.getBlockState(pos);

                                        if (!state.isAir()) {
                                            BlockMaterial mat = MaterialManager.getBlockMaterial(state);
                                            if (!mat.isCastShadows() || mat.getOpacity() <= 0.0f) {
                                                continue;
                                            }

                                            light.getCachedShadowBlocks().add(pos);
                                            BlockEntity be = chunk.getBlockEntity(pos);
                                            if (be != null) {
                                                light.getCachedShadowBlockEntities().add(be);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                uniqueBlockSet.addAll(light.getCachedShadowBlocks());
                blockEntitiesToRender.addAll(light.getCachedShadowBlockEntities());
            }
            blocksToRender.addAll(uniqueBlockSet);
        }

        int rad = Math.min((int) Math.ceil(light.getRadius()), 16);
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity == mc.cameraEntity && mc.options.getCameraType().isFirstPerson()) {
                continue;
            }
            double dist = entity.distanceToSqr(lightX, lightY, lightZ);
            if (dist <= rad * rad) {
                EntityMaterial mat = MaterialManager.getEntityMaterial(entity);
                if (!mat.isCastShadows() || mat.getOpacity() <= 0.0f) {
                    continue;
                }

                entitiesToRender.add(entity);

                if (renderStatic) {
                    BlockPos groundPos1 = BlockPos.containing(entity.getX(), entity.getY() - 0.1, entity.getZ());
                    BlockPos groundPos2 = groundPos1.below();

                    for (BlockPos gp : new BlockPos[]{groundPos1, groundPos2}) {
                        if (uniqueBlockSet.add(gp.immutable())) {
                            BlockState state = mc.level.getBlockState(gp);
                            if (!state.isAir()) {
                                BlockMaterial blockMat = MaterialManager.getBlockMaterial(state);
                                if (!blockMat.isCastShadows() || blockMat.getOpacity() <= 0.0f) {
                                    continue;
                                }

                                BlockEntity be = mc.level.getBlockEntity(gp);
                                if (be != null) {
                                    blockEntitiesToRender.add(be);
                                }
                            } else {
                                uniqueBlockSet.remove(gp);
                            }
                        }
                    }
                }
            }
        }
        return allChunksLoaded;
    }

    private void renderLayerQueues(LightSource<?> light, int directionIndex, int layer,
                                   boolean renderStatic,
                                   BlockRenderDispatcher blockRenderer,
                                   EntityRenderDispatcher entityRenderer,
                                   BlockEntityRenderDispatcher blockEntityRenderer,
                                   ShadowFramebuffer blockFb,
                                   ShadowFramebuffer blockEntityFb,
                                   ShadowFramebuffer entityFb,
                                   ShadowFramebuffer particleFb,
                                   Vec3 camPos) {

        Matrix4f lightProj = new Matrix4f();
        Matrix4f lightView = new Matrix4f();

        Minecraft mc = Minecraft.getInstance();
        double renderX = Mth.lerp(mc.getFrameTime(), light.getPrevX(), light.getX());
        double renderY = Mth.lerp(mc.getFrameTime(), light.getPrevY(), light.getY());
        double renderZ = Mth.lerp(mc.getFrameTime(), light.getPrevZ(), light.getZ());
        Vector3f absPos = new Vector3f((float) renderX, (float) renderY, (float) renderZ);

        if (!light.isOmnidirectional()) {
            float fov = (float) Math.toRadians(light.getOuterCutoff() * 2.0f);
            lightProj.perspective(fov, 1.0f, light.getShadowNear(), light.getShadowFar());

            Vector3f dir = new Vector3f(light.getDirection()).normalize();
            Vector3f target = new Vector3f(absPos).add(dir);
            Vector3f up = new Vector3f(0f, 1f, 0f);
            if (Math.abs(dir.y) > 0.99f) {
                up.set(0f, 0f, 1f);
            }
            lightView.lookAt(absPos, target, up);
        } else {
            float fov = (float) Math.toRadians(92.0f);
            lightProj.perspective(fov, 1.0f, light.getShadowNear(), light.getShadowFar());

            Vector3f target = new Vector3f();
            switch (directionIndex) {
                case 0: target.set(absPos).add(new Vector3f(1f, 0f, 0f)); break;
                case 1: target.set(absPos).add(new Vector3f(-1f, 0f, 0f)); break;
                case 2: target.set(absPos).add(new Vector3f(0f, 1f, 0f)); break;
                case 3: target.set(absPos).add(new Vector3f(0f, -1f, 0f)); break;
                case 4: target.set(absPos).add(new Vector3f(0f, 0f, 1f)); break;
                case 5: target.set(absPos).add(new Vector3f(0f, 0f, -1f)); break;
            }
            lightView.lookAt(absPos, target, UPS[directionIndex % 6]);
        }

        ShadowPass.lightSpaceMatrices[layer].set(new Matrix4f(lightProj).mul(lightView));

        boolean hasBlocks = renderStatic && !blocksToRender.isEmpty();
        boolean hasBlockEntities = renderStatic && !blockEntitiesToRender.isEmpty();
        boolean hasEntities = !entitiesToRender.isEmpty();

        if (!hasBlocks && !hasBlockEntities && !hasEntities) {
            return;
        }

        this.blockBuilder.discard();
        this.blockEntityBuilder.discard();
        this.entityBuilder.discard();
        this.particleBuilder.discard();

        MultiBufferSource.BufferSource blockBufferSource = createBufferSource(blockBuilder, false);
        MultiBufferSource.BufferSource blockEntityBufferSource = createBufferSource(blockEntityBuilder, true);
        MultiBufferSource.BufferSource entityBufferSource = createBufferSource(entityBuilder, true);

        MultiBufferSource.BufferSource particleBufferSource = new ShadowBufferSourceBridge(particleBuilder, new HashMap<>());

        RenderSystem.setProjectionMatrix(lightProj, RenderSystem.getVertexSorting());

        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        modelViewStack.mulPoseMatrix(lightView);
        RenderSystem.applyModelViewMatrix();

        PoseStack poseStack = new PoseStack();
        double lightX = light.getX();
        double lightY = light.getY();
        double lightZ = light.getZ();

        if (hasBlocks) {
            blockFb.bindLayer(layer);
            boolean useColor = blockFb.isHasColor();
            GL11.glColorMask(useColor, useColor, useColor, useColor);

            for (BlockPos p : blocksToRender) {
                double dx = p.getX() + 0.5 - lightX;
                double dy = p.getY() + 0.5 - lightY;
                double dz = p.getZ() + 0.5 - lightZ;

                if (isOutsideFace(light.isOmnidirectional(), directionIndex, dx, dy, dz)) {
                    continue;
                }

                BlockState state = mc.level.getBlockState(p);
                BlockMaterial mat = MaterialManager.getBlockMaterial(state);

                RenderSystem.setShaderColor(mat.getTintR(), mat.getTintG(), mat.getTintB(), mat.getOpacity());

                poseStack.pushPose();
                poseStack.translate(p.getX(), p.getY(), p.getZ());
                blockRenderer.renderSingleBlock(state, poseStack, blockBufferSource, 15728880, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
            blockBufferSource.endBatch();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        if (hasBlockEntities) {
            blockEntityFb.bindLayer(layer);
            boolean useColor = blockEntityFb.isHasColor();
            GL11.glColorMask(useColor, useColor, useColor, useColor);

            for (BlockEntity be : blockEntitiesToRender) {
                BlockPos p = be.getBlockPos();
                double dx = p.getX() + 0.5 - lightX;
                double dy = p.getY() + 0.5 - lightY;
                double dz = p.getZ() + 0.5 - lightZ;

                if (isOutsideFace(light.isOmnidirectional(), directionIndex, dx, dy, dz)) {
                    continue;
                }

                BlockState state = be.getBlockState();
                BlockMaterial mat = MaterialManager.getBlockMaterial(state);

                RenderSystem.setShaderColor(mat.getTintR(), mat.getTintG(), mat.getTintB(), mat.getOpacity());

                poseStack.pushPose();
                poseStack.translate(p.getX(), p.getY(), p.getZ());
                blockEntityRenderer.render(be, mc.getFrameTime(), poseStack, blockEntityBufferSource);
                poseStack.popPose();
            }
            blockEntityBufferSource.endBatch();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        if (hasEntities) {
            entityFb.bindLayer(layer);
            boolean useColor = entityFb.isHasColor();
            GL11.glColorMask(useColor, useColor, useColor, useColor);

            float partialTicks = mc.getFrameTime();

            boolean originalHitboxes = entityRenderer.shouldRenderHitBoxes();
            entityRenderer.setRenderHitBoxes(false);

            try {
                for (var entity : entitiesToRender) {
                    double smoothX = Mth.lerp(partialTicks, entity.xOld, entity.getX());
                    double smoothY = Mth.lerp(partialTicks, entity.yOld, entity.getY());
                    double smoothZ = Mth.lerp(partialTicks, entity.zOld, entity.getZ());

                    double dx = smoothX - lightX;
                    double dy = smoothY + (entity.getBbHeight() * 0.5) - lightY;
                    double dz = smoothZ - lightZ;

                    if (isOutsideFace(light.isOmnidirectional(), directionIndex, dx, dy, dz)) {
                        continue;
                    }

                    poseStack.pushPose();
                    poseStack.translate(smoothX, smoothY, smoothZ);

                    EntityMaterial mat = MaterialManager.getEntityMaterial(entity);

                    RenderSystem.setShaderColor(mat.getTintR(), mat.getTintG(), mat.getTintB(), mat.getOpacity());

                    entityRenderer.render(
                            entity,
                            0.0, 0.0, 0.0,
                            entity.getYRot(),
                            partialTicks,
                            poseStack,
                            entityBufferSource,
                            15728880
                    );

                    poseStack.popPose();
                }
            } finally {
                entityRenderer.setRenderHitBoxes(originalHitboxes);
            }
            entityBufferSource.endBatch();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        particleFb.bindLayer(layer);
        boolean useColor = particleFb.isHasColor();
        GL11.glColorMask(useColor, useColor, useColor, useColor);
        GlStateManager._depthMask(true);

        PoseStack particlePose = new PoseStack();
        particlePose.translate(camPos.x, camPos.y, camPos.z);

        mc.particleEngine.render(particlePose, particleBufferSource, mc.gameRenderer.lightTexture(), mc.gameRenderer.getMainCamera(), mc.getFrameTime());
        particleBufferSource.endBatch();

        GL11.glColorMask(false, false, false, false);
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private boolean isOutsideFace(boolean isOmnidirectional, int direction, double dx, double dy, double dz) {
        if (!isOmnidirectional) {
            return false;
        }
        double margin = 1.0;
        return switch (direction) {
            case 0 -> dx <= -0.05 || Math.abs(dy) > dx + margin || Math.abs(dz) > dx + margin;
            case 1 -> dx >= 0.05 || Math.abs(dy) > -dx + margin || Math.abs(dz) > -dx + margin;
            case 2 -> dy <= -0.05 || Math.abs(dx) > dy + margin || Math.abs(dz) > dy + margin;
            case 3 -> dy >= 0.05 || Math.abs(dx) > -dy + margin || Math.abs(dz) > -dy + margin;
            case 4 -> dz <= -0.05 || Math.abs(dx) > dz + margin || Math.abs(dy) > dz + margin;
            case 5 -> dz >= 0.05 || Math.abs(dx) > -dz + margin || Math.abs(dy) > -dz + margin;
            default -> false;
        };
    }
}