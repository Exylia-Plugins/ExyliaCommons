package net.exylia.commons.utils.player;

import net.exylia.commons.utils.DebugUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.getModifiers().forEach(maxHealth::removeModifier);
            maxHealth.setBaseValue(20.0);
        }
        player.setHealth(20);
        player.setNoDamageTicks(20);
        player.setMaximumNoDamageTicks(20);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setWalkSpeed(0.2F);
        player.setExhaustion(0.0f);
        player.setFireTicks(0);
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
        player.setFallDistance(0);
        player.setFlying(false);
        player.setAllowFlight(false);
    }
}
