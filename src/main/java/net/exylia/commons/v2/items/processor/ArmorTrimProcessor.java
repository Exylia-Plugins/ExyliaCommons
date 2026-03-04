package net.exylia.commons.v2.items.processor;

import net.exylia.commons.v2.items.config.ArmorTrimConfig;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;

public class ArmorTrimProcessor {

    public static void apply(ItemStack itemStack, ArmorTrimConfig armorTrimConfig,
                             Player player, PlaceholderContext context) {
        if (armorTrimConfig == null || !armorTrimConfig.hasConfiguration()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (!(meta instanceof ArmorMeta armorMeta)) {
            return;
        }

        try {
            armorTrimConfig.applyTrim(armorMeta, player, context);
            itemStack.setItemMeta(meta);
        } catch (Exception e) {
            DebugAPI.logLibWarn("ArmorTrimProcessor: Failed to apply armor trim - " + e.getMessage());
        }
    }

    public static void apply(ItemStack itemStack, ArmorTrimConfig armorTrimConfig) {
        apply(itemStack, armorTrimConfig, null, null);
    }
}
