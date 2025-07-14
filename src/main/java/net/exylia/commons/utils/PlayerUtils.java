package net.exylia.commons.utils;

import org.bukkit.entity.Player;

public class PlayerUtils {
    public static String getPlayerIP(Player player) {
        try {
            if (player.getAddress() != null && player.getAddress().getAddress() != null) {
                return player.getAddress().getAddress().getHostAddress();
            }
        } catch (Exception e) {
            DebugUtils.logInternalError("Error obteniendo IP del jugador " + player.getName() + ": " + e.getMessage());
        }
        return null;
    }
}
