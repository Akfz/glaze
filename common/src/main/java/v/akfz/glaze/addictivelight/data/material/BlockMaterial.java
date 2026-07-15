package v.akfz.glaze.addictivelight.data.material;

import lombok.Getter;
import lombok.Setter;
import v.akfz.aslib.util.json.JsonData;
import java.util.List;

@Getter
@Setter
public class BlockMaterial implements JsonData {
    private int runtimeID = -1;

    private float roughness = 0.8f;
    private float metallic = 0.0f;
    private float emissive = 0.0f;
    private float opacity = 1.0f;
    private boolean castShadows = true;

    private float subsurface = 0.0f;
    private float specular = 0.5f;

    private float tintR = 1.0f;
    private float tintG = 1.0f;
    private float tintB = 1.0f;

    public BlockMaterial() {}

    public BlockMaterial(float roughness, float metallic, float emissive, float opacity) {
        this.roughness = roughness;
        this.metallic = metallic;
        this.emissive = emissive;
        this.opacity = opacity;
    }

    public BlockMaterial roughness(float roughness) { this.roughness = roughness; return this; }
    public BlockMaterial metallic(float metallic) { this.metallic = metallic; return this; }
    public BlockMaterial emissive(float emissive) { this.emissive = emissive; return this; }
    public BlockMaterial opacity(float opacity) { this.opacity = opacity; return this; }
    public BlockMaterial subsurface(float sss) { this.subsurface = sss; return this; }
    public BlockMaterial specular(float specular) { this.specular = specular; return this; }
    public BlockMaterial castShadows(boolean castShadows) { this.castShadows = castShadows; return this; }
    public BlockMaterial tintR(float tintR) { this.tintR = tintR; return this; }
    public BlockMaterial tintG(float tintG) { this.tintG = tintG; return this; }
    public BlockMaterial tintB(float tintB) { this.tintB = tintB; return this; }
}