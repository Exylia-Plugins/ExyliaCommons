package net.exylia.commons.v2.reward.api;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.reward.core.RewardManager;
import net.exylia.commons.v2.reward.core.RewardStats;
import net.exylia.commons.v2.reward.model.RewardContext;
import net.exylia.commons.v2.reward.model.RewardEntry;
import net.exylia.commons.v2.reward.model.RewardResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RewardAPI {

    private RewardAPI() {
    }

    public static void initialize(JavaPlugin plugin) {
        RewardManager.getInstance().initialize(plugin);
    }

    public static CompletableFuture<List<RewardResult>> give(
            Player player,
            ConfigurationSection section
    ) {
        return RewardManager.getInstance().giveFromConfig(player, section);
    }

    public static CompletableFuture<List<RewardResult>> give(
            Player player,
            ConfigurationSection section,
            PlaceholderContext context
    ) {
        RewardContext rewardContext = RewardContext.builder()
                .player(player)
                .placeholderContext(context)
                .build();

        return RewardManager.getInstance().giveFromConfig(player, section, rewardContext);
    }

    public static CompletableFuture<List<RewardResult>> giveFromKey(
            Player player,
            ConfigurationSection section,
            String key
    ) {
        return RewardManager.getInstance().giveFromConfigKey(player, section, key);
    }

    public static CompletableFuture<List<RewardResult>> giveFromKey(
            Player player,
            ConfigurationSection section,
            String key,
            PlaceholderContext context
    ) {
        RewardContext rewardContext = RewardContext.builder()
                .player(player)
                .placeholderContext(context)
                .build();

        return RewardManager.getInstance().giveFromConfigKey(player, section, key, rewardContext);
    }

    public static CompletableFuture<List<RewardResult>> give(
            Player player,
            List<RewardEntry> entries
    ) {
        return RewardManager.getInstance().giveEntries(player, entries);
    }

    public static CompletableFuture<List<RewardResult>> give(
            Player player,
            List<RewardEntry> entries,
            PlaceholderContext context
    ) {
        RewardContext rewardContext = RewardContext.builder()
                .player(player)
                .placeholderContext(context)
                .build();
        return RewardManager.getInstance().giveEntries(player, entries, rewardContext);
    }

    public static RewardBuilder builder() {
        return new RewardBuilder();
    }

    public static RewardStats getStats() {
        return RewardManager.getInstance().getStats();
    }

    public static void shutdown() {
        RewardManager.getInstance().shutdown();
    }
}
