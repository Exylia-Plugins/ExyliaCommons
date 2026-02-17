package net.exylia.commons.v2.items.processor;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;

public class AttributeProcessor {

    private static boolean supportsDataComponentAPI = false;
    private static Class<?> dataComponentTypeKeysClass = null;
    private static Class<?> dataComponentTypesClass = null;
    private static Class<?> itemDisplayClass = null;

    static {
        detectDataComponentAPISupport();
    }

    private static void detectDataComponentAPISupport() {
        try {
            dataComponentTypeKeysClass = Class.forName("io.papermc.paper.registry.keys.DataComponentTypeKeys");
            dataComponentTypesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            itemDisplayClass = Class.forName("io.papermc.paper.datacomponent.item.ItemAttributeModifiers");
            supportsDataComponentAPI = true;
        } catch (Exception e) {
            supportsDataComponentAPI = false;
        }
    }

    public static void applyHideAttributes(ItemStack itemStack) {
        if (itemStack == null) return;

        if (supportsDataComponentAPI) {
            try {
                hideAttributesUsingDataComponent(itemStack);
            } catch (Exception e) {
                hideAttributesUsingItemFlag(itemStack);
            }
        } else {
            hideAttributesUsingItemFlag(itemStack);
        }
    }

    @SuppressWarnings("unchecked")
    private static void hideAttributesUsingDataComponent(ItemStack itemStack) {
        try {
            Class<?> dataComponentTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType");
            Class<?> registryAccessClass = Class.forName("io.papermc.paper.registry.RegistryAccess");
            Class<?> registryKeyClass = Class.forName("io.papermc.paper.registry.RegistryKey");

            Object attributeModifiersType = dataComponentTypesClass.getField("ATTRIBUTE_MODIFIERS").get(null);

            Object registryAccess = registryAccessClass.getMethod("registryAccess").invoke(null);
            Object dataComponentTypeKey = registryKeyClass.getField("DATA_COMPONENT_TYPE").get(null);
            Object registry = registryAccess.getClass().getMethod("getRegistry", registryKeyClass).invoke(registryAccess, dataComponentTypeKey);

            Object attributeModifiersKey = dataComponentTypeKeysClass.getField("ATTRIBUTE_MODIFIERS").get(null);
            Object attributeModifiersComponent = registry.getClass().getMethod("get", Object.class).invoke(registry, attributeModifiersKey);

            Class<?> itemAttributeModifiersClass = Class.forName("io.papermc.paper.datacomponent.item.ItemAttributeModifiers");
            Object attributeModifiersBuilder = itemAttributeModifiersClass.getMethod("itemAttributes").invoke(null);
            attributeModifiersBuilder = attributeModifiersBuilder.getClass()
                .getMethod("showInTooltip", boolean.class)
                .invoke(attributeModifiersBuilder, false);
            Object attributeModifiers = attributeModifiersBuilder.getClass().getMethod("build").invoke(attributeModifiersBuilder);

            itemStack.getClass().getMethod("setData", dataComponentTypeClass, Object.class)
                .invoke(itemStack, attributeModifiersType, attributeModifiers);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hide attributes using DataComponent API", e);
        }
    }

    private static void hideAttributesUsingItemFlag(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemStack.setItemMeta(meta);
        }
    }

    public static void applyGlowing(ItemStack itemStack, boolean glowing) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Enchantment unbreaking = getUnbreakingEnchantment();
            if (unbreaking != null) {
                if (glowing) {
                    meta.addEnchant(unbreaking, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    meta.removeEnchant(unbreaking);
                }
            }
            itemStack.setItemMeta(meta);
        }
    }

    private static Enchantment cachedUnbreaking = null;
    private static boolean enchantmentResolved = false;

    private static Enchantment getUnbreakingEnchantment() {
        if (enchantmentResolved) return cachedUnbreaking;
        synchronized (AttributeProcessor.class) {
            if (enchantmentResolved) return cachedUnbreaking;
            cachedUnbreaking = resolveEnchantment();
            enchantmentResolved = true;
            return cachedUnbreaking;
        }
    }

    private static Enchantment resolveEnchantment() {
        try {
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
            if (ench != null) return ench;
        } catch (Exception ignored) {}

        try {
            java.lang.reflect.Field field = Enchantment.class.getField("DURABILITY");
            return (Enchantment) field.get(null);
        } catch (Exception ignored) {}

        try {
            Iterable<? extends Keyed> enchants = (Iterable<? extends Keyed>) Registry.ENCHANTMENT;
            for (Keyed keyed : enchants) {
                if (keyed.getKey().getKey().equals("unbreaking")) {
                    return (Enchantment) keyed;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    public static void applyItemFlags(ItemStack itemStack, ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
    }
}
