#version 330 core

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uDepth;
uniform sampler2D uAlbedo;
uniform sampler2D uNormal;
uniform sampler2D uPBR;

uniform sampler2DArrayShadow uBlockShadowMapArray;
uniform sampler2DArrayShadow uBlockEntityShadowMapArray;
uniform sampler2DArrayShadow uEntityShadowMapArray;
uniform sampler2DArrayShadow uParticleShadowMapArray;

uniform sampler2DArray uEntityShadowColorArray;

uniform mat4 uInvProj;
uniform mat4 uInvView;
uniform float uNear;
uniform float uFar;
uniform vec3 uCamPos;
uniform int uFrames;

uniform float uBlockShadowSize;
uniform float uBlockEntityShadowSize;
uniform float uEntityShadowSize;
uniform float uParticleShadowSize;

layout (std140) uniform ShadowMatrixBuffer {
    mat4 uLightSpaceMatrices[256];
};

struct Light {
    vec4 posRad;
    vec4 colorInt;
    vec4 dirType;
    vec4 spotParams;
    vec4 areaParams;
    vec4 shadowParams;
    vec4 extraParams;
    vec4 lastParams;
    vec4 newParams1;
    vec4 newParams2;
};

layout (std140) uniform LightBuffer {
    Light uLights[256];
    vec4 uParams;
};

const float PI = 3.14159265359;

float interleavedGradientNoise(vec2 position) {
    return fract(52.9829189 * fract(dot(position, vec2(0.06711056, 0.00583715))));
}

vec3 getCameraRelativeWorldSpacePos(vec2 uv, float depth) {
    vec4 ndc = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 vPos = uInvProj * ndc;
    vPos /= vPos.w;
    return (uInvView * vPos).xyz;
}

int getCubemapFaceIndex(vec3 dir) {
    float absX = abs(dir.x);
    float absY = abs(dir.y);
    float absZ = abs(dir.z);
    if (absX >= absY && absX >= absZ) return (dir.x >= 0.0) ? 0 : 1;
    else if (absY >= absX && absY >= absZ) return (dir.y >= 0.0) ? 2 : 3;
    else return (dir.z >= 0.0) ? 4 : 5;
}

float sampleShadowMapSoft(sampler2DArrayShadow shadowMap, vec3 worldPos, vec3 N, int layerIndex, float baseBias, float softnessPixels, float shadowSize, int frameSeed) {
    float normalOffset = (N == vec3(0.0)) ? 0.0 : max(baseBias * 15.0, 0.04);
    float depthBias = (N == vec3(0.0)) ? baseBias : max(baseBias * 2.0, 0.004);

    vec3 biasedWorldPos = worldPos + N * normalOffset;

    vec4 lightSpacePos = uLightSpaceMatrices[layerIndex] * vec4(biasedWorldPos, 1.0);

    if (lightSpacePos.w <= 0.0)
        return 1.0;

    vec3 projCoords = lightSpacePos.xyz / lightSpacePos.w;
    vec3 texCoords = projCoords * 0.5 + 0.5;

    if (any(lessThan(texCoords, vec3(0.0))) || any(greaterThan(texCoords, vec3(1.0))))
        return 1.0;

    float softnessUV = (softnessPixels * 3.5) / 1024.0;
    if (softnessUV <= 0.0001) {
        return texture(shadowMap, vec4(texCoords.xy, float(layerIndex), texCoords.z - depthBias));
    }

    float noise = interleavedGradientNoise(gl_FragCoord.xy + float(frameSeed));
    float phi = noise * 2.0 * PI;

    float shadow = 0.0;
    float totalWeight = 0.0;
    int samples = clamp(int(uParams.y + 0.5), 1, 16);

    for (int i = 0; i < 16; i++) {
        if (i >= samples) break;

        float goldenAngle = 2.4;
        float r = sqrt(float(i) + 0.5) / sqrt(float(samples));
        float theta = float(i) * goldenAngle + phi;
        vec2 offset = vec2(cos(theta), sin(theta)) * r * softnessUV;

        float weight = exp(-r * r * 1.5);

        float s = texture(shadowMap, vec4(texCoords.xy + offset, float(layerIndex), texCoords.z - depthBias));
        shadow += s * weight;
        totalWeight += weight;
    }
    return shadow / max(totalWeight, 0.001);
}

