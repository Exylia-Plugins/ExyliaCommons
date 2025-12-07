package net.exylia.commons.v2.hologram.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

@Getter
@RequiredArgsConstructor
public class HologramLine {
    private final String text;
    private final Component component;
    private final HologramProperties properties;

    public HologramLine(String text) {
        this(text, null, null);
    }

    public HologramLine(String text, HologramProperties properties) {
        this(text, null, properties);
    }

    public HologramProperties getPropertiesOrDefault(HologramProperties defaultProperties) {
        return properties != null ? properties : defaultProperties;
    }

    public boolean hasCustomProperties() {
        return properties != null;
    }
}
