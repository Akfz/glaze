package v.akfz.glaze.network.light.delete;

import v.akfz.aslib.network.annotation.NetworkPacket;
import v.akfz.aslib.network.api.AbstractPacket;

import java.util.UUID;

@NetworkPacket("glaze:delete_light")
public class DeleteLightSourcePacket extends AbstractPacket {
    public String group;
    public UUID id;

    public DeleteLightSourcePacket(String group, UUID id) {
        this.group = group;
        this.id = id;
    }
}
