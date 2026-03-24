package net.exylia.commons.v2.visual.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class FireworkConfig extends VisualConfig {
    @Builder.Default
    private final FireworkEffect.Type type = FireworkEffect.Type.BALL;
    @Singular
    private final List<Color> colors;
    @Singular("fadeColor")
    private final List<Color> fadeColors;
    @Builder.Default
    private final boolean flicker = false;
    @Builder.Default
    private final boolean trail = false;
    @Builder.Default
    private final int power = 1;
    private final Location location;

    @Override
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();

        if (type == null) {
            errors.add("Firework type must be specified");
        }

        if (colors == null || colors.isEmpty()) {
            errors.add("At least one color must be specified");
        }

        if (power < 0 || power > 3) {
            errors.add("Power must be between 0 and 3");
        }

        ValidationResult baseResult = validateBase();
        if (!baseResult.isValid()) {
            errors.addAll(baseResult.getErrors());
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
