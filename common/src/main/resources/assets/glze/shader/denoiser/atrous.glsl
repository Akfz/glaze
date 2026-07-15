#version 330 core

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uLightTex;
uniform sampler2D uDepth;
uniform sampler2D uNormal;

uniform int uStepSize;
uniform float uDepthThreshold;
uniform float uNormalThreshold;
uniform float uLumaThreshold;

uniform float uNear;
uniform float uFar;

const float weights[5] = float[](1.0/16.0, 4.0/16.0, 6.0/16.0, 4.0/16.0, 1.0/16.0);

float getLinearDepth(float depth) {
    float z_ndc = depth * 2.0 - 1.0;
    return (2.0 * uNear * uFar) / (uFar + uNear - z_ndc * (uFar - uNear));
}

float getLuminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    float centerDepth = texture(uDepth, vTexCoord).r;
    if (centerDepth >= 1.0) {
        FragColor = texture(uLightTex, vTexCoord);
        return;
    }

    vec3 centerNormal = normalize(texture(uNormal, vTexCoord).xyz * 2.0 - 1.0);
    float centerLinearDepth = getLinearDepth(centerDepth);
    vec4 centerLight = texture(uLightTex, vTexCoord);
    float centerLuma = getLuminance(centerLight.rgb);

    vec4 sumLight = vec4(0.0);
    float sumWeight = 0.0;

    vec2 texelSize = 1.0 / vec2(textureSize(uLightTex, 0));

    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            vec2 offset = vec2(float(x), float(y)) * float(uStepSize) * texelSize;
            vec2 sampleUV = vTexCoord + offset;

            float sampleDepth = texture(uDepth, sampleUV).r;
            vec3 sampleNormal = normalize(texture(uNormal, sampleUV).xyz * 2.0 - 1.0);
            float sampleLinearDepth = getLinearDepth(sampleDepth);
            vec4 sampleLight = texture(uLightTex, sampleUV);
            float sampleLuma = getLuminance(sampleLight.rgb);

            float spatialWeight = weights[x + 2] * weights[y + 2];

            float depthDiff = abs(centerLinearDepth - sampleLinearDepth);
            float depthWeight = exp(-depthDiff / (centerLinearDepth * uDepthThreshold + 1e-4));

            float normalWeight = pow(max(0.0, dot(centerNormal, sampleNormal)), uNormalThreshold);

            float lumaDiff = abs(centerLuma - sampleLuma);
            float lumaWeight = exp(-lumaDiff / (uLumaThreshold + 1e-4));

            float weight = spatialWeight * depthWeight * normalWeight * lumaWeight;
            sumLight += sampleLight * weight;
            sumWeight += weight;
        }
    }

    FragColor = sumLight / max(sumWeight, 0.0001);
}