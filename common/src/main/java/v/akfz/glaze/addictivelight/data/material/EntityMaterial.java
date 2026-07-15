package v.akfz.glaze.addictivelight.data.material;

import lombok.Getter;
import lombok.Setter;
import v.akfz.aslib.util.json.JsonData;

@Getter
@Setter
public class EntityMaterial implements JsonData {
    private float opacity = 1.0f;
    private boolean castShadows = true;

    private float tintR = 0.0f;
    private float tintG = 0.0f;
    private float tintB = 0f;

    public EntityMaterial() {}

    public EntityMaterial(float opacity, float tintR, float tintG, float tintB) {
        this.opacity = opacity;
        this.tintR = tintR;
        this.tintG = tintG;
        this.tintB = tintB;
    }

    public EntityMaterial opacity(float opacity) { this.opacity = opacity; return this; }
    public EntityMaterial castShadows(boolean castShadows) { this.castShadows = castShadows; return this; }
    public EntityMaterial tintR(float tintR) { this.tintR = tintR; return this; }
    public EntityMaterial tintG(float tintG) { this.tintG = tintG; return this; }
    public EntityMaterial tintB(float tintB) { this.tintB = tintB; return this; }
}