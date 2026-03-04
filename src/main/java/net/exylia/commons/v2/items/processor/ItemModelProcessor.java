package net.exylia.commons.v2.items.processor;

import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemModelProcessor {

    public static void apply(ItemStack itemStack, String rawItemModel,
                           Player player, PlaceholderContext context) {
        if (rawItemModel == null || rawItemModel.isEmpty()) {
            return;
        }

        String processedModel = rawItemModel;
        if (player != null && context != null) {
            processedModel = Placeholders.process(rawItemModel, player, context);
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            if (isNumeric(processedModel)) {
                applyCustomModelData(meta, processedModel);
            } else if (processedModel.contains(":")) {
                applyItemModel(meta, processedModel);
            } else {
                DebugAPI.logLibWarn("ItemModelProcessor: Invalid format - must be 'namespace:key' or a numeric value");
                return;
            }
            itemStack.setItemMeta(meta);
        } catch (Exception e) {
            DebugAPI.logLibWarn("ItemModelProcessor: Failed to apply item model: " + processedModel);
        }
    }

    private static void applyCustomModelData(ItemMeta meta, String customModelData) {
        try {
            int modelData = Integer.parseInt(customModelData);
            meta.setCustomModelData(modelData);
        } catch (NumberFormatException e) {
            DebugAPI.logLibWarn("ItemModelProcessor: Invalid custom model data format: " + customModelData);
        }
    }

    private static void applyItemModel(ItemMeta meta, String itemModel) {
        try {
            String[] parts = itemModel.split(":", 2);
            if (parts.length == 2) {
                String namespace = parts[0];
                String key = parts[1];
                NamespacedKey namespacedKey = new NamespacedKey(namespace, key);

                try {
                    var method = meta.getClass().getMethod("setItemModel", NamespacedKey.class);
                    method.invoke(meta, namespacedKey);
                } catch (NoSuchMethodException e) {
                    DebugAPI.logLibWarn("ItemModelProcessor: setItemModel not available in this Bukkit version. Use custom model data instead.");
                }
            } else {
                DebugAPI.logLibWarn("ItemModelProcessor: Invalid item model format - expected 'namespace:key'");
            }
        } catch (Exception e) {
            DebugAPI.logLibWarn("ItemModelProcessor: Failed to parse item model: " + itemModel);
        }
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void apply(ItemStack itemStack, String rawItemModel) {
        apply(itemStack, rawItemModel, null, null);
    }
}
