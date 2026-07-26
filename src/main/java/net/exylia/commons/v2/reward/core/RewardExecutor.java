package net.exylia.commons.v2.reward.core;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.reward.config.RewardConfig;
import net.exylia.commons.v2.reward.config.RewardConfigLoader;
import net.exylia.commons.v2.reward.model.Reward;
import net.exylia.commons.v2.reward.model.RewardContext;
import net.exylia.commons.v2.reward.model.RewardResult;
import net.exylia.commons.v2.reward.model.RewardType;
import net.exylia.commons.v2.reward.processor.ConditionProcessor;
import net.exylia.commons.v2.reward.processor.ProbabilityProcessor;
import net.exylia.commons.v2.reward.provider.CommandRewardProvider;
import net.exylia.commons.v2.reward.provider.ItemRewardProvider;
import net.exylia.commons.v2.reward.provider.MessageRewardProvider;
import net.exylia.commons.v2.reward.provider.RewardProvider;
import net.exylia.commons.v2.visual.api.MessageAPI;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class RewardExecutor {

    private final Map<RewardType, RewardProvider> providers;
    private final RewardConfigLoader configLoader;
    private final AtomicLong totalGiven;
    private final AtomicLong totalFailed;

    public RewardExecutor() {
        this.providers = new EnumMap<>(RewardType.class);
        this.configLoader = new RewardConfigLoader();
        this.totalGiven = new AtomicLong(0);
        this.totalFailed = new AtomicLong(0);

        registerProviders();
    }

    private void registerProviders() {
        providers.put(RewardType.COMMAND, new CommandRewardProvider());
        providers.put(RewardType.ITEM, new ItemRewardProvider());
        providers.put(RewardType.MESSAGE, new MessageRewardProvider());
    }

    public CompletableFuture<List<RewardResult>> executeFromConfig(
            ConfigurationSection section,
            RewardContext context
    ) {
        List<RewardConfig> configs = configLoader.load(section);
        return executeAll(configs, context);
    }

    public CompletableFuture<List<RewardResult>> executeAll(
            List<RewardConfig> configs,
            RewardContext context
    ) {
        List<Reward> rewards = configs.stream()
                .map(this::convertToReward)
                .sorted(Comparator.comparingInt(Reward::getPriority).reversed())
                .collect(Collectors.toList());

        return executeRewards(rewards, context);
    }

    public CompletableFuture<List<RewardResult>> executeRewards(
            List<Reward> rewards,
            RewardContext context
    ) {
        List<Reward> sorted = rewards.stream()
                .sorted(Comparator.comparingInt(Reward::getPriority).reversed())
                .collect(Collectors.toList());

        List<CompletableFuture<RewardResult>> futures = sorted.stream()
                .map(reward -> executeSingle(reward, context))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<RewardResult> executeSingle(Reward reward, RewardContext context) {
        DebugAPI.logLibDebug(DebugCategory.REWARD,
            "Executing reward: " + reward.getId() + " for " + context.getPlayer().getName());

        if (!context.isSkipProbability() && !ProbabilityProcessor.shouldGive(reward)) {
            DebugAPI.logLibDebug(DebugCategory.REWARD, "Reward skipped (probability): " + reward.getId());
            return CompletableFuture.completedFuture(RewardResult.skippedProbability(reward));
        }

        if (!context.isSkipPermissionCheck() && reward.getPermission() != null && !reward.getPermission().isBlank()
                && !context.getPlayer().hasPermission(reward.getPermission())) {
            DebugAPI.logLibDebug(DebugCategory.REWARD, "Reward skipped (permission): " + reward.getId());
            return CompletableFuture.completedFuture(RewardResult.skippedPermission(reward));
        }

        if (!context.isSkipConditions() && reward.getCondition() != null) {
            boolean conditionMet = ConditionProcessor.evaluate(
                    reward.getCondition(),
                    context.getPlayer(),
                    context.getPlaceholderContext()
            );

            if (!conditionMet) {
                DebugAPI.logLibDebug(DebugCategory.REWARD, "Reward skipped (condition): " + reward.getId());
                return CompletableFuture.completedFuture(RewardResult.skippedCondition(reward));
            }
        }

        RewardProvider provider = providers.get(reward.getType());
        if (provider == null) {
            DebugAPI.logLibError(DebugCategory.REWARD,
                "No provider found for reward type: " + reward.getType() + " (ID: " + reward.getId() + ")");
            totalFailed.incrementAndGet();
            return CompletableFuture.completedFuture(
                    RewardResult.failure(reward, "No provider found for type: " + reward.getType())
            );
        }

        return provider.provide(reward, context)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        DebugAPI.logLibDebug(DebugCategory.REWARD,
                            "Reward executed successfully: " + reward.getId());
                        totalGiven.incrementAndGet();

                        if (!context.isSilent() && reward.getMessage() != null) {
                            String processedMessage = Placeholders.process(
                                    reward.getMessage(),
                                    context.getPlayer(),
                                    context.getPlaceholderContext()
                            );

                            MessageAPI.send(
                                    context.getPlayer(),
                                    processedMessage,
                                    context.getPlaceholderContext()
                            );
                        }
                    } else {
                        DebugAPI.logLibError(DebugCategory.REWARD,
                            "Reward execution failed: " + reward.getId() + " - " + result.getMessage());
                        totalFailed.incrementAndGet();
                    }

                    return result;
                })
                .exceptionally(throwable -> {
                    DebugAPI.logLibError(DebugCategory.REWARD,
                        "Unexpected error executing reward: " + reward.getId(), throwable);
                    totalFailed.incrementAndGet();
                    return RewardResult.builder()
                            .success(false)
                            .reward(reward)
                            .error(throwable)
                            .message("Unexpected error: " + throwable.getMessage())
                            .build();
                });
    }

    private Reward convertToReward(RewardConfig config) {
        return Reward.builder()
                .id(UUID.randomUUID().toString())
                .type(config.getType())
                .data(config.getRawData())
                .chance(config.getChance())
                .condition(config.getCondition())
                .permission(config.getPermission())
                .message(config.getMessage())
                .priority(config.getPriority())
                .build();
    }

    public RewardStats getStats() {
        return RewardStats.builder()
                .totalRewardsGiven(totalGiven.get())
                .totalRewardsFailed(totalFailed.get())
                .build();
    }
}
