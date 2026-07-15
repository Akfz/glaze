package v.akfz.glaze.addictivelight.render.passes;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;
import v.akfz.glaze.addictivelight.data.block.VoxelGrid;
import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.addictivelight.data.material.BlockMaterial;
import v.akfz.glaze.addictivelight.data.material.MaterialManager;
import v.akfz.glaze.addictivelight.render.AddictiveLight;
import v.akfz.glaze.shader.impl.ShaderProgram;
import v.akfz.glaze.shader.util.QuadMesh;
import v.akfz.glaze.shader.util.TextureBuffer;

import java.util.Map;

public class MaterialPass implements RenderPass {
    @Getter
    private final TextureBuffer materialBuffer = new TextureBuffer();
    private ShaderProgram materialShader;
    private int mrtFbo = 0;

    public void invalidateFbo() {
        if (this.mrtFbo != 0) {
            GL30.glDeleteFramebuffers(this.mrtFbo);
            this.mrtFbo = 0;
        }
    }

    private void checkInit() {
        if (this.materialShader != null) return;
        RenderSystem.assertOnRenderThreadOrInit();

        ResourceLocation vertex = new ResourceLocation("glze", "shader/vertex.glsl");
        ResourceLocation template = new ResourceLocation("glze", "shader/light/materialpass.glsl");

        this.materialShader = new ShaderProgram(vertex, template);
    }

    private void ensureFbo(RenderTarget albedo, RenderTarget normal, RenderTarget pbr) {
        if (this.mrtFbo != 0) return;
        RenderSystem.assertOnRenderThreadOrInit();

        this.mrtFbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.mrtFbo);

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, albedo.getColorTextureId(), 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, normal.getColorTextureId(), 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT2, GL11.GL_TEXTURE_2D, pbr.getColorTextureId(), 0);

        int[] drawBuffers = { GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_COLOR_ATTACHMENT2 };
        GL20.glDrawBuffers(drawBuffers);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void render(Object... objects) {
        checkInit();

        RenderTarget materialAlbedo = (RenderTarget) objects[0];
        RenderTarget materialNormal = (RenderTarget) objects[1];
        RenderTarget materialPbr = (RenderTarget) objects[2];
        QuadMesh quadMesh = (QuadMesh) objects[3];
        Matrix4f capturedView = (Matrix4f) objects[4];
        Matrix4f capturedProj = (Matrix4f) objects[5];

        ensureFbo(materialAlbedo, materialNormal, materialPbr);

        Map<ResourceLocation, BlockMaterial> registry = MaterialManager.getBlockRegistry();
        int numMaterials = registry.size();
        if (numMaterials == 0) {
            numMaterials = 1;
        }

        materialBuffer.resize(numMaterials, 2);
        float[] data = new float[numMaterials * 2 * 4];

        for (BlockMaterial mat : registry.values()) {
            int id = mat.getRuntimeID();
            if (id < 0 || id >= numMaterials) continue;

            int pbrIndex = id * 4;
            data[pbrIndex] = mat.getRoughness();
            data[pbrIndex + 1] = mat.getMetallic();
            data[pbrIndex + 2] = mat.getEmissive();
            data[pbrIndex + 3] = mat.getSpecular();

            int tintIndex = (numMaterials * 4) + (id * 4);
            data[tintIndex] = mat.getTintR();
            data[tintIndex + 1] = mat.getTintG();
            data[tintIndex + 2] = mat.getTintB();
            data[tintIndex + 3] = mat.getOpacity();
        }
        materialBuffer.upload(data);

        Minecraft mc = Minecraft.getInstance();
        int width = materialAlbedo.width;
        int height = materialAlbedo.height;

        int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.mrtFbo);
        GL11.glViewport(0, 0, width, height);

        RenderSystem.disableDepthTest();
        GlStateManager._depthMask(false);

        GL11.glClearColor(0.8f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        VoxelGrid voxelGrid = DataManager.INSTANCE.getVoxelGrid();

        materialShader.use(() -> {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getMainRenderTarget().getDepthTextureId());
            materialShader.uniformManager.set("uDepth", 0);

            RenderSystem.activeTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, AddictiveLight.INSTANCE.getPingBuffer().getColorTextureId());
            materialShader.uniformManager.set("uAlbedo", 1);

            voxelGrid.getTextureBuffer().bind(2);
            materialShader.uniformManager.set("uVoxelGrid", 2);

            materialBuffer.bind(3);
            materialShader.uniformManager.set("uMaterialBuffer", 3);

            Matrix4f invView = new Matrix4f(capturedView).invert();
            Matrix4f invProj = new Matrix4f(capturedProj).invert();

            materialShader.uniformManager.set("uInvProj", invProj);
            materialShader.uniformManager.set("uInvView", invView);

            Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
            materialShader.uniformManager.set("uCamPos", new Vector3f((float) camPos.x, (float) camPos.y, (float) camPos.z));

            int startX = (int) Math.floor(camPos.x) - voxelGrid.getGridXZ() / 2;
            int startY = (int) Math.floor(camPos.y) - voxelGrid.getGridY() / 2;
            int startZ = (int) Math.floor(camPos.z) - voxelGrid.getGridXZ() / 2;

            materialShader.uniformManager.set("uGridStart", new Vector3f((float) startX, (float) startY, (float) startZ));
            materialShader.uniformManager.set("uGridXZ", voxelGrid.getGridXZ());
            materialShader.uniformManager.set("uGridY", voxelGrid.getGridY());

            quadMesh.render();
        });

        voxelGrid.getTextureBuffer().unbind(2);
        materialBuffer.unbind(3);
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
    }

    @Override
    public void cleanup() {
        this.materialBuffer.cleanup();
        if (this.materialShader != null) {
            this.materialShader.cleanup();
            this.materialShader = null;
        }
        if (this.mrtFbo != 0) {
            GL30.glDeleteFramebuffers(this.mrtFbo);
            this.mrtFbo = 0;
        }
    }
}