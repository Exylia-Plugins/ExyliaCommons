package net.exylia.commons.selection.visualizer;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Particle;

@Getter
@Setter
public class ParticleConfig {
    private Particle particleType;
    private int particleCount;
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    private double speed;
    private long updateInterval;
    private long maxVolumeForVisualization;
    private boolean enabledByDefault;

    public ParticleConfig() {
        this.particleType = Particle.SOUL_FIRE_FLAME;
        this.particleCount = 1;
        this.offsetX = 0.0;
        this.offsetY = 0.0;
        this.offsetZ = 0.0;
        this.speed = 0.0;
        this.updateInterval = 4L;  
        this.maxVolumeForVisualization = 100000L;  
        this.enabledByDefault = true;
    }

    public ParticleConfig particleType(Particle particleType) {
        this.particleType = particleType;
        return this;
    }

    public ParticleConfig particleCount(int count) {
        this.particleCount = count;
        return this;
    }

    public ParticleConfig offset(double x, double y, double z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        return this;
    }

    public ParticleConfig speed(double speed) {
        this.speed = speed;
        return this;
    }

    public ParticleConfig updateInterval(long ticks) {
        this.updateInterval = ticks;
        return this;
    }

    public ParticleConfig maxVolume(long maxVolume) {
        this.maxVolumeForVisualization = maxVolume;
        return this;
    }

    public ParticleConfig enabledByDefault(boolean enabled) {
        this.enabledByDefault = enabled;
        return this;
    }

    public static ParticleConfig subtle() {
        return new ParticleConfig()
                .particleType(Particle.CRIT)
                .speed(0.05)
                .updateInterval(8L);
    }

    public static ParticleConfig vibrant() {
        return new ParticleConfig()
                .particleType(Particle.FLAME)
                .particleCount(2)
                .speed(0.2)
                .updateInterval(2L);
    }

    public static ParticleConfig magical() {
        return new ParticleConfig()
                .particleType(Particle.ENCHANTMENT_TABLE)
                .particleCount(3)
                .offset(0.1, 0.1, 0.1)
                .speed(0.3)
                .updateInterval(3L);
    }

    public static ParticleConfig performance() {
        return new ParticleConfig()
                .particleType(Particle.CRIT)
                .particleCount(1)
                .speed(0.1)
                .updateInterval(10L)
                .maxVolume(1000000L);
    }
}
