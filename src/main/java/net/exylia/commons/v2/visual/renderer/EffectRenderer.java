package net.exylia.commons.v2.visual.renderer;

import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.config.EffectConfig;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.concurrent.CompletableFuture;

public class EffectRenderer implements VisualRenderer<EffectConfig> {
    private static final EffectRenderer INSTANCE = new EffectRenderer();

    private EffectRenderer() {
    }

    public static EffectRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<Void> renderAsync(Player player, EffectConfig config, PlaceholderContext context) {
        return CompletableFuture.runAsync(() -> {
            PotionEffect effect = new PotionEffect(
                    config.getEffectType(),
                    config.getDurationTicks(),
                    config.getAmplifier(),
                    config.isAmbient(),
                    config.isParticles(),
                    config.isIcon()
            );

            SchedulerManager.getInstance().runSync(() -> {
                if (player.isOnline()) {
                    player.addPotionEffect(effect);
                }
            });
        }, AsyncExecutor.getInstance().getGeneralExecutor());
    }

    @Override
    public void cleanup(Player player, String visualId) {
    }
}
