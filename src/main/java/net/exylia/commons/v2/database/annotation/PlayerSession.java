package net.exylia.commons.v2.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a database entity as player-session critical.
 * <p>
 * Entities annotated with {@code @PlayerSession} represent per-player data that
 * must be immediately consistent across servers in a multi-server network.
 * <p>
 * When Redis is enabled, the system guarantees that any {@code save()} call
 * writes to Redis immediately (write-through), so that when a player switches
 * servers, the destination server always reads the latest state.
 * <p>
 * Additionally, a {@code PlayerQuitEvent} listener is automatically registered
 * to flush pending write-behind data to the database before the player session ends.
 * <p>
 * Example:
 * <pre>
 * {@code
 * @PlayerSession
 * @Table(name = "player_data", version = "1.0")
 * public class PlayerDataEntity extends Entity { ... }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlayerSession {

    /**
     * The field name that holds the player UUID when it is NOT the primary key.
     * <p>
     * Leave empty (default) when the primary key IS the player UUID (1:1 relationship).
     * Set this when the entity has a separate UUID field and multiple entries per player (1:N).
     * <p>
     * Example (1:1, PK = UUID):
     * <pre>{@code @PlayerSession }</pre>
     *
     * Example (1:N, separate UUID field):
     * <pre>{@code @PlayerSession(playerField = "uuid") }</pre>
     */
    String playerField() default "";
}
