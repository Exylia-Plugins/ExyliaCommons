package net.exylia.commons.v2.visual.core;

import lombok.Getter;
import net.exylia.commons.v2.visual.config.SoundConfig;
import net.exylia.commons.v2.visual.config.ParticleConfig;
import net.exylia.commons.v2.visual.config.FireworkConfig;
import net.exylia.commons.v2.visual.config.EffectConfig;
import net.exylia.commons.v2.visual.config.VisualConfig;
import net.exylia.commons.v2.visual.renderer.SoundRenderer;
import net.exylia.commons.v2.visual.renderer.ParticleRenderer;
import net.exylia.commons.v2.visual.renderer.FireworkRenderer;
import net.exylia.commons.v2.visual.renderer.EffectRenderer;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.exception.LimitExceededException;
import net.exylia.commons.v2.visual.exception.VisualException;
import net.exylia.commons.v2.visual.instance.*;
import net.exylia.commons.v2.visual.renderer.VisualRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
public class VisualManager {
    private static volatile VisualManager instance;
    private static final Object LOCK = new Object();

    private Plugin plugin;
    private boolean initialized = false;

    private VisualManager() {
    }

    public static VisualManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new VisualManager();
                }
            }
        }
        return instance;
    }

    public void initialize(Plugin plugin) {
        if (initialized) {
            throw new IllegalStateException("VisualManager is already initialized");
        }

        this.plugin = plugin;
        this.initialized = true;
    }

    public <T extends VisualConfig> CompletableFuture<String> sendSimple(
            Player player,
            T config,
            PlaceholderContext context,
            VisualRenderer<T> renderer,
            VisualType type
    ) {
        validateInitialized();
        validateParameters(player, config);

        if (!VisualLimiter.canAdd(player, type)) {
            return CompletableFuture.failedFuture(
                    new LimitExceededException("Player has reached limit for " + type)
            );
        }

        String id = generateId(type);
        PlaceholderContext enrichedContext = enrichContext(context, player, id);

        SimpleVisualInstance<T> instance = new SimpleVisualInstance<>(
                id, config, player, enrichedContext, renderer
        );

        VisualRegistry.getInstance().register(player.getUniqueId(), id, instance, type);

        return instance.start()
                .thenApply(v -> id)
                .exceptionally(throwable -> {
                    VisualRegistry.getInstance().remove(player.getUniqueId(), id);
                    throw new VisualException("Failed to send visual", throwable);
                });
    }

    public <T extends VisualConfig> CompletableFuture<String> sendContinuous(
            Player player,
            T config,
            PlaceholderContext context,
            VisualRenderer<T> renderer,
            VisualType type
    ) {
        validateInitialized();
        validateParameters(player, config);

        if (!VisualLimiter.canAdd(player, type)) {
            return CompletableFuture.failedFuture(
                    new LimitExceededException("Player has reached limit for " + type)
            );
        }

        String id = generateId(type);
        PlaceholderContext enrichedContext = enrichContext(context, player, id);

        ContinuousVisualInstance<T> instance = new ContinuousVisualInstance<>(
                id, config, player, enrichedContext, renderer
        );

        VisualRegistry.getInstance().register(player.getUniqueId(), id, instance, type);

        return instance.start()
                .thenApply(v -> id)
                .exceptionally(throwable -> {
                    VisualRegistry.getInstance().remove(player.getUniqueId(), id);
                    throw new VisualException("Failed to send continuous visual", throwable);
                });
    }

    public <T extends VisualConfig> CompletableFuture<String> sendCountdown(
            Player player,
            T config,
            PlaceholderContext context,
            VisualRenderer<T> renderer,
            VisualType type,
            long durationTicks
    ) {
        validateInitialized();
        validateParameters(player, config);

        if (!VisualLimiter.canAdd(player, type)) {
            return CompletableFuture.failedFuture(
                    new LimitExceededException("Player has reached limit for " + type)
            );
        }

        String id = generateId(type);
        PlaceholderContext enrichedContext = enrichContext(context, player, id);
        enrichedContext.put("countdown_duration", durationTicks);

        CountdownVisualInstance<T> instance = new CountdownVisualInstance<>(
                id, config, player, enrichedContext, renderer, durationTicks
        );

        VisualRegistry.getInstance().register(player.getUniqueId(), id, instance, type);

        return instance.start()
                .thenApply(v -> id)
                .exceptionally(throwable -> {
                    VisualRegistry.getInstance().remove(player.getUniqueId(), id);
                    throw new VisualException("Failed to send countdown visual", throwable);
                });
    }

    public boolean cancel(UUID playerId, String visualId) {
        return VisualRegistry.getInstance().get(playerId, visualId)
                .map(instance -> {
                    instance.cancel();
                    return true;
                })
                .orElse(false);
    }

    public void cancelAll(UUID playerId) {
        VisualRegistry.getInstance().getByPlayer(playerId).forEach(VisualInstance::cancel);
        VisualRegistry.getInstance().removeAllByPlayer(playerId);
    }

    public void cancelAllByType(Player player, VisualType type) {
        VisualRegistry.getInstance().getByPlayerAndType(player, type)
                .forEach(VisualInstance::cancel);
    }

    private PlaceholderContext enrichContext(PlaceholderContext context, Player player, String id) {
        PlaceholderContext enriched = context != null ? context.copy() : PlaceholderContext.create();
        enriched.withPlayer(player);
        enriched.withCurrentTime();
        enriched.put("visual_id", id);
        return enriched;
    }

    private String generateId(VisualType type) {
        return type.name().toLowerCase() + "_" +
                System.currentTimeMillis() + "_" +
                ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private void validateInitialized() {
        if (!initialized) {
            throw new IllegalStateException("VisualManager is not initialized. Call initialize() first.");
        }
    }

    private void validateParameters(Player player, VisualConfig config) {
        if (player == null || !player.isOnline()) {
            throw new IllegalArgumentException("Player must be online");
        }

        if (config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("Config must be enabled");
        }
    }

    public CompletableFuture<String> playSound(Player player, SoundConfig config, PlaceholderContext context) {
        return sendSimple(player, config, context, SoundRenderer.getInstance(), VisualType.SOUND);
    }

    public CompletableFuture<String> playSoundContinuous(Player player, SoundConfig config, PlaceholderContext context) {
        return sendContinuous(player, config, context, SoundRenderer.getInstance(), VisualType.SOUND);
    }

    public CompletableFuture<String> playSoundCountdown(Player player, SoundConfig config, PlaceholderContext context, long durationTicks) {
        return sendCountdown(player, config, context, SoundRenderer.getInstance(), VisualType.SOUND, durationTicks);
    }

    public CompletableFuture<String> spawnParticle(Player player, ParticleConfig config, PlaceholderContext context) {
        return sendSimple(player, config, context, ParticleRenderer.getInstance(), VisualType.PARTICLE);
    }

    public CompletableFuture<String> spawnParticleContinuous(Player player, ParticleConfig config, PlaceholderContext context) {
        return sendContinuous(player, config, context, ParticleRenderer.getInstance(), VisualType.PARTICLE);
    }

    public CompletableFuture<String> spawnParticleCountdown(Player player, ParticleConfig config, PlaceholderContext context, long durationTicks) {
        return sendCountdown(player, config, context, ParticleRenderer.getInstance(), VisualType.PARTICLE, durationTicks);
    }

    public CompletableFuture<String> launchFirework(Player player, FireworkConfig config, PlaceholderContext context) {
        return sendSimple(player, config, context, FireworkRenderer.getInstance(), VisualType.FIREWORK);
    }

    public CompletableFuture<String> launchFireworkContinuous(Player player, FireworkConfig config, PlaceholderContext context) {
        return sendContinuous(player, config, context, FireworkRenderer.getInstance(), VisualType.FIREWORK);
    }

    public CompletableFuture<String> launchFireworkCountdown(Player player, FireworkConfig config, PlaceholderContext context, long durationTicks) {
        return sendCountdown(player, config, context, FireworkRenderer.getInstance(), VisualType.FIREWORK, durationTicks);
    }

    public CompletableFuture<String> applyEffect(Player player, EffectConfig config, PlaceholderContext context) {
        return sendSimple(player, config, context, EffectRenderer.getInstance(), VisualType.EFFECT);
    }

    public CompletableFuture<String> applyEffectContinuous(Player player, EffectConfig config, PlaceholderContext context) {
        return sendContinuous(player, config, context, EffectRenderer.getInstance(), VisualType.EFFECT);
    }

    public CompletableFuture<String> applyEffectCountdown(Player player, EffectConfig config, PlaceholderContext context, long durationTicks) {
        return sendCountdown(player, config, context, EffectRenderer.getInstance(), VisualType.EFFECT, durationTicks);
    }

    public <T extends VisualConfig> CompletableFuture<String> sendGlobalCountdown(
            String id,
            T config,
            PlaceholderContext baseContext,
            VisualRenderer<T> renderer,
            VisualType type,
            long durationTicks,
            Predicate<Player> playerFilter,
            Consumer<GlobalCountdownContext> onComplete,
            Consumer<GlobalCountdownContext> onCancel,
            Consumer<GlobalCountdownContext> onTick
    ) {
        validateInitialized();

        if (id == null) {
            id = generateId(type) + "_global";
        }

        PlaceholderContext enrichedContext = PlaceholderContext.create();
        if (baseContext != null) {
            enrichedContext = baseContext.copy();
        }
        enrichedContext.withCurrentTime();
        enrichedContext.put("visual_id", id);
        enrichedContext.put("countdown_duration", durationTicks);

        GlobalCountdownInstance<T> instance = new GlobalCountdownInstance<>(
                id, config, durationTicks, enrichedContext, renderer,
                playerFilter, onComplete, onCancel, onTick
        );

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> playerFilter == null || playerFilter.test(p))
                .forEach(p -> instance.addPlayer(p.getUniqueId()));

        GlobalVisualRegistry.getInstance().register(id, instance);

        String finalId = id;
        String finalId1 = id;
        return instance.start()
                .thenApply(v -> finalId)
                .exceptionally(throwable -> {
                    GlobalVisualRegistry.getInstance().unregister(finalId1);
                    throw new VisualException("Failed to send global countdown", throwable);
                });
    }

    public void shutdown() {
        VisualRegistry.getInstance().clear();
        GlobalVisualRegistry.getInstance().clear();
        initialized = false;
    }
}
