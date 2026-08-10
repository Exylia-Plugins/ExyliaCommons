package net.exylia.commons.v2.world.provider;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.world.provider.WorldsReflection.BackendUnavailableException;
import org.bukkit.plugin.Plugin;

import java.util.function.Function;

/**
 * Resolves the {@link WorldsBackend} matching the installed Worlds plugin, once per server run.
 *
 * <p>Detection is by <em>capability</em>, not by version string: each backend is constructed in
 * turn and the first one that binds all of its members wins. A Worlds release that renames or
 * removes a method therefore fails during construction and is skipped, instead of producing a
 * {@code NoSuchMethodException} at the moment a world is being created.
 *
 * <p>The result — including "no backend" — is memoized, so servers without Worlds pay the
 * classloading cost once rather than on every call.
 *
 * @since 1.0.0
 */
public final class WorldsBackendDetector {

    /** Newest generation first: a server can only have one Worlds jar installed. */
    private static final Function<Plugin, WorldsBackend>[] FACTORIES = factories();

    private static volatile boolean resolved;
    private static volatile WorldsBackend backend;

    private WorldsBackendDetector() {
        throw new UnsupportedOperationException("Utility class");
    }

    @SuppressWarnings("unchecked")
    private static Function<Plugin, WorldsBackend>[] factories() {
        return new Function[]{
                (Function<Plugin, WorldsBackend>) Worlds4Backend::new,
                (Function<Plugin, WorldsBackend>) Worlds3Backend::new
        };
    }

    /**
     * Returns the resolved backend, or {@code null} when no compatible Worlds version is present.
     *
     * <p>Safe to call from any thread and at any point in the lifecycle. Resolution is deferred
     * to the first call so that it happens after the Worlds plugin has enabled.
     *
     * @return the backend, or {@code null}
     */
    public static WorldsBackend backend() {
        if (resolved) {
            return backend;
        }
        synchronized (WorldsBackendDetector.class) {
            if (resolved) {
                return backend;
            }
            backend = detect();
            resolved = true;
            return backend;
        }
    }

    private static WorldsBackend detect() {
        Plugin plugin;
        try {
            plugin = WorldsReflection.worldsPlugin();
        } catch (Throwable t) {
            DebugAPI.logLibWarn("[Worlds] Plugin lookup failed: " + t);
            return null;
        }
        if (plugin == null) {
            return null;
        }
        for (Function<Plugin, WorldsBackend> factory : FACTORIES) {
            try {
                WorldsBackend candidate = factory.apply(plugin);
                DebugAPI.logLibInfo("[Worlds] Bound to " + candidate.name()
                        + " (plugin version " + version(plugin) + ")");
                return candidate;
            } catch (BackendUnavailableException ignored) {
                // Wrong generation, or an incompatible release of it: try the next candidate.
            } catch (Throwable t) {
                // Deliberately broad. Probing a foreign plugin's classes can surface anything a
                // classloader chooses to throw — including NoClassDefFoundError from an optional
                // transitive dependency. Detection is best-effort and must never abort the
                // enable of the plugin that merely asked whether Worlds was usable.
                DebugAPI.logLibWarn("[Worlds] Backend probe failed: " + t);
            }
        }
        DebugAPI.logLibWarn("[Worlds] Plugin version " + version(plugin)
                + " is installed but exposes no supported API"
                + " (supported: 3.12.x for MC 1.21.x, 4.x for MC 26.x)."
                + " World operations will fall back to vanilla Bukkit.");
        return null;
    }

    /** Reads the plugin version defensively; diagnostics must not themselves fail detection. */
    private static String version(Plugin plugin) {
        try {
            return plugin.getPluginMeta().getVersion();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    /**
     * Discards the memoized result so the next call re-probes.
     *
     * <p>Intended for reload flows and tests; the Worlds plugin itself is not designed to be
     * swapped at runtime.
     */
    public static void reset() {
        synchronized (WorldsBackendDetector.class) {
            backend = null;
            resolved = false;
        }
    }
}
