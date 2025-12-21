package net.exylia.commons.v2.items.processor;

import net.exylia.commons.v2.items.utils.EnchantmentUtils;
import net.exylia.commons.v2.placeholders.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class EnchantmentProcessor {

    public static void apply(ItemStack itemStack, Map<String, Integer> enchantments,
                           Player player, PlaceholderContext context) {
        if (enchantments == null || enchantments.isEmpty()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            String enchantName = entry.getKey();
            String levelStr = String.valueOf(entry.getValue());

            if (player != null && context != null) {
                enchantName = Placeholders.process(enchantName, player, context);
                levelStr = Placeholders.process(levelStr, player, context);
            }

            try {
                Enchantment enchantment = EnchantmentUtils.getByName(enchantName);
                int level = EnchantmentUtils.parseLevel(levelStr);

                if (enchantment != null) {
                    meta.addEnchant(enchantment, level, true);
                }
            } catch (Exception ignored) {
            }
        }

        itemStack.setItemMeta(meta);
    }

    public static void apply(ItemStack itemStack, Map<String, Integer> enchantments) {
        apply(itemStack, enchantments, null, null);
    }
}
