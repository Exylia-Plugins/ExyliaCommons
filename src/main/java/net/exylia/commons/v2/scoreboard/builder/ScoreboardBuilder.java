package net.exylia.commons.v2.scoreboard.builder;

import net.exylia.commons.v2.scoreboard.config.UpdateConfig;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class ScoreboardBuilder {

    private String title = "";
    private final List<String> lines = new ArrayList<>();
    private long updateInterval = 20L;
    private boolean enabled = true;

    public static ScoreboardBuilder create() {
        return new ScoreboardBuilder();
    }

    public ScoreboardBuilder title(String title) {
        this.title = title != null ? title : "";
        return this;
    }

    public ScoreboardBuilder line(String line) {
        if (line != null) this.lines.add(line);
        return this;
    }

    public ScoreboardBuilder lines(String... lines) {
        if (lines != null) this.lines.addAll(Arrays.asList(lines));
        return this;
    }

    public ScoreboardBuilder lines(List<String> lines) {
        if (lines != null) this.lines.addAll(lines);
        return this;
    }

    public ScoreboardBuilder updateInterval(long ticks) {
        this.updateInterval = Math.max(1L, ticks);
        return this;
    }

    public ScoreboardBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Scoreboard build() {
        validate();

        List<ScoreboardLine> scoreboardLines = IntStream.range(0, lines.size())
                .mapToObj(i -> ScoreboardLine.of(i, lines.get(i)))
                .toList();

        return Scoreboard.builder()
                .title(title)
                .lines(scoreboardLines)
                .updateConfig(UpdateConfig.builder().updateInterval(updateInterval).build())
                .enabled(enabled)
                .build();
    }

    private void validate() {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Scoreboard title cannot be null or empty");
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Scoreboard must have at least one line");
        }
        if (lines.size() > 15) {
            throw new IllegalArgumentException("Scoreboard cannot have more than 15 lines");
        }
    }
}
