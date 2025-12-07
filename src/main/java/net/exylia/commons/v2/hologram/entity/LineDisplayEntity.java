package net.exylia.commons.v2.hologram.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.hologram.model.HologramLine;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import org.bukkit.Location;

@Getter
@RequiredArgsConstructor
public class LineDisplayEntity {
    private final HologramDisplayEntity displayEntity;
    private final HologramLine line;
    private final int lineIndex;

    public void update(String text) {
        displayEntity.update(text);
    }

    public void updateWithLine(HologramLine newLine) {
        displayEntity.update(newLine.getText());

        if (newLine.hasCustomProperties()) {
            displayEntity.applyProperties(newLine.getProperties());
        }
    }

    public void remove() {
        displayEntity.remove();
    }

    public void teleport(Location location) {
        displayEntity.teleport(location);
    }

    public boolean isValid() {
        return displayEntity.isValid();
    }

    public Location getLocation() {
        return displayEntity.getLocation();
    }

    public String getText() {
        return line.getText();
    }

    public HologramProperties getProperties() {
        return displayEntity.getProperties();
    }
}
