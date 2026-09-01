package v.akfz.glazelib.shader.api;

public interface IShaderProgram {

    int getProgramID();
    String getProgramName();

    boolean isValid();

    int getUniformLocation(String name);
    ShaderUniformManager getUniformManager();

    void use();
    void use(Runnable action);

    void stop();
    void cleanup();
    void reload();

    void checkActive();

    default void dispatch(int x, int y, int z) {
        throw new UnsupportedOperationException(
                "Compute dispatch not supported by this shader type: " + getProgramName()
        );
    }

    default void dispatchForSize(int width, int height, int localSizeX, int localSizeY) {
        int groupsX = (width + localSizeX - 1) / localSizeX;
        int groupsY = (height + localSizeY - 1) / localSizeY;
        dispatch(groupsX, groupsY, 1);
    }
}