package v.akfz.glaze.module;

public interface RenderModule {
    void render(Object... args);

    void onEnable();
    void onDisable();

    String getModuleId();
    boolean isEnabled();
}
