package net.exylia.commons.utils.effects;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class EffectUtils {
    public static void applyEffects(LivingEntity livingEntity, List<String> effects) {
        for (String effectString : effects) {
            try {
                String[] parts = effectString.split("\\|");
                if (parts.length >= 3) {
                    PotionEffectType type = PotionEffectType.getByName(parts[0]);
                    int level = Integer.parseInt(parts[1]) - 1;
                    int durationSeconds = Integer.parseInt(parts[2]) * 20;

                    if (type != null) {
                        PotionEffect effect = new PotionEffect(type, durationSeconds, level, false, false, false);
                        livingEntity.addPotionEffect(effect);
                    }
                }
            } catch (Exception e) {
                logInternalWarn("Error al aplicar efecto a jugador: " + e.getMessage());
            }
        }
    }

    public static void applyRandomEffects(LivingEntity livingEntity, List<String> effects, int amount) {
        Random random = new Random();
        List<String> copy = new ArrayList<>(effects);

        for (int i = 0; i < amount && !copy.isEmpty(); i++) {
            String randomEffect = copy.remove(random.nextInt(copy.size()));
            applyEffects(livingEntity, Collections.singletonList(randomEffect));
        }
    }


    public static void removeEffects(LivingEntity livingEntity, List<String> effects) {
        for (String effectString : effects) {
            try {
                PotionEffectType type = PotionEffectType.getByName(effectString);
                if (type != null) {
                    livingEntity.removePotionEffect(type);
                }
            } catch (Exception e) {
                logInternalWarn("Error al remover efecto a jugador: " + e.getMessage());
            }
        }
    }

    public static List<PotionEffect> getEffects(List<String> effects) {
        if (effects == null) return null;

        return effects.stream().map(e -> {
            String[] parts = e.split("\\|");
            if (parts.length < 1 || parts[0].trim().isEmpty()) {
                return null;
            }
            PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase());
            if (type == null) {
                return null;
            }
            int amplifier = 0;
            int durationTicks = 600;
            if (parts.length >= 2) {
                try {
                    amplifier = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
            if (parts.length >= 3) {
                try {
                    durationTicks = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException ignored) {
                }
            }

            return new PotionEffect(type, durationTicks, amplifier, false, false, false);

        }).filter(Objects::nonNull).toList();
    }
}
