package net.exylia.commons.v2.visual.config;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class SoundConfig extends VisualConfig {
    private final Sound sound;
    @Builder.Default
    private final float volume = 1.0f;
    @Builder.Default
    private final float pitch = 1.0f;
    @Builder.Default
    private final SoundScope scope = SoundScope.PLAYER;
    private final Location location;

    @Override
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();

        if (sound == null) {
            errors.add("Sound must be specified");
        }

        if (volume < 0 || volume > Float.MAX_VALUE) {
            errors.add("Volume must be >= 0");
        }

        if (pitch < 0.5f || pitch > 2.0f) {
            errors.add("Pitch must be between 0.5 and 2.0");
        }

        if (scope == SoundScope.LOCATION && location == null) {
            errors.add("Location must be specified when scope is LOCATION");
        }

        ValidationResult baseResult = validateBase();
        if (!baseResult.isValid()) {
            errors.addAll(baseResult.getErrors());
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    public enum SoundScope {
        PLAYER,
        NEARBY,
        LOCATION
    }
}
