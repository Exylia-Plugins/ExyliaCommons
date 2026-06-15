package net.exylia.commons.v2.compat;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SoundCompat {

    private static final Map<String, Sound> NORMALIZED_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> NORMALIZED_KEY_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean cacheInitialized = false;

    private SoundCompat() {}

    public static String keyStringOf(String name) {
        if (name == null || name.isBlank()) return null;
        String lower = name.toLowerCase();
        if (!lower.contains(":")) lower = "minecraft:" + lower;
        String[] parts = lower.split(":", 2);
        try {
            if (Registry.SOUNDS.get(new NamespacedKey(parts[0], parts[1])) != null)
                return parts[0] + ":" + parts[1];
            String dot = parts[1].replace("_", ".");
            if (Registry.SOUNDS.get(new NamespacedKey(parts[0], dot)) != null)
                return parts[0] + ":" + dot;
        } catch (Exception ignored) {}
        initCacheIfNeeded();
        return NORMALIZED_KEY_CACHE.get(normalize(name));
    }

    public static Sound fromName(String name) {
        if (name == null || name.isBlank()) return null;

        Sound result = tryRegistry(name);
        if (result != null) return result;

        result = tryEnumValueOf(name);
        if (result != null) return result;

        return tryNormalizedSearch(name);
    }

    private static Sound tryRegistry(String name) {
        try {
            String lower = name.toLowerCase();
            if (!lower.contains(":")) lower = "minecraft:" + lower;
            String[] parts = lower.split(":", 2);
            Sound result = Registry.SOUNDS.get(new NamespacedKey(parts[0], parts[1]));
            if (result != null) return result;

            String dotKey = parts[1].replace("_", ".");
            return Registry.SOUNDS.get(new NamespacedKey(parts[0], dotKey));
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Sound tryEnumValueOf(String name) {
        try {
            Class<?> cls = Sound.class;
            if (!cls.isEnum()) return null;
            String enumName = name.toUpperCase().replace(".", "_").replace(":", "_");
            return (Sound) Enum.valueOf((Class<Enum>) cls, enumName);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Sound tryNormalizedSearch(String name) {
        initCacheIfNeeded();
        return NORMALIZED_CACHE.get(normalize(name));
    }

    @SuppressWarnings("unchecked")
    private static void initCacheIfNeeded() {
        if (cacheInitialized) return;
        synchronized (NORMALIZED_CACHE) {
            if (cacheInitialized) return;
            try {
                for (Keyed keyed : (Iterable<? extends Keyed>) Registry.SOUNDS) {
                    String keyPart = keyed.getKey().getKey();
                    String normalized = normalize(keyPart);
                    NORMALIZED_CACHE.put(normalized, (Sound) keyed);
                    NORMALIZED_KEY_CACHE.put(normalized, keyed.getKey().getNamespace() + ":" + keyPart);
                }
            } catch (Exception ignored) {}
            cacheInitialized = true;
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase().replace("_", "").replace(".", "").replace(":", "");
    }
}
