package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.visual.config.EffectConfig;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class EffectBuilder extends VisualBuilder<EffectConfig, EffectBuilder> {
    private PotionEffectType effectType;
    private int amplifier = 0;
    private int durationTicks = 200;
    private boolean ambient = false;
    private boolean particles = true;
    private boolean icon = true;

    private EffectBuilder() {
    }

    public static EffectBuilder create() {
        return new EffectBuilder();
    }

    public EffectBuilder effect(PotionEffectType effectType) {
        this.effectType = effectType;
        return this;
    }

    public EffectBuilder effect(String effectName) {
        this.effectType = PotionEffectType.getByName(effectName.toUpperCase());
        if (this.effectType == null) {
            throw new IllegalArgumentException("Invalid effect type: " + effectName);
        }
        return this;
    }

    public EffectBuilder amplifier(int amplifier) {
        this.amplifier = amplifier;
        return this;
    }

    public EffectBuilder level(int level) {
        return amplifier(level - 1);
    }

    public EffectBuilder durationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
        return this;
    }

    public EffectBuilder durationSeconds(int durationSeconds) {
        this.durationTicks = durationSeconds * 20;
        return this;
    }

    public EffectBuilder ambient(boolean ambient) {
        this.ambient = ambient;
        return this;
    }

    public EffectBuilder ambient() {
        return ambient(true);
    }

    public EffectBuilder particles(boolean particles) {
        this.particles = particles;
        return this;
    }

    public EffectBuilder noParticles() {
        return particles(false);
    }

    public EffectBuilder icon(boolean icon) {
        this.icon = icon;
        return this;
    }

    public EffectBuilder noIcon() {
        return icon(false);
    }

    public EffectBuilder infinite() {
        this.durationTicks = -1;
        return this;
    }

    public static EffectConfig fromString(String effectString) {
        return fromString(effectString, false, true, true);
    }

    public static EffectConfig fromString(String effectString, boolean forceInfinite, boolean particles, boolean icon) {
        String[] parts = effectString.split("\\|");
        EffectBuilder builder = create();

        if (parts.length >= 1) {
            builder.effect(parts[0].trim());
        }
        if (parts.length >= 2) {
            try {
                builder.amplifier(Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (parts.length >= 3) {
            String durationPart = parts[2].trim().toLowerCase();
            if (durationPart.equals("infinite") || durationPart.equals("-1")) {
                builder.infinite();
            } else {
                try {
                    builder.durationSeconds(Integer.parseInt(durationPart));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (forceInfinite) {
            builder.infinite();
        }

        builder.particles(particles);
        builder.icon(icon);
        builder.ambient();

        return builder.build();
    }

    @Override
    protected ValidationResult validateInternal() {
        List<String> errors = new ArrayList<>();

        if (effectType == null) {
            errors.add("Effect type must be specified");
        }

        if (amplifier < 0 || amplifier > 255) {
            errors.add("Amplifier must be between 0 and 255");
        }

        if (durationTicks < 1 && durationTicks != -1) {
            errors.add("Duration must be >= 1 tick or -1 for infinite");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    protected EffectConfig buildInternal() {
        EffectConfig config = EffectConfig.builder()
                .effectType(effectType)
                .amplifier(amplifier)
                .durationTicks(durationTicks)
                .ambient(ambient)
                .particles(particles)
                .icon(icon)
                .build();

        config.setEnabled(enabled);
        config.setUpdateInterval(updateInterval);

        return config;
    }
}
