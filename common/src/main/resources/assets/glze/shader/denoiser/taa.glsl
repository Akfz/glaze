#version 330 core

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uCurrentTex;
uniform sampler2D uHistoryTex;
uniform float uBlendFactor;
uniform float uVarianceScale;

float getLuminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

vec3 tonemap(vec3 color) {
    return color / (1.0 + max(0.0, getLuminance(color)));
}

vec3 untonemap(vec3 color) {
    return color / max(1e-5, 1.0 - getLuminance(color));
}

void main() {
    vec4 current = texture(uCurrentTex, vTexCoord);
    vec4 history = texture(uHistoryTex, vTexCoord);

    vec3 sum = vec3(0.0);
    vec3 sum2 = vec3(0.0);
    vec2 texelSize = 1.0 / vec2(textureSize(uCurrentTex, 0));

    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec3 neighbor = texture(uCurrentTex, vTexCoord + vec2(x, y) * texelSize).rgb;
            vec3 neighborMap = tonemap(neighbor);
            sum += neighborMap;
            sum2 += neighborMap * neighborMap;
        }
    }

    vec3 mean = sum / 9.0;
    vec3 stdDev = sqrt(max(vec3(0.0), (sum2 / 9.0) - (mean * mean)));

    vec3 minColor = mean - uVarianceScale * stdDev;
    vec3 maxColor = mean + uVarianceScale * stdDev;

    vec3 historyMap = tonemap(history.rgb);
    vec3 clampedHistoryMap = clamp(historyMap, minColor, maxColor);

    vec3 currentMap = tonemap(current.rgb);
    vec3 blendedMap = mix(clampedHistoryMap, currentMap, uBlendFactor);
    vec3 blended = untonemap(blendedMap);

    FragColor = vec4(max(vec3(0.0), blended), current.a);
}