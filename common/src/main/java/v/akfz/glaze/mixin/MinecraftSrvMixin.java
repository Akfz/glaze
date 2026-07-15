package v.akfz.glaze.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.aslib.network.AsLibNetworking;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.addictivelight.data.nbt.WorldLightStorage;
import v.akfz.glaze.network.light.update.SyncLightsPacket;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftSrvMixin {
    @Inject(method = "prepareLevels", at = @At("TAIL"))
    private void glaze$loadWorldLights(CallbackInfo ci) {
        WorldLightStorage.get((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void glaze$saveLightsOnStop(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (DataManager.INSTANCE.getLightManager().isDirty()) {
            WorldLightStorage.get(server).setDirty();
        }
    }

    @Inject(method = "tickChildren", at = @At("TAIL"))
    private void glaze$tickServerLights(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (DataManager.INSTANCE.getLightManager().isDirty()) {
            WorldLightStorage.get(server).setDirty();

            CompoundTag nbt = new CompoundTag();
            DataManager.INSTANCE.getLightManager().save(nbt);
            SyncLightsPacket packet = new SyncLightsPacket(nbt);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                AsLibNetworking.SENDER.sendToPlayer(player, packet);
            }

            DataManager.INSTANCE.getLightManager().setClean();
        }
    }
}