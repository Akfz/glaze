package v.akfz.glaze.network.light.delete;

import net.minecraft.network.FriendlyByteBuf;
import v.akfz.aslib.network.api.PacketDecoder;

public class DeleteLightSourceDecoder implements PacketDecoder<DeleteLightSourcePacket> {
    @Override
    public DeleteLightSourcePacket decode(FriendlyByteBuf buf) {
        return new DeleteLightSourcePacket(
                buf.readUtf(),
                buf.readUUID()
        );
    }
}