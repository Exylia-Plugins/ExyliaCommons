package net.exylia.commons.v2.scoreboard.renderer;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

@Getter
public class FastBoardComponentAdapter {

    private FastBoardComponent fastBoard;
    private final Player player;
    private Component lastTitle;
    private List<Component> lastLines;
    private boolean deleted;

    public FastBoardComponentAdapter(Player player) {
        this.player = player;
        this.fastBoard = new FastBoardComponent(player);
        this.deleted = false;
    }

    public void reinitialize() {
        if (deleted) return;
        try {
            fastBoard.delete();
        } catch (Exception ignored) {
        }
        fastBoard = new FastBoardComponent(player);
        lastTitle = null;
        lastLines = null;
    }

    public void updateTitle(Component title) {
        if (deleted) {
            return;
        }

        if (!title.equals(lastTitle)) {
            fastBoard.updateTitle(title);
            lastTitle = title;
        }
    }

    public void updateLines(List<Component> lines) {
        if (deleted) {
            return;
        }

        if (!lines.equals(lastLines)) {
            fastBoard.updateLines(lines.toArray(new Component[0]));
            lastLines = List.copyOf(lines);
        }
    }

    public org.bukkit.scoreboard.Scoreboard getBukkitScoreboard() {
        return player.getScoreboard();
    }

    public void delete() {
        if (!deleted) {
            fastBoard.delete();
            deleted = true;
            lastTitle = null;
            lastLines = null;
        }
    }
}
