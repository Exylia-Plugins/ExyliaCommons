package net.exylia.commons.v2.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Map;

public final class PotionTypeCompat {

    private static final Map<String, String> LEGACY_TO_KEY = Map.ofEntries(
        Map.entry("INSTANT_HEAL",     "healing"),
        Map.entry("INSTANT_DAMAGE",   "harming"),
        Map.entry("REGEN",            "regeneration"),
        Map.entry("REGENERATION",     "regeneration"),
        Map.entry("JUMP",             "leaping"),
        Map.entry("LEAPING",          "leaping"),
        Map.entry("SPEED",            "swiftness"),
        Map.entry("SWIFTNESS",        "swiftness"),
        Map.entry("STRENGTH",         "strength"),
        Map.entry("NIGHT_VISION",     "night_vision"),
        Map.entry("FIRE_RESISTANCE",  "fire_resistance"),
        Map.entry("WATER_BREATHING",  "water_breathing"),
        Map.entry("SLOW_FALLING",     "slow_falling"),
        Map.entry("POISON",           "poison"),
        Map.entry("WEAKNESS",         "weakness"),
        Map.entry("SLOWNESS",         "slowness"),
        Map.entry("INVISIBILITY",     "invisibility"),
        Map.entry("TURTLE_MASTER",    "turtle_master"),
        Map.entry("LUCK",             "luck"),
        Map.entry("UNLUCK",           "unluck"),
        Map.entry("AWKWARD",          "awkward"),
        Map.entry("MUNDANE",          "mundane"),
        Map.entry("THICK",            "thick"),
        Map.entry("WATER",            "water")
    );

    private PotionTypeCompat() {}

    public static PotionType resolve(String name, boolean extended, boolean upgraded) {
        String base = toBaseKey(name);
        if (upgraded) {
            PotionType strong = get("strong_" + base);
            if (strong != null) return strong;
        }
        if (extended) {
            PotionType longType = get("long_" + base);
            if (longType != null) return longType;
        }
        PotionType type = get(base);
        return type != null ? type : fallback();
    }

    public static void applyToMeta(PotionMeta meta, String name, boolean extended, boolean upgraded) {
        PotionType type = resolve(name, extended, upgraded);
        try {
            meta.setBasePotionType(type);
        } catch (NoSuchMethodError e) {
            try {
                meta.getClass().getMethod("setBasePotionData", org.bukkit.potion.PotionData.class)
                    .invoke(meta, new org.bukkit.potion.PotionData(type, extended, upgraded));
            } catch (Exception ignored) {}
        }
    }

    private static String toBaseKey(String name) {
        if (name == null) return "water";
        String mapped = LEGACY_TO_KEY.get(name.toUpperCase());
        if (mapped != null) return mapped;
        String lower = name.toLowerCase();
        return lower.contains(":") ? lower.split(":")[1] : lower;
    }

    private static PotionType get(String key) {
        try {
            return Registry.POTION.get(NamespacedKey.minecraft(key));
        } catch (Exception e) {
            return null;
        }
    }

    private static PotionType fallback() {
        PotionType water = get("water");
        return water != null ? water : Registry.POTION.iterator().next();
    }
}
