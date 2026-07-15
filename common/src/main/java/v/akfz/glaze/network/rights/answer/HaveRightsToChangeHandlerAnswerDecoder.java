package v.akfz.glaze.network.rights.answer;

import net.minecraft.network.FriendlyByteBuf;
import v.akfz.aslib.network.api.PacketDecoder;

public class HaveRightsToChangeHandlerAnswerDecoder implements PacketDecoder<HaveRightsToChangeHandlerAnswer> {
    @Override
    public HaveRightsToChangeHandlerAnswer decode(FriendlyByteBuf friendlyByteBuf) {
        return new HaveRightsToChangeHandlerAnswer(friendlyByteBuf.readBoolean());
    }
}
