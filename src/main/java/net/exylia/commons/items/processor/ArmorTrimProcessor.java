package net.exylia.commons.items.processor;

import net.exylia.commons.items.config.ArmorTrimConfig;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;

public class ArmorTrimProcessor {

    public static void apply(ItemStack itemStack, ArmorTrimConfig armorTrimConfig) {
        if (armorTrimConfig == null || !armorTrimConfig.hasConfiguration()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (!(meta instanceof ArmorMeta armorMeta)) {
            return;
        }

        try {
            armorTrimConfig.applyTrim(armorMeta);
            itemStack.setItemMeta(meta);
        } catch (Exception e) {
            DebugUtils.logInternalWarn("ArmorTrimProcessor: Failed to apply armor trim - " + e.getMessage());
        }
    }
}
