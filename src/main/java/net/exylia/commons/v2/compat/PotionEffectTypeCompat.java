package net.exylia.commons.v2.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public final class PotionEffectTypeCompat {

    private static final Map<String, String> LEGACY_TO_KEY = Map.ofEntries(
        Map.entry("SLOW",               "slowness"),
        Map.entry("FAST_DIGGING",       "haste"),
        Map.entry("SLOW_DIGGING",       "mining_fatigue"),
        Map.entry("INCREASE_DAMAGE",    "strength"),
        Map.entry("HEAL",               "instant_health"),
        Map.entry("HARM",               "instant_damage"),
        Map.entry("JUMP",               "jump_boost"),
        Map.entry("CONFUSION",          "nausea"),
        Map.entry("REGENERATION",       "regeneration"),
        Map.entry("DAMAGE_RESISTANCE",  "resistance"),
        Map.entry("FIRE_RESISTANCE",    "fire_resistance"),
        Map.entry("WATER_BREATHING",    "water_breathing"),
        Map.entry("INVISIBILITY",       "invisibility"),
        Map.entry("BLINDNESS",          "blindness"),
        Map.entry("NIGHT_VISION",       "night_vision"),
        Map.entry("HUNGER",             "hunger"),
        Map.entry("WEAKNESS",           "weakness"),
        Map.entry("POISON",             "poison"),
        Map.entry("WITHER",             "wither"),
        Map.entry("HEALTH_BOOST",       "health_boost"),
        Map.entry("ABSORPTION",         "absorption"),
        Map.entry("SATURATION",         "saturation"),
        Map.entry("GLOWING",            "glowing"),
        Map.entry("LEVITATION",         "levitation"),
        Map.entry("LUCK",               "luck"),
        Map.entry("UNLUCK",             "unluck"),
        Map.entry("SLOW_FALLING",       "slow_falling"),
        Map.entry("CONDUIT_POWER",      "conduit_power"),
        Map.entry("DOLPHINS_GRACE",     "dolphins_grace"),
        Map.entry("BAD_OMEN",           "bad_omen"),
        Map.entry("HERO_OF_THE_VILLAGE","hero_of_the_village"),
        Map.entry("DARKNESS",           "darkness")
    );

    public static final PotionEffectType SLOWNESS        = resolve("SLOWNESS");
    public static final PotionEffectType HASTE           = resolve("HASTE");
    public static final PotionEffectType MINING_FATIGUE  = resolve("MINING_FATIGUE");
    public static final PotionEffectType STRENGTH        = resolve("STRENGTH");
    public static final PotionEffectType INSTANT_HEALTH  = resolve("INSTANT_HEALTH");
    public static final PotionEffectType INSTANT_DAMAGE  = resolve("INSTANT_DAMAGE");
    public static final PotionEffectType JUMP_BOOST      = resolve("JUMP_BOOST");
    public static final PotionEffectType NAUSEA          = resolve("NAUSEA");
    public static final PotionEffectType REGENERATION    = resolve("REGENERATION");
    public static final PotionEffectType RESISTANCE      = resolve("RESISTANCE");
    public static final PotionEffectType FIRE_RESISTANCE = resolve("FIRE_RESISTANCE");
    public static final PotionEffectType WATER_BREATHING = resolve("WATER_BREATHING");
    public static final PotionEffectType INVISIBILITY    = resolve("INVISIBILITY");
    public static final PotionEffectType BLINDNESS       = resolve("BLINDNESS");
    public static final PotionEffectType NIGHT_VISION    = resolve("NIGHT_VISION");
    public static final PotionEffectType HUNGER          = resolve("HUNGER");
    public static final PotionEffectType WEAKNESS        = resolve("WEAKNESS");
    public static final PotionEffectType POISON          = resolve("POISON");
    public static final PotionEffectType SPEED           = resolve("SPEED");

    private PotionEffectTypeCompat() {}

    public static PotionEffectType resolve(String name) {
        if (name == null) return null;
        String upper = name.toUpperCase();
        String key = LEGACY_TO_KEY.getOrDefault(upper, name.toLowerCase());
        try {
            return Registry.EFFECT.get(NamespacedKey.minecraft(key));
        } catch (Exception e) {
            try {
                return PotionEffectType.getByName(upper);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
