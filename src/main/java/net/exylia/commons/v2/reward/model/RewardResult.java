package net.exylia.commons.v2.reward.model;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class RewardResult {
    private final boolean success;
    private final String message;
    private final Reward reward;
    private final Throwable error;

    @Builder.Default
    private final long executionTimeMillis = 0;

    @Builder.Default
    private final List<String> details = new ArrayList<>();

    @Builder.Default
    private final boolean skippedByProbability = false;

    @Builder.Default
    private final boolean skippedByCondition = false;

    @Builder.Default
    private final boolean skippedByPermission = false;

    public static RewardResult success(Reward reward) {
        return RewardResult.builder()
                .success(true)
                .reward(reward)
                .message("Reward given successfully")
                .build();
    }

    public static RewardResult success(Reward reward, String message) {
        return RewardResult.builder()
                .success(true)
                .reward(reward)
                .message(message)
                .build();
    }

    public static RewardResult failure(Reward reward, String message) {
        return RewardResult.builder()
                .success(false)
                .reward(reward)
                .message(message)
                .build();
    }

    public static RewardResult failure(Reward reward, Throwable error) {
        return RewardResult.builder()
                .success(false)
                .reward(reward)
                .message(error.getMessage())
                .error(error)
                .build();
    }

    public static RewardResult skippedProbability(Reward reward) {
        return RewardResult.builder()
                .success(false)
                .reward(reward)
                .skippedByProbability(true)
                .message("Skipped by probability check")
                .build();
    }

    public static RewardResult skippedCondition(Reward reward) {
        return RewardResult.builder()
                .success(false)
                .reward(reward)
                .skippedByCondition(true)
                .message("Skipped by condition check")
                .build();
    }

    public static RewardResult skippedPermission(Reward reward) {
        return RewardResult.builder()
                .success(false)
                .reward(reward)
                .skippedByPermission(true)
                .message("Skipped by permission check")
                .build();
    }
}
