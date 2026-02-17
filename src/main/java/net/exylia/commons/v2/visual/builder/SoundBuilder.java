package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.visual.config.SoundConfig;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundBuilder extends VisualBuilder<SoundConfig, SoundBuilder> {
    private static final Map<String, Sound> SOUND_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean cacheInitialized = false;

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
        this.sound = resolveSound(soundName);
        if (this.sound == null) {
            throw new IllegalArgumentException("Invalid sound name: " + soundName);
        }
        return this;
    }

    private Sound resolveSound(String soundName) {
        Sound result = tryRegistryLookup(soundName);
        if (result != null) return result;

        result = tryEnumValueOf(soundName);
        if (result != null) return result;

        result = tryRegistrySearch(soundName);
        if (result != null) return result;

        return null;
    }

    private Sound tryRegistryLookup(String soundName) {
        try {
            String key = soundName.toLowerCase();
            if (!key.contains(":")) {
                key = "minecraft:" + key;
            }
            String[] parts = key.split(":", 2);
            NamespacedKey namespacedKey = new NamespacedKey(parts[0], parts[1]);
            return Registry.SOUNDS.get(namespacedKey);
        } catch (Exception e) {
            return null;
        }
    }

    private Sound tryRegistrySearch(String soundName) {
        try {
            initCacheIfNeeded();
            String normalized = normalize(soundName);
            return SOUND_CACHE.get(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase().replace("_", "").replace(".", "").replace(":", "");
    }

    @SuppressWarnings("unchecked")
    private static void initCacheIfNeeded() {
        if (cacheInitialized) return;
        synchronized (SOUND_CACHE) {
            if (cacheInitialized) return;
            try {
                Iterable<? extends Keyed> sounds = (Iterable<? extends Keyed>) Registry.SOUNDS;
                for (Keyed keyed : sounds) {
                    SOUND_CACHE.put(normalize(keyed.getKey().getKey()), (Sound) keyed);
                }
            } catch (Exception ignored) {}
            cacheInitialized = true;
        }
    }

    @SuppressWarnings("unchecked")
    private Sound tryEnumValueOf(String soundName) {
        try {
            Class<?> soundClass = Sound.class;
            if (!soundClass.isEnum()) return null;
            String enumName = soundName.toUpperCase().replace(".", "_").replace(":", "_");
            return (Sound) Enum.valueOf((Class<Enum>) soundClass, enumName);
        } catch (Exception e) {
            return null;
        }
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
        DebugAPI.logLibDebug(DebugCategory.VISUAL, "Sound string: " + soundString);
        if (parts.length >= 1) {
            DebugAPI.logLibDebug(DebugCategory.VISUAL, "Sound name: " + parts[0].trim());
            builder.sound(parts[0].trim());
        }
        if (parts.length >= 2) {
            try {
                DebugAPI.logLibDebug(DebugCategory.VISUAL, "Volume: " + parts[1].trim());
                builder.volume(Float.parseFloat(parts[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (parts.length >= 3) {
            try {
                DebugAPI.logLibDebug(DebugCategory.VISUAL, "Pitch: " + parts[2].trim());
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
