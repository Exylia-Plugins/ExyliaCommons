package net.exylia.commons.v2.ui.selector.impl.namedcommand;

import net.exylia.commons.v2.namedcommand.model.NamedCommandEntry;
import net.exylia.commons.v2.ui.selector.core.AbstractSelector;
import net.exylia.commons.v2.ui.selector.impl.namedcommand.menu.NamedCommandListMenu;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class NamedCommandEditorSelector extends AbstractSelector<List<NamedCommandEntry>> {

    private List<NamedCommandEntry> initialCommands = new ArrayList<>();

    private NamedCommandEditorSelector(Player player) {
        super(player);
        this.title = "Commands";
    }

    public static NamedCommandEditorSelector of(Player player) {
        return new NamedCommandEditorSelector(player);
    }

    public NamedCommandEditorSelector title(String title) {
        this.title = title;
        return this;
    }

    public NamedCommandEditorSelector commands(List<NamedCommandEntry> commands) {
        this.initialCommands = new ArrayList<>(commands);
        return this;
    }

    public NamedCommandEditorSelector onSave(BiConsumer<Player, List<NamedCommandEntry>> callback) {
        this.onSelect = callback;
        return this;
    }

    public NamedCommandEditorSelector onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    @Override
    public void open() {
        NamedCommandEditorSession session = new NamedCommandEditorSession(
                player,
                initialCommands,
                title,
                onSelect,
                onCancel
        );
        NamedCommandEditorRegistry.getInstance().put(session);
        NamedCommandListMenu.open(player, session);
    }
}
