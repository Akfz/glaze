package v.akfz.glaze.addictivelight.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import v.akfz.glaze.addictivelight.data.SettingsData;
import v.akfz.glaze.addictivelight.data.light.LightSource;
import v.akfz.glaze.addictivelight.data.light.LightRedactor;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.addictivelight.localutil.ShadowFramebuffer;
import v.akfz.glaze.addictivelight.render.passes.LightPass;
import v.akfz.glaze.addictivelight.render.passes.ShadowPass;
import v.akfz.glaze.addictivelight.render.passes.MaterialPass;
import v.akfz.glaze.addictivelight.render.passes.denoiser.DenoiserPass;
import v.akfz.glaze.module.RenderModule;
import v.akfz.glaze.module.RenderModuleManager;
import v.akfz.glaze.shader.impl.ShaderProgram;
import v.akfz.glaze.shader.util.HDRFramebuffer;
import v.akfz.glaze.shader.util.QuadMesh;
import v.akfz.glaze.shader.util.UniformBuffer;

import java.nio.FloatBuffer;
import java.util.Collection;

public class AddictiveLight implements RenderModule {
    private AddictiveLight() {}

    public static final AddictiveLight INSTANCE = new AddictiveLight();

    private boolean enabled = true;

    @Getter private LightRedactor redactor;

    private ShadowFramebuffer blockShadowBuffer;
    private ShadowFramebuffer blockEntityShadowBuffer;
    private ShadowFramebuffer entityShadowBuffer;
    private ShadowFramebuffer particleShadowBuffer;

    @Getter private RenderTarget materialAlbedoBuffer;
    @Getter private RenderTarget materialNormalBuffer;
    @Getter private RenderTarget materialPbrBuffer;
    private MaterialPass materialPass;

    private DenoiserPass denoiserPass;

    @Getter private RenderTarget pingBuffer;
    private RenderTarget lightPassBuffer;

    private FloatBuffer uboBuffer;
    private FloatBuffer matrixBuffer;
    @Getter private UniformBuffer shadowMatrixUbo;
    private UniformBuffer lightUbo;

    private ShadowPass shadowPass;
    private LightPass lightPass;

    @Getter private final QuadMesh quadMesh = new QuadMesh();
    private ShaderProgram blitShader;

    private boolean initialized = false;
    private boolean meshInitialized = false;

    private ShaderProgram shadowDebugShader;

    private void renderShadowDebug(int screenWidth, int screenHeight, int buffer) {
        int quadSize = 120;
        int y = 10;

        RenderSystem.disableDepthTest();
        GL11.glDepthMask(false);

        int texTarget = GL30.GL_TEXTURE_2D_ARRAY;
        int activeTexId = 0;
        switch (buffer) {
            case 0: if (blockShadowBuffer != null) activeTexId = blockShadowBuffer.getDepthTextureId(); break;
            case 1: if (blockEntityShadowBuffer != null) activeTexId = blockEntityShadowBuffer.getDepthTextureId(); break;
            case 2: if (entityShadowBuffer != null) activeTexId = entityShadowBuffer.getDepthTextureId(); break;
            case 3: if (particleShadowBuffer != null) activeTexId = particleShadowBuffer.getDepthTextureId(); break;
        }

        if (activeTexId == 0) {
            GL11.glDepthMask(true);
            return;
        }

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(texTarget, activeTexId);
        GL11.glTexParameteri(texTarget, GL30.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);

        shadowDebugShader.use(() -> {
            shadowDebugShader.uniformManager.set("uShadowMapArray", 0);

            for (int i = 0; i < 6; i++) {
                int x = y + i * (quadSize + y);
                GL11.glViewport(x, screenHeight - y - quadSize, quadSize, quadSize);
                shadowDebugShader.uniformManager.set("uLayer", i);
                quadMesh.render();
            }
        });

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(texTarget, activeTexId);
        GL11.glTexParameteri(texTarget, GL30.GL_TEXTURE_COMPARE_MODE, GL30.GL_COMPARE_REF_TO_TEXTURE);
        GL11.glBindTexture(texTarget, 0);

        GL11.glDepthMask(true);
        GL11.glViewport(0, 0, screenWidth, screenHeight);
    }

