package net.exylia.commons.databaseV2.entity;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.databaseV2.annotation.Table;

@Getter
@Setter
public abstract class Entity {

    @lombok.experimental.Accessors(fluent = true)
    protected long createdAt = System.currentTimeMillis();

    @lombok.experimental.Accessors(fluent = true)
    protected long updatedAt = System.currentTimeMillis();

    public Entity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }

    public abstract Object getId();
}
