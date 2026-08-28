#version 330 core

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uTexture;
uniform sampler2D uDepth;

uniform float uNear;
uniform float uFar;

float getLinearDepth(vec2 uv) {
    float depth = texture(uDepth, uv).r;
    float z_ndc = depth * 2.0 - 1.0;
    return (2.0 * uNear * uFar) / (uFar + uNear - z_ndc * (uFar - uNear));
}

void main() {
    vec4 sceneColor = texture(uTexture, vTexCoord);

    float dist = getLinearDepth(vTexCoord);

    float fogStart = 5.0;
    float fogEnd = 80.0;

    float fogFactor = clamp((dist - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
    vec3 fogColor = vec3(0.0, 0.4, 0.8);
    vec3 finalColor = mix(sceneColor.rgb, fogColor, fogFactor);

    FragColor = vec4(finalColor, sceneColor.a);
}