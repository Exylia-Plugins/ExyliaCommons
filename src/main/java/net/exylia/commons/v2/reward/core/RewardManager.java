package net.exylia.commons.v2.reward.core;

import net.exylia.commons.v2.reward.config.RewardConfigLoader;
import net.exylia.commons.v2.reward.model.Reward;
import net.exylia.commons.v2.reward.model.RewardContext;
import net.exylia.commons.v2.reward.model.RewardResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RewardManager {

    private static volatile RewardManager instance;

    private JavaPlugin plugin;
    private RewardExecutor executor;
    private RewardConfigLoader configLoader;

    private RewardManager() {
    }

    public static RewardManager getInstance() {
        if (instance == null) {
            synchronized (RewardManager.class) {
                if (instance == null) {
                    instance = new RewardManager();
                }
            }
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = new RewardExecutor();
        this.configLoader = new RewardConfigLoader();
    }

    public CompletableFuture<List<RewardResult>> giveFromConfig(
            Player player,
            ConfigurationSection section
    ) {
        RewardContext context = RewardContext.builder()
                .player(player)
                .build();

        return executor.executeFromConfig(section, context);
    }

    public CompletableFuture<List<RewardResult>> giveFromConfig(
            Player player,
            ConfigurationSection section,
            RewardContext context
    ) {
        return executor.executeFromConfig(section, context);
    }

    public CompletableFuture<List<RewardResult>> giveFromConfigKey(
            Player player,
            ConfigurationSection section,
            String key
    ) {
        RewardContext context = RewardContext.builder()
                .player(player)
                .build();

        return giveFromConfigKey(player, section, key, context);
    }

    public CompletableFuture<List<RewardResult>> giveFromConfigKey(
            Player player,
            ConfigurationSection section,
            String key,
            RewardContext context
    ) {
        if (section == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        ConfigurationSection rewardSection = section.getConfigurationSection(key);
        if (rewardSection == null && !section.isList(key)) {
            return CompletableFuture.completedFuture(List.of());
        }

        return executor.executeFromConfig(section, context);
    }

    public CompletableFuture<RewardResult> giveSingle(Reward reward, RewardContext context) {
        return executor.executeSingle(reward, context);
    }

    public RewardStats getStats() {
        return executor.getStats();
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
