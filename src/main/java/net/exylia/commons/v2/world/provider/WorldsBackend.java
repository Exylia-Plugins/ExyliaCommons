package net.exylia.commons.v2.world.provider;

import net.kyori.adventure.key.Key;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

/**
 * Version-specific adapter over the "Worlds" plugin (net.thenextlvl.worlds).
 *
 * <p>Each implementation binds reflectively against exactly one API generation and resolves
 * every member it needs <em>once</em>, at construction time. A backend that cannot resolve its
 * mandatory members refuses to construct, so a resolved backend is always fully usable — the
 * caller never discovers a missing method halfway through building a world.
 *
 * <p>Implementations are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public interface WorldsBackend {

    /**
     * Returns a short human-readable identifier of the bound API generation, such as
     * {@code "Worlds 4.x"}. Used for diagnostics only.
     *
     * @return the backend name
     */
    String name();

    /**
     * Creates a level through the Worlds plugin.
     *
     * @param key        the namespaced key identifying the level
     * @param legacyName the legacy world folder/name to expose the level under
     * @param voidPreset whether to force a structure-less void/flat preset. When {@code false}
     *                   the caller is expected to apply its own chunk generator/biome provider
     *                   after creation.
     * @return a future completing with the created world, never {@code null} itself; the future
     *         may complete with {@code null} if the plugin declined the creation
     */
    CompletableFuture<World> createWorld(Key key, String legacyName, boolean voidPreset);

    /**
     * Deletes the given world through the Worlds plugin.
     *
     * @param world the world to delete
     * @return a future completing with {@code true} on success
     */
    CompletableFuture<Boolean> deleteWorld(World world);
}
