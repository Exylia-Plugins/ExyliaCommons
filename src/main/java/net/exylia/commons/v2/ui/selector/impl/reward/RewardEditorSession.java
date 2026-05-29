package net.exylia.commons.v2.ui.selector.impl.reward;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.reward.model.RewardEntry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Getter
public class RewardEditorSession {

    private final Player player;
    private final List<RewardEntry> rewards;
    private final BiConsumer<Player, List<RewardEntry>> onSave;
    private final Runnable onCancel;

    @Setter
    private String title;

    public RewardEditorSession(
            Player player,
            List<RewardEntry> initialRewards,
            String title,
            BiConsumer<Player, List<RewardEntry>> onSave,
            Runnable onCancel
    ) {
        this.player = player;
        this.rewards = new ArrayList<>(initialRewards);
        this.title = title;
        this.onSave = onSave;
        this.onCancel = onCancel;
    }

    public void addReward(RewardEntry entry) {
        rewards.add(entry);
    }

    public void removeReward(String id) {
        rewards.removeIf(r -> r.getId().equals(id));
    }

    public RewardEntry findById(String id) {
        return rewards.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
    }

    public void replaceReward(RewardEntry updated) {
        int idx = -1;
        for (int i = 0; i < rewards.size(); i++) {
            if (rewards.get(i).getId().equals(updated.getId())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            rewards.set(idx, updated);
        } else {
            rewards.add(updated);
        }
    }
}
