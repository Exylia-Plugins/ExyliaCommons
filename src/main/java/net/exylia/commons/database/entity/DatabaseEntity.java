package net.exylia.commons.database.entity;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.serialization.SerializationUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public abstract class DatabaseEntity {

    @Column(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    protected long createdAt;

    @Column(name = "updated_at", defaultValue = "CURRENT_TIMESTAMP")
    protected long updatedAt;

    public DatabaseEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }

    protected String serializeComponent(Component component) {
        return SerializationUtils.serializeComponent(component);
    }

    protected Component deserializeComponent(String json) {
        return SerializationUtils.deserializeComponent(json);
    }

    protected String serializeItemStack(ItemStack item) {
        return SerializationUtils.serializeItemStack(item);
    }

    protected ItemStack deserializeItemStack(String base64) {
        return SerializationUtils.deserializeItemStack(base64);
    }

    protected String serializeItemArray(ItemStack[] items) {
        return SerializationUtils.serializeItemArray(items);
    }

    protected ItemStack[] deserializeItemArray(String base64) {
        return SerializationUtils.deserializeItemArray(base64);
    }

    protected String serializePotionEffects(List<PotionEffect> effects) {
        return SerializationUtils.serializePotionEffectsToJson(effects);
    }

    protected List<PotionEffect> deserializePotionEffects(String json) {
        return SerializationUtils.deserializePotionEffectsFromJson(json);
    }

    protected String serializeLocation(Location location) {
        return SerializationUtils.serializeLocation(location);
    }

    protected Location deserializeLocation(String locationString) {
        return SerializationUtils.deserializeLocation(locationString);
    }

    protected String serializeStringList(List<String> list) {
        return SerializationUtils.serializeStringList(list);
    }

    protected List<String> deserializeStringList(String json) {
        return SerializationUtils.deserializeStringList(json);
    }

    protected String serializeMap(Map<String, Object> map) {
        return SerializationUtils.serializeMap(map);
    }

    protected Map<String, Object> deserializeMap(String json) {
        return SerializationUtils.deserializeMap(json);
    }

}
