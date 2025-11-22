package net.exylia.commons.items.processor;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AttributeProcessor {

    public static void applyHideAttributes(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addAttributeModifier(
                    Attribute.GENERIC_LUCK,
                    new AttributeModifier(UUID.randomUUID(), "luck_boost", 0.0, AttributeModifier.Operation.ADD_NUMBER)
            );
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
        }
    }

    public static void applyGlowing(ItemStack itemStack, boolean glowing) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (glowing) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeEnchant(Enchantment.UNBREAKING);
            }
            itemStack.setItemMeta(meta);
        }
    }

    public static void applyItemFlags(ItemStack itemStack, ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
    }
}
