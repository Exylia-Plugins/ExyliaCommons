package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.EffectConfig;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class EffectRenderer implements VisualRenderer<EffectConfig> {
    private static final EffectRenderer INSTANCE = new EffectRenderer();

    private EffectRenderer() {
    }

    public static EffectRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void render(Player player, EffectConfig config, PlaceholderContext context) {
        if (!player.isOnline()) return;

        int duration = config.getDurationTicks() == -1
                ? PotionEffect.INFINITE_DURATION
                : config.getDurationTicks();

        PotionEffect effect = new PotionEffect(
                config.getEffectType(),
                duration,
                config.getAmplifier(),
                config.isAmbient(),
                config.isParticles(),
                config.isIcon()
        );

        player.addPotionEffect(effect);
    }

    @Override
    public void cleanup(Player player, String visualId) {
    }
}
