package v.akfz.glaze.network.light.delete;

import net.minecraft.network.FriendlyByteBuf;
import v.akfz.aslib.network.api.PacketEncoder;

public class DeleteLightSourceEncoder implements PacketEncoder<DeleteLightSourcePacket> {
    @Override
    public void encode(DeleteLightSourcePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.group);
        buf.writeUUID(packet.id);
    }
}