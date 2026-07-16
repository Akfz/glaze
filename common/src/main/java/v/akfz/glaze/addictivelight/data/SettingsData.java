package v.akfz.glaze.addictivelight.data;

import org.lwjgl.glfw.GLFW;
import v.akfz.aslib.util.json.JsonData;
import v.akfz.glaze.addictivelight.render.passes.denoiser.Denoiser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsData implements JsonData {
    public int materialXZRadius = 32;
    public int materialYRadius = 32;
    public int maxLights = 256;
    public int blockShadowSize = 1024;
    public int blockEntityShadowSize = 512;
    public int entityShadowSize = 512;
    public int particleShadowSize = 256;
    public int shadowSamples = 8;
    public int volumetricSteps = 12;

    public float renderScale = 1.0f;

    public float exposure = 1;
    public float contrast = 1;
    public float saturation = 1;

    public Denoiser pickedDenoiser = Denoiser.TAA;
    public float atrousDepthThreshold = 0.1f;
    public float atrousNormalThreshold = 16.0f;

    public float atrousLumaThreshold = 1.5f;
    public float taaBlendFactor = 0.1f;
    public float taaVarianceScale = 1.5f;

    public float svgfDepthThreshold = 0.1f;
    public float svgfNormalThreshold = 16.0f;
    public float svgfLumaThreshold = 4.0f;

    public boolean isDevMode = false;
    public boolean debug = false;
    public boolean debugShadows = false;
    public int debugShadow = 0;
    public boolean debugLight = false;
    public boolean debugLightInfo = false;
    public boolean debugLightFrustum = false;

    public int KEY_TO_ADD_LIGHT_SPOT;
    public int KEY_TO_ADD_LIGHT_AREA_SPHERE;
    public int KEY_TO_ADD_LIGHT_AREA_RECT;
    public int KEY_TO_ADD_LIGHT_POINT;

    public int KEY_TO_OPEN_SETTINGS = GLFW.GLFW_KEY_0;

    public boolean isAllAllowedToChangeLightSources = false;
    public List<String> allowedPlayers = new ArrayList<>();

    public List<String> disabledLightBlocks = new ArrayList<>();

    public Map<String, BlockLightSettings> customLightBlocks = new HashMap<>();

    public static class BlockLightSettings {
        public float r = 1.0f;
        public float g = 1.0f;
        public float b = 1.0f;
        public float intensity = 40.0f;
        public float radius = 10.0f;
        public int type = 0;
        public float linear = 0.09f;
        public float quadratic = 0.032f;
        public float falloffExponent = 2.0f;
        public boolean shadowsEnabled = true;
        public float shadowSoftness = 0.05f;
        public float shadowBias = 0.002f;
        public boolean volumetric = false;
        public float volumetricStrength = 0.0f;
        public float mieG = 0.0f;
        public float fogDensity = 0.05f;
        public float fogAbsorption = 0.1f;
        public float width = 1.0f;
        public float height = 1.0f;
        public float sourceSize = 0.05f;
        public float shadowNear = 0.2f;
        public float shadowFar = 64.0f;
    }
}