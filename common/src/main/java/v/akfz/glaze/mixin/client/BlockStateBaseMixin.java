package v.akfz.glaze.mixin.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import v.akfz.glaze.addictivelight.data.manager.DataManager;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow public abstract Block getBlock();

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void glaze$onGetLightEmission(CallbackInfoReturnable<Integer> cir) {
        Block block = this.getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

        if (blockId != null) {
            String blockIdStr = blockId.toString();

            if (DataManager.INSTANCE.getSettingsData().disabledLightBlocks.contains(blockIdStr)) {
                cir.setReturnValue(0);
            }
        }
    }
}