    private void checkInit() {
        if (!initialized) {
            RenderSystem.assertOnRenderThreadOrInit();

            ResourceLocation vertex = new ResourceLocation("glze", "shader/vertex.glsl");
            ResourceLocation fragment = new ResourceLocation("glze", "shader/light/blit.glsl");
            this.blitShader = new ShaderProgram(vertex, fragment);

            ResourceLocation debugFrag = new ResourceLocation("glze", "shader/debug/shadow_debug.glsl");
            this.shadowDebugShader = new ShaderProgram(vertex, debugFrag);

            this.shadowPass = new ShadowPass();
            this.materialPass = new MaterialPass();
            this.lightPass = new LightPass();
            this.denoiserPass = new DenoiserPass();

            this.redactor = new LightRedactor();

            int sizeInBytes = (256 * 160) + 16;
            this.lightUbo = new UniformBuffer();
            this.lightUbo.init(sizeInBytes, 2);

            this.shadowMatrixUbo = new UniformBuffer();
            this.shadowMatrixUbo.init(256 * 64, 3);

            this.initialized = true;
        }
    }

    private void ensureResources(int unscaledW, int unscaledH, int scaledW, int scaledH) {
        if (!meshInitialized) {
            quadMesh.init();
            meshInitialized = true;
        }

        if (materialAlbedoBuffer == null || materialAlbedoBuffer.width != scaledW || materialAlbedoBuffer.height != scaledH) {
            if (materialAlbedoBuffer != null) materialAlbedoBuffer.destroyBuffers();
            materialAlbedoBuffer = new TextureTarget(scaledW, scaledH, true, Minecraft.ON_OSX);
            if (materialPass != null) materialPass.invalidateFbo();
        }
        if (materialNormalBuffer == null || materialNormalBuffer.width != scaledW || materialNormalBuffer.height != scaledH) {
            if (materialNormalBuffer != null) materialNormalBuffer.destroyBuffers();
            materialNormalBuffer = new TextureTarget(scaledW, scaledH, true, Minecraft.ON_OSX);
            if (materialPass != null) materialPass.invalidateFbo();
        }
        if (materialPbrBuffer == null || materialPbrBuffer.width != scaledW || materialPbrBuffer.height != scaledH) {
            if (materialPbrBuffer != null) materialPbrBuffer.destroyBuffers();
            materialPbrBuffer = new TextureTarget(scaledW, scaledH, true, Minecraft.ON_OSX);
            if (materialPass != null) materialPass.invalidateFbo();
        }

        createShadowBuffers();

        if (pingBuffer == null || pingBuffer.width != unscaledW || pingBuffer.height != unscaledH) {
            if (pingBuffer != null) pingBuffer.destroyBuffers();
            pingBuffer = new TextureTarget(unscaledW, unscaledH, true, Minecraft.ON_OSX);
        }

        if (lightPassBuffer == null || lightPassBuffer.width != scaledW || lightPassBuffer.height != scaledH) {
            if (lightPassBuffer != null) lightPassBuffer.destroyBuffers();
            lightPassBuffer = new HDRFramebuffer(scaledW, scaledH, false);
        }
    }

    public void recreateShadowBuffers() {
        if (blockShadowBuffer != null) blockShadowBuffer.destroyBuffers();
        if (blockEntityShadowBuffer != null) blockEntityShadowBuffer.destroyBuffers();
        if (entityShadowBuffer != null) entityShadowBuffer.destroyBuffers();
        if (particleShadowBuffer != null) particleShadowBuffer.destroyBuffers();
        blockShadowBuffer = null;
        blockEntityShadowBuffer = null;
        entityShadowBuffer = null;
        particleShadowBuffer = null;
        createShadowBuffers();
        DataManager.INSTANCE.getLightManager().getAllSources().forEach(light -> light.setDirty(true));
    }

