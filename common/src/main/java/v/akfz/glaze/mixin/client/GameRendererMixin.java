package v.akfz.glaze.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.glaze.impl.post.PostProcessRenderer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Shadow @Final Minecraft minecraft;

	@Inject(method = "render(FJZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
	public void glaze$onPostProcessRenderer(float pPartialTicks,long pNanoTime,boolean pRenderLevel,CallbackInfo ci) {
		PostProcessRenderer postProcessRenderer = PostProcessRenderer.INSTANCE;
		if (postProcessRenderer.renderGlobal) {
			postProcessRenderer.render(minecraft.getMainRenderTarget());
		}
	}
}
