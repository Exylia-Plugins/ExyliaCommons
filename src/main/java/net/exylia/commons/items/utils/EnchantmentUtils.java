package net.exylia.commons.items.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public class EnchantmentUtils {

    public static Enchantment getByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        try {
            return Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Exception e) {
            for (Enchantment enchant : Enchantment.values()) {
                if (enchant.getKey().getKey().equalsIgnoreCase(name) ||
                    enchant.toString().equalsIgnoreCase(name)) {
                    return enchant;
                }
            }
            return null;
        }
    }

    public static int parseLevel(String levelStr) {
        try {
            return Integer.parseInt(levelStr.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static boolean isValidEnchantment(String name) {
        return getByName(name) != null;
    }
}
