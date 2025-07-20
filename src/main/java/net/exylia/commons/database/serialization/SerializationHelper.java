package net.exylia.commons.database.serialization;

import net.exylia.commons.database.annotations.SerializationType;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.serialization.RegionSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Helper para manejar la auto-serialización de tipos complejos
 * ACTUALIZADO: Ahora incluye soporte para Region
 */
public class SerializationHelper {

    /**
     * Serializa automáticamente un valor basado en su tipo y configuración
     */
    @SuppressWarnings("unchecked")
    public static Object autoSerializeValue(Object value, Field field, SerializationType serType) {
        if (value == null) return null;

        Class<?> type = field.getType();
        SerializationType actualType = serType == SerializationType.AUTO ? detectSerializationType(type, field) : serType;

        // NUEVO: Soporte para Region - SIEMPRE usar RegionSerializer para Region
        if (type == Region.class || value instanceof Region) {
            try {
                String result = RegionSerializer.serialize((Region) value);
                if (result == null || result.trim().isEmpty()) {
                    throw new RuntimeException("RegionSerializer returned null or empty result");
                }
                return result;
            } catch (Exception e) {
                throw new RuntimeException("Error serializing Region: " + e.getMessage(), e);
            }
        }

        // Tipos específicos de Bukkit
        else if (type == Location.class) {
            return SerializationUtils.serializeLocation((Location) value);
        } else if (type == Component.class) {
            return SerializationUtils.serializeComponent((Component) value);
        } else if (type == ItemStack.class) {
            if (actualType == SerializationType.YAML) {
                return SerializationUtils.serializeItemsToYaml(new ItemStack[]{(ItemStack) value});
            } else {
                return SerializationUtils.serializeItemStack((ItemStack) value);
            }
        } else if (type == ItemStack[].class) {
            if (actualType == SerializationType.YAML) {
                return SerializationUtils.serializeItemsToYaml((ItemStack[]) value);
            } else {
                return SerializationUtils.serializeItemArray((ItemStack[]) value);
            }
        }

        // Colecciones
        else if (Collection.class.isAssignableFrom(type)) {
            return handleCollectionSerialization(value, field, actualType);
        }

        // Mapas
        else if (Map.class.isAssignableFrom(type)) {
            return SerializationUtils.serializeMap((Map<String, Object>) value);
        }

        // Arrays primitivos y de objetos
        else if (type.isArray()) {
            return handleArraySerialization(value, actualType);
        }

        // Objetos complejos - usar BASE64 por defecto
        else {
            if (actualType == SerializationType.JSON) {
                // Intentar JSON primero, fallback a BASE64
                try {
                    return SerializationUtils.serializeMap((Map<String, Object>) value);
                } catch (Exception e) {
                    return SerializationUtils.serializeObject(value);
                }
            } else {
                return SerializationUtils.serializeObject(value);
            }
        }
    }

    /**
     * Deserializa automáticamente un valor basado en su tipo y configuración
     */
    public static Object autoDeserializeValue(Object serializedValue, Field field, SerializationType serType) {
        if (serializedValue == null) return null;

        String stringValue = serializedValue.toString();
        Class<?> type = field.getType();
        SerializationType actualType = serType == SerializationType.AUTO ? detectSerializationType(type, field) : serType;

        // NUEVO: Soporte para Region - SIEMPRE usar RegionSerializer para Region
        if (type == Region.class) {
            try {
                return RegionSerializer.deserialize(stringValue);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing Region: " + e.getMessage(), e);
            }
        }

        // Tipos específicos de Bukkit
        else if (type == Location.class) {
            return SerializationUtils.deserializeLocation(stringValue);
        } else if (type == Component.class) {
            return SerializationUtils.deserializeComponent(stringValue);
        } else if (type == ItemStack.class) {
            if (actualType == SerializationType.YAML) {
                ItemStack[] items = SerializationUtils.deserializeItemsFromYaml(stringValue, 1);
                return items.length > 0 ? items[0] : null;
            } else {
                return SerializationUtils.deserializeItemStack(stringValue);
            }
        } else if (type == ItemStack[].class) {
            if (actualType == SerializationType.YAML) {
                // Para YAML necesitamos saber el tamaño, usar un tamaño por defecto grande
                return SerializationUtils.deserializeItemsFromYaml(stringValue, 54); // Tamaño de inventario estándar
            } else {
                return SerializationUtils.deserializeItemArray(stringValue);
            }
        }

        // Colecciones
        else if (Collection.class.isAssignableFrom(type)) {
            return handleCollectionDeserialization(stringValue, field, actualType);
        }

        // Mapas
        else if (Map.class.isAssignableFrom(type)) {
            return SerializationUtils.deserializeMap(stringValue);
        }

        // Arrays
        else if (type.isArray()) {
            return handleArrayDeserialization(stringValue, type, actualType);
        }

        // Objetos complejos
        else {
            if (actualType == SerializationType.JSON) {
                try {
                    return SerializationUtils.deserializeMap(stringValue);
                } catch (Exception e) {
                    return SerializationUtils.deserializeObject(stringValue, type);
                }
            } else {
                return SerializationUtils.deserializeObject(stringValue, type);
            }
        }
    }

