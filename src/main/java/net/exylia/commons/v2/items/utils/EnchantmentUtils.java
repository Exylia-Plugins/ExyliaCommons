package net.exylia.commons.v2.items.utils;

import net.exylia.commons.v2.compat.EnchantmentCompat;
import org.bukkit.enchantments.Enchantment;

public class EnchantmentUtils {

    /**
     * Resolves an enchantment by key or legacy name.
     *
     * <p>Delegates to {@link EnchantmentCompat}, which reads {@code Registry.ENCHANTMENT} and
     * maps legacy field names. The previous {@code Enchantment.values()} fallback would throw
     * {@link IncompatibleClassChangeError} on API versions where {@code Enchantment} is no
     * longer an enum.
     */
    public static Enchantment getByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return EnchantmentCompat.getByKey(name.toLowerCase());
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
