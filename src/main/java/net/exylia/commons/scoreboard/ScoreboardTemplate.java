package net.exylia.commons.scoreboard;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.placeholders.ExyliaContext;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Template simplificado para scoreboards
 */
class ScoreboardTemplate {

    @Getter
    private final String id;
    @Getter
    private final String title;
    @Getter
    private final Map<Integer, String> lines;
    @Getter
    private final int updateTicks;
    @Setter
    private Function<Player, ExyliaContext> contextProvider;

    public ScoreboardTemplate(String id, String title, Map<Integer, String> lines, int updateTicks) {
        this.id = id;
        this.title = title;
        this.lines = lines;
        this.updateTicks = updateTicks;
        this.contextProvider = null;
    }

    public ScoreboardTemplate(String id, String title, Map<Integer, String> lines, int updateTicks,
                              Function<Player, ExyliaContext> contextProvider) {
        this.id = id;
        this.title = title;
        this.lines = lines;
        this.updateTicks = updateTicks;
        this.contextProvider = contextProvider;
    }

    public ExyliaContext getContext(Player player) {
        return contextProvider != null ? contextProvider.apply(player) : ExyliaContext.create();
    }

    public boolean shouldUpdate() { return updateTicks > 0; }

    public boolean hasContextProvider() {
        return contextProvider != null;
    }
}