    /**
     * Detecta automáticamente el mejor tipo de serialización para un campo
     */
    private static SerializationType detectSerializationType(Class<?> type, Field field) {
        // NUEVO: Region - usar JSON para legibilidad y flexibilidad
        if (type == Region.class) {
            return SerializationType.JSON;
        }

        // Tipos de Bukkit específicos
        if (type == Location.class || type == Component.class) {
            return SerializationType.STRING;
        }

        // ItemStacks - usar BASE64 por defecto, pero permitir YAML
        if (type == ItemStack.class || type == ItemStack[].class) {
            return SerializationType.BASE64;
        }

        // PotionEffects - mejor en JSON
        if (type == PotionEffect.class || (type.isArray() && type.getComponentType() == PotionEffect.class)) {
            return SerializationType.JSON;
        }

        // Colecciones y Mapas - JSON
        if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            return SerializationType.JSON;
        }

        // Arrays primitivos - JSON
        if (type.isArray() && (type.getComponentType().isPrimitive() || type.getComponentType() == String.class)) {
            return SerializationType.JSON;
        }

        // Objetos complejos - BASE64 para compatibilidad total
        return SerializationType.BASE64;
    }

    /**
     * Maneja la serialización de colecciones
     */
    @SuppressWarnings("unchecked")
    private static String handleCollectionSerialization(Object value, Field field, SerializationType serType) {
        Collection<?> collection = (Collection<?>) value;

        // Detectar el tipo genérico
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();

            if (typeArgs.length > 0) {
                Class<?> elementType = (Class<?>) typeArgs[0];

                // Lista de strings
                if (elementType == String.class) {
                    return SerializationUtils.serializeStringList((List<String>) collection);
                }

                // Lista de PotionEffects
                if (elementType == PotionEffect.class) {
                    return SerializationUtils.serializePotionEffectsToJson((List<PotionEffect>) collection);
                }

                // NUEVO: Lista de Regions
                if (elementType == Region.class) {
                    List<Region> regions = (List<Region>) collection;
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < regions.size(); i++) {
                        if (i > 0) json.append(",");
                        json.append(RegionSerializer.serialize(regions.get(i)));
                    }
                    json.append("]");
                    return json.toString();
                }
            }
        }

        // Fallback genérico
        return SerializationUtils.serializeObject(value);
    }

    /**
     * Maneja la deserialización de colecciones
     */
    private static Object handleCollectionDeserialization(String stringValue, Field field, SerializationType serType) {
        // Detectar el tipo genérico
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();

            if (typeArgs.length > 0) {
                Class<?> elementType = (Class<?>) typeArgs[0];

                // Lista de strings
                if (elementType == String.class) {
                    return SerializationUtils.deserializeStringList(stringValue);
                }

                // Lista de PotionEffects
                if (elementType == PotionEffect.class) {
                    return SerializationUtils.deserializePotionEffectsFromJson(stringValue);
                }

                // NUEVO: Lista de Regions
                if (elementType == Region.class) {
                    try {
                        // Parsear JSON array manualmente para regions
                        List<Region> regions = new java.util.ArrayList<>();
                        if (stringValue.startsWith("[") && stringValue.endsWith("]")) {
                            String content = stringValue.substring(1, stringValue.length() - 1);
                            if (!content.trim().isEmpty()) {
                                // Simple parser para objetos JSON separados por comas
                                int braceCount = 0;
                                StringBuilder currentRegion = new StringBuilder();

                                for (char c : content.toCharArray()) {
                                    if (c == '{') braceCount++;
                                    else if (c == '}') braceCount--;

                                    currentRegion.append(c);

                                    if (braceCount == 0 && currentRegion.length() > 0) {
                                        String regionJson = currentRegion.toString().trim();
                                        if (regionJson.endsWith(",")) {
                                            regionJson = regionJson.substring(0, regionJson.length() - 1);
                                        }

                                        Region region = RegionSerializer.deserialize(regionJson);
                                        if (region != null) {
                                            regions.add(region);
                                        }

                                        currentRegion.setLength(0);
                                    }
                                }
                            }
                        }
                        return regions;
                    } catch (Exception e) {
                        // Fallback a deserialización de objeto
                        return SerializationUtils.deserializeObject(stringValue, field.getType());
                    }
                }
            }
        }

        // Fallback genérico
        return SerializationUtils.deserializeObject(stringValue, field.getType());
    }

    /**
     * Maneja la serialización de arrays
     */
    private static String handleArraySerialization(Object value, SerializationType serType) {
        Class<?> componentType = value.getClass().getComponentType();

        if (componentType == PotionEffect.class) {
            PotionEffect[] effects = (PotionEffect[]) value;
            return SerializationUtils.serializePotionEffectsToJson(List.of(effects));
        }

        // NUEVO: Array de Regions
        if (componentType == Region.class) {
            Region[] regions = (Region[]) value;
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < regions.length; i++) {
                if (i > 0) json.append(",");
                json.append(RegionSerializer.serialize(regions[i]));
            }
            json.append("]");
            return json.toString();
        }

        // Arrays primitivos y otros - usar serialización de objeto
        return SerializationUtils.serializeObject(value);
    }

    /**
     * Maneja la deserialización de arrays
     */
    private static Object handleArrayDeserialization(String stringValue, Class<?> arrayType, SerializationType serType) {
        Class<?> componentType = arrayType.getComponentType();

        if (componentType == PotionEffect.class) {
            List<PotionEffect> effects = SerializationUtils.deserializePotionEffectsFromJson(stringValue);
            return effects.toArray(new PotionEffect[0]);
        }

        // NUEVO: Array de Regions (similar a la deserialización de List<Region>)
        if (componentType == Region.class) {
            try {
                List<Region> regions = new java.util.ArrayList<>();
                if (stringValue.startsWith("[") && stringValue.endsWith("]")) {
                    String content = stringValue.substring(1, stringValue.length() - 1);
                    if (!content.trim().isEmpty()) {
                        int braceCount = 0;
                        StringBuilder currentRegion = new StringBuilder();

                        for (char c : content.toCharArray()) {
                            if (c == '{') braceCount++;
                            else if (c == '}') braceCount--;

                            currentRegion.append(c);

                            if (braceCount == 0 && currentRegion.length() > 0) {
                                String regionJson = currentRegion.toString().trim();
                                if (regionJson.endsWith(",")) {
                                    regionJson = regionJson.substring(0, regionJson.length() - 1);
                                }

                                Region region = RegionSerializer.deserialize(regionJson);
                                if (region != null) {
                                    regions.add(region);
                                }

                                currentRegion.setLength(0);
                            }
                        }
                    }
                }
                return regions.toArray(new Region[0]);
            } catch (Exception e) {
                return SerializationUtils.deserializeObject(stringValue, arrayType);
            }
        }

        // Arrays primitivos y otros
        return SerializationUtils.deserializeObject(stringValue, arrayType);
    }

    /**
     * Verifica si un tipo necesita serialización automática
     */
    public static boolean needsAutoSerialization(Class<?> type) {
        return type == Location.class ||
                type == Component.class ||
                type == ItemStack.class ||
                type == ItemStack[].class ||
                type == PotionEffect.class ||
                type == PotionEffect[].class ||
                type == Region.class ||  // NUEVO
                Collection.class.isAssignableFrom(type) ||
                Map.class.isAssignableFrom(type) ||
                (type.isArray() && !type.getComponentType().isPrimitive() && type.getComponentType() != String.class) ||
                (!type.isPrimitive() &&
                        type != String.class &&
                        type != Integer.class &&
                        type != Long.class &&
                        type != Double.class &&
                        type != Float.class &&
                        type != Boolean.class &&
                        !type.isEnum());
    }
}