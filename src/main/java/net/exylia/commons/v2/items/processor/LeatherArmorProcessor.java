package net.exylia.commons.v2.items.processor;

import net.exylia.commons.v2.items.config.LeatherArmorConfig;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class LeatherArmorProcessor {

    public static void apply(ItemStack itemStack, LeatherArmorConfig leatherArmorConfig,
                           Player player, PlaceholderContext context) {
        if (leatherArmorConfig == null || !leatherArmorConfig.hasConfiguration()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (!(meta instanceof LeatherArmorMeta leatherMeta)) {
            return;
        }

        try {
            leatherArmorConfig.applyColor(leatherMeta, player, context);
            itemStack.setItemMeta(meta);
        } catch (Exception e) {
            DebugAPI.logLibWarn("LeatherArmorProcessor: Failed to apply leather armor color - " + e.getMessage());
        }
    }

    public static void apply(ItemStack itemStack, LeatherArmorConfig leatherArmorConfig) {
        apply(itemStack, leatherArmorConfig, null, null);
    }
}
