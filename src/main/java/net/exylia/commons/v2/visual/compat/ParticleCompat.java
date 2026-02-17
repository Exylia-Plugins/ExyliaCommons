package net.exylia.commons.v2.visual.compat;

import org.bukkit.Particle;

public final class ParticleCompat {
    private static final Particle DUST_PARTICLE;

    static {
        Particle dust = null;
        try {
            dust = Particle.valueOf("DUST");
        } catch (IllegalArgumentException e) {
            try {
                dust = Particle.valueOf("REDSTONE");
            } catch (IllegalArgumentException ignored) {
            }
        }
        DUST_PARTICLE = dust;
    }

    private ParticleCompat() {
    }

    public static Particle getDustParticle() {
        return DUST_PARTICLE;
    }

    public static boolean isDustParticle(Particle particle) {
        return DUST_PARTICLE != null && particle == DUST_PARTICLE;
    }
}
