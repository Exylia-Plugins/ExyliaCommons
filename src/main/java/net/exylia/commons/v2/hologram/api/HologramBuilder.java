package net.exylia.commons.v2.hologram.api;

import net.exylia.commons.v2.hologram.core.HologramManager;
import net.exylia.commons.v2.hologram.model.Hologram;
import net.exylia.commons.v2.hologram.model.HologramConfig;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import net.exylia.commons.v2.hologram.model.HologramTemplate;
import net.exylia.commons.v2.hologram.visibility.VisibilityCondition;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.Bukkit;
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
    private boolean enabled = true;
    private PlaceholderContext placeholderContext = null;

    public HologramBuilder(String id, Location location) {
        this.id = id;
        this.location = location;
    }

    public HologramBuilder(String id, Location location, HologramTemplate template) {
        this.id = id;
        this.location = applyOffset(location, template);
        applyTemplate(template);
    }

    public static HologramBuilder fromTemplate(String id, Location location, HologramTemplate template) {
        return new HologramBuilder(id, location, template);
    }

    private static Location applyOffset(Location location, HologramTemplate template) {
        if (template.getOffsetX() == 0.0 && template.getOffsetY() == 0.0 && template.getOffsetZ() == 0.0) {
            return location;
        }
        return location.clone().add(template.getOffsetX(), template.getOffsetY(), template.getOffsetZ());
    }

    public HologramBuilder applyTemplate(HologramTemplate template) {
        if (template.getLines() != null && !template.getLines().isEmpty()) {
            this.lines.addAll(template.getLines());
        }

        if (template.getProperties() != null) {
            HologramProperties props = template.getProperties();
            this.propertiesBuilder = HologramProperties.builder()
                .billboard(props.getBillboard())
                .alignment(props.getAlignment())
                .scaleX(props.getScaleX())
                .scaleY(props.getScaleY())
                .scaleZ(props.getScaleZ())
                .shadow(props.isShadow())
                .seeThrough(props.isSeeThrough())
                .lineWidth(props.getLineWidth())
                .backgroundColor(props.getBackgroundColor())
                .backgroundAlpha(props.getBackgroundAlpha())
                .textOpacity(props.getTextOpacity())
                .defaultBackground(props.isDefaultBackground())
                .lineSpacing(props.getLineSpacing())
                .brightness(props.getBrightness());
        }

        if (template.getConfig() != null) {
            HologramConfig cfg = template.getConfig();
            this.configBuilder = HologramConfig.builder()
                .updateInterval(cfg.getUpdateInterval())
                .autoUpdate(cfg.isAutoUpdate())
                .spawnOnChunkLoad(cfg.isSpawnOnChunkLoad())
                .removeOnChunkUnload(cfg.isRemoveOnChunkUnload());
        }

        this.persistent = template.isPersistent();
        this.perPlayer = template.isPerPlayer();
        this.viewDistance = template.getViewDistance();
        this.enabled = template.isEnabled();

        if (template.getVisibilityCondition() != null) {
            this.visibilityCondition = template.getVisibilityCondition();
        }

        return this;
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

    public HologramBuilder backgroundAlpha(int alpha) {
        propertiesBuilder.backgroundAlpha(alpha);
        return this;
    }

    public HologramBuilder textOpacity(byte opacity) {
        propertiesBuilder.textOpacity(opacity);
        return this;
    }

    public HologramBuilder textOpacity(int opacity) {
        propertiesBuilder.textOpacity((byte) opacity);
        return this;
    }

    public HologramBuilder defaultBackground(boolean defaultBackground) {
        propertiesBuilder.defaultBackground(defaultBackground);
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

    public HologramBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public HologramBuilder placeholderContext(PlaceholderContext context) {
        this.placeholderContext = context;
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
                viewDistance,
                enabled,
                placeholderContext
        );
    }

    public Hologram build() {
        if (Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                "HologramBuilder.build() cannot be called from the main thread as it would cause a deadlock. Use buildAsync() instead."
            );
        }
        return buildAsync().join();
    }
}
