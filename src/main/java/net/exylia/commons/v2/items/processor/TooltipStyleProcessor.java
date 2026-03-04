package net.exylia.commons.v2.items.processor;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;

public class TooltipStyleProcessor {

    private static Boolean methodAvailable = null;
    private static Method setTooltipStyleMethod = null;

    public static void apply(ItemStack itemStack, String rawTooltipStyle,
                           Player player, PlaceholderContext context) {
        if (rawTooltipStyle == null || rawTooltipStyle.isEmpty()) {
            return;
        }

        if (!isMethodAvailable()) {
            return;
        }

        String processedStyle = rawTooltipStyle;
        if (player != null && context != null) {
            processedStyle = Placeholders.process(rawTooltipStyle, player, context);
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            if (!processedStyle.contains(":")) {
                DebugAPI.logLibWarn("TooltipStyleProcessor: Invalid format - must be 'namespace:key'");
                return;
            }

            String[] parts = processedStyle.split(":", 2);
            if (parts.length == 2) {
                String namespace = parts[0];
                String key = parts[1];
                NamespacedKey namespacedKey = new NamespacedKey(namespace, key);

                setTooltipStyleMethod.invoke(meta, namespacedKey);
                itemStack.setItemMeta(meta);
            }
        } catch (Exception e) {
            DebugAPI.logLibWarn("TooltipStyleProcessor: Failed to apply tooltip style: " + processedStyle);
        }
    }

    private static boolean isMethodAvailable() {
        if (methodAvailable != null) {
            return methodAvailable;
        }

        try {
            setTooltipStyleMethod = ItemMeta.class.getMethod("setTooltipStyle", NamespacedKey.class);
            methodAvailable = true;
        } catch (NoSuchMethodException e) {
            methodAvailable = false;
        }

        return methodAvailable;
    }

    public static void apply(ItemStack itemStack, String rawTooltipStyle) {
        apply(itemStack, rawTooltipStyle, null, null);
    }
}
