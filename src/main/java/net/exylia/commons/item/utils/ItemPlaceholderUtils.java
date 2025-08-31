package net.exylia.commons.item.utils;

import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.handlers.ItemInteractionHandler;
import org.bukkit.entity.Player;

import static net.exylia.commons.utils.TimeFormatter.timeFormatter;

/**
 * Utilidades para procesar placeholders específicos de items
 */
public class ItemPlaceholderUtils {

    /**
     * Procesa placeholders de usos en un texto
     * %current_uses% -> usos actuales
     * %max_uses% -> usos máximos
     */
    public static String processUsePlaceholders(String text, InteractiveItem item) {
        if (!item.hasLimitedUses() || text == null) {
            return text;
        }

        return text.replace("%current_uses%", String.valueOf(item.getCurrentUses()))
                .replace("%max_uses%", String.valueOf(item.getMaxUses()));
    }

    /**
     * Procesa placeholders de cooldown en un texto
     * %cooldown_formatted% -> tiempo formateado (ej: "1m 30.2s")
     * %cooldown_seconds% -> segundos restantes (con decimales)
     */
    public static String processCooldownPlaceholders(String text, InteractiveItem item, Player player) {
        if (!item.getConfiguration().hasCooldown() || text == null || player == null) {
            return text;
        }

        // Formatear tiempo
        String formattedTime = timeFormatter.format(item.getCooldownSeconds());

        return text.replace("%cooldown_formatted%", formattedTime)
                .replace("%cooldown_seconds%", String.valueOf(item.getCooldownSeconds()));
    }

    /**
     * Procesa placeholders de expiración en un texto
     * %expiration_remaining% -> tiempo restante formateado (ej: "2d 5h 30m")
     * %expiration_date% -> fecha de expiración formateada
     * %is_expired% -> true/false si el item está expirado
     */
    public static String processExpirationPlaceholders(String text, InteractiveItem item) {
        if (text == null) {
            return text;
        }

        if (!item.hasExpiration()) {
            return text.replace("%expiration_remaining%", "Sin expiración")
                    .replace("%expiration_date%", "Sin expiración")
                    .replace("%is_expired%", "false");
        }

        String remainingTime = item.getFormattedRemainingTime();
        String expirationDate = item.getFormattedExpirationDate();
        boolean isExpired = item.isExpired();

        return text.replace("%expiration_remaining%", remainingTime)
                .replace("%expiration_date%", expirationDate)
                .replace("%is_expired%", String.valueOf(isExpired));
    }

    /**
     * Procesa todos los placeholders de item en un texto
     */
    public static String processAllItemPlaceholders(String text, InteractiveItem item, Player player) {
        if (text == null) return null;

        String processed = processUsePlaceholders(text, item);
        processed = processCooldownPlaceholders(processed, item, player);
        processed = processExpirationPlaceholders(processed, item);

        return processed;
    }

    /**
     * Verifica si un texto contiene placeholders de item
     */
    public static boolean containsItemPlaceholders(String text) {
        if (text == null) return false;

        return text.contains("%current_uses%") ||
                text.contains("%max_uses%") ||
                text.contains("%cooldown_formatted%") ||
                text.contains("%cooldown_seconds%") ||
                text.contains("%expiration_remaining%") ||
                text.contains("%expiration_date%") ||
                text.contains("%is_expired%");
    }
}