package net.exylia.commons.v2.world.api;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.world.provider.WorldsBackend;
import net.exylia.commons.v2.world.provider.WorldsBackendDetector;
import net.kyori.adventure.key.Key;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

/**
 * Facade over the "Worlds" plugin (net.thenextlvl.worlds), used on Folia servers where
 * {@link org.bukkit.WorldCreator} cannot be driven safely from an arbitrary thread.
 *
 * <p>Worlds ships one API generation per Minecraft line, and the generations are not source- or
 * binary-compatible with each other:
 *
 * <table border="1">
 *   <caption>Supported generations</caption>
 *   <tr><th>Worlds</th><th>Minecraft</th><th>API root</th><th>Notes</th></tr>
 *   <tr><td>3.12.x</td><td>1.21.4 – 1.21.11</td><td>{@code net.thenextlvl.worlds.api}</td>
 *       <td>Java 21 bytecode; {@code WorldsProvider} service</td></tr>
 *   <tr><td>4.0.0</td><td>26.1.2</td><td>{@code net.thenextlvl.worlds}</td>
 *       <td>Java 25 bytecode; no {@code legacyName}</td></tr>
 *   <tr><td>4.1.0 – 4.4.0</td><td>26.1.2 / 26.2</td><td>{@code net.thenextlvl.worlds}</td>
 *       <td>Java 25 bytecode; full support</td></tr>
 * </table>
 *
 * <p>Because 4.x is published as Java 25 bytecode while this library targets Java 21, it cannot be
 * placed on the compile classpath at all. Both generations are therefore bound reflectively by
 * {@link WorldsBackendDetector}, which probes each candidate <em>once</em> at first use and keeps
 * the one whose members all resolve. A Worlds release that renames a method is rejected during
 * that probe rather than blowing up mid-creation, and callers simply see "no backend".
 *
 * <p>Every method degrades safely: with no compatible Worlds installed, creation completes with
 * {@code null} and deletion with {@code false}, so callers can fall back to a vanilla Bukkit path.
 * Use {@link #isAvailable()} to choose that path up front.
 *
 * @since 1.0.0
 */
public final class WorldsPluginBridge {

    private WorldsPluginBridge() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns whether a compatible Worlds version is installed and successfully bound.
     *
     * <p>Prefer this over inspecting a {@code null} result: it distinguishes "Worlds is not
     * usable here" from "Worlds tried and failed to create this particular world".
     *
     * @return {@code true} if world operations will be delegated to the Worlds plugin
     */
    public static boolean isAvailable() {
        return WorldsBackendDetector.backend() != null;
    }

    /**
     * Returns the bound API generation for diagnostics, such as {@code "Worlds 4.x"}.
     *
     * @return the backend name, or {@code "none"} when unavailable
     */
    public static String backendName() {
        WorldsBackend backend = WorldsBackendDetector.backend();
        return backend != null ? backend.name() : "none";
    }

    /**
     * Creates a world named {@code legacyName} through the Worlds plugin, using the void preset
     * (no terrain, no structures). Equivalent to {@code createWorld(key, legacyName, true)}.
     *
     * @param key        the namespaced key identifying the level, e.g. {@code myplugin:autoworld}
     * @param legacyName the legacy world folder/name to expose the level under
     * @return a future completing with the created world, or {@code null} if no compatible Worlds
     *         version is installed or creation failed
     */
    public static CompletableFuture<World> createWorld(Key key, String legacyName) {
        return createWorld(key, legacyName, true);
    }

    /**
     * Creates a world named {@code legacyName} through the Worlds plugin.
     *
     * @param key        the namespaced key identifying the level, e.g. {@code myplugin:autoworld}
     * @param legacyName the legacy world folder/name to expose the level under
     * @param voidPreset whether to force the void preset (no terrain, no structures). Pass
     *                   {@code false} to apply your own chunk generator/biome provider after
     *                   creation instead.
     * @return a future completing with the created world, or {@code null} if no compatible Worlds
     *         version is installed or creation failed
     */
    public static CompletableFuture<World> createWorld(Key key, String legacyName, boolean voidPreset) {
        WorldsBackend backend = WorldsBackendDetector.backend();
        if (backend == null) {
            return CompletableFuture.completedFuture(null);
        }
        return backend.createWorld(key, legacyName, voidPreset)
                .exceptionally(t -> {
                    DebugAPI.logLibWarn("[Worlds] " + backend.name() + " failed to create '"
                            + legacyName + "': " + rootCause(t));
                    return null;
                });
    }

    /**
     * Deletes the given world through the Worlds plugin.
     *
     * @param world the world to delete
     * @return a future completing with {@code true} on success, {@code false} if no compatible
     *         Worlds version is installed or the deletion failed
     */
    public static CompletableFuture<Boolean> deleteWorld(World world) {
        WorldsBackend backend = WorldsBackendDetector.backend();
        if (backend == null) {
            return CompletableFuture.completedFuture(false);
        }
        return backend.deleteWorld(world)
                .exceptionally(t -> {
                    DebugAPI.logLibWarn("[Worlds] " + backend.name() + " failed to delete '"
                            + world.getName() + "': " + rootCause(t));
                    return false;
                });
    }

    /**
     * Unwraps the {@link CompletableFuture} wrapper so the log line names the real failure
     * rather than a generic {@code CompletionException}.
     */
    private static String rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        return cause.toString();
    }
}
