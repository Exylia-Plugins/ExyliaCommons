package net.exylia.commons.item.utils;

import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.handlers.ItemInteractionHandler;
import org.bukkit.entity.Player;

import static net.exylia.commons.utils.TimeFormatter.timeFormatter;

public class ItemPlaceholderUtils {

    public static String processUsePlaceholders(String text, InteractiveItem item) {
        if (text == null) {
            return text;
        }

        if (!item.hasLimitedUses()) {
            return text;
        }

        int currentUses = item.getCurrentUses();
        int maxUses = item.getMaxUses();
        return text.replace("%current_uses%", String.valueOf(currentUses))
                .replace("%max_uses%", String.valueOf(maxUses));
    }

    public static String processCooldownPlaceholders(String text, InteractiveItem item, Player player) {
        if (!item.getConfiguration().hasCooldown() || text == null || player == null) {
            return text;
        }

        String formattedTime = timeFormatter.format(item.getCooldownSeconds());

        return text.replace("%cooldown_formatted%", formattedTime)
                .replace("%cooldown_seconds%", String.valueOf(item.getCooldownSeconds()));
    }

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

    public static String processAllItemPlaceholders(String text, InteractiveItem item, Player player) {
        if (text == null) return null;

        String processed = processUsePlaceholders(text, item);
        processed = processCooldownPlaceholders(processed, item, player);
        processed = processExpirationPlaceholders(processed, item);

        return processed;
    }

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
