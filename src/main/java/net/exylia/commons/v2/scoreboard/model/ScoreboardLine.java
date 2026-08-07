package net.exylia.commons.v2.scoreboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder
@With
@AllArgsConstructor
public final class ScoreboardLine {

    private final int position;
    private final String content;

    public static ScoreboardLine of(int position, String content) {
        return new ScoreboardLine(position, content == null ? "" : content);
    }

    public boolean isDynamic() {
        return content != null && content.contains("%");
    }
}
