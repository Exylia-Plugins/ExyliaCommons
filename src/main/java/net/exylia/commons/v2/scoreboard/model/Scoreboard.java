package net.exylia.commons.v2.scoreboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import net.exylia.commons.v2.scoreboard.config.UpdateConfig;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public final class Scoreboard {

    @Builder.Default
    private final String id = UUID.randomUUID().toString();

    private final String title;
    private final List<ScoreboardLine> lines;
    private final UpdateConfig updateConfig;

    @Builder.Default
    private final boolean enabled = true;

    public UpdateConfig getEffectiveUpdateConfig() {
        return updateConfig != null ? updateConfig : UpdateConfig.defaults();
    }

    public long getUpdateInterval() {
        return getEffectiveUpdateConfig().getUpdateInterval();
    }

    public boolean isSmartUpdate() {
        return getEffectiveUpdateConfig().isSmartUpdate();
    }

    public boolean isCacheEnabled() {
        return getEffectiveUpdateConfig().isCacheEnabled();
    }
}