float henyeyGreenstein(float cosTheta, float g) {
    float g2 = g * g;
    float denom = 1.0 + g2 - 2.0 * g * cosTheta;
    return (1.0 - g2) / max(4.0 * PI * pow(denom, 1.5), 0.001);
}

vec3 F_Schlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

float D_GGX(float NoH, float roughness) {
    float a = roughness * roughness, a2 = a * a;
    float NoH2 = NoH * NoH;
    float denom = (NoH2 * (a2 - 1.0) + 1.0);
    return a2 / (PI * denom * denom + 1e-7);
}

float G_SchlickGGX(float NoV, float NoL, float roughness) {
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;
    float g1v = NoV / (NoV * (1.0 - k) + k);
    float g1l = NoL / (NoL * (1.0 - k) + k);
    return g1v * g1l;
}

vec3 calculatePBR(vec3 N, vec3 V, vec3 L, vec3 albedo, float roughness, float metallic, float specular) {
    vec3 H = normalize(V + L);
    float NoV = max(dot(N, V), 0.0001);
    float NoL = max(dot(N, L), 0.0);
    float NoH = max(dot(N, H), 0.0);
    float VoH = max(dot(V, H), 0.0);

    vec3 F0 = mix(vec3(0.08 * specular), albedo, metallic);
    vec3 F = F_Schlick(VoH, F0);

    float D = D_GGX(NoH, roughness);
    float G = G_SchlickGGX(NoV, NoL, roughness);

    vec3 spec = (D * G * F) / (4.0 * NoV + 0.0001);

    vec3 kS = F;
    vec3 kD = (vec3(1.0) - kS) * (1.0 - metallic);

    float fd90 = 0.5 + 2.0 * VoH * VoH * roughness;
    float lightScatter = 1.0 + (fd90 - 1.0) * pow(1.0 - NoL, 5.0);
    float viewScatter  = 1.0 + (fd90 - 1.0) * pow(1.0 - NoV, 5.0);
    vec3 diff = kD * albedo * (lightScatter * viewScatter) / PI;

    return (diff + spec) * NoL;
}

