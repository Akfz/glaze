package v.akfz.glaze.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.glaze.addictivelight.data.SettingsData;
import v.akfz.glaze.addictivelight.data.light.LightDebugRenderer;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.addictivelight.gui.MainGui;
import v.akfz.glaze.addictivelight.render.AddictiveLight;
import v.akfz.glaze.module.RenderModuleManager;
import v.akfz.glaze.pprmodule.PostProcessRenderer;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private long glaze$lastTime = System.currentTimeMillis();

    @Unique
    private Matrix4f glaze$capturedViewMatrix;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", ordinal = 0))
    private void glaze$captureMatrix(PoseStack poseStack, float partialTick, long finishTimeNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        this.glaze$capturedViewMatrix = new Matrix4f(poseStack.last().pose());
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V"))
    private void glaze$onRenderDebugWorld(PoseStack poseStack, float partialTick, long finishTimeNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        SettingsData data = DataManager.INSTANCE.getSettingsData();
        if (data.debug && data.debugLight) {
            DataManager.INSTANCE.getLightManager().getAllSources().forEach(source ->
                    LightDebugRenderer.render(poseStack, source, data.debugLightInfo, data.debugLightFrustum)
            );
        }
    }

    @Inject(method = "renderLevel", at= @At("TAIL"))
    public void onEndOfRenderLevel(PoseStack p_109600_, float p_109601_, long p_109602_, boolean p_109603_, Camera p_109604_, GameRenderer p_109605_, LightTexture p_109606_, Matrix4f p_254120_, CallbackInfo ci){
        if (System.currentTimeMillis() - glaze$lastTime >= 5000) {
            RenderModuleManager.INSTANCE.updateData();
            glaze$lastTime = System.currentTimeMillis();
        }
        long window = minecraft.getWindow().getWindow();
        SettingsData data = DataManager.INSTANCE.getSettingsData();

        if (minecraft.screen == null && data.KEY_TO_OPEN_SETTINGS > 0 && org.lwjgl.glfw.GLFW.glfwGetKey(window, data.KEY_TO_OPEN_SETTINGS) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            minecraft.setScreen(new MainGui());
        }

        AddictiveLight alr = RenderModuleManager.INSTANCE.getByID("LS-Light");
        if (alr != null) {
            alr.render(glaze$capturedViewMatrix, p_254120_);
        }

        PostProcessRenderer postProcessRenderer = RenderModuleManager.INSTANCE.getByID("LS-PProcess");
        if (postProcessRenderer != null && !postProcessRenderer.renderGlobal) {
            postProcessRenderer.render(minecraft.getMainRenderTarget());
        }
    }
}