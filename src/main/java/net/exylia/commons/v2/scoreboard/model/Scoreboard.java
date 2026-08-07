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
public class Scoreboard {

    @Builder.Default
    private final String id = UUID.randomUUID().toString();

    private final String title;
    private final List<ScoreboardLine> lines;
    private final UpdateConfig updateConfig;

    @Builder.Default
    private final boolean enabled = true;

    public long getUpdateInterval() {
        return updateConfig != null ? updateConfig.getUpdateInterval() : UpdateConfig.defaults().getUpdateInterval();
    }
}
