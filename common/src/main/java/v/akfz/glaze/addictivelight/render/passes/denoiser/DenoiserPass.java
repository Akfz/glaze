package v.akfz.glaze.addictivelight.render.passes.denoiser;

import v.akfz.glaze.addictivelight.data.manager.DataManager;
import v.akfz.glaze.addictivelight.render.passes.RenderPass;

public class DenoiserPass implements RenderPass {
    public Denoiser currentDenoiser = Denoiser.A_TROUS;

    private final ATrousDenoiser aTrousDenoiser = new ATrousDenoiser();
    private final TAADenoiser taaDenoiser = new TAADenoiser();
    private final SVGFDenoiser svgfDenoiser = new SVGFDenoiser();

    @Override
    public void render(Object... objects) {
        currentDenoiser = DataManager.INSTANCE.getSettingsData().pickedDenoiser;
        if (currentDenoiser == null || currentDenoiser.equals(Denoiser.none)) return;

        switch (currentDenoiser) {
            case A_TROUS -> aTrousDenoiser.render(objects);
            case TAA -> taaDenoiser.render(objects);
            case SVGF -> svgfDenoiser.render(objects);
        }
    }

    @Override
    public void cleanup() {
        aTrousDenoiser.cleanup();
        taaDenoiser.cleanup();
        svgfDenoiser.cleanup();
    }
}