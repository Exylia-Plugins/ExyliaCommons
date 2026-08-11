package net.exylia.commons.v2.effect.model;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Execution context for an effect. Mirrors
 * {@link net.exylia.commons.v2.reward.model.RewardContext}.
 *
 * <p>Either {@code player} or {@code location} must be present. When only a player is given, the
 * location defaults to the player's location; when only a location is given, player-targeted
 * effects (title, actionbar, message, potion) are skipped.
 */
@Getter
@Builder(toBuilder = true)
public class EffectContext {

    @Nullable
    private final Player player;

    /** Where the effect happens. Defaults to the player's location when null. */
    @Nullable
    private final Location location;

    /** Optional entity target, forwarded to the sequence engine for {@code [POTION]}. */
    @Nullable
    private final Entity targetEntity;

    @Builder.Default
    private final PlaceholderContext placeholderContext = PlaceholderContext.create();

    @Builder.Default
    private final Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    private final UUID executionId = UUID.randomUUID();

    @Builder.Default
    private final long executionStartTime = System.currentTimeMillis();

    @Builder.Default
    private final boolean skipConditions = false;

    @Builder.Default
    private final boolean skipProbability = false;

    @Builder.Default
    private final boolean skipPermissionCheck = false;

    /** When true, {@code delay} is ignored and every effect plays immediately. */
    @Builder.Default
    private final boolean skipDelay = false;

    public static EffectContext of(Player player) {
        return EffectContext.builder().player(player).build();
    }

    public static EffectContext of(Player player, PlaceholderContext context) {
        return EffectContext.builder().player(player).placeholderContext(context).build();
    }

    public static EffectContext at(Location location) {
        return EffectContext.builder().location(location).build();
    }

    public static EffectContext of(Player player, Location location) {
        return EffectContext.builder().player(player).location(location).build();
    }

    /** @return the effect origin: the explicit location, else the player's location, else null. */
    @Nullable
    public Location resolveLocation() {
        if (location != null) return location;
        return player != null ? player.getLocation() : null;
    }

    /** @return a placeholder context bound to the context player when one is present. */
    public PlaceholderContext resolvePlaceholderContext() {
        PlaceholderContext base = placeholderContext != null ? placeholderContext : PlaceholderContext.create();
        return player != null ? base.withPlayer(player) : base;
    }
}
