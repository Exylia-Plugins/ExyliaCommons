package net.exylia.commons.v2.scoreboard.renderer;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class FastBoardComponentAdapter {

    private final FastBoardComponent fastBoard;
    private final Player player;
    private final Map<String, String> lastContent;
    private boolean deleted;

    public FastBoardComponentAdapter(Player player) {
        this.player = player;
        this.fastBoard = new FastBoardComponent(player);
        this.lastContent = new HashMap<>();
        this.deleted = false;
    }

    public void updateTitle(Component title) {
        if (deleted) {
            return;
        }

        String serialized = serializeComponent(title);
        if (hasChanged("title", serialized)) {
            fastBoard.updateTitle(title);
            updateLastContent("title", serialized);
        }
    }

    public void updateLines(List<Component> lines) {
        if (deleted) {
            return;
        }

        String linesKey = "lines";
        String linesValue = lines.stream()
                .map(this::serializeComponent)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");

        if (hasChanged(linesKey, linesValue)) {
            fastBoard.updateLines(lines.toArray(new Component[0]));
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

    private String serializeComponent(Component component) {
        if (component == null) {
            return "";
        }
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    private boolean hasChanged(String key, String newValue) {
        String oldValue = lastContent.get(key);
        return oldValue == null || !oldValue.equals(newValue);
    }

    private void updateLastContent(String key, String value) {
        lastContent.put(key, value);
    }
}
