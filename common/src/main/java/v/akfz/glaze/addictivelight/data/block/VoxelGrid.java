package v.akfz.glaze.addictivelight.data.block;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import v.akfz.glaze.addictivelight.data.material.BlockMaterial;
import v.akfz.glaze.addictivelight.data.material.MaterialManager;
import v.akfz.glaze.mixin.client.LevelRendererAccessor;
import v.akfz.glaze.shader.util.TextureBuffer;

public class VoxelGrid {
    @Getter
    private final int gridXZ;
    @Getter
    private final int gridY;
    @Getter
    private final TextureBuffer textureBuffer = new TextureBuffer();

    private double lastCamX = Double.NaN;
    private double lastCamY = Double.NaN;
    private double lastCamZ = Double.NaN;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private boolean forceUpdate = true;

    public VoxelGrid(int gridXZ, int gridY) {
        this.gridXZ = gridXZ;
        this.gridY = gridY;
    }

    public void markDirty() {
        this.forceUpdate = true;
    }

    public void update() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        float yaw = mc.gameRenderer.getMainCamera().getYRot();
        float pitch = mc.gameRenderer.getMainCamera().getXRot();

        double eps = 0.001;
        float rotEps = 0.01f;

        boolean camMoved = Math.abs(lastCamX - camPos.x) > eps ||
                Math.abs(lastCamY - camPos.y) > eps ||
                Math.abs(lastCamZ - camPos.z) > eps;

        boolean camRotated = Math.abs(lastYaw - yaw) > rotEps ||
                Math.abs(lastPitch - pitch) > rotEps;

        if (!forceUpdate && !camMoved && !camRotated) {
            return;
        }

        this.forceUpdate = false;
        this.lastCamX = camPos.x;
        this.lastCamY = camPos.y;
        this.lastCamZ = camPos.z;
        this.lastYaw = yaw;
        this.lastPitch = pitch;

        LevelRendererAccessor lra = (LevelRendererAccessor) mc.levelRenderer;
        Frustum frustum = lra.getCapturedFrustum() != null ? lra.getCapturedFrustum() : lra.getCullingFrustum();

        int texWidth = gridXZ;
        int texHeight = gridXZ * gridY;
        textureBuffer.resize(texWidth, texHeight);

        float[] voxelData = new float[gridXZ * gridXZ * gridY * 4];

        populateAll(camPos, frustum, voxelData);

        textureBuffer.upload(voxelData);
    }

    private void populateAll(Vec3 camPos, Frustum frustum, float[] voxelData) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        BlockPos playerPos = BlockPos.containing(camPos.x, camPos.y, camPos.z);
        int centerX = playerPos.getX();
        int centerY = playerPos.getY();
        int centerZ = playerPos.getZ();

        int startX = centerX - gridXZ / 2;
        int startY = centerY - gridY / 2;
        int startZ = centerZ - gridXZ / 2;

        int subSize = 8;

        for (int cy = 0; cy < gridY; cy += subSize) {
            int ey = Math.min(cy + subSize, gridY);
            for (int cx = 0; cx < gridXZ; cx += subSize) {
                int ex = Math.min(cx + subSize, gridXZ);
                for (int cz = 0; cz < gridXZ; cz += subSize) {
                    int ez = Math.min(cz + subSize, gridXZ);

                    boolean subChunkVisible = true;
                    if (frustum != null) {
                        AABB subBox = new AABB(
                                startX + cx, startY + cy, startZ + cz,
                                startX + ex, startY + ey, startZ + ez
                        );
                        subChunkVisible = frustum.isVisible(subBox);
                    }

                    for (int y = cy; y < ey; y++) {
                        for (int z = cz; z < ez; z++) {
                            int chunkZ = (startZ + z) >> 4;

                            for (int x = cx; x < ex; x++) {
                                int pixelIdx = (y * gridXZ + z) * gridXZ + x;
                                int floatIdx = pixelIdx * 4;

                                if (!subChunkVisible) {
                                    voxelData[floatIdx]     = 0.0f;
                                    voxelData[floatIdx + 1] = 0.0f;
                                    voxelData[floatIdx + 2] = 0.0f;
                                    voxelData[floatIdx + 3] = 0.0f;
                                    continue;
                                }

                                int worldX = startX + x;
                                int worldY = startY + y;
                                int worldZ = startZ + z;

                                int chunkX = worldX >> 4;
                                if (!mc.level.hasChunk(chunkX, chunkZ)) {
                                    voxelData[floatIdx]     = 0.0f;
                                    voxelData[floatIdx + 1] = 0.0f;
                                    voxelData[floatIdx + 2] = 0.0f;
                                    voxelData[floatIdx + 3] = 0.0f;
                                    continue;
                                }

                                BlockPos pos = new BlockPos(worldX, worldY, worldZ);
                                BlockState state = mc.level.getBlockState(pos);

                                if (state.isAir()) {
                                    voxelData[floatIdx]     = 0.0f;
                                    voxelData[floatIdx + 1] = 0.0f;
                                    voxelData[floatIdx + 2] = 0.0f;
                                    voxelData[floatIdx + 3] = 0.0f;
                                } else {
                                    BlockMaterial mat = MaterialManager.getBlockMaterial(state);
                                    voxelData[floatIdx]     = (float) mat.getRuntimeID() + 1.0f;
                                    voxelData[floatIdx + 1] = mat.getRoughness();
                                    voxelData[floatIdx + 2] = mat.getMetallic();
                                    voxelData[floatIdx + 3] = mat.getEmissive();
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    public void cleanup() {
        this.textureBuffer.cleanup();
    }
}