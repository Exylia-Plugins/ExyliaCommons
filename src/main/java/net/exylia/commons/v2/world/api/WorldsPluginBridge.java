package net.exylia.commons.v2.world.api;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Reflection bridge against the "Worlds" plugin (net.thenextlvl.worlds), used on Folia
 * servers where {@link org.bukkit.WorldCreator} cannot be used from the main thread safely.
 * <p>
 * Supports both Worlds 4.x (Folia 1.21.6+ / "Level" API) and Worlds 3.x (Folia 1.21 /
 * "WorldsProvider" API), falling back gracefully when neither is present so callers can
 * degrade to a vanilla Bukkit world creation path.
 * <p>
 * This class is intentionally dependency-free of any specific plugin's world model — callers
 * pass the {@link Key} and legacy world name/folder they want, always requesting a
 * structure-less void-preset level, and receive back a plain {@link World}/{@code Boolean}.
 */
public final class WorldsPluginBridge {

    private WorldsPluginBridge() {}

    /**
     * Attempts to create a world named {@code legacyName} through the Worlds plugin, using
     * the void preset (no terrain, no structures). Equivalent to
     * {@code createWorld(key, legacyName, true)}.
     *
     * @param key        the namespaced key identifying the level (e.g. {@code myplugin:autoworld})
     * @param legacyName the legacy world folder/name to expose the level under
     * @return a future completing with the created {@link World}, or {@code null} if the
     *         Worlds plugin is not installed or creation failed
     */
    public static CompletableFuture<World> createWorld(Key key, String legacyName) {
        return createWorld(key, legacyName, true);
    }

    /**
     * Attempts to create a world named {@code legacyName} through the Worlds plugin.
     *
     * @param key        the namespaced key identifying the level (e.g. {@code myplugin:autoworld})
     * @param legacyName the legacy world folder/name to expose the level under
     * @param voidPreset whether to force the void preset/flat generator (no terrain, no
     *                   structures). Pass {@code false} to let the caller apply its own
     *                   chunk generator/biome provider after creation instead.
     * @return a future completing with the created {@link World}, or {@code null} if the
     *         Worlds plugin is not installed or creation failed
     */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<World> createWorld(Key key, String legacyName, boolean voidPreset) {
        // Try Worlds 4.x (Folia 26+)
        try {
            Class<?> levelClass = Class.forName("net.thenextlvl.worlds.Level");
            Class<?> builderClass = Class.forName("net.thenextlvl.worlds.Level$Builder");

            Object builder = levelClass.getMethod("builder", Key.class).invoke(null, key);
            builder = builderClass.getMethod("legacyName", String.class).invoke(builder, legacyName);
            builder = builderClass.getMethod("structures", Boolean.class).invoke(builder, Boolean.FALSE);

            if (voidPreset) {
                Class<?> generatorTypeClass = Class.forName("net.thenextlvl.worlds.generator.GeneratorType");
                Class<?> flatClass = Class.forName("net.thenextlvl.worlds.generator.GeneratorType$Flat");
                Class<?> presetClass = Class.forName("net.thenextlvl.worlds.preset.Preset");

                Object flatType = generatorTypeClass.getField("FLAT").get(null);
                Object thePreset = presetClass.getField("THE_VOID").get(null);
                Object flatWithVoid = flatClass.getMethod("with", presetClass).invoke(flatType, thePreset);
                builder = builderClass.getMethod("generatorType", generatorTypeClass).invoke(builder, flatWithVoid);
            }

            Object level = builderClass.getMethod("build").invoke(builder);
            return (CompletableFuture<World>) levelClass.getMethod("create").invoke(level);
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            DebugAPI.logLibWarn("[WorldsPluginBridge] Worlds 4.x API error: " + e.getMessage());
        }

        // Try Worlds 3.x (Folia 1.21)
        try {
            Class<?> providerClass = Class.forName("net.thenextlvl.worlds.api.WorldsProvider");
            Object provider = Bukkit.getServicesManager().load((Class<Object>) providerClass);
            if (provider == null) return CompletableFuture.completedFuture(null);

            Object thePreset = null;
            if (voidPreset) {
                Class<?> presetsClass = Class.forName("net.thenextlvl.worlds.api.preset.Presets");
                thePreset = presetsClass.getField("THE_VOID").get(null);
            }

            Object builder = providerClass.getMethod("levelBuilder", Path.class)
                    .invoke(provider, Path.of(legacyName));
            for (java.lang.reflect.Method m : builder.getClass().getMethods()) {
                if (m.getName().equals("key") && m.getParameterCount() == 1) builder = m.invoke(builder, key);
                else if (m.getName().equals("name") && m.getParameterCount() == 1) builder = m.invoke(builder, legacyName);
                else if (m.getName().equals("structures") && m.getParameterCount() == 1) builder = m.invoke(builder, Boolean.FALSE);
                else if (voidPreset && m.getName().equals("preset") && m.getParameterCount() == 1) builder = m.invoke(builder, thePreset);
            }
            Object level = builder.getClass().getMethod("build").invoke(builder);
            return (CompletableFuture<World>) level.getClass().getMethod("createAsync").invoke(level);
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            DebugAPI.logLibWarn("[WorldsPluginBridge] Worlds 3.x API error: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Attempts to delete/unload the given world through the Worlds plugin.
     *
     * @param world the world to delete
     * @return a future completing with {@code true} if the deletion succeeded, {@code false}
     *         if the Worlds plugin is not installed or deletion failed
     */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Boolean> deleteWorld(World world) {
        // Try Worlds 3.x
        try {
            Class<?> providerClass = Class.forName("net.thenextlvl.worlds.api.WorldsProvider");
            Object provider = Bukkit.getServicesManager().load((Class<Object>) providerClass);
            if (provider != null) {
                Object levelView = providerClass.getMethod("levelView").invoke(provider);
                CompletableFuture<Object> future = (CompletableFuture<Object>) levelView.getClass()
                        .getMethod("deleteAsync", World.class, boolean.class).invoke(levelView, world, true);
                return future.thenApply(result -> {
                    try {
                        return (Boolean) result.getClass().getMethod("isSuccess").invoke(result);
                    } catch (Exception e) {
                        return false;
                    }
                });
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            DebugAPI.logLibWarn("[WorldsPluginBridge] Worlds 3.x delete error: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(false);
    }
}
