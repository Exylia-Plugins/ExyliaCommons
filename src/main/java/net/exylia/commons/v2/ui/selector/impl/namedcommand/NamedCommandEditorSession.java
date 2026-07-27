package net.exylia.commons.v2.ui.selector.impl.namedcommand;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.namedcommand.model.NamedCommandEntry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Getter
public class NamedCommandEditorSession {

    private final Player player;
    private final List<NamedCommandEntry> commands;
    private final BiConsumer<Player, List<NamedCommandEntry>> onSave;
    private final Runnable onCancel;

    @Setter
    private String title;

    public NamedCommandEditorSession(
            Player player,
            List<NamedCommandEntry> initialCommands,
            String title,
            BiConsumer<Player, List<NamedCommandEntry>> onSave,
            Runnable onCancel
    ) {
        this.player = player;
        this.commands = new ArrayList<>(initialCommands);
        this.title = title;
        this.onSave = onSave;
        this.onCancel = onCancel;
    }

    public void addCommand(NamedCommandEntry entry) {
        commands.add(entry);
    }

    public void removeCommand(String id) {
        commands.removeIf(c -> c.getId().equals(id));
    }

    public NamedCommandEntry findById(String id) {
        return commands.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    public void replaceCommand(NamedCommandEntry updated) {
        int idx = -1;
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i).getId().equals(updated.getId())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            commands.set(idx, updated);
        } else {
            commands.add(updated);
        }
    }
}
