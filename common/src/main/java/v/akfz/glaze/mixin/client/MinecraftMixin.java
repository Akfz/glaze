package v.akfz.glaze.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.addictivelight.render.AddictiveLight;
import v.akfz.glaze.pprmodule.PostProcessRenderer;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "close", at = @At("HEAD"))
    public void glze$close(CallbackInfo ci) {
        AddictiveLight.INSTANCE.cleanup();
        PostProcessRenderer.INSTANCE.cleanup(true);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void glaze$update(CallbackInfo ci) {
        Minecraft client = (Minecraft)(Object)this;
        if (client.screen instanceof TitleScreen) {
            DataManager.INSTANCE.getLightManager().clean();
            AddictiveLight.INSTANCE.cleanup();
        }
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void onHandleInput(CallbackInfo ci) {
        if (AddictiveLight.INSTANCE.isEnabled() && AddictiveLight.INSTANCE.getRedactor().getPickedSource() != null) {
            Minecraft client = (Minecraft)(Object)this;
            Options options = client.options;
            while (options.keyAttack.consumeClick());
            while (options.keyUse.consumeClick());
            while (options.keyDrop.consumeClick());
            while (options.keyInventory.consumeClick());
            ci.cancel();
        }
    }
}
