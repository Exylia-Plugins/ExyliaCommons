package net.exylia.commons.v2.world.provider;

import net.exylia.commons.v2.world.provider.WorldsReflection.BackendUnavailableException;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.CompletableFuture;

/**
 * Backend for the Worlds 4.x API ({@code net.thenextlvl.worlds.*}), which targets Minecraft
 * 26.1.2/26.2 and is published as Java 25 bytecode.
 *
 * <p>4.x is a full rewrite of 3.x, not an evolution: the {@code api} package segment is gone,
 * {@code Presets} collapsed into constants on {@code Preset}, the void world is expressed as
 * {@code GeneratorType.FLAT.with(Preset.THE_VOID)} instead of a standalone preset, and creation
 * moved from {@code Level#createAsync()} to {@code Level#create()}. Deletion is no longer on a
 * {@code LevelView} but on {@code WorldsAccess#delete(World)}, and already returns a boolean.
 *
 * <p>{@code Level.Builder#legacyName(String)} only exists from 4.1.0 onward, so it is resolved
 * optionally: on 4.0.0 the level is created under its key-derived name instead of failing.
 *
 * <p>Because the API classes are Java 25 bytecode, this repository cannot compile against them
 * under its Java 21 toolchain — every member is therefore bound reflectively, which also keeps
 * the class loadable on servers where Worlds is absent.
 *
 * @since 1.0.0
 */
final class Worlds4Backend implements WorldsBackend {

    private static final String ACCESS = "net.thenextlvl.worlds.WorldsAccess";
    private static final String LEVEL = "net.thenextlvl.worlds.Level";
    private static final String BUILDER = "net.thenextlvl.worlds.Level$Builder";
    private static final String PRESET = "net.thenextlvl.worlds.preset.Preset";
    private static final String GENERATOR_TYPE = "net.thenextlvl.worlds.generator.GeneratorType";
    private static final String FLAT = "net.thenextlvl.worlds.generator.GeneratorType$Flat";

    private final Object access;
    private final Object voidGenerator;

    private final MethodHandle levelBuilder;
    private final MethodHandle builderStructures;
    private final MethodHandle builderGeneratorType;
    /** Added in 4.1.0; {@code null} on 4.0.0. */
    private final MethodHandle builderLegacyName;
    private final MethodHandle builderBuild;
    private final MethodHandle levelCreate;
    private final MethodHandle accessDelete;

    /**
     * Binds against the installed Worlds 4.x API.
     *
     * @throws BackendUnavailableException if Worlds is absent, is a different generation, or is a
     *                                     4.x release whose signatures no longer match
     */
    Worlds4Backend(Plugin plugin) {
        Class<?> accessClass = WorldsReflection.require(plugin, ACCESS);
        Class<?> levelClass = WorldsReflection.require(plugin, LEVEL);
        Class<?> builderClass = WorldsReflection.require(plugin, BUILDER);
        Class<?> presetClass = WorldsReflection.require(plugin, PRESET);
        Class<?> generatorTypeClass = WorldsReflection.require(plugin, GENERATOR_TYPE);
        Class<?> flatClass = WorldsReflection.require(plugin, FLAT);

        // WorldsAccess extends Plugin and is bound through a StaticBinder whose lookup can fail
        // on a partially initialised server. The plugin instance is the access implementation,
        // so prefer it directly and only fall back to the static accessor.
        if (accessClass.isInstance(plugin)) {
            this.access = plugin;
        } else {
            try {
                this.access = WorldsReflection.method(accessClass, "access").invoke();
            } catch (Throwable t) {
                throw new BackendUnavailableException("WorldsAccess.access() failed", t);
            }
        }
        if (this.access == null) {
            throw new BackendUnavailableException("WorldsAccess is unavailable");
        }

        // GeneratorType.FLAT is typed as Flat and carries CLASSIC_FLAT by default; with(preset)
        // returns a new Flat bound to the void preset. Resolving this eagerly means a server
        // running a 4.x build that renamed either constant is rejected before any world is built.
        Object flat = WorldsReflection.staticField(generatorTypeClass, "FLAT");
        Object theVoid = WorldsReflection.staticField(presetClass, "THE_VOID");
        try {
            this.voidGenerator = WorldsReflection
                    .method(flatClass, "with", presetClass)
                    .invoke(flat, theVoid);
        } catch (Throwable t) {
            throw new BackendUnavailableException("GeneratorType.Flat#with(Preset) failed", t);
        }

        this.levelBuilder = WorldsReflection.method(levelClass, "builder", Key.class);
        this.builderStructures = WorldsReflection.method(builderClass, "structures", Boolean.class);
        this.builderGeneratorType =
                WorldsReflection.method(builderClass, "generatorType", generatorTypeClass);
        this.builderLegacyName =
                WorldsReflection.optionalMethod(builderClass, "legacyName", String.class);
        this.builderBuild = WorldsReflection.method(builderClass, "build");
        this.levelCreate = WorldsReflection.method(levelClass, "create");
        this.accessDelete = WorldsReflection.method(accessClass, "delete", World.class);
    }

    @Override
    public String name() {
        return builderLegacyName != null ? "Worlds 4.x" : "Worlds 4.0.x";
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<World> createWorld(Key key, String legacyName, boolean voidWorld) {
        try {
            Object builder = levelBuilder.invoke(key);
            builder = builderStructures.invoke(builder, Boolean.FALSE);
            if (builderLegacyName != null) {
                builder = builderLegacyName.invoke(builder, legacyName);
            }
            if (voidWorld) {
                builder = builderGeneratorType.invoke(builder, voidGenerator);
            }
            Object level = builderBuild.invoke(builder);
            return (CompletableFuture<World>) levelCreate.invoke(level);
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> deleteWorld(World world) {
        try {
            return (CompletableFuture<Boolean>) accessDelete.invoke(access, world);
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }
}
