package smth.template_mod.mixin;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TestMixin {

    @Inject(at = @At("HEAD"), method = "init()V")
    private void onInit(CallbackInfo info) {
        System.out.println("=============================================");
        System.out.println("Template Mod!");
        System.out.println("=============================================");
    }
}