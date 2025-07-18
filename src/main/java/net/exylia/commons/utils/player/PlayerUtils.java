package net.exylia.commons.utils.player;

import net.exylia.commons.utils.DebugUtils;
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
    public static void resetPlayer(Player player) {
        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType()));
        player.setHealth(20);
        player.setNoDamageTicks(20);
        player.setMaximumNoDamageTicks(20);
        player.setMaxHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setWalkSpeed(0.2F);
        player.setExhaustion(0.0f);
        player.setFireTicks(0);
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
    }
}
