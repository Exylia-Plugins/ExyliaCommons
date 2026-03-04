package net.exylia.commons.v2.snapshot.store;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.database.annotation.Column;
import net.exylia.commons.v2.database.annotation.Table;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.snapshot.model.SnapshotData;
import org.bukkit.Location;

import java.util.UUID;

@Getter
@Setter
@Table(name = "snapshot_player_states", version = "1.0")
public class PlayerStateRecord extends Entity {

    @Column(primaryKey = true, length = 36)
    private String uuid;

    @Column(autoSerialize = true, length = 65535)
    private SnapshotData snapshot;

    @Column(length = 64)
    private String contextId;

    @Column(autoSerialize = true)
    private Location lastLocation;

    public PlayerStateRecord() {}

    public PlayerStateRecord(UUID uuid, SnapshotData snapshot, String contextId, Location location) {
        this.uuid = uuid.toString();
        this.snapshot = snapshot;
        this.contextId = contextId;
        this.lastLocation = location;
    }

    @Override
    public Object getId() {
        return uuid;
    }
}
