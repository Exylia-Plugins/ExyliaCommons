package net.exylia.commons.v2.hologram.api;

import net.exylia.commons.v2.hologram.core.HologramManager;
import net.exylia.commons.v2.hologram.model.Hologram;
import net.exylia.commons.v2.hologram.model.HologramConfig;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import net.exylia.commons.v2.hologram.visibility.VisibilityCondition;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HologramBuilder {
    private final String id;
    private final Location location;
    private final List<String> lines = new ArrayList<>();
    private HologramProperties.HologramPropertiesBuilder propertiesBuilder = HologramProperties.builder();
    private HologramConfig.HologramConfigBuilder configBuilder = HologramConfig.builder();
    private boolean persistent = false;
    private boolean perPlayer = false;
    private VisibilityCondition visibilityCondition = null;
    private double viewDistance = 50.0;

    public HologramBuilder(String id, Location location) {
        this.id = id;
        this.location = location;
    }

    public HologramBuilder line(String text) {
        lines.add(text);
        return this;
    }

    public HologramBuilder lines(String... lines) {
        this.lines.addAll(Arrays.asList(lines));
        return this;
    }

    public HologramBuilder lines(List<String> lines) {
        this.lines.addAll(lines);
        return this;
    }

    public HologramBuilder billboard(Display.Billboard mode) {
        propertiesBuilder.billboard(mode);
        return this;
    }

    public HologramBuilder alignment(TextDisplay.TextAlignment alignment) {
        propertiesBuilder.alignment(alignment);
        return this;
    }

    public HologramBuilder scale(float x, float y, float z) {
        propertiesBuilder.scaleX(x).scaleY(y).scaleZ(z);
        return this;
    }

    public HologramBuilder shadow(boolean shadow) {
        propertiesBuilder.shadow(shadow);
        return this;
    }

    public HologramBuilder seeThrough(boolean seeThrough) {
        propertiesBuilder.seeThrough(seeThrough);
        return this;
    }

    public HologramBuilder lineWidth(int lineWidth) {
        propertiesBuilder.lineWidth(lineWidth);
        return this;
    }

    public HologramBuilder backgroundColor(Color color) {
        propertiesBuilder.backgroundColor(color);
        return this;
    }

    public HologramBuilder lineSpacing(double spacing) {
        propertiesBuilder.lineSpacing(spacing);
        return this;
    }

    public HologramBuilder brightness(int brightness) {
        propertiesBuilder.brightness(brightness);
        return this;
    }

    public HologramBuilder updateInterval(long ticks) {
        configBuilder.updateInterval(ticks);
        return this;
    }

    public HologramBuilder autoUpdate(boolean autoUpdate) {
        configBuilder.autoUpdate(autoUpdate);
        return this;
    }

    public HologramBuilder spawnOnChunkLoad(boolean spawn) {
        configBuilder.spawnOnChunkLoad(spawn);
        return this;
    }

    public HologramBuilder removeOnChunkUnload(boolean remove) {
        configBuilder.removeOnChunkUnload(remove);
        return this;
    }

    public HologramBuilder persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    public HologramBuilder perPlayer(boolean perPlayer) {
        this.perPlayer = perPlayer;
        return this;
    }

    public HologramBuilder viewDistance(double distance) {
        this.viewDistance = distance;
        return this;
    }

    public HologramBuilder visibilityCondition(VisibilityCondition condition) {
        this.visibilityCondition = condition;
        return this;
    }

    public CompletableFuture<Hologram> buildAsync() {
        return HologramManager.getInstance().createHologramAsync(
                id,
                location,
                lines,
                propertiesBuilder.build(),
                configBuilder.build(),
                persistent,
                perPlayer,
                visibilityCondition,
                viewDistance
        );
    }

    public Hologram build() {
        return buildAsync().join();
    }
}
