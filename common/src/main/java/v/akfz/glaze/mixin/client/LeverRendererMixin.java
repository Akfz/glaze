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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.glaze.impl.post.PostProcessRenderer;

@Mixin(LevelRenderer.class)
public class LeverRendererMixin {
	@Shadow @Final private Minecraft minecraft;

	@Inject(method = "renderLevel", at= @At("TAIL"))
	public void onEndOfRenderLevel(PoseStack pPoseStack,float pPartialTick,long pFinishNanoTime,boolean pRenderBlockOutline,Camera pCamera,GameRenderer pGameRenderer,LightTexture pLightTexture,Matrix4f pProjectionMatrix,CallbackInfo ci) {
		PostProcessRenderer postProcessRenderer = PostProcessRenderer.INSTANCE;
		if (!postProcessRenderer.renderGlobal) {
			postProcessRenderer.render(minecraft.getMainRenderTarget());
		}
	}
}
