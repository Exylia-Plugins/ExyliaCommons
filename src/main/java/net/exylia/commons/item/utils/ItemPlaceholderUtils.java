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

        // Obtener cooldown restante
        double remainingSeconds = ItemInteractionHandler.getRemainingCooldown(player, item.getId());

        if (remainingSeconds <= 0.0) {
            // Sin cooldown activo
            return text.replace("%cooldown_formatted%", "Listo")
                    .replace("%cooldown_seconds%", "0.0");
        }

        // Formatear tiempo
        String formattedTime = timeFormatter.format(remainingSeconds);

        return text.replace("%cooldown_formatted%", formattedTime)
                .replace("%cooldown_seconds%", String.valueOf(remainingSeconds));
    }

    /**
     * Procesa todos los placeholders de item en un texto
     */
    public static String processAllItemPlaceholders(String text, InteractiveItem item, Player player) {
        if (text == null) return null;

        String processed = processUsePlaceholders(text, item);
        processed = processCooldownPlaceholders(processed, item, player);

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
                text.contains("%cooldown_seconds%");
    }
}