    private void createShadowBuffers() {
        SettingsData data = DataManager.INSTANCE.getSettingsData();
        int blockShadowSize = data.blockShadowSize;
        int blockEntityShadowSize = data.blockEntityShadowSize;
        int entityShadowSize = data.entityShadowSize;
        int particleShadowSize = data.particleShadowSize;

        if (blockShadowBuffer == null || blockShadowBuffer.width != blockShadowSize) {
            if (blockShadowBuffer != null) blockShadowBuffer.destroyBuffers();
            blockShadowBuffer = new ShadowFramebuffer(blockShadowSize, blockShadowSize, false);
        }
        if (blockEntityShadowBuffer == null || blockEntityShadowBuffer.width != blockEntityShadowSize) {
            if (blockEntityShadowBuffer != null) blockEntityShadowBuffer.destroyBuffers();
            blockEntityShadowBuffer = new ShadowFramebuffer(blockEntityShadowSize, blockEntityShadowSize, false);
        }
        if (entityShadowBuffer == null || entityShadowBuffer.width != entityShadowSize) {
            if (entityShadowBuffer != null) entityShadowBuffer.destroyBuffers();
            entityShadowBuffer = new ShadowFramebuffer(entityShadowSize, entityShadowSize, true);
        }
        if (particleShadowBuffer == null || particleShadowBuffer.width != particleShadowSize) {
            if (particleShadowBuffer != null) particleShadowBuffer.destroyBuffers();
            particleShadowBuffer = new ShadowFramebuffer(particleShadowSize, particleShadowSize, false);
        }
    }

    private void ensureFloatBuffers(int maxLights) {
        int uboSize = (256 * 40) + 4;
        if (this.uboBuffer == null || this.uboBuffer.capacity() < uboSize) {
            this.uboBuffer = org.lwjgl.BufferUtils.createFloatBuffer(uboSize);
        }
        if (this.matrixBuffer == null) {
            this.matrixBuffer = org.lwjgl.BufferUtils.createFloatBuffer(256 * 16);
        }
    }

