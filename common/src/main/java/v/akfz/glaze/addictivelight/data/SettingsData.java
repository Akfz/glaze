package v.akfz.glaze.addictivelight.data;

import org.lwjgl.glfw.GLFW;
import v.akfz.aslib.util.json.JsonData;
import v.akfz.glaze.addictivelight.render.passes.denoiser.Denoiser;

import java.util.ArrayList;
import java.util.List;

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
}