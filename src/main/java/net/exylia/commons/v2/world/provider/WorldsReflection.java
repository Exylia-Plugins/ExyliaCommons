package net.exylia.commons.v2.world.provider;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection primitives shared by the Worlds backends.
 *
 * <p>Every lookup here is performed against the <em>Worlds plugin's own classloader</em> rather
 * than the loader of ExyliaCommons. On Paper, plugins are isolated: a plugin that does not declare
 * Worlds as a dependency cannot see {@code net.thenextlvl.worlds.*} through a plain
 * {@link Class#forName(String)}, which would make detection fail even though the plugin is
 * installed. Resolving through the plugin instance removes that dependency on load order and on
 * {@code paper-plugin.yml} declarations.
 *
 * <p>All failures surface as {@link BackendUnavailableException} so a backend can abort its own
 * construction cleanly and let the next generation be tried.
 *
 * @since 1.0.0
 */
final class WorldsReflection {

    /** The Bukkit plugin name under which Worlds registers, identical across 3.x and 4.x. */
    static final String PLUGIN_NAME = "Worlds";

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private WorldsReflection() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Signals that a backend cannot bind against the installed Worlds version.
     *
     * <p>Unchecked so backend constructors can be written as straight-line code, and caught
     * exclusively by the detector.
     */
    static final class BackendUnavailableException extends RuntimeException {

        BackendUnavailableException(String message) {
            super(message, null, false, false);
        }

        BackendUnavailableException(String message, Throwable cause) {
            super(message, cause, false, false);
        }
    }

    /**
     * Returns the installed Worlds plugin, or {@code null} when it is absent or disabled.
     *
     * <p>Not cached: a plugin can be enabled or disabled at runtime, and this is only consulted
     * during one-shot backend detection.
     */
    static Plugin worldsPlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        return plugin != null && plugin.isEnabled() ? plugin : null;
    }

    /**
     * Loads a Worlds API class through the Worlds plugin's classloader.
     *
     * @throws BackendUnavailableException if the class is absent, i.e. the installed Worlds
     *                                     version belongs to a different API generation
     */
    static Class<?> require(Plugin plugin, String className) {
        try {
            return Class.forName(className, false, plugin.getClass().getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            throw new BackendUnavailableException("missing class " + className, e);
        }
    }

    /**
     * Resolves a virtual/interface method and unreflects it into a {@link MethodHandle}.
     *
     * <p>Method handles are resolved once and invoked many times; unlike {@link Method#invoke},
     * an {@code invokeExact}-shaped handle avoids per-call access checks and argument boxing
     * arrays, which matters because world creation runs on gameplay paths.
     *
     * @throws BackendUnavailableException if the method does not exist with that exact signature,
     *                                     which is precisely how an incompatible patch release of
     *                                     Worlds is detected
     */
    static MethodHandle method(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            Method method = owner.getMethod(name, parameterTypes);
            method.setAccessible(true);
            return LOOKUP.unreflect(method);
        } catch (NoSuchMethodException | IllegalAccessException | RuntimeException e) {
            throw new BackendUnavailableException(
                    "missing method " + owner.getName() + '#' + name, e);
        }
    }

    /**
     * Resolves an optional virtual/interface method.
     *
     * <p>Used for members that exist only in some releases of a generation — for example
     * {@code Level.Builder#legacyName}, added in Worlds 4.1.0. Returns {@code null} when absent
     * so the caller can degrade instead of rejecting the whole backend.
     */
    static MethodHandle optionalMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return method(owner, name, parameterTypes);
        } catch (BackendUnavailableException e) {
            return null;
        }
    }

    /**
     * Reads a public static field, typically an API constant such as a preset or generator type.
     *
     * @throws BackendUnavailableException if the constant is absent or unreadable
     */
    static Object staticField(Class<?> owner, String name) {
        try {
            Field field = owner.getField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException | RuntimeException e) {
            throw new BackendUnavailableException(
                    "missing constant " + owner.getName() + '.' + name, e);
        }
    }
}
