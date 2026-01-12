package net.exylia.commons.v2.reward.provider;

import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.reward.model.Reward;
import net.exylia.commons.v2.reward.model.RewardContext;
import net.exylia.commons.v2.reward.model.RewardResult;
import net.exylia.commons.v2.visual.api.MessageAPI;

import java.util.concurrent.CompletableFuture;

public class MessageRewardProvider implements RewardProvider {

    @Override
    public CompletableFuture<RewardResult> provide(Reward reward, RewardContext context) {
        String message = (String) reward.getData();

        String processed = Placeholders.process(
                message,
                context.getPlayer(),
                context.getPlaceholderContext()
        );

        try {
            MessageAPI.send(context.getPlayer(), processed, context.getPlaceholderContext());
            return CompletableFuture.completedFuture(RewardResult.success(reward));
        } catch (Exception throwable) {
            return CompletableFuture.completedFuture(
                    RewardResult.builder()
                            .success(false)
                            .reward(reward)
                            .error(throwable)
                            .message("Failed to send message: " + throwable.getMessage())
                            .build()
            );
        }
    }
}
