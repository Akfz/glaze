#version 330 core
in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uVanillaTex;
uniform sampler2D uHdrTex;

uniform float uExposure;
uniform float uContrast;
uniform float uSaturation;

vec3 ACESTonemap(vec3 color) {
    float a = 2.51f;
    float b = 0.03f;
    float c = 2.43f;
    float d = 0.59f;
    float e = 0.14f;
    return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0f, 1.0f);
}

vec3 applyContrast(vec3 color, float contrast) {
    return (color - 0.5f) * contrast + 0.5f;
}

vec3 applySaturation(vec3 color, float saturation) {
    float luma = dot(color, vec3(0.2126f, 0.7152f, 0.0722f));
    return mix(vec3(luma), color, saturation);
}

void main() {
    vec4 vanilla = texture(uVanillaTex, vTexCoord);
    vec4 hdrLight = texture(uHdrTex, vTexCoord);

    vec3 rawColor = vanilla.rgb + hdrLight.rgb;
    vec3 exposedColor = rawColor * uExposure;
    vec3 tonemapped = ACESTonemap(exposedColor);

    vec3 finalColor = applyContrast(tonemapped, uContrast);
    finalColor = applySaturation(finalColor, uSaturation);

    FragColor = vec4(finalColor, vanilla.a);
}