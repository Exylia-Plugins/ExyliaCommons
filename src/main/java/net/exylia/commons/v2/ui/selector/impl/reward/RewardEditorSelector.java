package net.exylia.commons.v2.ui.selector.impl.reward;

import net.exylia.commons.v2.reward.model.RewardEntry;
import net.exylia.commons.v2.ui.selector.core.AbstractSelector;
import net.exylia.commons.v2.ui.selector.impl.reward.menu.RewardListMenu;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class RewardEditorSelector extends AbstractSelector<List<RewardEntry>> {

    private List<RewardEntry> initialRewards = new ArrayList<>();

    private RewardEditorSelector(Player player) {
        super(player);
        this.title = "Reward Editor";
    }

    public static RewardEditorSelector of(Player player) {
        return new RewardEditorSelector(player);
    }

    public RewardEditorSelector title(String title) {
        this.title = title;
        return this;
    }

    public RewardEditorSelector rewards(List<RewardEntry> rewards) {
        this.initialRewards = new ArrayList<>(rewards);
        return this;
    }

    public RewardEditorSelector onSave(BiConsumer<Player, List<RewardEntry>> callback) {
        this.onSelect = callback;
        return this;
    }

    public RewardEditorSelector onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    @Override
    public void open() {
        RewardEditorSession session = new RewardEditorSession(
                player,
                initialRewards,
                title,
                onSelect,
                onCancel
        );
        RewardEditorRegistry.getInstance().put(session);
        RewardListMenu.open(player, session);
    }
}