void main() {
    float depth = texture(uDepth, vTexCoord).r;
    bool isSky = (depth >= 1.0);

    vec3 pixelWorldPos = getCameraRelativeWorldSpacePos(vTexCoord, isSky ? 1.0 : depth);
    vec3 lightAccum = vec3(0.0);

    vec3 albedo_pbr = texture(uAlbedo, vTexCoord).rgb;
    vec3 N = normalize(texture(uNormal, vTexCoord).xyz * 2.0 - 1.0);

    vec4 pbr = texture(uPBR, vTexCoord);
    float rough = max(pbr.r, 0.04);
    float metal = pbr.g;
    float emissive = pbr.b * 10.0;
    float specularVal = pbr.a;

    vec3 emissiveColor = albedo_pbr;

    vec3 V = normalize(-pixelWorldPos);
    float viewDist = isSky ? uFar : length(pixelWorldPos);

    int activeCount = int(uParams.x + 0.5);
    int frameSeed = uFrames;

    float ditherBase = interleavedGradientNoise(gl_FragCoord.xy + float(frameSeed) * 7.19);

    for (int i = 0; i < activeCount; i++) {
        if (uLights[i].newParams2.z < 0.5) continue;

        vec3 lightPos = uLights[i].posRad.xyz;
        float radius = uLights[i].posRad.w;
        float invRadius = 1.0 / radius;
        vec3 lightCol = uLights[i].colorInt.rgb;
        float intensity = uLights[i].colorInt.w;

        vec3 toLight = lightPos - pixelWorldPos;
        float dist = length(toLight);
        vec3 L = normalize(toLight);
        float NoL = isSky ? 0.0 : max(dot(N, L), 0.0);

        if (dist < 0.8 && emissive > 0.01) {
            emissiveColor = max(emissiveColor, lightCol);
        }

        int type = int(uLights[i].dirType.w + 0.5);

        float linear = uLights[i].spotParams.z;
        float quadratic = uLights[i].spotParams.w;
        float falloffExp = uLights[i].newParams1.z;

        float distToLightCamera = length(lightPos);
        vec3 lightDirCamera = lightPos / max(distToLightCamera, 0.001);
        float cosAngle = dot(-V, lightDirCamera);
        float cosJittered = clamp(cosAngle + (ditherBase - 0.5) * 0.01, 0.0, 1.0);

        float gVal = clamp(uLights[i].extraParams.z, -0.99, 0.99);

        bool isMieGEnabled = (abs(gVal) > 0.01);

        float flareShadow = 1.0;
        if (uLights[i].areaParams.z > 0.5) {
            int volLayer = int(uLights[i].shadowParams.x + 0.5);
            bool volOmni = (type == 0 || type == 3 || type == 4);
            if (volOmni) volLayer += getCubemapFaceIndex(-lightPos);

            float baseBias = uLights[i].shadowParams.z;
            float volBias = max(baseBias * 2.0, 0.005);

            flareShadow = sampleShadowMapSoft(uBlockShadowMapArray, lightPos + uCamPos, vec3(0.0), volLayer, volBias, 0.0, uBlockShadowSize, frameSeed);
            if (flareShadow > 0.001) {
                flareShadow *= sampleShadowMapSoft(uBlockEntityShadowMapArray, lightPos + uCamPos, vec3(0.0), volLayer, volBias, 0.0, uBlockEntityShadowSize, frameSeed);
            }
        }

        if (!isSky && dist < radius && NoL > 0.0) {
            float normDist = dist * invRadius;

            float baseAtten = 1.0 / (1.0 + linear * dist + quadratic * dist * dist);
            float atten = pow(baseAtten, falloffExp * 0.5);

            float window = 1.0 - smoothstep(0.0, 1.0, normDist);
            atten *= window;

            float indirectBaseAtten = 1.0 / (1.0 + linear * dist + quadratic * dist * dist);
            float indirectAtten = pow(indirectBaseAtten, falloffExp * 0.35);

            float indirectWindow = 1.0 - smoothstep(0.0, 1.0, normDist);
            indirectAtten *= indirectWindow;
            float wrapNoL = max(dot(N, L) * 0.5 + 0.5, 0.0);

            bool validCone = true;
            if (type == 1 || type == 2) {
                float theta = dot(-L, normalize(uLights[i].dirType.xyz));
                if (theta <= 0.0) validCone = false;
                else if (type == 1) {
                    float eps = cos(radians(uLights[i].spotParams.x)) - cos(radians(uLights[i].spotParams.y));
                    atten *= clamp((theta - cos(radians(uLights[i].spotParams.y))) / (eps + 1e-5), 0.0, 1.0);
                    indirectAtten *= clamp((theta - cos(radians(uLights[i].spotParams.y))) / (eps + 1e-5), 0.0, 1.0);
                } else {
                    atten *= clamp(theta, 0.0, 1.0);
                    indirectAtten *= clamp(theta, 0.0, 1.0);
                }
            }

            if (validCone) {
                float baseBias = uLights[i].shadowParams.z;
                float bias = clamp(baseBias / max(NoL, 0.05), 0.0001, 0.01);
                float softnessPixels = max(uLights[i].shadowParams.y, uLights[i].newParams1.w * 3.0);

                float shadow = 1.0;
                vec3 shadowColor = vec3(1.0);

                if (uLights[i].areaParams.z > 0.5) {
                    int startLayer = int(uLights[i].shadowParams.x + 0.5);
                    int targetLayer = startLayer;
                    bool isOmni = (type == 0 || type == 3 || type == 4);
                    if (isOmni) targetLayer += getCubemapFaceIndex(-toLight);

                    vec3 absWorldPos = pixelWorldPos + uCamPos;
                    int seed = frameSeed + i * 1000;

                    float sBlock = sampleShadowMapSoft(uBlockShadowMapArray, absWorldPos, N, targetLayer, bias, softnessPixels, uBlockShadowSize, seed);
                    shadow *= sBlock;

                    if (shadow > 0.001) {
                        float sTileBlock = sampleShadowMapSoft(uBlockEntityShadowMapArray, absWorldPos, N, targetLayer, bias, softnessPixels, uBlockEntityShadowSize, seed);
                        shadow *= sTileBlock;

                        if (shadow > 0.001) {
                            float sParticle = sampleShadowMapSoft(uParticleShadowMapArray, absWorldPos, N, targetLayer, bias, softnessPixels, uParticleShadowSize, seed);
                            shadow *= sParticle;

                            float sEntity = sampleShadowMapSoft(uEntityShadowMapArray, absWorldPos, N, targetLayer, bias, softnessPixels, uEntityShadowSize, seed);
                            if (sEntity < 1.0) {
                                float normalOffset = max(bias * 15.0, 0.04);
                                vec4 lightSpacePos = uLightSpaceMatrices[targetLayer] * vec4(absWorldPos + N * normalOffset, 1.0);
                                vec3 projCoords = lightSpacePos.xyz / lightSpacePos.w;
                                vec3 texCoords = projCoords * 0.5 + 0.5;
                                vec3 entityColor = texture(uEntityShadowColorArray, vec3(texCoords.xy, float(targetLayer))).rgb;
                                shadowColor = mix(vec3(1.0), entityColor, 1.0 - sEntity);
                            }
                        }
                    }
                }

                vec3 brdf = calculatePBR(N, V, L, albedo_pbr, rough, metal, specularVal);
                float fogDensity = uLights[i].newParams1.x;
                float fogTransmittance = exp(-fogDensity * dist);
                vec3 directLight = lightCol * shadowColor * (intensity * 50.0) * atten * shadow * brdf * fogTransmittance;

                vec3 indirectLight = albedo_pbr * lightCol * shadowColor * (intensity * 8.5) * indirectAtten * wrapNoL * shadow * fogTransmittance;

                lightAccum += directLight + indirectLight;
            }
        }

        vec3 scatteredLight = vec3(0.0);
        if (uLights[i].extraParams.x > 0.5 && uParams.z > 0.5 && !isMieGEnabled && uLights[i].extraParams.y > 0.0) {
            float volStrength = uLights[i].extraParams.y;
            float fogAbsorption = uLights[i].newParams1.y;

            vec3 rayDir = normalize(pixelWorldPos);
            float tStart = 0.0;
            float tEnd = viewDist;

            float b = dot(rayDir, lightPos);
            float c = dot(lightPos, lightPos) - radius * radius;
            float h = b * b - c;

            if (h >= 0.0) {
                float t0 = b - sqrt(h);
                float t1 = b + sqrt(h);
                tStart = max(0.0, t0);
                tEnd = min(viewDist, t1);
            } else tStart = tEnd = 0.0;

            if (tStart < tEnd) {
                float segmentLength = tEnd - tStart;

                int steps = int(uParams.z + 0.5);
                float stepSize = segmentLength / float(steps);
                float transmittance = 1.0;

                for (int s = 0; s < 32; s++) {
                    if (s >= steps) break;

                    float t = tStart + (float(s) + ditherBase) * stepSize;
                    vec3 samplePos = rayDir * t;
                    vec3 sampleAbsPos = samplePos + uCamPos;

                    vec3 Lv = lightPos - samplePos;
                    float d = length(Lv);

                    if (d < radius) {
                        vec3 Ld = Lv / d;
                        float cosTheta = dot(rayDir, Ld);

                        float phaseForward  = henyeyGreenstein(cosTheta, 0.15);
                        float phaseBackward = henyeyGreenstein(cosTheta, -0.05);
                        float phase = mix(phaseForward, phaseBackward, 0.25);

                        float volNormDist = d * invRadius;

                        float volBaseAtten = 1.0 / (1.0 + linear * d + quadratic * d * d);
                        float volAtten = pow(volBaseAtten, falloffExp * 0.5);

                        float volWindow = 1.0 - smoothstep(0.0, 1.0, volNormDist);
                        volAtten *= volWindow;

                        if (type == 1 || type == 2) {
                            float theta = dot(-Ld, normalize(uLights[i].dirType.xyz));
                            if (theta <= 0.0) volAtten = 0.0;
                            else if (type == 1) {
                                float eps = cos(radians(uLights[i].spotParams.x)) - cos(radians(uLights[i].spotParams.y));
                                volAtten *= clamp((theta - cos(radians(uLights[i].spotParams.y))) / (eps + 1e-5), 0.0, 1.0);
                            } else volAtten *= clamp(theta, 0.0, 1.0);
                        }

                        if (volAtten > 0.0) {
                            float volShadow = 1.0;
                            if (uLights[i].areaParams.z > 0.5) {
                                int volLayer = int(uLights[i].shadowParams.x + 0.5);
                                bool volOmni = (type == 0 || type == 3 || type == 4);
                                if (volOmni) volLayer += getCubemapFaceIndex(-Ld);

                                float baseBias = uLights[i].shadowParams.z;
                                float volBias = max(baseBias * 2.0, 0.005);

                                float sBlock = sampleShadowMapSoft(uBlockShadowMapArray, sampleAbsPos, vec3(0.0), volLayer, volBias, 0.0, uBlockShadowSize, frameSeed);
                                volShadow *= sBlock;

                                if (volShadow > 0.001) {
                                    float sTileBlock = sampleShadowMapSoft(uBlockEntityShadowMapArray, sampleAbsPos, vec3(0.0), volLayer, volBias, 0.0, uBlockEntityShadowSize, frameSeed);
                                    volShadow *= sTileBlock;

                                    if (volShadow > 0.001) {
                                        float sEntity = sampleShadowMapSoft(uEntityShadowMapArray, sampleAbsPos, vec3(0.0), volLayer, volBias, 0.0, uEntityShadowSize, frameSeed);
                                        volShadow *= sEntity;
                                    }
                                }
                            }

                            float stepContrib = volStrength * phase * volAtten * volShadow * stepSize;
                            scatteredLight += lightCol * (intensity * 50.0) * stepContrib * transmittance;
                        }
                    }
                    transmittance *= exp(-fogAbsorption * stepSize);
                    if (transmittance < 0.01) break;
                }
            }

            lightAccum += scatteredLight;
        }

        if (uLights[i].extraParams.x > 0.5 && uParams.z > 0.5 && isMieGEnabled && viewDist > distToLightCamera) {
            if (cosAngle > 0.0) {
                float gFactor = mix(0.15, 3.5, (gVal + 1.0) * 0.5);

                float coreExponent = (2048.0 / gFactor) + 1024.0 * distToLightCamera;
                float glowExponent = (128.0 / gFactor) + 128.0 * distToLightCamera;

                float core = pow(cosJittered, coreExponent);
                float glow = pow(cosJittered, glowExponent);
                float phasePoint = mix(core, glow, 0.05);

                float safeDist = max(distToLightCamera, 0.25);
                float distAtten = 1.0 / (safeDist * safeDist + 0.1);

                float mieStrength = uLights[i].extraParams.y * 5.0 * radius;

                vec3 miePoint = lightCol * (intensity * 50.0) * mieStrength * phasePoint * distAtten * flareShadow;
                lightAccum += miePoint;
            }
        }
    }

    if (!isSky) {
        lightAccum += emissiveColor * emissive;
    }

    FragColor = vec4(lightAccum, 1.0);
}