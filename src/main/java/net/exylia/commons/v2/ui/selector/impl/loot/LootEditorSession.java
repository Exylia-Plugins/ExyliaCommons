package net.exylia.commons.v2.ui.selector.impl.loot;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.loot.model.LootEntry;
import net.exylia.commons.v2.ui.selector.core.SelectorButton;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Getter
public class LootEditorSession {

    private final Player player;
    private final List<LootEntry> entries;
    private final BiConsumer<Player, List<LootEntry>> onSave;
    private final Runnable onCancel;
    private final Map<String, SelectorButton<LootEditorSession>> customButtons = new LinkedHashMap<>();

    /** When false (default), the "add entry" flow skips the type-select step entirely and only
     * ever creates ITEM entries — chests/spawners keep their exact pre-existing UX. When true,
     * the caller opted in (e.g. mines) and gets a type-select step (Item / Command) first. */
    @Setter
    private boolean allowCommands = false;

    @Setter
    private String title;

    public LootEditorSession(
            Player player,
            List<LootEntry> initialEntries,
            String title,
            BiConsumer<Player, List<LootEntry>> onSave,
            Runnable onCancel
    ) {
        this.player = player;
        this.entries = new ArrayList<>(initialEntries);
        this.title = title;
        this.onSave = onSave;
        this.onCancel = onCancel;
    }

    public void addCustomButton(SelectorButton<LootEditorSession> button) {
        customButtons.put(button.getKey(), button);
    }

    public SelectorButton<LootEditorSession> findCustomButton(String key) {
        return customButtons.get(key);
    }

    public void replaceEntries(List<LootEntry> replacement) {
        entries.clear();
        entries.addAll(replacement);
    }

    public void addEntry(LootEntry entry) {
        entries.add(entry);
    }

    public void removeEntry(String id) {
        entries.removeIf(e -> e.getId().equals(id));
    }

    public LootEntry findById(String id) {
        return entries.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
    }

    public void replaceEntry(LootEntry updated) {
        int idx = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId().equals(updated.getId())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            entries.set(idx, updated);
        } else {
            entries.add(updated);
        }
    }
}
