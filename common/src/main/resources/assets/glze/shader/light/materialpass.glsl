#version 330 core

in vec2 vTexCoord;

layout (location = 0) out vec4 outAlbedo;
layout (location = 1) out vec3 outNormal;
layout (location = 2) out vec4 outPBR;

uniform sampler2D uDepth;
uniform sampler2D uAlbedo;
uniform sampler2D uVoxelGrid;
uniform sampler2D uMaterialBuffer;

uniform mat4 uInvProj;
uniform mat4 uInvView;
uniform vec3 uCamPos;

uniform vec3 uGridStart;
uniform int uGridXZ;
uniform int uGridY;

vec3 getCameraRelativeWorldSpacePos(vec2 uv, float depth) {
    vec4 ndc = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 vPos = uInvProj * ndc;
    vPos /= vPos.w;
    return (uInvView * vPos).xyz;
}

int getMaterialID(ivec3 p) {
    if (p.x < 0 || p.x >= uGridXZ || p.y < 0 || p.y >= uGridY || p.z < 0 || p.z >= uGridXZ)
        return -1;
    int tx = p.x;
    int ty = p.y * uGridXZ + p.z;
    float val = texelFetch(uVoxelGrid, ivec2(tx, ty), 0).r;
    return int(val + 0.5) - 1;
}

void main() {
    float depth = texture(uDepth, vTexCoord).r;

    if (depth >= 1.0) {
        outAlbedo = vec4(pow(texture(uAlbedo, vTexCoord).rgb, vec3(2.2)), 1.0);
        outNormal = vec3(0.0, 0.0, 1.0);
        outPBR = vec4(0.8, 0.0, 0.0, 0.04);
        return;
    }

    vec3 pixelWorldPos = getCameraRelativeWorldSpacePos(vTexCoord, depth);

    vec2 texelSize = 1.0 / vec2(textureSize(uDepth, 0));
    float dRight = textureOffset(uDepth, vTexCoord, ivec2(1, 0)).r;
    float dUp    = textureOffset(uDepth, vTexCoord, ivec2(0, 1)).r;
    if (dRight >= 1.0) dRight = depth;
    if (dUp >= 1.0)    dUp = depth;

    vec3 pRight = getCameraRelativeWorldSpacePos(vTexCoord + vec2(texelSize.x, 0.0), dRight);
    vec3 pUp    = getCameraRelativeWorldSpacePos(vTexCoord + vec2(0.0, texelSize.y), dUp);

    vec3 N = normalize(cross(pRight - pixelWorldPos, pUp - pixelWorldPos));
    if (dot(N, pixelWorldPos) > 0.0) N = -N;

    vec3 albedo = pow(texture(uAlbedo, vTexCoord).rgb, vec3(2.2));

    float rough = 0.8;
    float metal = 0.0;
    float emissive = 0.0;
    float spec = 0.04;

    vec3 absWorldPos = pixelWorldPos + uCamPos;
    vec3 offsetWorldPos = absWorldPos - N * 0.02;
    ivec3 gridCoords = ivec3(floor(offsetWorldPos - uGridStart));

    int matID = getMaterialID(gridCoords);
    if (matID >= 0) {
        vec4 pbr = texelFetch(uMaterialBuffer, ivec2(matID, 0), 0);
        vec4 tint = texelFetch(uMaterialBuffer, ivec2(matID, 1), 0);
        rough = pbr.r;
        metal = pbr.g;
        emissive = pbr.b;
        spec = pbr.a;
        albedo *= pow(tint.rgb, vec3(2.2));
    }

    outAlbedo = vec4(albedo, 1.0);
    outNormal = N * 0.5 + 0.5;
    outPBR = vec4(rough, metal, emissive / 10.0, spec);
}