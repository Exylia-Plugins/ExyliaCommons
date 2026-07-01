package net.exylia.commons.v2.ui.selector.impl.effect;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public record PotionEffectResult(
        PotionEffectType effectType,
        int amplifier,
        int durationSeconds
) {

    public int level() {
        return amplifier + 1;
    }

    public PotionEffect toBukkitEffect() {
        int ticks = durationSeconds == -1 ? Integer.MAX_VALUE : durationSeconds * 20;
        return new PotionEffect(effectType, ticks, amplifier, false, false, false);
    }

    public String formattedTypeName() {
        String raw = effectType.getKey().getKey().replace('_', ' ');
        StringBuilder sb = new StringBuilder(raw.length());
        boolean capitalizeNext = true;
        for (char c : raw.toCharArray()) {
            if (c == ' ') {
                sb.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
