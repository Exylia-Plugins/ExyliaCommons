package net.exylia.commons.v2.effect.api;

import net.exylia.commons.v2.effect.config.EffectConfigLoader;
import net.exylia.commons.v2.effect.core.EffectManager;
import net.exylia.commons.v2.effect.core.EffectStats;
import net.exylia.commons.v2.effect.model.EffectContext;
import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectResult;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static facade for the configurable effect subsystem: particles, sounds, potions, fireworks,
 * titles, actionbars, messages, and sequence choreographies.
 *
 * <p>Effects are content, so they are authored in YAML and played through this facade, the same
 * way {@link net.exylia.commons.v2.reward.api.RewardAPI} handles rewards.
 *
 * <pre>{@code
 * // Global effects, or per-block overrides when the block declares its own
 * EffectAPI.playVariant(player, mineSection, "blocks", block.getType().name());
 * }</pre>
 *
 * <p>Initialized automatically during bootstrap. Gating (chance/condition/permission) and delays
 * are evaluated per entry; all Bukkit work is dispatched on the owning region thread, so these
 * methods are safe to call from async code.
 */
public final class EffectAPI {

    private EffectAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        EffectManager.getInstance().initialize(plugin);
    }

    public static boolean isInitialized() {
        return EffectManager.getInstance().isInitialized();
    }

    // ------------------------------------------------------------ from entries

    public static List<EffectResult> play(Player player, @Nullable List<EffectEntry> entries) {
        return play(entries, EffectContext.of(player));
    }

    public static List<EffectResult> play(
            Player player,
            @Nullable List<EffectEntry> entries,
            PlaceholderContext placeholders
    ) {
        return play(entries, EffectContext.of(player, placeholders));
    }

    public static List<EffectResult> playAt(Location location, @Nullable List<EffectEntry> entries) {
        return play(entries, EffectContext.at(location));
    }

    public static List<EffectResult> play(@Nullable List<EffectEntry> entries, EffectContext context) {
        return EffectManager.getInstance().play(entries, context);
    }

    public static EffectResult play(Player player, EffectEntry entry) {
        return playSingle(entry, EffectContext.of(player));
    }

    public static EffectResult play(Player player, EffectEntry entry, PlaceholderContext placeholders) {
        return playSingle(entry, EffectContext.of(player, placeholders));
    }

    public static EffectResult playSingle(EffectEntry entry, EffectContext context) {
        return EffectManager.getInstance().playSingle(entry, context);
    }

    // ------------------------------------------------------------- from config

    /** Plays the effects declared under the {@code effects} key of {@code section}. */
    public static List<EffectResult> play(Player player, @Nullable ConfigurationSection section) {
        return playFromKey(player, section, EffectConfigLoader.DEFAULT_KEY);
    }

    /** Plays the effects declared under an arbitrary key of {@code section}. */
    public static List<EffectResult> playFromKey(
            Player player,
            @Nullable ConfigurationSection section,
            String key
    ) {
        return playFromKey(section, key, EffectContext.of(player));
    }

    public static List<EffectResult> playFromKey(
            @Nullable ConfigurationSection section,
            String key,
            EffectContext context
    ) {
        return EffectManager.getInstance().playFromConfig(section, key, context);
    }

    // ------------------------------------------------------------ variant form

    /**
     * Plays the effects of a variant, falling back to the global ones. If the variant declares its
     * own {@code effects} they fully replace the global list (override semantics).
     *
     * <pre>{@code
     * effects:                       # global fallback
     *   sounds:
     *     - sound: BLOCK_STONE_BREAK
     * blocks:
     *   DIAMOND_ORE:                 # per-block override
     *     effects:
     *       sounds:
     *         - sound: ENTITY_PLAYER_LEVELUP
     * }</pre>
     *
     * @param variants   name of the section holding per-variant overrides, e.g. {@code blocks}
     * @param variantKey the variant to resolve, e.g. {@code DIAMOND_ORE} (case-insensitive)
     */
    public static List<EffectResult> playVariant(
            Player player,
            @Nullable ConfigurationSection root,
            String variants,
            @Nullable String variantKey
    ) {
        return playVariant(root, variants, variantKey, EffectConfigLoader.DEFAULT_KEY,
                EffectContext.of(player));
    }

    /** @see #playVariant(Player, ConfigurationSection, String, String) */
    public static List<EffectResult> playVariant(
            Player player,
            @Nullable ConfigurationSection root,
            String variants,
            @Nullable String variantKey,
            Location location
    ) {
        return playVariant(root, variants, variantKey, EffectConfigLoader.DEFAULT_KEY,
                EffectContext.of(player, location));
    }

    /** @see #playVariant(Player, ConfigurationSection, String, String) */
    public static List<EffectResult> playVariant(
            @Nullable ConfigurationSection root,
            String variants,
            @Nullable String variantKey,
            String effectsKey,
            EffectContext context
    ) {
        return EffectManager.getInstance().playVariant(root, variants, variantKey, effectsKey, context);
    }

    // -------------------------------------------------------------- loading

    /** Parses the {@code effects} key without playing it — useful to preload or edit. */
    public static List<EffectEntry> load(@Nullable ConfigurationSection section) {
        return EffectManager.getInstance().getConfigLoader().load(section);
    }

    /** Parses an arbitrary key without playing it. */
    public static List<EffectEntry> load(@Nullable ConfigurationSection section, String key) {
        return EffectManager.getInstance().getConfigLoader().loadFromKey(section, key);
    }

    /** Resolves variant-or-global effects without playing them. */
    public static List<EffectEntry> loadVariant(
            @Nullable ConfigurationSection root,
            String variants,
            @Nullable String variantKey
    ) {
        return EffectManager.getInstance().getConfigLoader().resolve(root, variants, variantKey);
    }

    // ---------------------------------------------------------------- misc

    public static EffectBuilder builder() {
        return new EffectBuilder();
    }

    public static EffectStats getStats() {
        return EffectManager.getInstance().getStats();
    }

    public static void shutdown() {
        EffectManager.getInstance().shutdown();
    }
}
