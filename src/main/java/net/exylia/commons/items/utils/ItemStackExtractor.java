package net.exylia.commons.items.utils;

import net.exylia.commons.items.model.ItemData;
import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemStackExtractor {

    public static ItemData extractFromItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return ItemData.builder().build();
        }

        ItemData.ItemDataBuilder builder = ItemData.builder();

        extractMaterial(itemStack, builder);
        extractAmount(itemStack, builder);
        extractDisplayName(itemStack, builder);
        extractLore(itemStack, builder);
        extractEnchantments(itemStack, builder);
        extractAttributes(itemStack, builder);
        extractPotionConfig(itemStack, builder);
        extractLeatherArmorColor(itemStack, builder);
        extractCustomNBT(itemStack, builder);
        extractItemFlags(itemStack, builder);

        return builder.build();
    }

    private static void extractMaterial(ItemStack itemStack, ItemData.ItemDataBuilder builder) {
        builder.rawMaterial(itemStack.getType().name());
    }

    private static void extractAmount(ItemStack itemStack, ItemData.ItemDataBuilder builder) {
        if (itemStack.getAmount() > 1) {
            builder.rawAmount(String.valueOf(itemStack.getAmount()));
        }
    }

    private static void extractDisplayName(ItemStack itemStack, ItemData.ItemDataBuilder builder) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            builder.rawName(meta.getDisplayName());
        }
    }

    private static void extractLore(ItemStack itemStack, ItemData.ItemDataBuilder builder) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = new ArrayList<>();
            List<?> loreComponents = meta.lore();
            if (loreComponents != null) {
                for (Object component : loreComponents) {
                    if (component != null) {
                        lore.add(component.toString());
                    }
                }
            }
            if (!lore.isEmpty()) {
                builder.rawLore(lore);
            }
        }
    }

    private static void extractEnchantments(ItemStack itemStack, ItemData.ItemDataBuilder builder) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && !meta.getEnchants().isEmpty()) {
            Map<String, Integer> enchantments = new HashMap<>();
            meta.getEnchants().forEach((enchant, level) -> {
                if (enchant.getKey() != null) {
                    enchantments.put(enchant.getKey().getKey(), level);
                }
            });
            if (!enchantments.isEmpty()) {
                builder.rawEnchantments(enchantments);
            }
        }
    }

    private static void extractAttributes(ItemStack itemStack, ItemData.ItemDataBuilder builder) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        List<String> attributes = new ArrayList<>();

        try {
            java.lang.reflect.Method getAttributesMethod = ItemMeta.class.getMethod("getAttributeModifiers");
            Object attributeMap = getAttributesMethod.invoke(meta);

            if (attributeMap instanceof Map<?, ?> map) {
                for (Object key : map.keySet()) {
                    Object value = map.get(key);
                    if (value instanceof java.util.Collection<?> collection) {
                        for (Object modifierObj : collection) {
                            String attributeString = extractAttributeModifier(key, modifierObj);
                            if (attributeString != null && !attributeString.isEmpty()) {
                                attributes.add(attributeString);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (!attributes.isEmpty()) {
            builder.rawAttributes(attributes);
        }
    }

    private static String extractAttributeModifier(Object attributeKey, Object modifier) {
        try {
            java.lang.reflect.Method getAmountMethod = modifier.getClass().getMethod("getAmount");
            double amount = ((Number) getAmountMethod.invoke(modifier)).doubleValue();

            String attributeName = attributeKey.toString()
                    .replace("Attribute(", "")
                    .replace(")", "")
                    .replace("GENERIC_", "")
                    .toUpperCase();

            if (amount != 0.0) {
                return attributeName + "|" + amount;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static void extractPotionConfig(ItemStack itemStack, ItemData.ItemDataBuilder builder) {
        ItemMeta meta = itemStack.getItemMeta();
        if (!(meta instanceof PotionMeta potionMeta)) {
            return;
        }

        net.exylia.commons.items.config.PotionConfig potionConfig = new net.exylia.commons.items.config.PotionConfig();

        try {
            java.lang.reflect.Method getBasePotionDataMethod = PotionMeta.class.getMethod("getBasePotionData");
            PotionData potionData = (PotionData) getBasePotionDataMethod.invoke(potionMeta);
            if (potionData != null && potionData.getType() != null) {
                potionConfig.setBasePotionType(potionData.getType().name());
                if (potionData.isUpgraded()) {
                    potionConfig.setPotionUpgraded(true);
                }
                if (potionData.isExtended()) {
                    potionConfig.setPotionExtended(true);
                }
            }
        } catch (Exception ignored) {
        }

        try {
            java.lang.reflect.Method getColorMethod = PotionMeta.class.getMethod("getColor");
            Color color = (Color) getColorMethod.invoke(potionMeta);
            if (color != null) {
                potionConfig.setPotionColor(color);
            }
        } catch (Exception ignored) {
        }

        if (potionConfig.hasConfiguration()) {
            builder.potionConfig(potionConfig);
        }
    }

    private static void extractLeatherArmorColor(ItemStack itemStack, ItemData.ItemDataBuilder builder) {
        ItemMeta meta = itemStack.getItemMeta();
        if (!(meta instanceof LeatherArmorMeta leatherMeta)) {
            return;
        }

        try {
            java.lang.reflect.Method getColorMethod = LeatherArmorMeta.class.getMethod("getColor");
            Color color = (Color) getColorMethod.invoke(leatherMeta);
            if (color != null) {
                net.exylia.commons.items.config.LeatherArmorConfig leatherConfig = new net.exylia.commons.items.config.LeatherArmorConfig();
                leatherConfig.setColor(color);
                builder.leatherArmorConfig(leatherConfig);
            }
        } catch (Exception ignored) {
        }
    }

    private static void extractCustomNBT(ItemStack itemStack, ItemData.ItemDataBuilder builder) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            Map<String, String> nbtData = new HashMap<>();

            for (Object keyObj : container.getKeys()) {
                if (keyObj instanceof org.bukkit.NamespacedKey key) {
                    String value = container.get(key, PersistentDataType.STRING);
                    if (value != null) {
                        nbtData.put(key.getKey(), value);
                    }
                }
            }

            if (!nbtData.isEmpty()) {
                builder.customNBT(nbtData);
            }
        } catch (Exception ignored) {
        }
    }

    private static void extractItemFlags(ItemStack itemStack, ItemData.ItemDataBuilder builder) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && !meta.getItemFlags().isEmpty()) {
            for (org.bukkit.inventory.ItemFlag flag : meta.getItemFlags()) {
                if (flag == org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES) {
                    builder.hideAttributes(true);
                    break;
                }
            }
        }
    }
}
