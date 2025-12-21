package net.exylia.commons.v2.items.skull;

import net.exylia.commons.v2.placeholders.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.skull.api.SkullAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

public class SkullParser {

    public static boolean isSkullString(String material) {
        if (material == null) return false;
        String lower = material.toLowerCase();
        return lower.startsWith("playerhead:") || lower.startsWith("playerhead-") ||
               lower.startsWith("basehead:") || lower.startsWith("basehead-") ||
               lower.startsWith("urlhead:") || lower.startsWith("urlhead-");
    }

    public static CompletableFuture<ItemStack> parseAsync(String skullString, Player player, PlaceholderContext context) {
        return Placeholders.processAsync(skullString, player, context)
            .thenCompose(processed -> {
                String normalized = processed.replace("-", ":");

                if (normalized.startsWith("playerhead:")) {
                    String name = normalized.substring("playerhead:".length());
                    return SkullAPI.fromPlayerAsync(name);
                } else if (normalized.startsWith("basehead:")) {
                    String base64 = normalized.substring("basehead:".length());
                    return SkullAPI.fromTextureAsync(base64);
                } else if (normalized.startsWith("urlhead:")) {
                    String url = normalized.substring("urlhead:".length());
                    return SkullAPI.fromTextureURLAsync(url);
                }
                return CompletableFuture.completedFuture(new ItemStack(Material.PLAYER_HEAD));
            });
    }

    public static ItemStack parse(String skullString, Player player, PlaceholderContext context) {
        try {
            return parseAsync(skullString, player, context).join();
        } catch (Exception e) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
    }
}
