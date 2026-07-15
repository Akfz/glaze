package v.akfz.glaze.network.rights.answer;

import v.akfz.aslib.network.annotation.NetworkPacket;
import v.akfz.aslib.network.api.AbstractPacket;

@NetworkPacket("glaze:ask_to_change_answer")
public class HaveRightsToChangeHandlerAnswer extends AbstractPacket {
    public boolean answer;
    public HaveRightsToChangeHandlerAnswer(boolean answer) {
        this.answer = answer;
    }
}
