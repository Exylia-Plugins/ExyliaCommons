package net.exylia.commons.v2.world.provider;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Reflection primitives shared by the Worlds backends.
 *
 * <p>Two constraints shape this class, both learned from real failures:
 *
 * <p><b>1. Lookups go through the Worlds plugin's own classloader.</b> On Paper, plugins are
 * isolated: a plugin that does not declare Worlds as a dependency cannot see
 * {@code net.thenextlvl.worlds.*} via a plain {@link Class#forName(String)}, so detection would
 * fail even with Worlds installed. Resolving through the plugin instance removes the dependency
 * on load order and on {@code paper-plugin.yml} declarations.
 *
 * <p><b>2. Members are resolved with {@link MethodHandles.Lookup}, never with
 * {@link Class#getMethod}.</b> {@code getMethod} and {@code getDeclaredMethods} force the JVM to
 * resolve the descriptor of <em>every</em> method on the class. {@code WorldsProvider} declares
 * {@code default GroupProvider groupProvider()} returning a type from the separate, optional
 * <b>PerWorlds</b> plugin. On a server without PerWorlds, merely asking for an unrelated method
 * throws {@link NoClassDefFoundError} for {@code net/thenextlvl/perworlds/GroupProvider}. By
 * contrast {@code findVirtual}/{@code findStatic} resolve only the single descriptor requested,
 * so an absent optional dependency on a method we never call stays harmless.
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
     * <p>Initialization is deliberately requested ({@code initialize = true}) so that a class
     * whose static initializer cannot run — for instance a preset holder needing a transitive
     * library that is absent — fails here, during detection, instead of at first use.
     *
     * @throws BackendUnavailableException if the class is absent or cannot be initialized, i.e.
     *                                     the installed Worlds version belongs to a different
     *                                     API generation
     */
    static Class<?> require(Plugin plugin, String className) {
        try {
            return Class.forName(className, true, plugin.getClass().getClassLoader());
        } catch (ClassNotFoundException | LinkageError | RuntimeException e) {
            throw new BackendUnavailableException("cannot load " + className, e);
        }
    }

    /**
     * Resolves a virtual or interface method into a {@link MethodHandle}.
     *
     * <p>Only the requested descriptor is resolved, so unrelated methods referencing absent
     * optional plugins never trigger classloading.
     *
     * @param owner      the declaring class or interface
     * @param name       the method name
     * @param returnType the exact declared return type
     * @param parameters the exact declared parameter types
     * @throws BackendUnavailableException if no method with that exact signature exists, which is
     *                                     precisely how an incompatible release is detected
     */
    static MethodHandle virtual(Class<?> owner, String name, Class<?> returnType, Class<?>... parameters) {
        try {
            return LOOKUP.findVirtual(owner, name, MethodType.methodType(returnType, parameters));
        } catch (NoSuchMethodException | IllegalAccessException | LinkageError | RuntimeException e) {
            throw new BackendUnavailableException(
                    "missing method " + owner.getName() + '#' + name, e);
        }
    }

    /**
     * Resolves an optional virtual or interface method.
     *
     * <p>Used for members that exist only in some releases of a generation — for example
     * {@code Level.Builder#legacyName}, added in Worlds 4.1.0. Returns {@code null} when absent
     * so the caller can degrade instead of rejecting the whole backend.
     */
    static MethodHandle optionalVirtual(Class<?> owner, String name, Class<?> returnType, Class<?>... parameters) {
        try {
            return virtual(owner, name, returnType, parameters);
        } catch (BackendUnavailableException e) {
            return null;
        }
    }

    /**
     * Resolves a static method into a {@link MethodHandle}.
     *
     * @throws BackendUnavailableException if no static method with that exact signature exists
     */
    static MethodHandle staticMethod(Class<?> owner, String name, Class<?> returnType, Class<?>... parameters) {
        try {
            return LOOKUP.findStatic(owner, name, MethodType.methodType(returnType, parameters));
        } catch (NoSuchMethodException | IllegalAccessException | LinkageError | RuntimeException e) {
            throw new BackendUnavailableException(
                    "missing static method " + owner.getName() + '#' + name, e);
        }
    }

    /**
     * Reads a public static field, typically an API constant such as a preset or generator type.
     *
     * @param owner the declaring class
     * @param name  the constant name
     * @param type  the exact declared field type
     * @throws BackendUnavailableException if the constant is absent or unreadable
     */
    static Object staticField(Class<?> owner, String name, Class<?> type) {
        try {
            return LOOKUP.findStaticGetter(owner, name, type).invoke();
        } catch (Throwable t) {
            throw new BackendUnavailableException(
                    "missing constant " + owner.getName() + '.' + name, t);
        }
    }
}
