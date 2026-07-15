package v.akfz.glaze.network.light.update;

import net.minecraft.nbt.CompoundTag;
import v.akfz.aslib.network.annotation.NetworkPacket;
import v.akfz.aslib.network.api.AbstractPacket;

@NetworkPacket("glaze:sync_lights")
public class SyncLightsPacket extends AbstractPacket {
    private final CompoundTag nbt;

    public SyncLightsPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public CompoundTag getNbt() {
        return this.nbt;
    }
}