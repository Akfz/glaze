#version 330 core
in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2DArray uShadowMapArray;
uniform int uLayer;

void main() {
    ivec3 texSize = textureSize(uShadowMapArray, 0);
    ivec3 coords = ivec3(int(vTexCoord.x * texSize.x), int(vTexCoord.y * texSize.y), uLayer);
    float depth = texelFetch(uShadowMapArray, coords, 0).r;

    float near = 0.1;
    float far = 32.0;
    float z_ndc = depth * 2.0 - 1.0;
    float linearDepth = (2.0 * near) / (far + near - z_ndc * (far - near));

    FragColor = vec4(vec3(linearDepth), 1.0);
}