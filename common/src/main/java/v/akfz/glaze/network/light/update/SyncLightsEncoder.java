package v.akfz.glaze.network.light.update;

import net.minecraft.network.FriendlyByteBuf;
import v.akfz.aslib.network.api.PacketEncoder;

public class SyncLightsEncoder implements PacketEncoder<SyncLightsPacket> {
    @Override
    public void encode(SyncLightsPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.getNbt());
    }
}