    @Override
    public void render(Object... args) {
        if (args.length < 2 || !(args[0] instanceof Matrix4f viewMatrix) || !(args[1] instanceof Matrix4f projMatrix)) return;

        checkInit();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        redactor.update();

        ShaderInstance previousShader = RenderSystem.getShader();

        SettingsData data = DataManager.INSTANCE.getSettingsData();
        int width = mc.getMainRenderTarget().width;
        int height = mc.getMainRenderTarget().height;

        int scaledW = Math.max(1, (int) (width * data.renderScale));
        int scaledH = Math.max(1, (int) (height * data.renderScale));

        ensureResources(width, height, scaledW, scaledH);

        int initialFBO = mc.getMainRenderTarget().frameBufferId;

        boolean oldDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean oldBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean oldCullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean oldScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        shadowPass.render(blockShadowBuffer, blockEntityShadowBuffer, entityShadowBuffer, particleShadowBuffer, camPos);

        if (oldScissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager._disableDepthTest();
        GlStateManager._disableBlend();
        GlStateManager._disableCull();

        blitColor(mc.getMainRenderTarget(), pingBuffer);

        DataManager.INSTANCE.getVoxelGrid().update();
        materialPass.render(materialAlbedoBuffer, materialNormalBuffer, materialPbrBuffer, quadMesh, viewMatrix, projMatrix);

        Collection<LightSource<?>> lights = DataManager.INSTANCE.getLightManager().getStorage().getAllSources();
        int maxAllowedLayers = Math.min(
                Math.min(blockShadowBuffer.getAllocatedLayers(), blockEntityShadowBuffer.getAllocatedLayers()),
                Math.min(entityShadowBuffer.getAllocatedLayers(), particleShadowBuffer.getAllocatedLayers())
        );
        int activeCount = Math.min(lights.size(), maxAllowedLayers);

        ensureFloatBuffers(data.maxLights);

        this.uboBuffer.clear();
        int idx = 0;
        int shadowLayerCounter = 0;
        for (LightSource<?> light : lights) {
            if (idx >= maxAllowedLayers) break;

            int layersCount = light.isOmnidirectional() ? 6 : 1;
            boolean canHaveShadows = light.isActive() && light.isShadowsEnabled() && (shadowLayerCounter + layersCount <= maxAllowedLayers);

            this.uboBuffer.put((float) (light.getX() - camPos.x));
            this.uboBuffer.put((float) (light.getY() - camPos.y));
            this.uboBuffer.put((float) (light.getZ() - camPos.z));
            this.uboBuffer.put(light.getRadius());

            this.uboBuffer.put(light.getR());
            this.uboBuffer.put(light.getG());
            this.uboBuffer.put(light.getB());
            this.uboBuffer.put(light.getIntensity());

            this.uboBuffer.put(light.getDirection().x);
            this.uboBuffer.put(light.getDirection().y);
            this.uboBuffer.put(light.getDirection().z);
            this.uboBuffer.put((float) light.getType().ordinal());

            this.uboBuffer.put(light.getCutoff());
            this.uboBuffer.put(light.getOuterCutoff());
            this.uboBuffer.put(light.getLinear());
            this.uboBuffer.put(light.getQuadratic());

            this.uboBuffer.put(light.getWidth());
            this.uboBuffer.put(light.getHeight());
            this.uboBuffer.put(canHaveShadows ? 1.0f : 0.0f);
            this.uboBuffer.put(light.isIgnoreBlocks() ? 1.0f : 0.0f);

            this.uboBuffer.put(canHaveShadows ? (float) shadowLayerCounter : -1.0f);
            this.uboBuffer.put(light.getShadowSoftness());
            this.uboBuffer.put(light.getShadowBias());
            this.uboBuffer.put(0.0f);

            this.uboBuffer.put(light.isVolumetric() ? 1.0f : 0.0f);
            this.uboBuffer.put(light.getVolumetricStrength());
            this.uboBuffer.put(light.getMieG());
            this.uboBuffer.put(0.0f);

            this.uboBuffer.put(0.0f);
            this.uboBuffer.put(0.0f);
            this.uboBuffer.put(0.0f);
            this.uboBuffer.put(0.0f);

            this.uboBuffer.put(light.getFogDensity());
            this.uboBuffer.put(light.getFogAbsorption());
            this.uboBuffer.put(light.getFalloffExponent());
            this.uboBuffer.put(light.getSourceSize());

            this.uboBuffer.put(0.0f);
            this.uboBuffer.put(0.0f);
            this.uboBuffer.put(1.0f);
            this.uboBuffer.put(0.0f);

            if (canHaveShadows) {
                shadowLayerCounter += layersCount;
            }

            idx++;
        }

        for (int i = idx; i < 256; i++) {
            for (int j = 0; j < 40; j++) this.uboBuffer.put(0.0f);
        }

        uboBuffer.put((float) activeCount);
        uboBuffer.put((float) data.shadowSamples);
        uboBuffer.put((float) data.volumetricSteps);
        uboBuffer.put(0.0f);

        uboBuffer.flip();
        lightUbo.update(this.uboBuffer);

        this.matrixBuffer.clear();
        for (int i = 0; i < 256; i++) {
            ShadowPass.lightSpaceMatrices[i].get(i * 16, this.matrixBuffer);
        }
        this.matrixBuffer.position(0);
        this.matrixBuffer.limit(256 * 16);
        shadowMatrixUbo.update(this.matrixBuffer);

        lightPass.render(
                blockShadowBuffer,
                blockEntityShadowBuffer,
                entityShadowBuffer,
                particleShadowBuffer,
                lightPassBuffer,
                lightUbo,
                materialAlbedoBuffer,
                materialNormalBuffer,
                materialPbrBuffer,
                quadMesh,
                viewMatrix,
                projMatrix
        );

        denoiserPass.render(
                blockShadowBuffer,
                blockEntityShadowBuffer,
                entityShadowBuffer,
                particleShadowBuffer,
                lightPassBuffer,
                lightUbo,
                materialAlbedoBuffer,
                materialNormalBuffer,
                materialPbrBuffer,
                quadMesh,
                viewMatrix,
                projMatrix
        );

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, initialFBO);
        GL11.glViewport(0, 0, width, height);

        renderFinalBlit(lightPassBuffer, mc.getMainRenderTarget());

        if (data.debugShadows) {
            renderShadowDebug(width, height, data.debugShadow);
        }

        if (oldScissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        if (oldDepthTest) GlStateManager._enableDepthTest(); else GlStateManager._disableDepthTest();
        if (oldBlend) GlStateManager._enableBlend(); else GlStateManager._disableBlend();
        if (oldCullFace) GlStateManager._enableCull(); else GlStateManager._disableCull();

        resetTextures();
        DataManager.INSTANCE.getLightManager().savePrevPoses();

        if (previousShader != null) {
            RenderSystem.setShader(() -> previousShader);
            previousShader.apply();
        } else {
            RenderSystem.setShader(() -> null);
            GlStateManager._glUseProgram(0);
        }
    }

    private void renderFinalBlit(RenderTarget sourceHDR, RenderTarget targetMain) {
        targetMain.bindWrite(false);

        GL11.glDepthMask(false);

        blitShader.use(() -> {
            bindTex(0, pingBuffer.getColorTextureId(), "uVanillaTex");
            bindTex(1, sourceHDR.getColorTextureId(), "uHdrTex");

            SettingsData data = DataManager.INSTANCE.getSettingsData();
            blitShader.uniformManager.set("uExposure", data.exposure);
            blitShader.uniformManager.set("uContrast", data.contrast);
            blitShader.uniformManager.set("uSaturation", data.saturation);

            quadMesh.render();

            for (int i = 0; i <= 1; i++) {
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            }
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        });

        GL11.glDepthMask(true);
    }

    private void blitColor(RenderTarget source, RenderTarget target) {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GL30.glBlitFramebuffer(
                0, 0, source.width, source.height,
                0, 0, target.width, target.height,
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_NEAREST
        );
    }

    private void bindTex(int unit, int textureId, String uniformName) {
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        blitShader.uniformManager.set(uniformName, unit);
    }

    private void resetTextures() {
        for (int i = 0; i <= 8; i++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        if (lightUbo != null) {
            lightUbo.cleanup();
            lightUbo = null;
        }
        if (blitShader != null) {
            blitShader.cleanup();
            blitShader = null;
        }
        if (shadowDebugShader != null) {
            shadowDebugShader.cleanup();
            shadowDebugShader = null;
        }
        if (denoiserPass != null) {
            denoiserPass.cleanup();
            denoiserPass = null;
        }

        if (pingBuffer != null) {
            pingBuffer.destroyBuffers();
            pingBuffer = null;
        }
        if (lightPassBuffer != null) {
            lightPassBuffer.destroyBuffers();
            lightPassBuffer = null;
        }
        if (materialAlbedoBuffer != null) {
            materialAlbedoBuffer.destroyBuffers();
            materialAlbedoBuffer = null;
        }
        if (materialNormalBuffer != null) {
            materialNormalBuffer.destroyBuffers();
            materialNormalBuffer = null;
        }
        if (materialPbrBuffer != null) {
            materialPbrBuffer.destroyBuffers();
            materialPbrBuffer = null;
        }
        if (blockShadowBuffer != null) {
            blockShadowBuffer.destroyBuffers();
            blockShadowBuffer = null;
        }
        if (blockEntityShadowBuffer != null) {
            blockEntityShadowBuffer.destroyBuffers();
            blockEntityShadowBuffer = null;
        }
        if (entityShadowBuffer != null) {
            entityShadowBuffer.destroyBuffers();
            entityShadowBuffer = null;
        }
        if (particleShadowBuffer != null) {
            particleShadowBuffer.destroyBuffers();
            particleShadowBuffer = null;
        }

        if (shadowPass != null) {
            shadowPass.cleanup();
            shadowPass = null;
        }
        if (materialPass != null) {
            materialPass.cleanup();
            materialPass = null;
        }
        if (lightPass != null) {
            lightPass.cleanup();
            lightPass = null;
        }
        if (shadowMatrixUbo != null) {
            shadowMatrixUbo.cleanup();
            shadowMatrixUbo = null;
        }

        if (uboBuffer != null) this.uboBuffer = null;
        if (matrixBuffer != null) this.matrixBuffer = null;
        if (redactor != null) redactor = null;

        quadMesh.destroy();
        this.meshInitialized = false;
        DataManager.INSTANCE.cleanup();
        this.initialized = false;
    }

    @Override
    public void onEnable() {
        this.enabled = true;
    }

    @Override
    public void onDisable() {
        this.enabled = false;
        cleanup();
    }

    @Override
    public String getModuleId() {
        return "LS-Light";
    }

    @Override
    public boolean isEnabled() {
        return this.enabled && RenderModuleManager.INSTANCE.haveModule(this);
    }
}