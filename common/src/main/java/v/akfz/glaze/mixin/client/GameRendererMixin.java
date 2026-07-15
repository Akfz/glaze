package v.akfz.glaze.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.glaze.module.RenderModuleManager;
import v.akfz.glaze.pprmodule.PostProcessRenderer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final Minecraft minecraft;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
    public void glaze$onPPRenderer(float p_109094_, long p_109095_, boolean p_109096_, CallbackInfo ci) {

        PostProcessRenderer postProcessRenderer = RenderModuleManager.INSTANCE.getByID("LS-PProcess");
        if (postProcessRenderer != null && postProcessRenderer.renderGlobal) {
            postProcessRenderer.render(minecraft.getMainRenderTarget());
        }
    }
}
