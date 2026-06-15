package net.exylia.commons.v2.ui.selector.core;

import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

public abstract class AbstractSelector<R> {

    protected final Player player;
    protected String title;
    protected BiConsumer<Player, R> onSelect;
    protected Runnable onCancel;

    protected AbstractSelector(Player player) {
        this.player = player;
    }

    public abstract void open();
}
