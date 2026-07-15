package v.akfz.glaze.addictivelight.localutil;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.Map;

public class ShadowBufferSourceBridge extends MultiBufferSource.BufferSource {
    public ShadowBufferSourceBridge(BufferBuilder fallback, Map<RenderType, BufferBuilder> fixedBuffers) {
        super(fallback, fixedBuffers);
    }
}