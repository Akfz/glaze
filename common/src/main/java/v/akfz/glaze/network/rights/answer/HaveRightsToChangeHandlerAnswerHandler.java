package v.akfz.glaze.network.rights.answer;

import v.akfz.aslib.network.api.PacketHandler;
import v.akfz.glaze.addictivelight.render.AddictiveLight;

public class HaveRightsToChangeHandlerAnswerHandler implements PacketHandler<HaveRightsToChangeHandlerAnswer> {
    @Override
    public void handle(HaveRightsToChangeHandlerAnswer packet) {
        AddictiveLight.INSTANCE.getRedactor().answer(packet.answer);
    }
}
