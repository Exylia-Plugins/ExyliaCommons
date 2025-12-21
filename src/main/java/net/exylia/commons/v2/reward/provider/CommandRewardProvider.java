package net.exylia.commons.v2.reward.provider;

import net.exylia.commons.v2.command.api.CommandAPI;
import net.exylia.commons.v2.placeholders.Placeholders;
import net.exylia.commons.v2.reward.model.Reward;
import net.exylia.commons.v2.reward.model.RewardContext;
import net.exylia.commons.v2.reward.model.RewardResult;

import java.util.concurrent.CompletableFuture;

public class CommandRewardProvider implements RewardProvider {

    @Override
    public CompletableFuture<RewardResult> provide(Reward reward, RewardContext context) {
        String command = (String) reward.getData();

        String processed = Placeholders.process(
                command,
                context.getPlayer(),
                context.getPlaceholderContext()
        );

        return CommandAPI.execute(context.getPlayer(), processed, context.getPlaceholderContext())
                .thenApply(cmdResult -> {
                    if (cmdResult.isSuccess()) {
                        return RewardResult.success(reward);
                    } else {
                        return RewardResult.failure(reward, cmdResult.getMessage());
                    }
                })
                .exceptionally(throwable ->
                        RewardResult.builder()
                                .success(false)
                                .reward(reward)
                                .error(throwable)
                                .message("Failed to execute command: " + throwable.getMessage())
                                .build()
                );
    }
}
