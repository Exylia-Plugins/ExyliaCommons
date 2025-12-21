package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.v2.visual.config.SoundConfig;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

public class SoundBuilder extends VisualBuilder<SoundConfig, SoundBuilder> {
    private Sound sound;
    private float volume = 1.0f;
    private float pitch = 1.0f;
    private SoundConfig.SoundScope scope = SoundConfig.SoundScope.PLAYER;
    private Location location;

    private SoundBuilder() {
    }

    public static SoundBuilder create() {
        return new SoundBuilder();
    }

    public SoundBuilder sound(Sound sound) {
        this.sound = sound;
        return this;
    }

    public SoundBuilder sound(String soundName) {
        try {
            this.sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid sound name: " + soundName);
        }
        return this;
    }

    public SoundBuilder volume(float volume) {
        this.volume = volume;
        return this;
    }

    public SoundBuilder pitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public SoundBuilder scope(SoundConfig.SoundScope scope) {
        this.scope = scope;
        return this;
    }

    public SoundBuilder location(Location location) {
        this.location = location;
        return this;
    }

    public SoundBuilder nearby() {
        return scope(SoundConfig.SoundScope.NEARBY);
    }

    public SoundBuilder atLocation(Location location) {
        this.scope = SoundConfig.SoundScope.LOCATION;
        this.location = location;
        return this;
    }

    public static SoundConfig fromString(String soundString) {
        String[] parts = soundString.split("\\|");
        SoundBuilder builder = create();
        DebugUtils.logInternalDebug("Sound string: " + soundString);
        if (parts.length >= 1) {
            DebugUtils.logInternalDebug("Sound name: " + parts[0].trim());
            builder.sound(parts[0].trim());
        }
        if (parts.length >= 2) {
            try {
                DebugUtils.logInternalDebug("Volume: " + parts[1].trim());
                builder.volume(Float.parseFloat(parts[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (parts.length >= 3) {
            try {
                DebugUtils.logInternalDebug("Pitch: " + parts[2].trim());
                builder.pitch(Float.parseFloat(parts[2].trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        return builder.build();
    }

    @Override
    protected ValidationResult validateInternal() {
        List<String> errors = new ArrayList<>();

        if (sound == null) {
            errors.add("Sound must be specified");
        }

        if (volume < 0) {
            errors.add("Volume must be >= 0");
        }

        if (pitch < 0.5f || pitch > 2.0f) {
            errors.add("Pitch must be between 0.5 and 2.0");
        }

        if (scope == SoundConfig.SoundScope.LOCATION && location == null) {
            errors.add("Location must be specified when scope is LOCATION");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    protected SoundConfig buildInternal() {
        SoundConfig config = SoundConfig.builder()
                .sound(sound)
                .volume(volume)
                .pitch(pitch)
                .scope(scope)
                .location(location)
                .build();

        config.setEnabled(enabled);
        config.setUpdateInterval(updateInterval);
        config.setPermanent(permanent);

        return config;
    }
}
