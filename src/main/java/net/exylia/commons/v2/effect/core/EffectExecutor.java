package net.exylia.commons.v2.effect.core;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.effect.model.EffectContext;
import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectResult;
import net.exylia.commons.v2.effect.model.EffectScope;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.reward.processor.ConditionProcessor;
import net.exylia.commons.v2.reward.processor.ProbabilityProcessor;
import net.exylia.commons.v2.sequence.SequenceAPI;
import net.exylia.commons.v2.sequence.SequenceContext;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.visual.api.ActionBarAPI;
import net.exylia.commons.v2.visual.api.FireworkAPI;
import net.exylia.commons.v2.visual.api.MessageAPI;
import net.exylia.commons.v2.visual.api.ParticleAPI;
import net.exylia.commons.v2.visual.api.SoundAPI;
import net.exylia.commons.v2.visual.api.TitleAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Applies gating (probability, permission, condition), ordering, delays and scope resolution
 * before dispatching an {@link EffectEntry} to the visual APIs.
 *
 * <p>All Bukkit work is dispatched through {@link TaskAPI} on the region thread owning the
 * effect location, so this is safe to call from async code.
 */
public class EffectExecutor {

    private final AtomicLong totalPlayed = new AtomicLong();
    private final AtomicLong totalFailed = new AtomicLong();
    private final AtomicLong totalSkipped = new AtomicLong();

    /** Plays every entry, highest priority first. Gating and delays are honored per entry. */
    public List<EffectResult> play(@Nullable List<EffectEntry> entries, EffectContext context) {
        List<EffectResult> results = new ArrayList<>();
        if (entries == null || entries.isEmpty()) return results;

        List<EffectEntry> ordered = new ArrayList<>(entries);
        ordered.removeIf(java.util.Objects::isNull);
        ordered.sort(Comparator.comparingInt(EffectEntry::getPriority).reversed());

        for (EffectEntry entry : ordered) {
            results.add(playSingle(entry, context));
        }
        return results;
    }

    /** Plays a single entry after evaluating its gates. */
    public EffectResult playSingle(EffectEntry entry, EffectContext context) {
        if (entry == null) return EffectResult.failure(null, "Effect entry is null");

        Player player = context.getPlayer();

        if (!context.isSkipProbability() && !ProbabilityProcessor.shouldGive(entry.getChance())) {
            return skip(EffectResult.skippedProbability(entry), entry, "probability");
        }

        if (!context.isSkipPermissionCheck()
                && entry.getPermission() != null && !entry.getPermission().isBlank()) {
            if (player == null || !player.hasPermission(entry.getPermission())) {
                return skip(EffectResult.skippedPermission(entry), entry, "permission");
            }
        }

        if (!context.isSkipConditions() && entry.getCondition() != null && !entry.getCondition().isBlank()) {
            boolean met = ConditionProcessor.evaluate(
                    entry.getCondition(), player, context.resolvePlaceholderContext());
            if (!met) {
                return skip(EffectResult.skippedCondition(entry), entry, "condition");
            }
        }

        if (!entry.isPlayable()) {
            totalFailed.incrementAndGet();
            DebugAPI.logLibError(DebugCategory.EFFECT,
                    "Effect has no payload for type " + entry.getType() + ": " + entry.getId());
            return EffectResult.failure(entry, "Effect has no payload for type " + entry.getType());
        }

        Location origin = context.resolveLocation();
        if (origin == null) {
            totalFailed.incrementAndGet();
            return EffectResult.failure(entry, "EffectContext has neither player nor location");
        }

        long delay = context.isSkipDelay() ? 0L : entry.getDelayTicks();
        if (delay > 0) {
            TaskAPI.atLater(origin, () -> dispatchSafely(entry, context, origin),
                    delay * 50L, TimeUnit.MILLISECONDS);
            return EffectResult.delayed(entry);
        }

        TaskAPI.at(origin, () -> dispatchSafely(entry, context, origin));
        totalPlayed.incrementAndGet();
        return EffectResult.success(entry);
    }

    private EffectResult skip(EffectResult result, EffectEntry entry, String reason) {
        totalSkipped.incrementAndGet();
        DebugAPI.logLibDebug(DebugCategory.EFFECT, "Effect skipped (" + reason + "): " + entry.getId());
        return result;
    }

    private void dispatchSafely(EffectEntry entry, EffectContext context, Location origin) {
        try {
            dispatch(entry, context, origin);
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            DebugAPI.logLibError(DebugCategory.EFFECT,
                    "Failed to play effect " + entry.getId() + " (" + entry.getType() + "): " + e.getMessage(), e);
        }
    }

