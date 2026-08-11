package net.exylia.commons.v2.effect.model;

import net.exylia.commons.v2.visual.config.ParticleConfig;
import net.exylia.commons.v2.visual.config.SoundConfig;

/**
 * Who receives an effect.
 *
 * <ul>
 *   <li>{@link #PLAYER} — only the context player.</li>
 *   <li>{@link #NEARBY} — players around the context player (Bukkit default range).</li>
 *   <li>{@link #LOCATION} — everyone who can see/hear the context location.</li>
 *   <li>{@link #RADIUS} — players within {@code radius} blocks of the context location.</li>
 *   <li>{@link #GLOBAL} — every online player.</li>
 * </ul>
 */
public enum EffectScope {
    PLAYER,
    NEARBY,
    LOCATION,
    RADIUS,
    GLOBAL;

    public static EffectScope fromName(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return the closest {@link ParticleConfig.ParticleScope}. {@code RADIUS} and {@code GLOBAL}
     * are handled by the executor, not by the renderer, so they map to {@code LOCATION}.
     */
    public ParticleConfig.ParticleScope toParticleScope() {
        return switch (this) {
            case PLAYER -> ParticleConfig.ParticleScope.PLAYER;
            case NEARBY -> ParticleConfig.ParticleScope.NEARBY;
            case LOCATION, RADIUS, GLOBAL -> ParticleConfig.ParticleScope.LOCATION;
        };
    }

    /**
     * @return the closest {@link SoundConfig.SoundScope}. {@code RADIUS} and {@code GLOBAL}
     * are handled by the executor, not by the renderer, so they map to {@code LOCATION}.
     */
    public SoundConfig.SoundScope toSoundScope() {
        return switch (this) {
            case PLAYER -> SoundConfig.SoundScope.PLAYER;
            case NEARBY -> SoundConfig.SoundScope.NEARBY;
            case LOCATION, RADIUS, GLOBAL -> SoundConfig.SoundScope.LOCATION;
        };
    }

    /**
     * @return true when the effect must be resolved against a location rather than a single player.
     */
    public boolean requiresLocation() {
        return this == LOCATION || this == RADIUS;
    }
}
