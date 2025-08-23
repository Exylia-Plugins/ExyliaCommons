package net.exylia.commons.item.handlers;

import lombok.experimental.UtilityClass;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.effects.FullUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@UtilityClass
public class ItemEffectsHandler {

    public static void executeEffects(Player player, Location location, ItemConfiguration config) {
        if (!config.hasEffects()) {
            return;
        }

        try {
            FullUtils.playFullEffects(location, config.getEffectsOnUse());
        } catch (Exception e) {
            DebugUtils.logInternalError("Error executing effects: " + e.getMessage());
        }
    }

    public static void executeEffects(Player player, ItemConfiguration config) {
        executeEffects(player, player.getLocation(), config);
    }
}