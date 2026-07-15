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

void main() {
    vec4 current = texture(uCurrentTex, vTexCoord);
    vec4 history = texture(uHistoryTex, vTexCoord);

    vec3 sum = vec3(0.0);
    vec3 sum2 = vec3(0.0);
    vec2 texelSize = 1.0 / vec2(textureSize(uCurrentTex, 0));

    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec3 neighbor = texture(uCurrentTex, vTexCoord + vec2(x, y) * texelSize).rgb;
            sum += neighbor;
            sum2 += neighbor * neighbor;
        }
    }

    vec3 mean = sum / 9.0;
    vec3 stdDev = sqrt(max(vec3(0.0), (sum2 / 9.0) - (mean * mean)));

    vec3 minColor = mean - uVarianceScale * stdDev;
    vec3 maxColor = mean + uVarianceScale * stdDev;
    vec3 clampedHistory = clamp(history.rgb, minColor, maxColor);

    vec3 currentMap = current.rgb / (1.0 + getLuminance(current.rgb));
    vec3 historyMap = clampedHistory / (1.0 + getLuminance(clampedHistory));

    vec3 blended = mix(historyMap, currentMap, uBlendFactor);
    blended = blended / (1.0 - getLuminance(blended) + 1e-6);

    FragColor = vec4(max(vec3(0.0), blended), current.a);
}