package v.akfz.glaze;

import v.akfz.aslib.initializer.generator.GenerateInitializer;
import v.akfz.aslib.initializer.generator.InitializerClass;
import v.akfz.aslib.initializer.generator.LoaderType;
import v.akfz.aslib.network.AsLibNetworking;
import v.akfz.aslib.util.GlobalUtils;
import v.akfz.glaze.addictivelight.render.AddictiveLight;
import v.akfz.glaze.module.RenderModuleManager;
import v.akfz.glaze.network.light.delete.DeleteLightSourceDecoder;
import v.akfz.glaze.network.light.delete.DeleteLightSourceEncoder;
import v.akfz.glaze.network.light.delete.DeleteLightSourceHandler;
import v.akfz.glaze.network.light.delete.DeleteLightSourcePacket;
import v.akfz.glaze.network.light.update.SyncLightsDecoder;
import v.akfz.glaze.network.light.update.SyncLightsEncoder;
import v.akfz.glaze.network.light.update.SyncLightsHandler;
import v.akfz.glaze.network.light.update.SyncLightsPacket;
import v.akfz.glaze.network.rights.HaveRightsToChange;
import v.akfz.glaze.network.rights.HaveRightsToChangeHandler;
import v.akfz.glaze.network.rights.answer.HaveRightsToChangeHandlerAnswer;
import v.akfz.glaze.network.rights.answer.HaveRightsToChangeHandlerAnswerDecoder;
import v.akfz.glaze.network.rights.answer.HaveRightsToChangeHandlerAnswerEncoder;
import v.akfz.glaze.network.rights.answer.HaveRightsToChangeHandlerAnswerHandler;
import v.akfz.glaze.pprmodule.PostProcessRenderer;

@GenerateInitializer(loader = LoaderType.Both, modId = "glze")
public class GlazeMain implements InitializerClass {

    @Override
    public void init() {
        AsLibNetworking.REGISTRY.register(
                HaveRightsToChange.class,
                (haveRightsToChange, friendlyByteBuf) -> {},
                friendlyByteBuf -> new HaveRightsToChange(),
                new HaveRightsToChangeHandler()
        );
        AsLibNetworking.REGISTRY.register(
                HaveRightsToChangeHandlerAnswer.class,
                new HaveRightsToChangeHandlerAnswerEncoder(),
                new HaveRightsToChangeHandlerAnswerDecoder(),
                new HaveRightsToChangeHandlerAnswerHandler()
        );
        AsLibNetworking.REGISTRY.register(
                DeleteLightSourcePacket.class,
                new DeleteLightSourceEncoder(),
                new DeleteLightSourceDecoder(),
                new DeleteLightSourceHandler()
        );
        AsLibNetworking.REGISTRY.register(
                SyncLightsPacket.class,
                new SyncLightsEncoder(),
                new SyncLightsDecoder(),
                new SyncLightsHandler()
        );
        if (GlobalUtils.isClientSide()) {
            //PostProcessRenderer.INSTANCE.addShader(new ResourceLocation("glze", "shader/test/pptest"));
            //PostProcessRenderer.INSTANCE.addShader(new ResourceLocation("glze", "shader/test/pptestdepth"));

            RenderModuleManager.INSTANCE.registerModule(AddictiveLight.INSTANCE);
            RenderModuleManager.INSTANCE.registerModule(PostProcessRenderer.INSTANCE);
        }
    }
}
