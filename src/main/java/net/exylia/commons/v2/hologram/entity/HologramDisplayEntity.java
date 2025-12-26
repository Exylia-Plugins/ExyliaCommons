package net.exylia.commons.v2.hologram.entity;

import lombok.Getter;
import net.exylia.commons.v2.hologram.model.HologramProperties;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.util.Collection;

@Getter
public class HologramDisplayEntity {
    private static final double DUPLICATE_CHECK_RADIUS = 0.75;

    private final TextDisplay entity;
    private final HologramProperties properties;

    private HologramDisplayEntity(TextDisplay entity, HologramProperties properties) {
        this.entity = entity;
        this.properties = properties;
    }

    public static HologramDisplayEntity create(Location location, String text, HologramProperties properties) {
        cleanupDuplicateEntities(location);

        TextDisplay display = (TextDisplay) location.getWorld()
                .spawnEntity(location, EntityType.TEXT_DISPLAY);

        HologramDisplayEntity displayEntity = new HologramDisplayEntity(display, properties);
        displayEntity.update(text);
        displayEntity.applyProperties(properties);

        return displayEntity;
    }

    public static void cleanupDuplicateEntities(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        Collection<Entity> nearbyEntities = location.getWorld()
                .getNearbyEntities(location, DUPLICATE_CHECK_RADIUS, DUPLICATE_CHECK_RADIUS, DUPLICATE_CHECK_RADIUS);

        nearbyEntities.stream()
                .filter(entity -> entity.getType() == EntityType.TEXT_DISPLAY)
                .filter(Entity::isValid)
                .forEach(Entity::remove);
    }

    public void update(String text) {
        if (entity != null && entity.isValid()) {
            Component component = ColorAPI.parse(text);
            entity.text(component);
        }
    }

    public void updateComponent(Component component) {
        if (entity != null && entity.isValid()) {
            entity.text(component);
        }
    }

    public void applyProperties(HologramProperties props) {
        if (entity == null || !entity.isValid()) {
            return;
        }

        entity.setBillboard(props.getBillboard());
        entity.setAlignment(props.getAlignment());

        org.bukkit.util.Transformation transformation = entity.getTransformation();
        transformation.getScale().set(props.getScaleX(), props.getScaleY(), props.getScaleZ());
        entity.setTransformation(transformation);

        entity.setShadowed(props.isShadow());
        entity.setSeeThrough(props.isSeeThrough());
        entity.setLineWidth(props.getLineWidth());

        if (props.getBackgroundColor() != null) {
            int alpha = props.getBackgroundAlpha();
            org.bukkit.Color bgColor = props.getBackgroundColor();
            if (alpha < 255) {
                org.bukkit.Color colorWithAlpha = org.bukkit.Color.fromARGB(alpha, bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue());
                entity.setBackgroundColor(colorWithAlpha);
            } else {
                entity.setBackgroundColor(bgColor);
            }
        } else if (props.getBackgroundAlpha() == 0) {
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        }

        entity.setTextOpacity(props.getTextOpacity());
        entity.setDefaultBackground(props.isDefaultBackground());

        if (props.getBrightness() >= 0) {
            int sky = (props.getBrightness() >> 4) & 0xF;
            int block = props.getBrightness() & 0xF;
            entity.setBrightness(new Display.Brightness(sky, block));
        }
    }

    public void remove() {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    public void teleport(Location location) {
        if (entity != null && entity.isValid()) {
            entity.teleport(location);
        }
    }

    public boolean isValid() {
        return entity != null && entity.isValid();
    }

    public Location getLocation() {
        return entity != null ? entity.getLocation() : null;
    }
}
