package v.akfz.glaze.addictivelight.data.light;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class LightDebugRenderer {
    public static void render(PoseStack poseStack, LightSource<?> source, boolean showInfo, boolean showFrustum) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        RenderSystem.disableDepthTest();

        poseStack.pushPose();
        poseStack.translate(source.getX() - camPos.x, source.getY() - camPos.y, source.getZ() - camPos.z);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        if (showInfo) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

            float clrR = source.getR();
            float clrG = source.getG();
            float clrB = source.getB();

            switch (source.getType()) {
                case POINT:
                    drawAxisCircles(poseStack, buffer, source.getRadius(), clrR, clrG, clrB, 1.0f);
                    break;
                case SPOT:
                    drawSpotCone(poseStack, buffer, source, clrR, clrG, clrB, 1.0f);
                    break;
                case AREA_RECTANGLE:
                    drawAreaRectangle(poseStack, buffer, source, clrR, clrG, clrB, 1.0f);
                    break;
                case AREA_SPHERE:
                    drawAxisCircles(poseStack, buffer, source.getSourceSize(), clrR, clrG, clrB, 1.0f);
                    break;
                default:
                    float r = source.getRadius();
                    LevelRenderer.renderLineBox(poseStack, buffer, -r, -r, -r, r, r, r, clrR, clrG, clrB, 1.0f);
                    break;
            }
            tesselator.end();

            double distToPlayer = mc.player.distanceToSqr(source.getX(), source.getY(), source.getZ());
            if (distToPlayer < 256.0) {
                poseStack.pushPose();
                poseStack.translate(0.0, 0.5, 0.0);
                poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                poseStack.scale(-0.025f, -0.025f, 0.025f);

                String info = String.format("ID: %s | Type: %s | Int: %.1f",
                        source.getId() != null ? source.getId() : "Simple", source.getType().name(), source.getIntensity());

                BufferBuilder fontBuilder = new BufferBuilder(2048);
                net.minecraft.client.renderer.MultiBufferSource.BufferSource localBufferSource =
                        net.minecraft.client.renderer.MultiBufferSource.immediate(fontBuilder);

                mc.font.drawInBatch(
                        info,
                        -mc.font.width(info) / 2.0f,
                        0.0f,
                        0xFFFFFF,
                        false,
                        poseStack.last().pose(),
                        localBufferSource,
                        net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,
                        0,
                        15728880
                );

                localBufferSource.endBatch();
                poseStack.popPose();
            }
        }

        poseStack.popPose();

        if (showFrustum) {
            Matrix4f[] lightMatrices = source.getLightSpaceMatrices();
            if (lightMatrices.length > 0) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

                Vector4f[] ndcCorners = {
                        new Vector4f(-1, -1, -1, 1), new Vector4f(1, -1, -1, 1),
                        new Vector4f(1,  1, -1, 1), new Vector4f(-1,  1, -1, 1),
                        new Vector4f(-1, -1,  1, 1), new Vector4f(1, -1,  1, 1),
                        new Vector4f(1,  1,  1, 1), new Vector4f(-1,  1,  1, 1)
                };

                for (Matrix4f lightSpace : lightMatrices) {
                    Matrix4f invLightSpace = new Matrix4f(lightSpace).invert();
                    Vector3f[] worldCorners = new Vector3f[8];

                    for (int i = 0; i < 8; i++) {
                        Vector4f worldPos = invLightSpace.transform(new Vector4f(ndcCorners[i]));
                        worldCorners[i] = new Vector3f(worldPos.x / worldPos.w, worldPos.y / worldPos.w, worldPos.z / worldPos.w);
                    }

                    float r = 1.0f, g = 0.6f, b = 0.0f, a = 1.0f;
                    Matrix4f pose = poseStack.last().pose();

                    drawDebugLine(buffer, pose, worldCorners[0], worldCorners[1], camPos, r, g, b, a);
                    drawDebugLine(buffer, pose, worldCorners[1], worldCorners[2], camPos, r, g, b, a);
                    drawDebugLine(buffer, pose, worldCorners[2], worldCorners[3], camPos, r, g, b, a);
                    drawDebugLine(buffer, pose, worldCorners[3], worldCorners[0], camPos, r, g, b, a);

                    drawDebugLine(buffer, pose, worldCorners[4], worldCorners[5], camPos, r, g, b, a);
                    drawDebugLine(buffer, pose, worldCorners[5], worldCorners[6], camPos, r, g, b, a);
                    drawDebugLine(buffer, pose, worldCorners[6], worldCorners[7], camPos, r, g, b, a);
                    drawDebugLine(buffer, pose, worldCorners[7], worldCorners[4], camPos, r, g, b, a);

                    drawDebugLine(buffer, pose, worldCorners[0], worldCorners[4], camPos, r, g, b, a);
                    drawDebugLine(buffer, pose, worldCorners[1], worldCorners[5], camPos, r, g, b, a);
                    drawDebugLine(buffer, pose, worldCorners[2], worldCorners[6], camPos, r, g, b, a);
                    drawDebugLine(buffer, pose, worldCorners[3], worldCorners[7], camPos, r, g, b, a);
                }

                tesselator.end();
            }
        }

        RenderSystem.enableDepthTest();
    }

    private static void drawAxisCircles(PoseStack poseStack, BufferBuilder buffer, float radius, float r, float g, float b, float a) {
        drawSingleCircle(poseStack, buffer, 0, radius, r, g, b, a);
        drawSingleCircle(poseStack, buffer, 1, radius, r, g, b, a);
        drawSingleCircle(poseStack, buffer, 2, radius, r, g, b, a);
    }

    private static void drawSingleCircle(PoseStack poseStack, BufferBuilder buffer, int plane, float radius, float r, float g, float b, float a) {
        int segments = 32;
        double step = 2 * Math.PI / segments;
        Matrix4f pose = poseStack.last().pose();

        float lastX = 0, lastY = 0, lastZ = 0;
        for (int i = 0; i <= segments; i++) {
            double angle = i * step;
            float cos = (float) Math.cos(angle) * radius;
            float sin = (float) Math.sin(angle) * radius;

            float x = 0, y = 0, z = 0;
            if (plane == 0) {
                x = cos; y = sin;
            } else if (plane == 1) {
                x = cos; z = sin;
            } else {
                y = cos; z = sin;
            }

            if (i > 0) {
                buffer.vertex(pose, lastX, lastY, lastZ).color(r, g, b, a).endVertex();
                buffer.vertex(pose, x, y, z).color(r, g, b, a).endVertex();
            }
            lastX = x; lastY = y; lastZ = z;
        }
    }

    private static void drawSpotCone(PoseStack poseStack, BufferBuilder buffer, LightSource<?> source, float r, float g, float b, float a) {
        Vector3f dir = new Vector3f(source.getDirection()).normalize();
        Vector3f right = new Vector3f();
        if (Math.abs(dir.y) > 0.9f) {
            right.set(1f, 0f, 0f).cross(dir).normalize();
        } else {
            right.set(0f, 1f, 0f).cross(dir).normalize();
        }
        Vector3f up = new Vector3f(dir).cross(right).normalize();

        float rad = source.getRadius();
        float baseRadius = rad * (float) Math.tan(Math.toRadians(source.getOuterCutoff()));
        Vector3f endCenter = new Vector3f(dir).mul(rad);

        int segments = 32;
        double step = 2 * Math.PI / segments;
        Matrix4f pose = poseStack.last().pose();

        Vector3f lastPoint = null;
        for (int i = 0; i <= segments; i++) {
            double angle = i * step;
            float cos = (float) Math.cos(angle) * baseRadius;
            float sin = (float) Math.sin(angle) * baseRadius;

            Vector3f point = new Vector3f(endCenter)
                    .add(new Vector3f(right).mul(cos))
                    .add(new Vector3f(up).mul(sin));

            if (i > 0) {
                buffer.vertex(pose, lastPoint.x, lastPoint.y, lastPoint.z).color(r, g, b, a).endVertex();
                buffer.vertex(pose, point.x, point.y, point.z).color(r, g, b, a).endVertex();

                if ((i - 1) % (segments / 4) == 0) {
                    buffer.vertex(pose, 0f, 0f, 0f).color(r, g, b, a).endVertex();
                    buffer.vertex(pose, lastPoint.x, lastPoint.y, lastPoint.z).color(r, g, b, a).endVertex();
                }
            }
            lastPoint = point;
        }
    }

    private static void drawAreaRectangle(PoseStack poseStack, BufferBuilder buffer, LightSource<?> source, float r, float g, float b, float a) {
        Vector3f dir = new Vector3f(source.getDirection()).normalize();
        Vector3f right = new Vector3f();
        if (Math.abs(dir.y) > 0.9f) {
            right.set(1f, 0f, 0f).cross(dir).normalize();
        } else {
            right.set(0f, 1f, 0f).cross(dir).normalize();
        }
        Vector3f up = new Vector3f(dir).cross(right).normalize();

        float halfW = source.getWidth() / 2.0f;
        float halfH = source.getHeight() / 2.0f;

        Vector3f p0 = new Vector3f(right).mul(-halfW).add(new Vector3f(up).mul(-halfH));
        Vector3f p1 = new Vector3f(right).mul(halfW).add(new Vector3f(up).mul(-halfH));
        Vector3f p2 = new Vector3f(right).mul(halfW).add(new Vector3f(up).mul(halfH));
        Vector3f p3 = new Vector3f(right).mul(-halfW).add(new Vector3f(up).mul(halfH));

        Matrix4f pose = poseStack.last().pose();

        buffer.vertex(pose, p0.x, p0.y, p0.z).color(r, g, b, a).endVertex();
        buffer.vertex(pose, p1.x, p1.y, p1.z).color(r, g, b, a).endVertex();

        buffer.vertex(pose, p1.x, p1.y, p1.z).color(r, g, b, a).endVertex();
        buffer.vertex(pose, p2.x, p2.y, p2.z).color(r, g, b, a).endVertex();

        buffer.vertex(pose, p2.x, p2.y, p2.z).color(r, g, b, a).endVertex();
        buffer.vertex(pose, p3.x, p3.y, p3.z).color(r, g, b, a).endVertex();

        buffer.vertex(pose, p3.x, p3.y, p3.z).color(r, g, b, a).endVertex();
        buffer.vertex(pose, p0.x, p0.y, p0.z).color(r, g, b, a).endVertex();

        buffer.vertex(pose, 0f, 0f, 0f).color(r, g, b, a).endVertex();
        buffer.vertex(pose, dir.x, dir.y, dir.z).color(r, g, b, a).endVertex();
    }

    private static void drawDebugLine(BufferBuilder builder, Matrix4f pose, Vector3f start, Vector3f end, Vec3 camPos, float r, float g, float b, float a) {
        builder.vertex(pose, (float) (start.x - camPos.x), (float) (start.y - camPos.y), (float) (start.z - camPos.z))
                .color(r, g, b, a).endVertex();
        builder.vertex(pose, (float) (end.x - camPos.x), (float) (end.y - camPos.y), (float) (end.z - camPos.z))
                .color(r, g, b, a).endVertex();
    }
}