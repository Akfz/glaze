package v.akfz.glaze.addictivelight.data.light;

import net.minecraft.nbt.CompoundTag;

public class SimpleLightSource extends LightSource<SimpleLightSource> {

    public SimpleLightSource(LightType type) {
        this();
        this.type(type);

        java.util.concurrent.ThreadLocalRandom rand = java.util.concurrent.ThreadLocalRandom.current();

        float randR = rand.nextFloat();
        float randG = rand.nextFloat();
        float randB = rand.nextFloat();
        this.color(new v.akfz.aslib.render.color.Color(randR, randG, randB));

        boolean isVolumetric = rand.nextBoolean();
        this.volumetric(isVolumetric);
        this.volumetricStrength(isVolumetric ? rand.nextFloat() * 4.5f + 0.5f : 0.0f);
        this.mieG(rand.nextFloat() * 1.3f - 0.5f);
        this.fogDensity(rand.nextFloat() * 0.29f + 0.01f);
        this.fogAbsorption(rand.nextFloat() * 0.29f + 0.01f);
        this.falloffExponent(rand.nextFloat() * 3.0f + 1.0f);

        if (type == LightType.SPOT) {
            float inner = rand.nextFloat() * 20.0f + 15.0f;
            this.setCutoff(inner);
            this.setOuterCutoff(inner + rand.nextFloat() * 15.0f + 5.0f);
            this.setRadius(rand.nextFloat() * 22.0f + 10.0f);
            this.setIntensity(rand.nextFloat() * 60.0f + 20.0f);
            this.setLinear(rand.nextFloat() * 0.07f + 0.01f);
            this.setQuadratic(rand.nextFloat() * 0.025f + 0.005f);
        } else if (type == LightType.AREA_RECTANGLE) {
            this.setWidth(rand.nextFloat() * 7.0f + 1.0f);
            this.setHeight(rand.nextFloat() * 5.0f + 1.0f);
            this.setOuterCutoff(rand.nextFloat() * 30.0f + 45.0f);
            this.setRadius(rand.nextFloat() * 16.0f + 8.0f);
            this.setIntensity(rand.nextFloat() * 70.0f + 30.0f);
        } else if (type == LightType.AREA_SPHERE) {
            this.setSourceSize(rand.nextFloat() * 2.5f + 0.5f);
            this.setRadius(rand.nextFloat() * 12.0f + 8.0f);
            this.setIntensity(rand.nextFloat() * 40.0f + 20.0f);
        } else if (type == LightType.POINT) {
            this.setRadius(rand.nextFloat() * 18.0f + 6.0f);
            this.setIntensity(rand.nextFloat() * 40.0f + 10.0f);
            this.setLinear(rand.nextFloat() * 0.09f + 0.01f);
            this.setQuadratic(rand.nextFloat() * 0.04f + 0.01f);
        }

        this.setDirty(true);
    }

    public SimpleLightSource() {
        super(0, 0, 0, 1.0f, 1.0f, 1.0f, 10.0f);
    }

    @Override
    public void toNBT(CompoundTag nbt) {
        this.writeBaseNBT(nbt);
    }

    @Override
    public SimpleLightSource fromNBT(CompoundTag nbt) {
        this.readBaseNBT(nbt);
        return this;
    }

    @Override
    public String getTypeId() {
        return "simple";
    }
}