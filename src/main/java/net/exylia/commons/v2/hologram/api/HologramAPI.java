package net.exylia.commons.v2.hologram.api;

import net.exylia.commons.v2.hologram.core.HologramManager;
import net.exylia.commons.v2.hologram.model.Hologram;
import net.exylia.commons.v2.hologram.persistence.HologramConfigLoader;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class HologramAPI {
    private HologramAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        HologramManager.initialize(plugin);
    }

    public static boolean isInitialized() {
        return HologramManager.isInitialized();
    }

    public static HologramBuilder create(String id, Location location) {
        return new HologramBuilder(id, location);
    }

    public static CompletableFuture<Hologram> createAsync(String id, Location location, String... lines) {
        return new HologramBuilder(id, location)
                .lines(lines)
                .buildAsync();
    }

    public static Optional<Hologram> get(String id) {
        return HologramManager.getInstance().getHologram(id);
    }

    public static CompletableFuture<Boolean> remove(String id) {
        return HologramManager.getInstance().removeHologramAsync(id);
    }

    public static void removeAll() {
        HologramManager.getInstance().removeAllHolograms();
    }

    public static Collection<Hologram> getAll() {
        return HologramManager.getInstance().getAllHolograms();
    }

    public static List<Hologram> getNearby(Location location, double radius) {
        return HologramManager.getInstance().getHologramsNearby(location, radius);
    }

    public static void reload() {
        HologramManager.getInstance().reload();
    }

    public static void shutdown() {
        HologramManager.getInstance().shutdown();
    }

    public static HologramManager getManager() {
        return HologramManager.getInstance();
    }

    public static Hologram loadFromConfig(String id, ConfigurationSection section) {
        return HologramConfigLoader.fromConfig(id, section);
    }

    public static CompletableFuture<Hologram> loadFromConfigAsync(String id, ConfigurationSection section) {
        return HologramConfigLoader.fromConfigAsync(id, section);
    }

    public static void saveToConfig(Hologram hologram, ConfigurationSection section) {
        HologramConfigLoader.saveToConfig(hologram, section);
    }
}
