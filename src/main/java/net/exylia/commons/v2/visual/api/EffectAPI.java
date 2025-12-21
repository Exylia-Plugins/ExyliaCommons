package net.exylia.commons.v2.visual.api;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.builder.EffectBuilder;
import net.exylia.commons.v2.visual.config.EffectConfig;
import net.exylia.commons.v2.visual.core.VisualManager;
import net.exylia.commons.v2.visual.renderer.EffectRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class EffectAPI {
    private EffectAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CompletableFuture<Void> apply(Player player, PotionEffectType effectType) {
        return apply(player, effectType, 0, 200);
    }

    public static CompletableFuture<Void> apply(Player player, PotionEffectType effectType, int amplifier, int durationTicks) {
        EffectConfig config = EffectBuilder.create()
                .effect(effectType)
                .amplifier(amplifier)
                .durationTicks(durationTicks)
                .build();

        return EffectRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> apply(Player player, String effectString) {
        EffectConfig config = EffectBuilder.fromString(effectString);
        return EffectRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> apply(Player player, EffectConfig config) {
        return EffectRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<Void> applyToAll(PotionEffectType effectType, int amplifier, int durationTicks) {
        EffectConfig config = EffectBuilder.create()
                .effect(effectType)
                .amplifier(amplifier)
                .durationTicks(durationTicks)
                .build();

        for (Player player : Bukkit.getOnlinePlayers()) {
            EffectRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
        }
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> applyToRecipients(Collection<Player> recipients, PotionEffectType effectType, int amplifier, int durationTicks) {
        EffectConfig config = EffectBuilder.create()
                .effect(effectType)
                .amplifier(amplifier)
                .durationTicks(durationTicks)
                .build();

        for (Player player : recipients) {
            EffectRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
        }
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> applyToFiltered(Predicate<Player> filter, PotionEffectType effectType, int amplifier, int durationTicks) {
        EffectConfig config = EffectBuilder.create()
                .effect(effectType)
                .amplifier(amplifier)
                .durationTicks(durationTicks)
                .build();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) {
                EffectRenderer.getInstance().renderAsync(player, config, PlaceholderContext.create());
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<String> applyContinuous(Player player, EffectConfig config) {
        return VisualManager.getInstance().applyEffectContinuous(player, config, PlaceholderContext.create());
    }

    public static CompletableFuture<String> applyCountdown(Player player, EffectConfig config, long durationTicks) {
        return VisualManager.getInstance().applyEffectCountdown(player, config, PlaceholderContext.create(), durationTicks);
    }

    public static EffectBuilder builder() {
        return EffectBuilder.create();
    }
}
