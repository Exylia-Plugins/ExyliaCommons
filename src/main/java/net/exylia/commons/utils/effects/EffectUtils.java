package net.exylia.commons.utils.effects;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class EffectUtils {
    public static void applyEffects(LivingEntity livingEntity, List<String> effects) {
        for (String effectString : effects) {
            try {
                String[] parts = effectString.split("\\|");
                if (parts.length >= 3) {
                    PotionEffectType type = PotionEffectType.getByName(parts[0]);
                    int amplifier = Integer.parseInt(parts[1]);
                    int durationTicks = Integer.parseInt(parts[2]);

                    if (type != null) {
                        PotionEffect effect = new PotionEffect(type, durationTicks, amplifier, false, false, false);
                        livingEntity.addPotionEffect(effect);
                    }
                }
            } catch (Exception e) {
                logInternalWarn("Error al aplicar efecto a jugador: " + e.getMessage());
            }
        }
    }

    public static void removeEffects(Player player, List<String> effects) {
        for (String effectString : effects) {
            try {
                String[] parts = effectString.split("\\|");
                if (parts.length >= 3) {
                    PotionEffectType type = PotionEffectType.getByName(parts[0]);
                    if (type != null) {
                        player.removePotionEffect(type);
                    }
                }
            } catch (Exception e) {
                logInternalWarn("Error al remover efecto a jugador: " + e.getMessage());
            }
        }
    }
}
