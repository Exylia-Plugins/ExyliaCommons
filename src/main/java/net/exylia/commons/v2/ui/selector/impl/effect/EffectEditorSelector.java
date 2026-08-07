package net.exylia.commons.v2.ui.selector.impl.effect;

import net.exylia.commons.v2.effect.model.EffectEntry;
import net.exylia.commons.v2.ui.selector.core.AbstractSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.editor.menu.EffectListMenu;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class EffectEditorSelector extends AbstractSelector<List<EffectEntry>> {
    private List<EffectEntry> initialEntries = new ArrayList<>();

    private EffectEditorSelector(Player player) {
        super(player);
        this.title = "Effects";
    }

    public static EffectEditorSelector of(Player player) {
        return new EffectEditorSelector(player);
    }

    public EffectEditorSelector title(String title) {
        this.title = title;
        return this;
    }

    public EffectEditorSelector entries(List<EffectEntry> entries) {
        this.initialEntries = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
        return this;
    }

    public EffectEditorSelector onSave(BiConsumer<Player, List<EffectEntry>> callback) {
        this.onSelect = callback;
        return this;
    }

    public EffectEditorSelector onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    @Override
    public void open() {
        EffectEditorSession session = new EffectEditorSession(player, initialEntries, title, onSelect, onCancel);
        EffectEditorRegistry.getInstance().put(session);
        EffectListMenu.open(player, session);
    }
}
