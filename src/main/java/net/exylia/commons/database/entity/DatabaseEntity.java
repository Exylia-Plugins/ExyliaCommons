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

/**
 * Clase base para entidades de base de datos con métodos helper de serialización
 */
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

    // ===== HELPER METHODS FOR SERIALIZATION =====

    /**
     * Serializa un Component para guardarlo en BD
     */
    protected String serializeComponent(Component component) {
        return SerializationUtils.serializeComponent(component);
    }

    /**
     * Deserializa un Component desde BD
     */
    protected Component deserializeComponent(String json) {
        return SerializationUtils.deserializeComponent(json);
    }

    /**
     * Serializa un ItemStack para guardarlo en BD
     */
    protected String serializeItemStack(ItemStack item) {
        return SerializationUtils.serializeItemStack(item);
    }

    /**
     * Deserializa un ItemStack desde BD
     */
    protected ItemStack deserializeItemStack(String base64) {
        return SerializationUtils.deserializeItemStack(base64);
    }

    /**
     * Serializa un array de ItemStacks para guardarlo en BD
     */
    protected String serializeItemArray(ItemStack[] items) {
        return SerializationUtils.serializeItemArray(items);
    }

    /**
     * Deserializa un array de ItemStacks desde BD
     */
    protected ItemStack[] deserializeItemArray(String base64) {
        return SerializationUtils.deserializeItemArray(base64);
    }

    /**
     * Serializa efectos de poción para guardarlos en BD (usando JSON por legibilidad)
     */
    protected String serializePotionEffects(List<PotionEffect> effects) {
        return SerializationUtils.serializePotionEffectsToJson(effects);
    }

    /**
     * Deserializa efectos de poción desde BD
     */
    protected List<PotionEffect> deserializePotionEffects(String json) {
        return SerializationUtils.deserializePotionEffectsFromJson(json);
    }

    /**
     * Serializa una Location para guardarla en BD
     */
    protected String serializeLocation(Location location) {
        return SerializationUtils.serializeLocation(location);
    }

    /**
     * Deserializa una Location desde BD
     */
    protected Location deserializeLocation(String locationString) {
        return SerializationUtils.deserializeLocation(locationString);
    }

    /**
     * Serializa una lista de strings para guardarla en BD
     */
    protected String serializeStringList(List<String> list) {
        return SerializationUtils.serializeStringList(list);
    }

    /**
     * Deserializa una lista de strings desde BD
     */
    protected List<String> deserializeStringList(String json) {
        return SerializationUtils.deserializeStringList(json);
    }

    /**
     * Serializa un mapa para guardarlo en BD
     */
    protected String serializeMap(Map<String, Object> map) {
        return SerializationUtils.serializeMap(map);
    }

    /**
     * Deserializa un mapa desde BD
     */
    protected Map<String, Object> deserializeMap(String json) {
        return SerializationUtils.deserializeMap(json);
    }

    // ===== GETTERS Y SETTERS =====

}