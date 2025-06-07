package net.exylia.commons.database.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Utilidades centralizadas para serialización/deserialización de objetos comunes de Bukkit
 */
public class SerializationUtils {

    private static final Gson GSON = new Gson();
    private static final GsonComponentSerializer COMPONENT_SERIALIZER = GsonComponentSerializer.gson();

    // ===== COMPONENT SERIALIZATION =====

    /**
     * Serializa un Component a JSON
     */
    public static String serializeComponent(Component component) {
        if (component == null) return null;
        return COMPONENT_SERIALIZER.serialize(component);
    }

    /**
     * Deserializa un Component desde JSON
     */
    public static Component deserializeComponent(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return COMPONENT_SERIALIZER.deserialize(json);
        } catch (JsonSyntaxException e) {
            return Component.text(json); // Fallback a texto plano
        }
    }

    // ===== ITEMSTACK SERIALIZATION =====

    /**
     * Serializa un ItemStack a Base64
     */
    public static String serializeItemStack(ItemStack item) {
        if (item == null) return null;
        return serializeObject(item);
    }

    /**
     * Deserializa un ItemStack desde Base64
     */
    public static ItemStack deserializeItemStack(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        return deserializeObject(base64, ItemStack.class);
    }

    /**
     * Serializa un array de ItemStacks a Base64
     */
    public static String serializeItemArray(ItemStack[] items) {
        if (items == null) return null;
        return serializeObject(items);
    }

    /**
     * Deserializa un array de ItemStacks desde Base64
     */
    public static ItemStack[] deserializeItemArray(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        return deserializeObject(base64, ItemStack[].class);
    }

    /**
     * Serializa ItemStacks a YAML (más legible para configs)
     */
    public static String serializeItemsToYaml(ItemStack[] items) {
        if (items == null || items.length == 0) return null;

        YamlConfiguration config = new YamlConfiguration();
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                config.set("items." + i, items[i]);
            }
        }
        return config.saveToString();
    }

    /**
     * Deserializa ItemStacks desde YAML
     */
    public static ItemStack[] deserializeItemsFromYaml(String yaml, int size) {
        if (yaml == null || yaml.isEmpty()) return new ItemStack[size];

        try {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            ItemStack[] items = new ItemStack[size];
            ConfigurationSection itemsSection = config.getConfigurationSection("items");

            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        if (slot >= 0 && slot < size) {
                            items[slot] = itemsSection.getItemStack(key);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            return items;
        } catch (Exception e) {
            return new ItemStack[size];
        }
    }

    // ===== POTION EFFECTS SERIALIZATION =====

    /**
     * Serializa una lista de PotionEffects a Base64
     */
    public static String serializePotionEffects(List<PotionEffect> effects) {
        if (effects == null || effects.isEmpty()) return null;
        return serializeObject(effects.toArray(new PotionEffect[0]));
    }

    /**
     * Deserializa PotionEffects desde Base64
     */
    public static List<PotionEffect> deserializePotionEffects(String base64) {
        if (base64 == null || base64.isEmpty()) return new ArrayList<>();

        PotionEffect[] effects = deserializeObject(base64, PotionEffect[].class);
        return effects != null ? Arrays.asList(effects) : new ArrayList<>();
    }

    /**
     * Serializa PotionEffects a formato de mapa (más legible)
     */
    public static String serializePotionEffectsToJson(List<PotionEffect> effects) {
        if (effects == null || effects.isEmpty()) return null;

        List<Map<String, Object>> effectMaps = new ArrayList<>();
        for (PotionEffect effect : effects) {
            Map<String, Object> effectMap = new HashMap<>();
            effectMap.put("type", effect.getType().getName());
            effectMap.put("duration", effect.getDuration());
            effectMap.put("amplifier", effect.getAmplifier());
            effectMap.put("ambient", effect.isAmbient());
            effectMap.put("particles", effect.hasParticles());
            effectMap.put("icon", effect.hasIcon());
            effectMaps.add(effectMap);
        }

        return GSON.toJson(effectMaps);
    }

    /**
     * Deserializa PotionEffects desde JSON
     */
    @SuppressWarnings("unchecked")
    public static List<PotionEffect> deserializePotionEffectsFromJson(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();

        try {
            List<Map<String, Object>> effectMaps = GSON.fromJson(json, List.class);
            List<PotionEffect> effects = new ArrayList<>();

            for (Map<String, Object> effectMap : effectMaps) {
                try {
                    String typeName = (String) effectMap.get("type");
                    int duration = ((Number) effectMap.get("duration")).intValue();
                    int amplifier = ((Number) effectMap.get("amplifier")).intValue();
                    boolean ambient = (Boolean) effectMap.getOrDefault("ambient", false);
                    boolean particles = (Boolean) effectMap.getOrDefault("particles", true);
                    boolean icon = (Boolean) effectMap.getOrDefault("icon", true);

                    PotionEffect effect = new PotionEffect(
                            Objects.requireNonNull(org.bukkit.potion.PotionEffectType.getByName(typeName)),
                            duration, amplifier, ambient, particles, icon
                    );
                    effects.add(effect);
                } catch (Exception e) {
                    // Ignorar efectos inválidos
                }
            }

            return effects;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ===== LOCATION SERIALIZATION =====

    /**
     * Serializa una Location a String
     */
    public static String serializeLocation(Location location) {
        if (location == null || location.getWorld() == null) return null;

        return String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    /**
     * Deserializa una Location desde String
     */
    public static Location deserializeLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) return null;

        try {
            String[] parts = locationString.split(",");
            if (parts.length != 6) return null;

            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);

            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Serializa Location a JSON (más detallado)
     */
    public static String serializeLocationToJson(Location location) {
        if (location == null) return null;

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("world", location.getWorld() != null ? location.getWorld().getName() : null);
        locationMap.put("x", location.getX());
        locationMap.put("y", location.getY());
        locationMap.put("z", location.getZ());
        locationMap.put("yaw", location.getYaw());
        locationMap.put("pitch", location.getPitch());

        return GSON.toJson(locationMap);
    }

    /**
     * Deserializa Location desde JSON
     */
    @SuppressWarnings("unchecked")
    public static Location deserializeLocationFromJson(String json) {
        if (json == null || json.isEmpty()) return null;

        try {
            Map<String, Object> locationMap = GSON.fromJson(json, Map.class);

            String worldName = (String) locationMap.get("world");
            if (worldName == null) return null;

            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;

            double x = ((Number) locationMap.get("x")).doubleValue();
            double y = ((Number) locationMap.get("y")).doubleValue();
            double z = ((Number) locationMap.get("z")).doubleValue();
            float yaw = ((Number) locationMap.get("yaw")).floatValue();
            float pitch = ((Number) locationMap.get("pitch")).floatValue();

            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    // ===== GENERIC OBJECT SERIALIZATION =====

    /**
     * Serializa cualquier objeto serializable a Base64
     */
    public static String serializeObject(Object object) {
        if (object == null) return null;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(object);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Deserializa un objeto desde Base64
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserializeObject(String base64, Class<T> type) {
        if (base64 == null || base64.isEmpty()) return null;

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            T object = (T) dataInput.readObject();
            dataInput.close();
            return object;
        } catch (Exception e) {
            return null;
        }
    }

    // ===== LIST AND MAP SERIALIZATION =====

    /**
     * Serializa una lista de strings a JSON
     */
    public static String serializeStringList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return GSON.toJson(list);
    }

    /**
     * Deserializa una lista de strings desde JSON
     */
    @SuppressWarnings("unchecked")
    public static List<String> deserializeStringList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();

        try {
            return GSON.fromJson(json, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Serializa un mapa a JSON
     */
    public static String serializeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        return GSON.toJson(map);
    }

    /**
     * Deserializa un mapa desde JSON
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deserializeMap(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();

        try {
            return GSON.fromJson(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // ===== UTILITY METHODS =====

    /**
     * Verifica si un string es un JSON válido
     */
    public static boolean isValidJson(String json) {
        try {
            GSON.fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * Obtiene el tamaño aproximado en bytes de un string
     */
    public static int getStringSize(String text) {
        return text != null ? text.getBytes().length : 0;
    }
}