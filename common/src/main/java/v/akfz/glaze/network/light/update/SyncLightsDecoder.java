package v.akfz.glaze.network.light.update;

import net.minecraft.network.FriendlyByteBuf;
import v.akfz.aslib.network.api.PacketDecoder;

public class SyncLightsDecoder implements PacketDecoder<SyncLightsPacket> {
    @Override
    public SyncLightsPacket decode(FriendlyByteBuf buf) {
        return new SyncLightsPacket(buf.readNbt());
    }
}