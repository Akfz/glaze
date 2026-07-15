package v.akfz.glaze.network.rights;

import net.minecraft.server.level.ServerPlayer;
import v.akfz.aslib.network.AsLibNetworking;
import v.akfz.aslib.network.api.PacketHandler;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.network.rights.answer.HaveRightsToChangeHandlerAnswer;

public class HaveRightsToChangeHandler implements PacketHandler<HaveRightsToChange> {
    @Override
    public void handle(HaveRightsToChange packet, ServerPlayer player) {
        boolean allowed = DataManager.INSTANCE.getSettingsData().isAllAllowedToChangeLightSources ||
                DataManager.INSTANCE.getSettingsData().allowedPlayers.contains(player.getName().getString());

        AsLibNetworking.SENDER.sendToPlayer(player, new HaveRightsToChangeHandlerAnswer(allowed));
    }
}