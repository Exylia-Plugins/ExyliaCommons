package net.exylia.commons.v2.hologram.persistence;

import net.exylia.commons.v2.hologram.api.HologramBuilder;
import net.exylia.commons.v2.hologram.model.Hologram;
import net.exylia.commons.v2.hologram.model.HologramConfig;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HologramConfigLoader {

    public static Hologram fromConfig(String id, ConfigurationSection section) {
        return fromConfigAsync(id, section).join();
    }

    public static CompletableFuture<Hologram> fromConfigAsync(String id, ConfigurationSection section) {
        return CompletableFuture.supplyAsync(() -> {
            Location location = loadLocation(section);
            HologramBuilder builder = new HologramBuilder(id, location);

            loadLines(section, builder);
            loadProperties(section, builder);
            loadConfig(section, builder);
            loadBehavior(section, builder);
            builder.enabled(section.getBoolean("enabled", true));

            return builder.buildAsync().join();
        });
    }

    public static Hologram fromConfig(String id, ConfigurationSection section, Location location) {
        return fromConfigAsync(id, section, location).join();
    }

    public static CompletableFuture<Hologram> fromConfigAsync(String id, ConfigurationSection section, Location location) {
        return CompletableFuture.supplyAsync(() -> {
            Location finalLocation = applyOffsetIfPresent(section, location);
            HologramBuilder builder = new HologramBuilder(id, finalLocation);

            loadLines(section, builder);
            loadProperties(section, builder);
            loadConfig(section, builder);
            loadBehavior(section, builder);
            builder.enabled(section.getBoolean("enabled", true));

            return builder.buildAsync().join();
        });
    }

    private static Location applyOffsetIfPresent(ConfigurationSection section, Location location) {
        ConfigurationSection offsetSection = section.getConfigurationSection("offset");
        if (offsetSection == null) {
            return location;
        }

        double offsetX = offsetSection.getDouble("x", 0.0);
        double offsetY = offsetSection.getDouble("y", 0.0);
        double offsetZ = offsetSection.getDouble("z", 0.0);

        if (offsetX == 0.0 && offsetY == 0.0 && offsetZ == 0.0) {
            return location;
        }

        return location.clone().add(offsetX, offsetY, offsetZ);
    }

    private static Location loadLocation(ConfigurationSection section) {
        ConfigurationSection locSection = section.getConfigurationSection("location");
        if (locSection == null) {
            throw new IllegalArgumentException("Location section is required");
        }

        String worldName = locSection.getString("world");
        if (worldName == null) {
            throw new IllegalArgumentException("World is required in location");
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalArgumentException("World not found: " + worldName);
        }

        double x = locSection.getDouble("x");
        double y = locSection.getDouble("y");
        double z = locSection.getDouble("z");
        float yaw = (float) locSection.getDouble("yaw", 0.0);
        float pitch = (float) locSection.getDouble("pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private static void loadLines(ConfigurationSection section, HologramBuilder builder) {
        List<String> lines = section.getStringList("lines");
        if (lines.isEmpty()) {
            String singleLine = section.getString("line");
            if (singleLine != null) {
                builder.line(singleLine);
            } else {
                throw new IllegalArgumentException("At least one line is required");
            }
        } else {
            builder.lines(lines);
        }
    }

    private static void loadProperties(ConfigurationSection section, HologramBuilder builder) {
        ConfigurationSection props = section.getConfigurationSection("properties");
        if (props == null) {
            return;
        }

        if (props.contains("billboard")) {
            String billboardStr = props.getString("billboard");
            try {
                Display.Billboard billboard = Display.Billboard.valueOf(billboardStr.toUpperCase());
                builder.billboard(billboard);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid billboard value: " + billboardStr);
            }
        }

        if (props.contains("alignment")) {
            String alignmentStr = props.getString("alignment");
            try {
                TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.valueOf(alignmentStr.toUpperCase());
                builder.alignment(alignment);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid alignment value: " + alignmentStr);
            }
        }

        if (props.contains("scale")) {
            ConfigurationSection scaleSection = props.getConfigurationSection("scale");
            if (scaleSection != null) {
                float x = (float) scaleSection.getDouble("x", 1.0);
                float y = (float) scaleSection.getDouble("y", 1.0);
                float z = (float) scaleSection.getDouble("z", 1.0);
                builder.scale(x, y, z);
            }
        }

        if (props.contains("shadow")) {
            builder.shadow(props.getBoolean("shadow"));
        }

        if (props.contains("seeThrough")) {
            builder.seeThrough(props.getBoolean("seeThrough"));
        }

        if (props.contains("lineWidth")) {
            builder.lineWidth(props.getInt("lineWidth"));
        }

        if (props.contains("backgroundColor")) {
            ConfigurationSection bgColor = props.getConfigurationSection("backgroundColor");
            if (bgColor != null) {
                int r = bgColor.getInt("r", 0);
                int g = bgColor.getInt("g", 0);
                int b = bgColor.getInt("b", 0);
                builder.backgroundColor(Color.fromRGB(r, g, b));
            }
        }

        if (props.contains("lineSpacing")) {
            builder.lineSpacing(props.getDouble("lineSpacing"));
        }

        if (props.contains("brightness")) {
            builder.brightness(props.getInt("brightness"));
        }
    }

    private static void loadConfig(ConfigurationSection section, HologramBuilder builder) {
        ConfigurationSection config = section.getConfigurationSection("config");
        if (config == null) {
            return;
        }

        if (config.contains("updateInterval")) {
            builder.updateInterval(config.getLong("updateInterval"));
        }

        if (config.contains("autoUpdate")) {
            builder.autoUpdate(config.getBoolean("autoUpdate"));
        }

        if (config.contains("spawnOnChunkLoad")) {
            builder.spawnOnChunkLoad(config.getBoolean("spawnOnChunkLoad"));
        }

        if (config.contains("removeOnChunkUnload")) {
            builder.removeOnChunkUnload(config.getBoolean("removeOnChunkUnload"));
        }
    }

    private static void loadBehavior(ConfigurationSection section, HologramBuilder builder) {
        if (section.contains("persistent")) {
            builder.persistent(section.getBoolean("persistent"));
        }

        if (section.contains("perPlayer")) {
            builder.perPlayer(section.getBoolean("perPlayer"));
        }

        if (section.contains("viewDistance")) {
            builder.viewDistance(section.getDouble("viewDistance"));
        }
    }

    public static void saveToConfig(Hologram hologram, ConfigurationSection section) {
        saveLocation(hologram.getLocation(), section.createSection("location"));
        saveLines(hologram, section);
        saveProperties(hologram.getProperties(), section.createSection("properties"));
        saveConfig(hologram.getConfig(), section.createSection("config"));
        saveBehavior(hologram, section);
    }

    private static void saveLocation(Location location, ConfigurationSection section) {
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    private static void saveLines(Hologram hologram, ConfigurationSection section) {
        List<String> lines = hologram.getLines().stream()
                .map(line -> line.getText())
                .toList();
        section.set("lines", lines);
    }

    private static void saveProperties(HologramProperties properties, ConfigurationSection section) {
        section.set("billboard", properties.getBillboard().name());
        section.set("alignment", properties.getAlignment().name());

        ConfigurationSection scaleSection = section.createSection("scale");
        scaleSection.set("x", properties.getScaleX());
        scaleSection.set("y", properties.getScaleY());
        scaleSection.set("z", properties.getScaleZ());

        section.set("shadow", properties.isShadow());
        section.set("seeThrough", properties.isSeeThrough());
        section.set("lineWidth", properties.getLineWidth());

        if (properties.getBackgroundColor() != null) {
            ConfigurationSection bgColor = section.createSection("backgroundColor");
            bgColor.set("r", properties.getBackgroundColor().getRed());
            bgColor.set("g", properties.getBackgroundColor().getGreen());
            bgColor.set("b", properties.getBackgroundColor().getBlue());
        }

        section.set("lineSpacing", properties.getLineSpacing());
        section.set("brightness", properties.getBrightness());
    }

    private static void saveConfig(HologramConfig config, ConfigurationSection section) {
        section.set("updateInterval", config.getUpdateInterval());
        section.set("autoUpdate", config.isAutoUpdate());
        section.set("spawnOnChunkLoad", config.isSpawnOnChunkLoad());
        section.set("removeOnChunkUnload", config.isRemoveOnChunkUnload());
    }

    private static void saveBehavior(Hologram hologram, ConfigurationSection section) {
        section.set("enabled", hologram.isEnabled());
        section.set("persistent", hologram.isPersistent());
        section.set("perPlayer", hologram.isPerPlayer());
        section.set("viewDistance", hologram.getViewDistance());
    }
}
