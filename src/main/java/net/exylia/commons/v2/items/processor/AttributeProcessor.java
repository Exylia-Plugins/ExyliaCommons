package net.exylia.commons.v2.items.processor;

import net.exylia.commons.v2.compat.EnchantmentCompat;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AttributeProcessor {

    private static boolean supportsDataComponentAPI = false;
    private static Class<?> dataComponentTypesClass = null;
    private static Class<?> dataComponentTypeClass = null;

    static {
        detectDataComponentAPISupport();
    }

    private static void detectDataComponentAPISupport() {
        try {
            dataComponentTypesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            dataComponentTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType");
            supportsDataComponentAPI = true;
        } catch (Exception e) {
            supportsDataComponentAPI = false;
        }
    }

    public static void applyHideAttributes(ItemStack itemStack) {
        if (itemStack == null) return;

        if (supportsDataComponentAPI) {
            hideAttributesUsingDataComponent(itemStack);
        }
        hideAttributesUsingItemFlag(itemStack);
    }

    private static void hideAttributesUsingDataComponent(ItemStack itemStack) {
        try {
            hideAttributeModifiersComponent(itemStack);
        } catch (Exception ignored) {}
        try {
            hidePotionContentsComponent(itemStack);
        } catch (Exception ignored) {}
        try {
            hideFireworksComponent(itemStack);
        } catch (Exception ignored) {}
    }

    private static void hideAttributeModifiersComponent(ItemStack itemStack) throws Exception {
        Object type = dataComponentTypesClass.getField("ATTRIBUTE_MODIFIERS").get(null);
        Class<?> itemAttributeModifiersClass = Class.forName("io.papermc.paper.datacomponent.item.ItemAttributeModifiers");
        Object builder = itemAttributeModifiersClass.getMethod("itemAttributes").invoke(null);
        builder = builder.getClass().getMethod("showInTooltip", boolean.class).invoke(builder, false);
        Object built = builder.getClass().getMethod("build").invoke(builder);
        itemStack.getClass().getMethod("setData", dataComponentTypeClass, Object.class)
                .invoke(itemStack, type, built);
    }

    @SuppressWarnings("unchecked")
    private static void hidePotionContentsComponent(ItemStack itemStack) throws Exception {
        Object type = dataComponentTypesClass.getField("POTION_CONTENTS").get(null);
        if (!(boolean) itemStack.getClass().getMethod("hasData", dataComponentTypeClass).invoke(itemStack, type)) return;

        Object existing = itemStack.getClass().getMethod("getData", dataComponentTypeClass).invoke(itemStack, type);
        Class<?> potionContentsClass = Class.forName("io.papermc.paper.datacomponent.item.PotionContents");

        Object builder = potionContentsClass.getMethod("potionContents").invoke(null);

        Optional<PotionType> potionOpt = (Optional<PotionType>) existing.getClass().getMethod("potion").invoke(existing);
        if (potionOpt.isPresent()) {
            builder = builder.getClass().getMethod("potion", PotionType.class).invoke(builder, potionOpt.get());
        }

        Optional<Color> colorOpt = (Optional<Color>) existing.getClass().getMethod("customColor").invoke(existing);
        if (colorOpt.isPresent()) {
            builder = builder.getClass().getMethod("customColor", Color.class).invoke(builder, colorOpt.get());
        }

        List<PotionEffect> effects = (List<PotionEffect>) existing.getClass().getMethod("customEffects").invoke(existing);
        if (!effects.isEmpty()) {
            builder = builder.getClass().getMethod("addAllCustomEffects", Collection.class).invoke(builder, effects);
        }

        builder = builder.getClass().getMethod("showInTooltip", boolean.class).invoke(builder, false);
        Object built = builder.getClass().getMethod("build").invoke(builder);
        itemStack.getClass().getMethod("setData", dataComponentTypeClass, Object.class)
                .invoke(itemStack, type, built);
    }

    @SuppressWarnings("unchecked")
    private static void hideFireworksComponent(ItemStack itemStack) throws Exception {
        Object type = dataComponentTypesClass.getField("FIREWORKS").get(null);
        if (!(boolean) itemStack.getClass().getMethod("hasData", dataComponentTypeClass).invoke(itemStack, type)) return;

        Object existing = itemStack.getClass().getMethod("getData", dataComponentTypeClass).invoke(itemStack, type);
        Class<?> fireworksClass = Class.forName("io.papermc.paper.datacomponent.item.Fireworks");

        int flightDuration = (int) existing.getClass().getMethod("flightDuration").invoke(existing);
        Object builder = fireworksClass.getMethod("fireworks", int.class).invoke(null, flightDuration);

        List<FireworkEffect> effects = (List<FireworkEffect>) existing.getClass().getMethod("effects").invoke(existing);
        if (!effects.isEmpty()) {
            builder = builder.getClass().getMethod("addAllEffects", Collection.class).invoke(builder, effects);
        }

        builder = builder.getClass().getMethod("showInTooltip", boolean.class).invoke(builder, false);
        Object built = builder.getClass().getMethod("build").invoke(builder);
        itemStack.getClass().getMethod("setData", dataComponentTypeClass, Object.class)
                .invoke(itemStack, type, built);
    }

    private static void hideAttributesUsingItemFlag(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
        }
    }

    public static void applyGlowing(ItemStack itemStack, boolean glowing) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Enchantment unbreaking = EnchantmentCompat.getUnbreaking();
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

    public static void applyItemFlags(ItemStack itemStack, ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
    }
}
