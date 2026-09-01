package v.akfz.glazelib.shader.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import v.akfz.glazelib.shader.api.IShaderProgram;
import v.akfz.glazelib.shader.api.ShaderUniformManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

public class GeometryShaderProgram implements IShaderProgram {
    private int programId;
    private final ResourceLocation vertexLocation;
    private final ResourceLocation geometryLocation;
    private final ResourceLocation fragmentLocation;
    private final Map<String, Integer> uniformLocationCache = new ConcurrentHashMap<>();
    public ShaderUniformManager uniformManager;
    private boolean isInUse = false;

    public GeometryShaderProgram(ResourceLocation vertexLocation, ResourceLocation geometryLocation, ResourceLocation fragmentLocation) {
        this.vertexLocation = vertexLocation;
        this.geometryLocation = geometryLocation;
        this.fragmentLocation = fragmentLocation;
        this.init();
    }

    private void init() {
        if (this.programId > 0) this.cleanup();
        this.programId = glCreateProgram();
        List<Integer> compiledShaders = new ArrayList<>();

        try {
            compiledShaders.add(this.createShader(this.vertexLocation, GL_VERTEX_SHADER));
            compiledShaders.add(this.createShader(this.geometryLocation, GL_GEOMETRY_SHADER));
            compiledShaders.add(this.createShader(this.fragmentLocation, GL_FRAGMENT_SHADER));

            for (int i = 0; i < compiledShaders.size(); i++) {
                glAttachShader(this.programId, compiledShaders.get(i));
            }

            glBindAttribLocation(this.programId, 0, "aPos");
            glBindAttribLocation(this.programId, 1, "aTexCoord");
            glLinkProgram(this.programId);
            this.checkProgramLinkStatus(this.programId);

            for (int shaderId : compiledShaders) {
                glDetachShader(this.programId, shaderId);
                glDeleteShader(shaderId);
            }

            this.uniformManager = new ShaderUniformManager(this);
            glValidateProgram(this.programId);
            this.checkProgramValidateStatus(this.programId);
        } catch (Exception e) {
            for (int shaderId : compiledShaders) {
                if (shaderId != 0) glDeleteShader(shaderId);
            }
            throw new RuntimeException("Failed to link geometry shader program: " + this.getProgramName(), e);
        }
    }

    private int createShader(ResourceLocation location, int type) {
        String source = this.loadShaderSource(location);
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shaderId);
            String typeName = type == GL_VERTEX_SHADER ? "Vertex" : (type == GL_GEOMETRY_SHADER ? "Geometry" : "Fragment");
            glDeleteShader(shaderId);
            throw new RuntimeException("Compile error in " + typeName + " shader (" + location + "):\n" + log);
        }
        return shaderId;
    }

    private String loadShaderSource(ResourceLocation location) {
        try {
            Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resourceOptional.isEmpty()) throw new IOException("Shader assets file not found: " + location);
            try (InputStream stream = resourceOptional.get().open()) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader source: " + location, e);
        }
    }

    public void reload() { this.cleanup(); this.init(); }
    public void use() { glUseProgram(this.programId); this.isInUse = true; }

    public void use(Runnable action) {
        this.use();
        try { action.run(); } finally { this.stop(); }
    }

    public void stop() { glUseProgram(0); this.isInUse = false; }

    public void cleanup() {
        if (this.programId != 0) {
            glDeleteProgram(this.programId);
            this.programId = 0;
        }
        this.uniformLocationCache.clear();
        this.isInUse = false;
    }

    @Override
    public int getUniformLocation(String name) {
        if (this.programId <= 0) return -1;
        return this.uniformLocationCache.computeIfAbsent(name, key -> glGetUniformLocation(this.programId, key));
    }

    @Override
    public void checkActive() {
        if (!this.isInUse) throw new IllegalStateException("Geometry shader program '" + this.getProgramName() + "' is not active!");
    }

    @Override
    public ShaderUniformManager getUniformManager() { return this.uniformManager; }

    @Override
    public int getProgramID() { return this.programId; }

    @Override
    public String getProgramName() {
        if (this.geometryLocation == null) return "UnknownGeometry";
        String path = this.geometryLocation.getPath();
        int lastSlash = path.lastIndexOf('/');
        return lastSlash != -1 ? path.substring(lastSlash + 1) : path;
    }

    @Override
    public boolean isValid() { return this.programId > 0 && glIsProgram(this.programId); }

    public ResourceLocation getVertexLocation() { return this.vertexLocation; }
    public ResourceLocation getGeometryLocation() { return this.geometryLocation; }
    public ResourceLocation getFragmentLocation() { return this.fragmentLocation; }

    private void checkProgramLinkStatus(int id) {
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Geometry program linkage error: " + glGetProgramInfoLog(id));
        }
    }

    private void checkProgramValidateStatus(int id) {
        if (glGetProgrami(id, GL_VALIDATE_STATUS) == GL_FALSE) {
            System.err.println("Geometry program validation warning: " + glGetProgramInfoLog(id));
        }
    }
}