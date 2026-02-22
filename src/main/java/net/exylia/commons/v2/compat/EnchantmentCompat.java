package net.exylia.commons.v2.compat;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

public final class EnchantmentCompat {

    private static volatile Enchantment cachedUnbreaking;
    private static volatile boolean unbreakingResolved = false;

    private EnchantmentCompat() {}

    public static Enchantment getUnbreaking() {
        if (unbreakingResolved) return cachedUnbreaking;
        synchronized (EnchantmentCompat.class) {
            if (unbreakingResolved) return cachedUnbreaking;
            cachedUnbreaking = resolve("unbreaking");
            unbreakingResolved = true;
        }
        return cachedUnbreaking;
    }

    public static Enchantment getByKey(String key) {
        return resolve(key);
    }

    @SuppressWarnings("unchecked")
    private static Enchantment resolve(String key) {
        try {
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
            if (ench != null) return ench;
        } catch (Exception ignored) {}

        String legacyName = toLegacyFieldName(key);
        if (legacyName != null) {
            try {
                return (Enchantment) Enchantment.class.getField(legacyName).get(null);
            } catch (Exception ignored) {}
        }

        try {
            for (Keyed keyed : (Iterable<? extends Keyed>) Registry.ENCHANTMENT) {
                if (keyed.getKey().getKey().equals(key)) return (Enchantment) keyed;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static String toLegacyFieldName(String key) {
        return switch (key) {
            case "unbreaking"           -> "DURABILITY";
            case "efficiency"           -> "DIG_SPEED";
            case "fortune"              -> "LOOT_BONUS_BLOCKS";
            case "looting"              -> "LOOT_BONUS_MOBS";
            case "sharpness"            -> "DAMAGE_ALL";
            case "smite"                -> "DAMAGE_UNDEAD";
            case "bane_of_arthropods"   -> "DAMAGE_ARTHROPODS";
            case "protection"           -> "PROTECTION_ENVIRONMENTAL";
            case "fire_protection"      -> "PROTECTION_FIRE";
            case "blast_protection"     -> "PROTECTION_EXPLOSIONS";
            case "projectile_protection"-> "PROTECTION_PROJECTILE";
            case "feather_falling"      -> "PROTECTION_FALL";
            case "respiration"          -> "OXYGEN";
            case "aqua_affinity"        -> "WATER_WORKER";
            case "power"                -> "ARROW_DAMAGE";
            case "punch"                -> "ARROW_KNOCKBACK";
            case "flame"                -> "ARROW_FIRE";
            case "infinity"             -> "ARROW_INFINITE";
            case "luck_of_the_sea"      -> "LUCK";
            default                     -> null;
        };
    }
}
