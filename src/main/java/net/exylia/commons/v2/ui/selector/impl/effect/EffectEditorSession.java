package net.exylia.commons.v2.ui.selector.impl.effect;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.effect.model.EffectEntry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Getter
public final class EffectEditorSession {
    private final Player player;
    private final List<EffectEntry> entries;
    private final BiConsumer<Player, List<EffectEntry>> onSave;
    private final Runnable onCancel;

    @Setter
    private String title;

    public EffectEditorSession(Player player, List<EffectEntry> initialEntries, String title,
                               BiConsumer<Player, List<EffectEntry>> onSave, Runnable onCancel) {
        this.player = player;
        this.entries = new ArrayList<>();
        if (initialEntries != null) {
            initialEntries.forEach(entry -> this.entries.add(entry.copy()));
        }
        this.title = title;
        this.onSave = onSave;
        this.onCancel = onCancel;
    }

    public void addEntry(EffectEntry entry) {
        if (entry != null) entries.add(entry);
    }

    public void removeEntry(String id) {
        entries.removeIf(entry -> entry.getId().equals(id));
    }

    public EffectEntry findById(String id) {
        return entries.stream().filter(entry -> entry.getId().equals(id)).findFirst().orElse(null);
    }

    public void replaceEntry(EffectEntry updated) {
        if (updated == null) return;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId().equals(updated.getId())) {
                entries.set(i, updated);
                return;
            }
        }
        entries.add(updated);
    }
}
