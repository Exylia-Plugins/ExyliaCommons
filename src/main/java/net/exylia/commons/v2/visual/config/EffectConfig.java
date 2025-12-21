package net.exylia.commons.v2.visual.config;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class EffectConfig extends VisualConfig {
    private final PotionEffectType effectType;
    @Builder.Default
    private final int amplifier = 0;
    @Builder.Default
    private final int durationTicks = 200;
    @Builder.Default
    private final boolean ambient = false;
    @Builder.Default
    private final boolean particles = true;
    @Builder.Default
    private final boolean icon = true;

    @Override
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();

        if (effectType == null) {
            errors.add("Effect type must be specified");
        }

        if (amplifier < 0 || amplifier > 255) {
            errors.add("Amplifier must be between 0 and 255");
        }

        if (durationTicks < 1) {
            errors.add("Duration must be >= 1 tick");
        }

        ValidationResult baseResult = validateBase();
        if (!baseResult.isValid()) {
            errors.addAll(baseResult.getErrors());
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
