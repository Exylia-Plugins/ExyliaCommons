package net.exylia.commons.scoreboard;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Function;

/**
 * Template simplificado para scoreboards
 */
public class ScoreboardTemplate {

    private final String id;
    private final String title;
    private final Map<Integer, String> lines;
    private final int updateTicks;
    private Function<Player, Object> contextProvider;

    public ScoreboardTemplate(String id, String title, Map<Integer, String> lines, int updateTicks) {
        this.id = id;
        this.title = title;
        this.lines = lines;
        this.updateTicks = updateTicks;
        this.contextProvider = null;
    }

    public ScoreboardTemplate(String id, String title, Map<Integer, String> lines, int updateTicks, Function<Player, Object> contextProvider) {
        this.id = id;
        this.title = title;
        this.lines = lines;
        this.updateTicks = updateTicks;
        this.contextProvider = contextProvider;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Map<Integer, String> getLines() {
        return lines;
    }

    public int getUpdateTicks() {
        return updateTicks;
    }

    public boolean shouldUpdate() {
        return updateTicks > 0;
    }

    public Object getContext(Player player) {
        return contextProvider != null ? contextProvider.apply(player) : null;
    }

    public void setContextProvider(Function<Player, Object> contextProvider) {
        this.contextProvider = contextProvider;
    }

    public boolean hasContext() {
        return contextProvider != null;
    }
}