package v.akfz.glaze.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.aslib.network.AsLibNetworking;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.network.light.update.SyncLightsPacket;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void glaze$syncLightsOnJoin(Connection connection, ServerPlayer player, CallbackInfo ci) {
        CompoundTag nbt = new CompoundTag();
        DataManager.INSTANCE.getLightManager().save(nbt);
        AsLibNetworking.SENDER.sendToPlayer(player, new SyncLightsPacket(nbt));
    }
}