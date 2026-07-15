package v.akfz.glaze.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.glaze.addictivelight.render.AddictiveLight;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"))
    private void glaze$onScroll(long windowPointer, double xoffset, double yoffset, CallbackInfo ci) {
        if (Minecraft.getInstance().screen == null) {
            if (AddictiveLight.INSTANCE.isEnabled()) AddictiveLight.INSTANCE.getRedactor().handleScroll(yoffset);
        }
    }
}