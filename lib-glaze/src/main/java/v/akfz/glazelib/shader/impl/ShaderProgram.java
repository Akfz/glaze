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

public class ShaderProgram implements IShaderProgram {
    private int programId;
    private final ResourceLocation vertexLocation;
    private final ResourceLocation fragmentLocation;
    private final String directFragmentSource;
    private final Map<String, Integer> uniformLocationCache = new ConcurrentHashMap<>();
    public ShaderUniformManager uniformManager;
    private boolean isInUse = false;

    public ShaderProgram(ResourceLocation vertexLocation, ResourceLocation fragmentLocation) {
        this.vertexLocation = vertexLocation;
        this.fragmentLocation = fragmentLocation;
        this.directFragmentSource = null;
        this.init();
    }

    public ShaderProgram(ResourceLocation vertexLocation, String fragmentSource) {
        this.vertexLocation = vertexLocation;
        this.fragmentLocation = null;
        this.directFragmentSource = fragmentSource;
        this.init();
    }

    public ResourceLocation getVertexLocation() { return vertexLocation; }
    public ResourceLocation getFragmentLocation() { return fragmentLocation; }

    private void init() {
        if (this.programId > 0) this.cleanup();
        this.programId = glCreateProgram();
        List<Integer> compiledShaders = new ArrayList<>();

        try {
            int vertexId = this.createShader(this.vertexLocation, GL_VERTEX_SHADER);
            glAttachShader(this.programId, vertexId);
            compiledShaders.add(vertexId);

            int fragmentId = (this.directFragmentSource != null)
                    ? this.createShaderFromSource(this.directFragmentSource, GL_FRAGMENT_SHADER, "DynamicFragmentShader")
                    : this.createShader(this.fragmentLocation, GL_FRAGMENT_SHADER);

            glAttachShader(this.programId, fragmentId);
            compiledShaders.add(fragmentId);

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
            throw new RuntimeException("Failed to link graphics shader program: " + this.getProgramName(), e);
        }
    }

    private int createShader(ResourceLocation location, int type) {
        return this.createShaderFromSource(this.loadShaderSource(location), type, location.toString());
    }

    private int createShaderFromSource(String source, int type, String name) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shaderId);
            String typeName = type == GL_VERTEX_SHADER ? "Vertex" : "Fragment";
            glDeleteShader(shaderId);
            throw new RuntimeException("Compile error in " + typeName + " shader (" + name + "):\n" + log);
        }
        return shaderId;
    }

    private String loadShaderSource(ResourceLocation location) {
        try {
            Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resourceOptional.isEmpty()) throw new IOException("Shader resource assets not found: " + location);
            try (InputStream stream = resourceOptional.get().open()) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader source file: " + location, e);
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
        if (!this.isInUse) throw new IllegalStateException("Shader program '" + this.getProgramName() + "' is not active!");
    }

    @Override
    public ShaderUniformManager getUniformManager() { return this.uniformManager; }

    @Override
    public int getProgramID() { return this.programId; }

    @Override
    public String getProgramName() {
        if (this.vertexLocation == null) return "Unknown";
        String path = this.vertexLocation.getPath();
        int lastSlash = path.lastIndexOf('/');
        return lastSlash != -1 ? path.substring(lastSlash + 1) : path;
    }

    @Override
    public boolean isValid() { return this.programId > 0 && glIsProgram(this.programId); }

    private void checkProgramLinkStatus(int id) {
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader program linkage error: " + glGetProgramInfoLog(id));
        }
    }

    private void checkProgramValidateStatus(int id) {
        if (glGetProgrami(id, GL_VALIDATE_STATUS) == GL_FALSE) {
            System.err.println("Shader program validation warning: " + glGetProgramInfoLog(id));
        }
    }
}