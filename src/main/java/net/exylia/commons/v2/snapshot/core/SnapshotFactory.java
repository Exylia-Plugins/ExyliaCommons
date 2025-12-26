package net.exylia.commons.v2.snapshot.core;

import net.exylia.commons.v2.snapshot.model.SnapshotData;
import org.bukkit.entity.Player;

public class SnapshotFactory {

    public SnapshotData create(Player player) {
        return SnapshotData.fromPlayer(player);
    }
}
