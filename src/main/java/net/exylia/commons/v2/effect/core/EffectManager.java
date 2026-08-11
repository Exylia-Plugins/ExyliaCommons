package net.exylia.commons.v2.effect.core;

import net.exylia.commons.v2.effect.config.EffectConfigLoader;
import net.exylia.commons.v2.effect.model.EffectContext;
import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.effect.model.EffectResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Singleton lifecycle for the effect subsystem. Mirrors
 * {@link net.exylia.commons.v2.reward.core.RewardManager}.
 *
 * <p>Initialized automatically during bootstrap; consumers use
 * {@link net.exylia.commons.v2.effect.api.EffectAPI} instead of this class.
 */
public class EffectManager {

    private static volatile EffectManager instance;

    private JavaPlugin plugin;
    private EffectExecutor executor;
    private EffectConfigLoader configLoader;

    private EffectManager() {
    }

    public static EffectManager getInstance() {
        if (instance == null) {
            synchronized (EffectManager.class) {
                if (instance == null) {
                    instance = new EffectManager();
                }
            }
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = new EffectExecutor();
        this.configLoader = new EffectConfigLoader();
    }

    public boolean isInitialized() {
        return executor != null;
    }

    private EffectExecutor executor() {
        if (executor == null) {
            throw new IllegalStateException("EffectManager not initialized");
        }
        return executor;
    }

    public EffectConfigLoader getConfigLoader() {
        if (configLoader == null) {
            throw new IllegalStateException("EffectManager not initialized");
        }
        return configLoader;
    }

    public List<EffectResult> play(@Nullable List<EffectEntry> entries, EffectContext context) {
        return executor().play(entries, context);
    }

    public EffectResult playSingle(EffectEntry entry, EffectContext context) {
        return executor().playSingle(entry, context);
    }

    public List<EffectResult> playFromConfig(
            @Nullable ConfigurationSection section,
            String key,
            EffectContext context
    ) {
        return play(getConfigLoader().loadFromKey(section, key), context);
    }

    public List<EffectResult> playVariant(
            @Nullable ConfigurationSection root,
            String variants,
            @Nullable String variantKey,
            String effectsKey,
            EffectContext context
    ) {
        return play(getConfigLoader().resolve(root, variants, variantKey, effectsKey), context);
    }

    public EffectStats getStats() {
        return executor().getStats();
    }

    public void reload() {
        if (plugin == null) {
            throw new IllegalStateException("EffectManager not initialized");
        }
        this.executor = new EffectExecutor();
        this.configLoader = new EffectConfigLoader();
    }

    public void shutdown() {
        this.plugin = null;
        this.executor = null;
        this.configLoader = null;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
