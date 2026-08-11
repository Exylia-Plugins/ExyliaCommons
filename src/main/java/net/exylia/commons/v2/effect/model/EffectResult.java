package net.exylia.commons.v2.effect.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Outcome of playing one {@link EffectEntry}. Mirrors
 * {@link net.exylia.commons.v2.reward.model.RewardResult}.
 */
@Getter
@Builder
public class EffectResult {

    private final boolean success;
    private final String message;
    private final EffectEntry entry;
    private final Throwable error;

    @Builder.Default
    private final boolean skippedByProbability = false;

    @Builder.Default
    private final boolean skippedByCondition = false;

    @Builder.Default
    private final boolean skippedByPermission = false;

    @Builder.Default
    private final boolean delayed = false;

    public static EffectResult success(EffectEntry entry) {
        return EffectResult.builder()
                .success(true)
                .entry(entry)
                .message("Effect played successfully")
                .build();
    }

    public static EffectResult delayed(EffectEntry entry) {
        return EffectResult.builder()
                .success(true)
                .entry(entry)
                .delayed(true)
                .message("Effect scheduled in " + entry.getDelayTicks() + " tick(s)")
                .build();
    }

    public static EffectResult failure(EffectEntry entry, String message) {
        return EffectResult.builder()
                .success(false)
                .entry(entry)
                .message(message)
                .build();
    }

    public static EffectResult failure(EffectEntry entry, Throwable error) {
        return EffectResult.builder()
                .success(false)
                .entry(entry)
                .error(error)
                .message(error.getMessage())
                .build();
    }

    public static EffectResult skippedProbability(EffectEntry entry) {
        return EffectResult.builder()
                .success(false)
                .entry(entry)
                .skippedByProbability(true)
                .message("Skipped by probability check")
                .build();
    }

    public static EffectResult skippedCondition(EffectEntry entry) {
        return EffectResult.builder()
                .success(false)
                .entry(entry)
                .skippedByCondition(true)
                .message("Skipped by condition check")
                .build();
    }

    public static EffectResult skippedPermission(EffectEntry entry) {
        return EffectResult.builder()
                .success(false)
                .entry(entry)
                .skippedByPermission(true)
                .message("Skipped by permission check")
                .build();
    }

    /** @return true when the effect did not run because a gate rejected it (not an error). */
    public boolean isSkipped() {
        return skippedByProbability || skippedByCondition || skippedByPermission;
    }
}
