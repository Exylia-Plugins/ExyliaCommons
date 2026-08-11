package net.exylia.commons.v2.ui.selector.impl.loot;

import net.exylia.commons.v2.loot.model.LootEntry;
import net.exylia.commons.v2.ui.selector.core.AbstractSelector;
import net.exylia.commons.v2.ui.selector.core.SelectorButton;
import net.exylia.commons.v2.ui.selector.impl.loot.menu.LootListMenu;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class LootEditorSelector extends AbstractSelector<List<LootEntry>> {

    private List<LootEntry> initialEntries = new ArrayList<>();
    private final List<SelectorButton<LootEditorSession>> customButtons = new ArrayList<>();
    private boolean allowCommands = false;

    private LootEditorSelector(Player player) {
        super(player);
        this.title = "Loot Table";
    }

    public static LootEditorSelector of(Player player) {
        return new LootEditorSelector(player);
    }

    public LootEditorSelector title(String title) {
        this.title = title;
        return this;
    }

    public LootEditorSelector entries(List<LootEntry> entries) {
        this.initialEntries = new ArrayList<>(entries);
        return this;
    }

    /** Opt in to letting entries be commands as well as items — shows a type-select step when
     * adding a new entry. Off by default so existing callers (chests, spawners) are unaffected. */
    public LootEditorSelector allowCommands(boolean allowCommands) {
        this.allowCommands = allowCommands;
        return this;
    }

    /** Adds a plugin-defined button to the loot list menu (presets, imports, bulk edits...). */
    public LootEditorSelector button(SelectorButton<LootEditorSession> button) {
        this.customButtons.add(button);
        return this;
    }

    public LootEditorSelector onSave(BiConsumer<Player, List<LootEntry>> callback) {
        this.onSelect = callback;
        return this;
    }

    public LootEditorSelector onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    @Override
    public void open() {
        LootEditorSession session = new LootEditorSession(
                player,
                initialEntries,
                title,
                onSelect,
                onCancel
        );
        session.setAllowCommands(allowCommands);
        customButtons.forEach(session::addCustomButton);
        LootEditorRegistry.getInstance().put(session);
        LootListMenu.open(player, session);
    }
}
