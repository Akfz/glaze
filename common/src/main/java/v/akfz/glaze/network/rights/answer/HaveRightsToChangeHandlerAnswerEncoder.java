package v.akfz.glaze.network.rights.answer;

import net.minecraft.network.FriendlyByteBuf;
import v.akfz.aslib.network.api.PacketEncoder;

public class HaveRightsToChangeHandlerAnswerEncoder implements PacketEncoder<HaveRightsToChangeHandlerAnswer> {
    @Override
    public void encode(HaveRightsToChangeHandlerAnswer haveRightsToChangeHandlerAnswer, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeBoolean(haveRightsToChangeHandlerAnswer.answer);
    }
}
