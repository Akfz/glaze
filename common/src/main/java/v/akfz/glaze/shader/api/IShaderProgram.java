package v.akfz.glaze.shader.api;

// ну напишу куда-нить сюда. Мой шейдер апи "низкоуровневый", где нужно все вручную добавлять, настраивать и т.д
// сделано так, потому-что майнкрафт шейдеры я не выкупил спустя кучу времени(у меня просто не получается заставить
// их работать), с этим сложнее, но намного "гибче"(более гибко). P.s сложнее потому-что всеми состояниями майна
// нужно управляться вручную
public interface IShaderProgram {
    int getUniformLocation(String name);
    void checkActive();
    int getProgramID();
    String getProgramName();

    default void dispatch(int x, int y, int z) {
        throw new UnsupportedOperationException("Compute dispatch not supported by this shader type.");
    }
}
