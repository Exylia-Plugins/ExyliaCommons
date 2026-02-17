package net.exylia.commons.v2.visual.config;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.visual.compat.ParticleCompat;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class ParticleConfig extends VisualConfig {
    private final Particle particle;
    @Builder.Default
    private final int count = 1;
    @Builder.Default
    private final double offsetX = 0.0;
    @Builder.Default
    private final double offsetY = 0.0;
    @Builder.Default
    private final double offsetZ = 0.0;
    @Builder.Default
    private final double extra = 0.0;
    private final Color color;
    @Builder.Default
    private final ParticleScope scope = ParticleScope.PLAYER;
    private final Location location;

    @Override
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();

        if (particle == null) {
            errors.add("Particle must be specified");
        }

        if (count < 1) {
            errors.add("Count must be >= 1");
        }

        if (scope == ParticleScope.LOCATION && location == null) {
            errors.add("Location must be specified when scope is LOCATION");
        }

        if (ParticleCompat.isDustParticle(particle) && color == null) {
            errors.add("Color must be specified for DUST particle");
        }

        ValidationResult baseResult = validateBase();
        if (!baseResult.isValid()) {
            errors.addAll(baseResult.getErrors());
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    public enum ParticleScope {
        PLAYER,
        NEARBY,
        LOCATION
    }
}