    /** Runs on the region thread owning {@code origin}. */
    private void dispatch(EffectEntry entry, EffectContext context, Location origin) {
        PlaceholderContext placeholders = context.resolvePlaceholderContext();
        EffectScope scope = entry.getScope() == null ? EffectScope.PLAYER : entry.getScope();

        switch (entry.getType()) {
            case PARTICLE -> dispatchParticle(entry, context, origin, scope);
            case SOUND -> dispatchSound(entry, context, origin, scope);
            case POTION -> forEachViewer(context, origin, scope, entry.getRadius(),
                    viewer -> net.exylia.commons.v2.visual.api.EffectAPI.apply(viewer, entry.toPotionConfig()));
            case FIREWORK -> FireworkAPI.launch(origin, entry.toFireworkConfig(origin));
            case TITLE -> forEachViewer(context, origin, scope, entry.getRadius(),
                    viewer -> TitleAPI.send(viewer, entry.toTitleConfig(), placeholders.withPlayer(viewer)));
            case ACTIONBAR -> forEachViewer(context, origin, scope, entry.getRadius(),
                    viewer -> ActionBarAPI.send(viewer, entry.toActionBarConfig(), placeholders.withPlayer(viewer)));
            case MESSAGE -> {
                List<String> lines = entry.messageLines();
                forEachViewer(context, origin, scope, entry.getRadius(), viewer -> {
                    if (entry.isCentered()) MessageAPI.sendCentered(viewer, lines, placeholders.withPlayer(viewer));
                    else MessageAPI.send(viewer, lines, placeholders.withPlayer(viewer));
                });
            }
            case SEQUENCE -> SequenceAPI.getExecutor().executeOnCurrentThread(
                    SequenceContext.builder()
                            .location(origin)
                            .sourcePlayer(context.getPlayer())
                            .targetEntity(context.getTargetEntity())
                            .build(),
                    entry.sequenceTokens());
        }
    }

    private void dispatchParticle(EffectEntry entry, EffectContext context, Location origin, EffectScope scope) {
        var config = entry.toParticleConfig(origin);
        switch (scope) {
            case PLAYER, NEARBY -> {
                Player player = context.getPlayer();
                if (player != null) ParticleAPI.spawn(player, config);
                else ParticleAPI.spawn(origin, config);
            }
            case LOCATION -> ParticleAPI.spawn(origin, config);
            case RADIUS -> ParticleAPI.spawnInRadius(origin, entry.getRadius(),
                    config.getParticle(), config.getCount());
            case GLOBAL -> ParticleAPI.spawnToFiltered(viewer -> true,
                    config.getParticle(), config.getCount());
        }
    }

    private void dispatchSound(EffectEntry entry, EffectContext context, Location origin, EffectScope scope) {
        var config = entry.toSoundConfig(origin);
        switch (scope) {
            case PLAYER, NEARBY -> {
                Player player = context.getPlayer();
                if (player != null) SoundAPI.play(player, config);
                else SoundAPI.playAt(origin, config.getSound(), config.getVolume(), config.getPitch());
            }
            case LOCATION -> SoundAPI.playAt(origin, config.getSound(), config.getVolume(), config.getPitch());
            case RADIUS -> SoundAPI.playInRadius(origin, entry.getRadius(),
                    config.getSound(), config.getVolume(), config.getPitch());
            case GLOBAL -> SoundAPI.playToFiltered(viewer -> true,
                    config.getSound(), config.getVolume(), config.getPitch());
        }
    }

    /** Resolves the audience for player-targeted effects (title, actionbar, message, potion). */
    private void forEachViewer(
            EffectContext context,
            Location origin,
            EffectScope scope,
            double radius,
            java.util.function.Consumer<Player> action
    ) {
        switch (scope) {
            case PLAYER -> {
                Player player = context.getPlayer();
                if (player != null) action.accept(player);
            }
            case NEARBY, LOCATION, RADIUS -> {
                if (origin.getWorld() == null) return;
                double effective = scope == EffectScope.RADIUS ? radius : 16.0;
                double radiusSquared = effective * effective;
                for (Player viewer : origin.getWorld().getPlayers()) {
                    if (viewer.getLocation().distanceSquared(origin) <= radiusSquared) action.accept(viewer);
                }
            }
            case GLOBAL -> Bukkit.getOnlinePlayers().forEach(action);
        }
    }

    public EffectStats getStats() {
        return EffectStats.builder()
                .totalEffectsPlayed(totalPlayed.get())
                .totalEffectsFailed(totalFailed.get())
                .totalEffectsSkipped(totalSkipped.get())
                .build();
    }
}
