package net.exylia.commons.v2.reward.core;

import net.exylia.commons.v2.placeholders.Placeholders;
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

        List<CompletableFuture<RewardResult>> futures = rewards.stream()
                .map(reward -> executeSingle(reward, context))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    public CompletableFuture<RewardResult> executeSingle(Reward reward, RewardContext context) {
        if (!context.isSkipProbability() && !ProbabilityProcessor.shouldGive(reward)) {
            return CompletableFuture.completedFuture(RewardResult.skippedProbability(reward));
        }

        if (!context.isSkipConditions() && reward.getCondition() != null) {
            boolean conditionMet = ConditionProcessor.evaluate(
                    reward.getCondition(),
                    context.getPlayer(),
                    context.getPlaceholderContext()
            );

            if (!conditionMet) {
                return CompletableFuture.completedFuture(RewardResult.skippedCondition(reward));
            }
        }

        RewardProvider provider = providers.get(reward.getType());
        if (provider == null) {
            totalFailed.incrementAndGet();
            return CompletableFuture.completedFuture(
                    RewardResult.failure(reward, "No provider found for type: " + reward.getType())
            );
        }

        return provider.provide(reward, context)
                .thenCompose(result -> {
                    if (result.isSuccess()) {
                        totalGiven.incrementAndGet();

                        if (!context.isSilent() && reward.getMessage() != null) {
                            String processedMessage = Placeholders.process(
                                    reward.getMessage(),
                                    context.getPlayer(),
                                    context.getPlaceholderContext()
                            );

                            return MessageAPI.send(
                                    context.getPlayer(),
                                    processedMessage,
                                    context.getPlaceholderContext()
                            ).thenApply(v -> result);
                        }
                    } else {
                        totalFailed.incrementAndGet();
                    }

                    return CompletableFuture.completedFuture(result);
                })
                .exceptionally(throwable -> {
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
