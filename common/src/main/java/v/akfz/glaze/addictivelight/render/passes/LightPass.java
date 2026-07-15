package v.akfz.glaze.addictivelight.render.passes;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import v.akfz.glaze.addictivelight.localutil.ShadowFramebuffer;
import v.akfz.glaze.addictivelight.render.AddictiveLight;
import v.akfz.glaze.shader.impl.ShaderProgram;
import v.akfz.glaze.shader.util.QuadMesh;
import v.akfz.glaze.shader.util.UniformBuffer;

public class LightPass implements RenderPass {
    private final ShaderProgram lightShader = new ShaderProgram(
            new ResourceLocation("glze", "shader/vertex.glsl"),
            new ResourceLocation("glze", "shader/light/addictivelight.glsl")
    );

    private int frameCounter = 0;

    @Override
    public void render(Object... objects) {
        ShadowFramebuffer blockFb = (ShadowFramebuffer) objects[0];
        ShadowFramebuffer blockEntityFb = (ShadowFramebuffer) objects[1];
        ShadowFramebuffer entityFb = (ShadowFramebuffer) objects[2];
        ShadowFramebuffer particleFb = (ShadowFramebuffer) objects[3];
        RenderTarget lightPassBuffer = (RenderTarget) objects[4];
        UniformBuffer lightUbo = (UniformBuffer) objects[5];
        RenderTarget materialAlbedo = (RenderTarget) objects[6];
        RenderTarget materialNormal = (RenderTarget) objects[7];
        RenderTarget materialPbr = (RenderTarget) objects[8];
        QuadMesh quadMesh = (QuadMesh) objects[9];
        Matrix4f capturedView = (Matrix4f) objects[10];
        Matrix4f capturedProj = (Matrix4f) objects[11];

        Minecraft mc = Minecraft.getInstance();
        int width = lightPassBuffer.width;
        int height = lightPassBuffer.height;

        lightPassBuffer.bindWrite(true);
        GL11.glViewport(0, 0, width, height);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        RenderSystem.disableDepthTest();

        lightShader.use(() -> {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getMainRenderTarget().getDepthTextureId());
            lightShader.uniformManager.set("uDepth", 0);
            lightShader.uniformManager.set("uFrames", frameCounter);

            lightShader.uniformManager.set("uBlockShadowSize", (float) blockFb.width);
            lightShader.uniformManager.set("uBlockEntityShadowSize", (float) blockEntityFb.width);
            lightShader.uniformManager.set("uEntityShadowSize", (float) entityFb.width);
            lightShader.uniformManager.set("uParticleShadowSize", (float) particleFb.width);

            blockFb.bindDepthArray(1);
            blockEntityFb.bindDepthArray(2);
            entityFb.bindDepthArray(3);
            particleFb.bindDepthArray(4);

            lightShader.uniformManager.set("uBlockShadowMapArray", 1);
            lightShader.uniformManager.set("uBlockEntityShadowMapArray", 2);
            lightShader.uniformManager.set("uEntityShadowMapArray", 3);
            lightShader.uniformManager.set("uParticleShadowMapArray", 4);

            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + 5);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, materialAlbedo.getColorTextureId());
            lightShader.uniformManager.set("uAlbedo", 5);

            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + 6);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, materialNormal.getColorTextureId());
            lightShader.uniformManager.set("uNormal", 6);

            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + 7);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, materialPbr.getColorTextureId());
            lightShader.uniformManager.set("uPBR", 7);

            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + 8);
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, entityFb.getColorBufferId());
            lightShader.uniformManager.set("uEntityShadowColorArray", 8);

            Matrix4f invView = new Matrix4f(capturedView).invert();
            Matrix4f invProj = new Matrix4f(capturedProj).invert();

            lightShader.uniformManager.set("uInvProj", invProj);
            lightShader.uniformManager.set("uInvView", invView);

            Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
            lightShader.uniformManager.set("uCamPos", new Vector3f((float) camPos.x, (float) camPos.y, (float) camPos.z));

            lightShader.uniformManager.set("uNear", 0.05f);
            float farPlane = (float) (mc.options.renderDistance().get() * 16);
            lightShader.uniformManager.set("uFar", farPlane);

            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, AddictiveLight.INSTANCE.getShadowMatrixUbo().getBindingPoint(), AddictiveLight.INSTANCE.getShadowMatrixUbo().getUboId());
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, lightUbo.getBindingPoint(), lightUbo.getUboId());

            lightShader.uniformManager.bindUbo("ShadowMatrixBuffer", AddictiveLight.INSTANCE.getShadowMatrixUbo());
            lightShader.uniformManager.bindUbo("LightBuffer", lightUbo);

            quadMesh.render();
        });

        for (int i = 1; i <= 8; i++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        frameCounter++;
    }

    @Override
    public void cleanup() {
        this.lightShader.cleanup();
    }
}