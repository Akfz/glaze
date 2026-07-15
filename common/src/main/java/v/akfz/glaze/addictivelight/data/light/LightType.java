package v.akfz.glaze.addictivelight.data.light;

import lombok.Getter;

@Getter
public enum LightType {
    POINT(1),
    SPOT(2),
    AREA_RECTANGLE(3),
    AREA_SPHERE(4),
    CUSTOM(5);

    private final int id;

    LightType(int id) {
        this.id = id;
    }
}