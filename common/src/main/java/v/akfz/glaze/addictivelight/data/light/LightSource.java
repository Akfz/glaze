package v.akfz.glaze.addictivelight.data.light;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import v.akfz.aslib.render.color.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Setter
@Getter
public abstract class LightSource<T extends LightSource<T>> {

    private static final Map<String, Supplier<? extends LightSource<?>>> REGISTRY = new ConcurrentHashMap<>();

    static {
        register("simple", SimpleLightSource::new);
    }

    public static void register(String id, Supplier<? extends LightSource<?>> factory) {
        REGISTRY.put(id, factory);
    }

    public static LightSource<?> create(String id) {
        Supplier<? extends LightSource<?>> factory = REGISTRY.get(id);
        return factory != null ? factory.get() : null;
    }

    private final List<BlockPos> cachedShadowBlocks = new ArrayList<>();
    private final List<BlockEntity> cachedShadowBlockEntities = new ArrayList<>();

    private double prevX;
    private double prevY;
    private double prevZ;

    private double x;
    private double y;
    private double z;

    private float r = 1.0f;
    private float g = 1.0f;
    private float b = 1.0f;

    private boolean active = true;
    private boolean dynamic = true;
    private LightType type = LightType.POINT;
    private final Vector3f position = new Vector3f();
    private final Vector3f direction = new Vector3f(0f, 0f, -1f);
    private Color color;
    private float intensity = 1.0f;

    private float linear = 0.09f;
    private float quadratic = 0.032f;
    private float cutoff = 0.0f;
    private float outerCutoff = 50.0f;

    private float radius = 1.0f;
    private float width = 1.0f;
    private float height = 1.0f;

    private float shadowSoftness = 0.05f;
    private float shadowBias = 0.00f;
    private boolean shadowsEnabled = true;
    private boolean ignoreBlocks = false;

    private boolean volumetric = false;
    private float volumetricStrength = 0.0f;
    private float mieG = 0.0f;
    private float fogDensity = 0.05f;
    private float fogAbsorption = 0.1f;

    private float falloffExponent = 2.0f;
    private float sourceSize = 0.05f;
    private float shadowNear = 1f;
    private float shadowFar = 64.0f;

    public String debugInfo;
    public boolean isDirty = true;

    public boolean save = true;
    private UUID id;

