package v.akfz.glaze.addictivelight.render.passes;

public interface RenderPass {
    void render(Object... objects);
    void cleanup();
}
