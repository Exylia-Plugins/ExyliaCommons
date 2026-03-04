package net.exylia.commons.v2.compat;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SoundCompat {

    private static final Map<String, Sound> NORMALIZED_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean cacheInitialized = false;

    private SoundCompat() {}

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
            String key = name.toLowerCase();
            if (!key.contains(":")) key = "minecraft:" + key;
            String[] parts = key.split(":", 2);
            return Registry.SOUNDS.get(new NamespacedKey(parts[0], parts[1]));
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
                    NORMALIZED_CACHE.put(normalize(keyed.getKey().getKey()), (Sound) keyed);
                }
            } catch (Exception ignored) {}
            cacheInitialized = true;
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase().replace("_", "").replace(".", "").replace(":", "");
    }
}