    public LightSource(double x, double y, double z, float r, float g, float b, float radius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.radius = radius;
        this.position.set((float) x, (float) y, (float) z);
        this.color = new Color(r, g, b);

        this.id = UUID.randomUUID();
    }

    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }

    public abstract void toNBT(CompoundTag nbt);
    public abstract T fromNBT(CompoundTag nbt);
    public abstract String getTypeId();

    public boolean isOmnidirectional() {
        return this.type == LightType.POINT || this.type == LightType.AREA_SPHERE || this.type.name().equals("CUSTOM");
    }

    public Matrix4f[] getLightSpaceMatrices() {
        if (!isOmnidirectional()) {
            Matrix4f[] matrices = new Matrix4f[1];

            float fov = (float) Math.toRadians(this.outerCutoff * 2.0f);
            Matrix4f proj = new Matrix4f().perspective(fov, 1.0f, this.shadowNear, this.shadowFar);

            Vector3f target = new Vector3f(position).add(direction);
            Vector3f up = new Vector3f(0f, 1f, 0f);
            if (Math.abs(direction.y) > 0.99f) {
                up.set(0f, 0f, 1f);
            }
            Matrix4f view = new Matrix4f().lookAt(position, target, up);

            matrices[0] = proj.mul(view);
            return matrices;
        } else {
            Matrix4f[] matrices = new Matrix4f[6];

            float fov = (float) Math.toRadians(90.0f);
            Matrix4f proj = new Matrix4f().perspective(fov, 1.0f, this.shadowNear, this.shadowFar);

            Vector3f[] targets = {
                    new Vector3f(1, 0, 0),
                    new Vector3f(-1, 0, 0),
                    new Vector3f(0, 1, 0),
                    new Vector3f(0, -1, 0),
                    new Vector3f(0, 0, 1),
                    new Vector3f(0, 0, -1)
            };

            Vector3f[] ups = {
                    new Vector3f(0, -1, 0),
                    new Vector3f(0, -1, 0),
                    new Vector3f(0, 0, 1),
                    new Vector3f(0, 0, -1),
                    new Vector3f(0, -1, 0),
                    new Vector3f(0, -1, 0)
            };

            for (int i = 0; i < 6; i++) {
                Vector3f target = new Vector3f(position).add(targets[i]);
                Matrix4f view = new Matrix4f().lookAt(position, target, ups[i]);
                matrices[i] = new Matrix4f(proj).mul(view);
            }
            return matrices;
        }
    }

    public T active(boolean active) { this.active = active; this.isDirty = true; return self(); }
    public T dynamic(boolean dynamic) { this.dynamic = dynamic; this.isDirty = true; return self(); }
    public T type(LightType type) { this.type = type; this.isDirty = true; return self(); }
    public T position(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
        this.position.set((float) x, (float) y, (float) z);
        this.isDirty = true;
        return self();
    }
    public T direction(Vector3f dir) { this.direction.set(dir); this.isDirty = true; return self(); }
    public T color(Color color) {
        this.color = color;
        this.r = (float) color.getRed();
        this.g = (float) color.getGreen();
        this.b = (float) color.getBlue();
        this.isDirty = true;
        return self();
    }
    public T intensity(float intensity) { this.intensity = intensity; this.isDirty = true; return self(); }
    public T linear(float linear) { this.linear = linear; this.isDirty = true; return self(); }
    public T quadratic(float quadratic) { this.quadratic = quadratic; this.isDirty = true; return self(); }
    public T cutoff(float cutoff) { this.cutoff = cutoff; this.isDirty = true; return self(); }
    public T outerCutoff(float outerCutoff) { this.outerCutoff = outerCutoff; this.isDirty = true; return self(); }
    public T radius(float radius) { this.radius = radius; this.isDirty = true; return self(); }
    public T width(float width) { this.width = width; this.isDirty = true; return self(); }
    public T height(float height) { this.height = height; this.isDirty = true; return self(); }
    public T shadowSoftness(float softness) { this.shadowSoftness = softness; this.isDirty = true; return self(); }
    public T shadowBias(float bias) { this.shadowBias = bias; this.isDirty = true; return self(); }
    public T shadowsEnabled(boolean enabled) { this.shadowsEnabled = enabled; this.isDirty = true; return self(); }
    public T ignoreBlocks(boolean ignore) { this.ignoreBlocks = ignore; this.isDirty = true; return self(); }
    public T volumetric(boolean vol) { this.volumetric = vol; this.isDirty = true; return self(); }
    public T volumetricStrength(float strength) { this.volumetricStrength = strength; this.isDirty = true; return self(); }
    public T mieG(float mieG) { this.mieG = mieG; this.isDirty = true; return self(); }
    public T fogDensity(float density) { this.fogDensity = density; this.isDirty = true; return self(); }
    public T fogAbsorption(float absorption) { this.fogAbsorption = absorption; this.isDirty = true; return self(); }
    public T falloffExponent(float exp) { this.falloffExponent = exp; this.isDirty = true; return self(); }
    public T sourceSize(float size) { this.sourceSize = size; this.isDirty = true; return self(); }
    public T shadowNear(float near) { this.shadowNear = near; this.isDirty = true; return self(); }
    public T shadowFar(float far) { this.shadowFar = far; this.isDirty = true; return self(); }
    public T save(boolean save) { this.save = save; return self(); }

    protected void writeBaseNBT(CompoundTag nbt) {
        nbt.putString("TypeId", getTypeId());
        nbt.putDouble("x", this.x);
        nbt.putDouble("y", this.y);
        nbt.putDouble("z", this.z);
        nbt.putFloat("r", this.r);
        nbt.putFloat("g", this.g);
        nbt.putFloat("b", this.b);
        nbt.putBoolean("active", this.active);
        nbt.putBoolean("dynamic", this.dynamic);
        nbt.putInt("type", this.type != null ? this.type.ordinal() : 0);
        nbt.putFloat("intensity", this.intensity);
        nbt.putFloat("linear", this.linear);
        nbt.putFloat("quadratic", this.quadratic);
        nbt.putFloat("cutoff", this.cutoff);
        nbt.putFloat("outerCutoff", this.outerCutoff);
        nbt.putFloat("radius", this.radius);
        nbt.putFloat("width", this.width);
        nbt.putFloat("height", this.height);
        nbt.putBoolean("shadowsEnabled", this.shadowsEnabled);
        nbt.putBoolean("ignoreBlocks", this.ignoreBlocks);
        nbt.putBoolean("volumetric", this.volumetric);
        nbt.putFloat("volumetricStrength", this.volumetricStrength);
        nbt.putFloat("mieG", this.mieG);
        nbt.putFloat("fogDensity", this.fogDensity);
        nbt.putFloat("fogAbsorption", this.fogAbsorption);
        nbt.putFloat("falloffExponent", this.falloffExponent);

        nbt.putFloat("dirX", this.direction.x);
        nbt.putFloat("dirY", this.direction.y);
        nbt.putFloat("dirZ", this.direction.z);

        if (this.id != null) nbt.putUUID("id", this.id);
    }

    protected void readBaseNBT(CompoundTag nbt) {
        this.x = nbt.getDouble("x");
        this.y = nbt.getDouble("y");
        this.z = nbt.getDouble("z");
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        this.position.set((float) x, (float) y, (float) z);
        this.r = nbt.getFloat("r");
        this.g = nbt.getFloat("g");
        this.b = nbt.getFloat("b");
        this.color = new Color(r, g, b);
        this.active = nbt.getBoolean("active");
        this.dynamic = !nbt.contains("dynamic") || nbt.getBoolean("dynamic");
        this.type = LightType.values()[nbt.getInt("type") % LightType.values().length];
        this.intensity = nbt.getFloat("intensity");
        this.linear = nbt.getFloat("linear");
        this.quadratic = nbt.getFloat("quadratic");
        this.cutoff = nbt.getFloat("cutoff");
        this.outerCutoff = nbt.getFloat("outerCutoff");
        this.radius = nbt.getFloat("radius");
        this.width = nbt.getFloat("width");
        this.height = nbt.getFloat("height");
        this.shadowsEnabled = nbt.getBoolean("shadowsEnabled");
        this.ignoreBlocks = nbt.getBoolean("ignoreBlocks");
        this.volumetric = nbt.getBoolean("volumetric");
        this.volumetricStrength = nbt.getFloat("volumetricStrength");
        this.mieG = nbt.getFloat("mieG");
        this.fogDensity = nbt.getFloat("fogDensity");
        this.fogAbsorption = nbt.getFloat("fogAbsorption");
        this.falloffExponent = nbt.getFloat("falloffExponent");

        if (nbt.contains("dirX")) {
            this.direction.set(nbt.getFloat("dirX"), nbt.getFloat("dirY"), nbt.getFloat("dirZ"));
        }

        if (nbt.contains("id")) this.id = nbt.getUUID("id");
    }
}