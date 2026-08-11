package net.exylia.commons.v2.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ParticleCompat {

    private static final Map<String, Particle> NORMALIZED_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean cacheInitialized = false;

    private ParticleCompat() {}

    public static Particle fromName(String name) {
        if (name == null || name.isBlank()) return null;

        Particle result = tryRegistry(name);
        if (result != null) return result;

        String modernKey = toModernKey(name.toUpperCase());
        if (modernKey != null) {
            result = tryRegistry(modernKey);
            if (result != null) return result;
        }

        return tryNormalizedSearch(name);
    }

    /**
     * Every particle known to the running server, as {@code UPPER_SNAKE_CASE} ids accepted by
     * {@link #fromName(String)}.
     *
     * <p>Read from {@link Registry#PARTICLE_TYPE} rather than {@code Particle.values()}, so this
     * keeps working if/when {@code Particle} stops being an enum — the same change that broke
     * {@code Sound.values()} with {@link IncompatibleClassChangeError}.
     */
    public static List<String> allNames() {
        List<String> names = new ArrayList<>();
        try {
            for (Particle particle : Registry.PARTICLE_TYPE) {
                names.add(particle.getKey().getKey().toUpperCase(Locale.ROOT));
            }
        } catch (Throwable ignored) {
            // Throwable, not Exception: API drift surfaces as LinkageError, which is not an
            // Exception and would otherwise escape into the caller's task.
        }
        return names;
    }

    private static Particle tryRegistry(String name) {
        try {
            String key = name.toLowerCase().replace(" ", "_");
            if (!key.contains(":")) key = "minecraft:" + key;
            String[] parts = key.split(":", 2);
            return Registry.PARTICLE_TYPE.get(new NamespacedKey(parts[0], parts[1]));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Particle tryNormalizedSearch(String name) {
        initCacheIfNeeded();
        return NORMALIZED_CACHE.get(normalize(name));
    }

    private static void initCacheIfNeeded() {
        if (cacheInitialized) return;
        synchronized (NORMALIZED_CACHE) {
            if (cacheInitialized) return;
            for (Particle p : Registry.PARTICLE_TYPE) {
                NORMALIZED_CACHE.put(normalize(p.getKey().getKey()), p);
            }
            cacheInitialized = true;
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase().replace("_", "").replace(".", "").replace(":", "");
    }

    private static String toModernKey(String legacyBukkitName) {
        return switch (legacyBukkitName) {
            case "EXPLOSION_NORMAL"    -> "poof";
            case "EXPLOSION_LARGE"     -> "explosion";
            case "EXPLOSION_HUGE"      -> "explosion_emitter";
            case "FIREWORKS_SPARK"     -> "firework";
            case "WATER_BUBBLE"        -> "bubble";
            case "WATER_SPLASH"        -> "splash";
            case "WATER_WAKE"          -> "fishing";
            case "SUSPENDED"           -> "underwater";
            case "SUSPENDED_DEPTH"     -> "underwater";
            case "CRIT_MAGIC"          -> "enchanted_hit";
            case "SMOKE_NORMAL"        -> "smoke";
            case "SMOKE_LARGE"         -> "large_smoke";
            case "SPELL"               -> "effect";
            case "SPELL_INSTANT"       -> "instant_effect";
            case "SPELL_MOB"           -> "entity_effect";
            case "SPELL_MOB_AMBIENT"   -> "ambient_entity_effect";
            case "SPELL_WITCH"         -> "witch";
            case "DRIP_WATER"          -> "dripping_water";
            case "DRIP_LAVA"           -> "dripping_lava";
            case "TOWN_AURA"           -> "mycelium";
            case "WATER_DROP"          -> "rain";
            case "ITEM_CRACK"          -> "item";
            case "BLOCK_CRACK"         -> "block";
            case "BLOCK_DUST"          -> "falling_dust";
            case "REDSTONE"            -> "dust";
            case "SNOW_SHOVEL"         -> "item_snowball";
            case "MOB_APPEARANCE"      -> "elder_guardian";
            case "FOOTSTEP"            -> "block_marker";
            default                    -> null;
        };
    }

    public static boolean isDustParticle(Particle particle) {
        if (particle == null) return false;
        Particle dust = fromName("dust");
        return particle == dust;
    }

    public static boolean isBlockParticle(Particle particle) {
        if (particle == null) return false;
        String key = particle.getKey().getKey();
        return key.equals("block") || key.equals("falling_dust") || key.equals("block_marker");
    }

    public static boolean isItemParticle(Particle particle) {
        if (particle == null) return false;
        return particle.getKey().getKey().equals("item");
    }

    public static Particle getDustParticle() {
        return fromName("dust");
    }
}
