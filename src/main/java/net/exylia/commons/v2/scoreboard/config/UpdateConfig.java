package net.exylia.commons.v2.scoreboard.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder
@With
@AllArgsConstructor
public class UpdateConfig {

    @Builder.Default
    private final long updateInterval = 20L;

    @Builder.Default
    private final boolean smartUpdate = true;

    @Builder.Default
    private final boolean cacheEnabled = true;

    public static UpdateConfig defaults() {
        return UpdateConfig.builder().build();
    }

    public static UpdateConfig fast() {
        return UpdateConfig.builder()
                .updateInterval(10L)
                .build();
    }

    public static UpdateConfig slow() {
        return UpdateConfig.builder()
                .updateInterval(40L)
                .build();
    }

    public static UpdateConfig minimal() {
        return UpdateConfig.builder()
                .updateInterval(100L)
                .build();
    }
}
