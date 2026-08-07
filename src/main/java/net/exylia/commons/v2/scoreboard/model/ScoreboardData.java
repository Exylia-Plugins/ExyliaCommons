package net.exylia.commons.v2.scoreboard.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreboardData {

    private String title;
    private List<String> lines;
    private boolean enabled;
    private long updateInterval;

    public static ScoreboardData fromScoreboard(Scoreboard scoreboard) {
        return new ScoreboardData(
            scoreboard.getTitle(),
            scoreboard.getLines().stream()
                .map(ScoreboardLine::getContent)
                .collect(Collectors.toList()),
            scoreboard.isEnabled(),
            scoreboard.getUpdateInterval()
        );
    }
}
