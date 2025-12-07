package net.exylia.commons.v2.database.entity;

import lombok.Getter;
import lombok.Setter;

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
