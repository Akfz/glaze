package v.akfz.glaze.network.light.update;

import net.minecraft.server.level.ServerPlayer;
import v.akfz.aslib.network.AsLibNetworking;
import v.akfz.aslib.network.api.PacketHandler;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.addictivelight.data.nbt.WorldLightStorage;

public class SyncLightsHandler implements PacketHandler<SyncLightsPacket> {
    @Override
    public void handle(SyncLightsPacket packet) {
        // Выполняется на стороне КЛИЕНТА
        DataManager.INSTANCE.getLightManager().load(packet.getNbt());
    }

    @Override
    public void handle(SyncLightsPacket packet, ServerPlayer player) {
        DataManager.INSTANCE.getLightManager().load(packet.getNbt());
        DataManager.INSTANCE.getLightManager().setClean();
        WorldLightStorage.get(player.server).setDirty();

        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player) {
                AsLibNetworking.SENDER.sendToPlayer(other, packet);
            }
        }
    }
}