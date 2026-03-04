package net.exylia.commons.v2.scoreboard.renderer;

import lombok.Getter;
import net.exylia.commons.v2.scoreboard.fastboard.FastBoard;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class FastBoardAdapter {

    private final FastBoard fastBoard;
    private final Player player;
    private final Map<String, String> lastContent;
    private boolean deleted;

    public FastBoardAdapter(Player player) {
        this.player = player;
        this.fastBoard = new FastBoard(player);
        this.lastContent = new HashMap<>();
        this.deleted = false;
    }

    public void updateTitle(String title) {
        if (deleted) {
            return;
        }

        if (hasChanged("title", title)) {
            fastBoard.updateTitle(title);
            updateLastContent("title", title);
        }
    }

    public void updateLines(List<String> lines) {
        if (deleted) {
            return;
        }

        String linesKey = "lines";
        String linesValue = String.join("|", lines);

        if (hasChanged(linesKey, linesValue)) {
            fastBoard.updateLines(lines.toArray(new String[0]));
            updateLastContent(linesKey, linesValue);
        }
    }

    public org.bukkit.scoreboard.Scoreboard getBukkitScoreboard() {
        return player.getScoreboard();
    }

    public void delete() {
        if (!deleted) {
            fastBoard.delete();
            deleted = true;
            lastContent.clear();
        }
    }

    private boolean hasChanged(String key, String newValue) {
        String oldValue = lastContent.get(key);
        return oldValue == null || !oldValue.equals(newValue);
    }

    private void updateLastContent(String key, String value) {
        lastContent.put(key, value);
    }
}
