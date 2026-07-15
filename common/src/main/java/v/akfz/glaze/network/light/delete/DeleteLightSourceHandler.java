package v.akfz.glaze.network.light.delete;

import net.minecraft.server.level.ServerPlayer;
import v.akfz.aslib.network.AsLibNetworking;
import v.akfz.aslib.network.api.PacketHandler;
import v.akfz.glaze.addictivelight.data.manager.DataManager;

public class DeleteLightSourceHandler implements PacketHandler<DeleteLightSourcePacket> {
    @Override
    public void handle(DeleteLightSourcePacket packet) {
        DataManager.INSTANCE.getLightManager().removeSource(packet.id);
    }

    @Override
    public void handle(DeleteLightSourcePacket packet, ServerPlayer player) {
        DataManager.INSTANCE.getLightManager().removeSource(packet.id);

        DataManager.INSTANCE.getLightManager().setClean();

        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player) {
                AsLibNetworking.SENDER.sendToPlayer(other, packet);
            }
        }
    }
}