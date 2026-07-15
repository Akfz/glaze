package v.akfz.glaze.addictivelight.data.light;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import v.akfz.aslib.network.AsLibNetworking;
import v.akfz.glaze.addictivelight.data.SettingsData;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.addictivelight.gui.RedactorGui;
import v.akfz.glaze.network.light.delete.DeleteLightSourcePacket;
import v.akfz.glaze.network.light.update.SyncLightsPacket;
import v.akfz.glaze.network.rights.HaveRightsToChange;

public class LightRedactor {
    public static final LightRedactor INSTANCE = new LightRedactor();

    private boolean isChangingGlobal = false;
    @Nullable private LightSource<?> pickedSource = null;

    private double grabDistance = 0.0;
    private boolean lastRmb = false;
    private boolean lastLmb = false;
    private boolean lastE = false;
    private boolean lastQ = false;

    private long lastSyncTime = 0;

    public void update() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || !DataManager.INSTANCE.getSettingsData().isDevMode) return;

        if (!isChangingGlobal && !mc.hasSingleplayerServer() && mc.player.tickCount % 100 == 0) {
            askToChange();
        }

        boolean rmb = mc.options.keyUse.isDown();
        boolean lmb = mc.options.keyAttack.isDown();
        boolean shift = mc.options.keyShift.isDown();
        boolean keyE = mc.options.keyInventory.isDown();
        boolean keyQ = mc.options.keyDrop.isDown();

        boolean rmbPressed = rmb && !lastRmb;
        boolean ePressed = keyE && !lastE;
        boolean qPressed = keyQ && !lastQ;

        lastRmb = rmb;
        lastLmb = lmb;
        lastE = keyE;
        lastQ = keyQ;

        if (pickedSource == null) {
            handleGrab(mc, rmbPressed, shift);
        } else {
            handleInteraction(mc, rmb, lmb, ePressed, qPressed);
        }
        createLight();
    }

    private long lastTime = 0;

    private void createLight() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        SettingsData data = DataManager.INSTANCE.getSettingsData();
        long now = System.currentTimeMillis();
        if (now - lastTime >= 500 && Minecraft.getInstance().screen == null) {
            SimpleLightSource light = null;

            if (data.KEY_TO_ADD_LIGHT_POINT > 0 && org.lwjgl.glfw.GLFW.glfwGetKey(window, data.KEY_TO_ADD_LIGHT_POINT) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                light = new SimpleLightSource(LightType.POINT);
            } else if (data.KEY_TO_ADD_LIGHT_SPOT > 0 && org.lwjgl.glfw.GLFW.glfwGetKey(window, data.KEY_TO_ADD_LIGHT_SPOT) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                light = new SimpleLightSource(LightType.SPOT);
            } else if (data.KEY_TO_ADD_LIGHT_AREA_RECT > 0 && org.lwjgl.glfw.GLFW.glfwGetKey(window, data.KEY_TO_ADD_LIGHT_AREA_RECT) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                light = new SimpleLightSource(LightType.AREA_RECTANGLE);
            } else if (data.KEY_TO_ADD_LIGHT_AREA_SPHERE > 0 && org.lwjgl.glfw.GLFW.glfwGetKey(window, data.KEY_TO_ADD_LIGHT_AREA_SPHERE) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                light = new SimpleLightSource(LightType.AREA_SPHERE);
            }

            if (light != null) {
                Minecraft clientMc = Minecraft.getInstance();
                if (clientMc.player != null) {
                    Vec3 eyePos = clientMc.player.getEyePosition(1.0f);
                    Vec3 look = clientMc.player.getViewVector(1.0f);
                    light.position(eyePos.x, eyePos.y, eyePos.z);
                    light.direction(new org.joml.Vector3f((float) look.x, (float) look.y, (float) look.z));
                }

                DataManager.INSTANCE.getLightManager().getStorage().add(Minecraft.getInstance().player.getName().getString(), light);
                sendChanges(false, light);
                lastTime = now;
            }
        }
    }

    private void handleGrab(Minecraft mc, boolean rmbPressed, boolean shift) {
        if (shift && rmbPressed) {
            LightSource<?> lookedAt = findLookedAtLight(mc);
            if (lookedAt != null) {
                this.pickedSource = lookedAt;
                Vec3 eyePos = mc.player.getEyePosition(1.0f);
                Vec3 lightPos = new Vec3(lookedAt.getX(), lookedAt.getY(), lookedAt.getZ());
                this.grabDistance = eyePos.distanceTo(lightPos);
                sendHotbarMessage(mc, "Источник света захвачен (" + (lookedAt.getId() != null ? lookedAt.getId() : "Simple") + ")");
            }
        }
    }

    private void handleInteraction(Minecraft mc, boolean rmb, boolean lmb, boolean ePressed, boolean qPressed) {
        if (pickedSource == null) return;

        boolean isSingleplayer = mc.hasSingleplayerServer();
        if (!isChangingGlobal && !isSingleplayer) {
            sendHotbarMessage(mc, "Внимание, все изменения не сохранятся и их не видят другие игроки! Сервер/хост запретил изменения");
        }

        if (lmb) {
            HitResult hit = mc.hitResult;
            Vec3 targetPos;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                targetPos = hit.getLocation();
            } else {
                Vec3 eyePos = mc.player.getEyePosition(1.0f);
                Vec3 lookVec = mc.player.getViewVector(1.0f);
                targetPos = eyePos.add(lookVec.scale(grabDistance));
            }

            double lerpFactor = 0.5;
            double smoothX = pickedSource.getPrevX() + (targetPos.x - pickedSource.getPrevX()) * lerpFactor;
            double smoothY = pickedSource.getPrevY() + (targetPos.y - pickedSource.getPrevY()) * lerpFactor;
            double smoothZ = pickedSource.getPrevZ() + (targetPos.z - pickedSource.getPrevZ()) * lerpFactor;

            pickedSource.position(smoothX, smoothY, smoothZ);
            markModified(pickedSource);
        }

        if (rmb) {
            Vec3 lookVec = mc.player.getViewVector(1.0f);
            pickedSource.direction(new org.joml.Vector3f((float) lookVec.x, (float) lookVec.y, (float) lookVec.z));
            markModified(pickedSource);
        }

        if (ePressed) {
            mc.setScreen(new RedactorGui(Component.literal("Настройка источника")));
        }

        if (qPressed) {
            sendChanges(false, pickedSource);
            pickedSource = null;
            sendHotbarMessage(mc, "Источник света отпущен");
        }
    }

    public void markModified(LightSource<?> source) {
        source.setDirty(true);
        long now = System.currentTimeMillis();
        if (now - lastSyncTime >= 50) {
            sendChanges(false, source);
            lastSyncTime = now;
        }
    }

    public void handleScroll(double amount) {
        if (pickedSource != null) {
            Minecraft mc = Minecraft.getInstance();
            boolean shift = mc.options.keyShift.isDown();
            double step = shift ? 0.1 : 0.5;
            this.grabDistance = Math.max(0.1, this.grabDistance + amount * step);
        }
    }

    @Nullable
    private LightSource<?> findLookedAtLight(Minecraft mc) {
        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        LightSource<?> lookedAt = null;
        double closestRayDist = 1.5;
        double bestT = Double.MAX_VALUE;

        for (LightSource<?> light : DataManager.INSTANCE.getLightManager().getAllSources()) {
            if (!light.isActive()) continue;
            Vec3 lightPos = new Vec3(light.getX(), light.getY(), light.getZ());
            Vec3 toLight = lightPos.subtract(eyePos);
            double t = toLight.dot(lookVec);
            if (t > 0.5 && t < 64.0) {
                Vec3 closestPointOnRay = eyePos.add(lookVec.scale(t));
                double distanceToRay = closestPointOnRay.distanceTo(lightPos);
                if (distanceToRay < closestRayDist && t < bestT) {
                    bestT = t;
                    lookedAt = light;
                }
            }
        }
        return lookedAt;
    }

    private void sendHotbarMessage(Minecraft mc, String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), true);
        }
    }

    @Nullable
    public LightSource<?> getPickedSource() {
        return pickedSource;
    }

    public void answer(boolean a) {
        this.isChangingGlobal = a;
        if (a) {
            sendHotbarMessage(Minecraft.getInstance(), "Разрешение получено, изменения синхронизируются");
        }
    }

    private void askToChange() {
        AsLibNetworking.SENDER.sendToServer(new HaveRightsToChange());
    }

    public void sendChanges(boolean delete, LightSource<?> source) {
        if (source != null) {
            Minecraft mc = Minecraft.getInstance();
            boolean isSingleplayer = mc.hasSingleplayerServer();
            if (!isChangingGlobal && !isSingleplayer) {
                return;
            }

            if (delete) {
                String groupName = DataManager.INSTANCE.getLightManager().getGroupName(source);

                DataManager.INSTANCE.getLightManager().removeSource(source.getId());
                if (pickedSource == source) {
                    pickedSource = null;
                }

                if (groupName != null) {
                    AsLibNetworking.SENDER.sendToServer(new DeleteLightSourcePacket(groupName, source.getId()));
                }
            } else {
                CompoundTag nbt = new CompoundTag();
                DataManager.INSTANCE.getLightManager().save(nbt);
                AsLibNetworking.SENDER.sendToServer(new SyncLightsPacket(nbt));
            }
        }
    }
}