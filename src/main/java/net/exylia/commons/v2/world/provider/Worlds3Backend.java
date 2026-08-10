package net.exylia.commons.v2.world.provider;

import net.exylia.commons.v2.world.provider.WorldsReflection.BackendUnavailableException;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Backend for the Worlds 3.x API ({@code net.thenextlvl.worlds.api.*}), which targets
 * Minecraft 1.21.x — including the 1.21.11-only 3.12.x line and its {@code -mc1.21.4},
 * {@code -mc1.21.8} and {@code -mc1.21.10} variants.
 *
 * <p>Entry point is {@code WorldsProvider}, published as a Bukkit service. Levels are configured
 * through {@code WorldsProvider#levelBuilder(Path)} and created with {@code Level#createAsync()};
 * deletion goes through {@code LevelView#deleteAsync(World, boolean)}, whose {@code DeletionResult}
 * is collapsed to a boolean via its {@code isSuccess()} contract (true for {@code SUCCESS} and
 * {@code SCHEDULED}).
 *
 * <p>The void preset comes from the {@code Presets.THE_VOID} constant. Note that 3.x models the
 * void as a <em>preset</em>, whereas 4.x models it as a flat <em>generator type</em> carrying a
 * preset — the two generations are not shape-compatible, which is why they need separate backends
 * rather than a shared reflective path.
 *
 * <p>{@code WorldsProvider} also declares {@code default GroupProvider groupProvider()} returning
 * a type from the optional PerWorlds plugin. Every lookup here is therefore signature-scoped (see
 * {@link WorldsReflection}); enumerating the interface's methods would hard-fail with
 * {@link NoClassDefFoundError} on servers without PerWorlds.
 *
 * @since 1.0.0
 */
final class Worlds3Backend implements WorldsBackend {

    private static final String PROVIDER = "net.thenextlvl.worlds.api.WorldsProvider";
    private static final String PRESETS = "net.thenextlvl.worlds.api.preset.Presets";
    private static final String PRESET = "net.thenextlvl.worlds.api.preset.Preset";
    private static final String LEVEL = "net.thenextlvl.worlds.api.level.Level";
    private static final String BUILDER = "net.thenextlvl.worlds.api.level.Level$Builder";
    private static final String LEVEL_VIEW = "net.thenextlvl.worlds.api.view.LevelView";
    private static final String DELETION_RESULT = LEVEL_VIEW + "$DeletionResult";

    private final Object provider;
    private final Object voidPreset;

    private final MethodHandle levelBuilder;
    private final MethodHandle builderKey;
    private final MethodHandle builderName;
    private final MethodHandle builderStructures;
    private final MethodHandle builderPreset;
    private final MethodHandle builderBuild;
    private final MethodHandle levelCreateAsync;

    private final MethodHandle levelView;
    private final MethodHandle deleteAsync;
    private final MethodHandle deletionIsSuccess;

    /**
     * Binds against the installed Worlds 3.x API.
     *
     * @throws BackendUnavailableException if Worlds is absent, is a different generation, or is a
     *                                     3.x release whose signatures no longer match
     */
    @SuppressWarnings("unchecked")
    Worlds3Backend(Plugin plugin) {
        Class<?> providerClass = WorldsReflection.require(plugin, PROVIDER);

        // 3.x publishes the provider as a Bukkit service; the plugin instance itself also
        // implements WorldsProvider, so fall back to it when the service is not yet registered
        // (service registration happens on enable and can lose a race with an early caller).
        Object resolved = Bukkit.getServicesManager().load((Class<Object>) providerClass);
        if (resolved == null && providerClass.isInstance(plugin)) {
            resolved = plugin;
        }
        if (resolved == null) {
            throw new BackendUnavailableException("WorldsProvider service is not registered");
        }
        this.provider = resolved;

        Class<?> presetClass = WorldsReflection.require(plugin, PRESET);
        Class<?> levelClass = WorldsReflection.require(plugin, LEVEL);
        Class<?> builderClass = WorldsReflection.require(plugin, BUILDER);
        Class<?> levelViewClass = WorldsReflection.require(plugin, LEVEL_VIEW);
        Class<?> deletionResultClass = WorldsReflection.require(plugin, DELETION_RESULT);

        this.voidPreset = WorldsReflection.staticField(
                WorldsReflection.require(plugin, PRESETS), "THE_VOID", presetClass);

        this.levelBuilder = WorldsReflection.virtual(
                providerClass, "levelBuilder", builderClass, Path.class);
        this.builderKey = WorldsReflection.virtual(builderClass, "key", builderClass, Key.class);
        this.builderName = WorldsReflection.virtual(builderClass, "name", builderClass, String.class);
        this.builderStructures = WorldsReflection.virtual(
                builderClass, "structures", builderClass, Boolean.class);
        this.builderPreset = WorldsReflection.virtual(
                builderClass, "preset", builderClass, presetClass);
        this.builderBuild = WorldsReflection.virtual(builderClass, "build", levelClass);
        this.levelCreateAsync = WorldsReflection.virtual(
                levelClass, "createAsync", CompletableFuture.class);

        this.levelView = WorldsReflection.virtual(providerClass, "levelView", levelViewClass);
        this.deleteAsync = WorldsReflection.virtual(
                levelViewClass, "deleteAsync", CompletableFuture.class, World.class, boolean.class);
        this.deletionIsSuccess = WorldsReflection.virtual(
                deletionResultClass, "isSuccess", boolean.class);
    }

    @Override
    public String name() {
        return "Worlds 3.x";
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<World> createWorld(Key key, String legacyName, boolean voidWorld) {
        try {
            // 3.x resolves the level path against the server's world container, so the bare
            // folder name is the correct argument here — passing an absolute path would place
            // the level outside the container and stop it loading on restart.
            Object builder = levelBuilder.invoke(provider, Path.of(legacyName));
            builder = builderKey.invoke(builder, key);
            builder = builderName.invoke(builder, legacyName);
            builder = builderStructures.invoke(builder, Boolean.FALSE);
            if (voidWorld) {
                builder = builderPreset.invoke(builder, voidPreset);
            }
            Object level = builderBuild.invoke(builder);
            return (CompletableFuture<World>) levelCreateAsync.invoke(level);
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> deleteWorld(World world) {
        try {
            Object view = levelView.invoke(provider);
            // schedule=true lets Worlds defer the deletion to a safe point when the world is
            // still loaded, instead of failing outright.
            CompletableFuture<Object> future =
                    (CompletableFuture<Object>) deleteAsync.invoke(view, world, true);
            return future.thenApply(result -> {
                try {
                    return (Boolean) deletionIsSuccess.invoke(result);
                } catch (Throwable t) {
                    return Boolean.FALSE;
                }
            });
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }
}
