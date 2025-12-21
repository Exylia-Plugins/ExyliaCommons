package net.exylia.commons.v2.reward.provider;

import net.exylia.commons.v2.reward.model.Reward;
import net.exylia.commons.v2.reward.model.RewardContext;
import net.exylia.commons.v2.reward.model.RewardResult;

import java.util.concurrent.CompletableFuture;

public interface RewardProvider {
    CompletableFuture<RewardResult> provide(Reward reward, RewardContext context);
}
