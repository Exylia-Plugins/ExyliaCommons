package net.exylia.commons.v2.hologram.core;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.hologram.model.*;
import net.exylia.commons.v2.hologram.visibility.VisibilityCondition;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class HologramFactory {
    private final JavaPlugin plugin;

    public Hologram create(
            String id,
            Location location,
            List<String> lineTexts,
            HologramProperties properties,
            HologramConfig config,
            boolean persistent,
            boolean perPlayer,
            VisibilityCondition visibilityCondition,
            double viewDistance,
            boolean enabled
    ) {
        validateParameters(id, location, lineTexts);

        HologramProperties finalProperties = properties != null ? properties : HologramProperties.defaultProperties();
        HologramConfig finalConfig = config != null ? config : HologramConfig.defaultConfig();

        List<HologramLine> lines = lineTexts.stream()
                .map(HologramLine::new)
                .collect(Collectors.toList());

        return new Hologram(
                id,
                location.clone(),
                lines,
                finalProperties,
                finalConfig,
                persistent,
                perPlayer,
                visibilityCondition,
                viewDistance,
                plugin,
                enabled
        );
    }

    private void validateParameters(String id, Location location, List<String> lines) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Hologram ID cannot be null or empty");
        }

        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }

        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }

        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Hologram must have at least one line");
        }
    }
}
