package net.exylia.commons.database.serialization;

import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.annotations.SerializationType;
import net.exylia.commons.database.exceptions.SerializationException;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.serialization.RegionSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Enhanced helper for managing auto-serialization with automatic collection initialization
 */
public class SerializationHelper {

    /**
     * Automatically serializes a value based on its type and configuration
     * Now handles null collections by creating empty ones when appropriate
     */
    @SuppressWarnings("unchecked")
    public static Object autoSerializeValue(Object value, Field field, SerializationType serType) {
        Class<?> type = field.getType();
        String entityClass = field.getDeclaringClass().getSimpleName();
        String fieldName = field.getName();

        try {
            // NEW: Handle null collections by creating empty ones for serialization
            if (value == null && CollectionUtils.isCollectionType(type)) {
                Column column = field.getAnnotation(Column.class);
                if (column != null && column.initializeEmpty()) {
                    value = CollectionUtils.createEmptyCollection(field);
                    if (value == null) {
                        return "[]"; // Empty JSON array as fallback
                    }
                }
            }

            if (value == null) return null;

            SerializationType actualType = serType == SerializationType.AUTO ? detectSerializationType(type, field) : serType;

            // Region serialization - ALWAYS use RegionSerializer for Region
            if (type == Region.class || value instanceof Region) {
                try {
                    String result = RegionSerializer.serialize((Region) value);
                    if (result == null || result.trim().isEmpty()) {
                        throw new RuntimeException("RegionSerializer returned null or empty result");
                    }
                    return result;
                } catch (Exception e) {
                    throw new SerializationException("Serialize", entityClass, fieldName, "Region", value,
                            "Failed to serialize Region using RegionSerializer", e);
                }
            }

            // Bukkit-specific types
            else if (type == Location.class) {
                String result = SerializationUtils.serializeLocation((Location) value);
                if (result == null) {
                    throw new RuntimeException("Location serialization returned null");
                }
                return result;
            } else if (type == Component.class) {
                String result = SerializationUtils.serializeComponent((Component) value);
                if (result == null) {
                    throw new RuntimeException("Component serialization returned null");
                }
                return result;
            } else if (type == ItemStack.class) {
                if (actualType == SerializationType.YAML) {
                    return SerializationUtils.serializeItemsToYaml(new ItemStack[]{(ItemStack) value});
                } else {
                    String result = SerializationUtils.serializeItemStack((ItemStack) value);
                    if (result == null) {
                        throw new RuntimeException("ItemStack serialization returned null");
                    }
                    return result;
                }
            } else if (type == ItemStack[].class) {
                if (actualType == SerializationType.YAML) {
                    return SerializationUtils.serializeItemsToYaml((ItemStack[]) value);
                } else {
                    String result = SerializationUtils.serializeItemArray((ItemStack[]) value);
                    if (result == null) {
                        throw new RuntimeException("ItemStack array serialization returned null");
                    }
                    return result;
                }
            }

            // Collections - Enhanced handling
            else if (Collection.class.isAssignableFrom(type)) {
                return handleCollectionSerialization(value, field, actualType);
            }

            // Maps
            else if (Map.class.isAssignableFrom(type)) {
                String result = SerializationUtils.serializeMap((Map<String, Object>) value);
                if (result == null) {
                    throw new RuntimeException("Map serialization returned null");
                }
                return result;
            }

            // Arrays
            else if (type.isArray()) {
                return handleArraySerialization(value, actualType);
            }

            // Complex objects
            else {
                if (actualType == SerializationType.JSON) {
                    try {
                        return SerializationUtils.serializeMap((Map<String, Object>) value);
                    } catch (Exception e) {
                        // Fallback to BASE64
                        return SerializationUtils.serializeObject(value);
                    }
                } else {
                    String result = SerializationUtils.serializeObject(value);
                    if (result == null) {
                        throw new RuntimeException("Object serialization returned null");
                    }
                    return result;
                }
            }

        } catch (Exception e) {
            if (e instanceof SerializationException) {
                throw e;
            } else {
                throw new SerializationException("Serialize", entityClass, fieldName, serType.toString(), value,
                        "Unexpected error during serialization", e);
            }
        }
    }

    /**
     * Automatically deserializes a value and ensures collections are never null
     */
    public static Object autoDeserializeValue(Object serializedValue, Field field, SerializationType serType) {
        if (serializedValue == null) {
            // NEW: Return empty collection instead of null for collection types
            if (CollectionUtils.isCollectionType(field.getType())) {
                Column column = field.getAnnotation(Column.class);
                if (column != null && column.initializeEmpty()) {
                    return CollectionUtils.createEmptyCollection(field);
                }
            }
            return null;
        }

        String stringValue = serializedValue.toString();
        Class<?> type = field.getType();
        String entityClass = field.getDeclaringClass().getSimpleName();
        String fieldName = field.getName();

        try {
            SerializationType actualType = serType == SerializationType.AUTO ? detectSerializationType(type, field) : serType;

            // Region deserialization
            if (type == Region.class) {
                try {
                    Region result = RegionSerializer.deserialize(stringValue);
                    if (result == null) {
                        throw new RuntimeException("RegionSerializer returned null");
                    }
                    return result;
                } catch (Exception e) {
                    throw new SerializationException("Deserialize", entityClass, fieldName, "Region", stringValue,
                            "Failed to deserialize Region using RegionSerializer", e);
                }
            }

            // Bukkit-specific types
            else if (type == Location.class) {
                Location result = SerializationUtils.deserializeLocation(stringValue);
                if (result == null) {
                    throw new RuntimeException("Location deserialization returned null for value: " + stringValue);
                }
                return result;
            } else if (type == Component.class) {
                Component result = SerializationUtils.deserializeComponent(stringValue);
                if (result == null) {
                    throw new RuntimeException("Component deserialization returned null for value: " + stringValue);
                }
                return result;
            } else if (type == ItemStack.class) {
                if (actualType == SerializationType.YAML) {
                    ItemStack[] items = SerializationUtils.deserializeItemsFromYaml(stringValue, 1);
                    return items.length > 0 ? items[0] : null;
                } else {
                    return SerializationUtils.deserializeItemStack(stringValue);
                }
            } else if (type == ItemStack[].class) {
                if (actualType == SerializationType.YAML) {
                    return SerializationUtils.deserializeItemsFromYaml(stringValue, 54);
                } else {
                    return SerializationUtils.deserializeItemArray(stringValue);
                }
            }

            // Collections - Enhanced handling with null safety
            else if (Collection.class.isAssignableFrom(type)) {
                Object result = handleCollectionDeserialization(stringValue, field, actualType);
                // Ensure we never return null for collections
                if (result == null) {
                    Column column = field.getAnnotation(Column.class);
                    if (column != null && column.initializeEmpty()) {
                        return CollectionUtils.createEmptyCollection(field);
                    }
                }
                return result;
            }

            // Maps
            else if (Map.class.isAssignableFrom(type)) {
                Map<String, Object> result = SerializationUtils.deserializeMap(stringValue);
                // Ensure we never return null for maps
                if (result == null) {
                    Column column = field.getAnnotation(Column.class);
                    if (column != null && column.initializeEmpty()) {
                        return CollectionUtils.createEmptyCollection(field);
                    }
                }
                return result;
            }

            // Arrays
            else if (type.isArray()) {
                Object result = handleArrayDeserialization(stringValue, type, actualType);
                // For arrays, return empty array if null
                if (result == null) {
                    return CollectionUtils.createEmptyCollection(field);
                }
                return result;
            }

            // Complex objects
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

        } catch (Exception e) {
            if (e instanceof SerializationException) {
                throw e;
            } else {
                throw new SerializationException("Deserialize", entityClass, fieldName, serType.toString(), stringValue,
                        "Unexpected error during deserialization", e);
            }
        }
    }

    /**
     * Detects the best serialization type for a field automatically
     */
    private static SerializationType detectSerializationType(Class<?> type, Field field) {
        // Region - use JSON for readability and flexibility
        if (type == Region.class) {
            return SerializationType.JSON;
        }

        // Bukkit-specific types
        if (type == Location.class || type == Component.class) {
            return SerializationType.STRING;
        }

        // ItemStacks - use BASE64 by default, but allow YAML
        if (type == ItemStack.class || type == ItemStack[].class) {
            return SerializationType.BASE64;
        }

        // PotionEffects - better in JSON
        if (type == PotionEffect.class || (type.isArray() && type.getComponentType() == PotionEffect.class)) {
            return SerializationType.JSON;
        }

        // Collections and Maps - JSON
        if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            return SerializationType.JSON;
        }

        // Primitive arrays - JSON
        if (type.isArray() && (type.getComponentType().isPrimitive() || type.getComponentType() == String.class)) {
            return SerializationType.JSON;
        }

        // Complex objects - BASE64 for full compatibility
        return SerializationType.BASE64;
    }

    /**
     * Enhanced collection serialization with better null handling
     */
    @SuppressWarnings("unchecked")
    private static String handleCollectionSerialization(Object value, Field field, SerializationType serType) {
        if (value == null) {
            return "[]"; // Return empty JSON array for null collections
        }

        Collection<?> collection = (Collection<?>) value;
        String entityClass = field.getDeclaringClass().getSimpleName();
        String fieldName = field.getName();

        try {
            // Detect generic type
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType paramType) {
                Type[] typeArgs = paramType.getActualTypeArguments();

                if (typeArgs.length > 0) {
                    Class<?> elementType = (Class<?>) typeArgs[0];

                    // String list
                    if (elementType == String.class) {
                        return SerializationUtils.serializeStringList((List<String>) collection);
                    }

                    // PotionEffect list
                    if (elementType == PotionEffect.class) {
                        return SerializationUtils.serializePotionEffectsToJson((List<PotionEffect>) collection);
                    }

                    // Region list
                    if (elementType == Region.class) {
                        List<Region> regions = new ArrayList<>((Collection<Region>) collection);
                        StringBuilder json = new StringBuilder("[");
                        for (int i = 0; i < regions.size(); i++) {
                            if (i > 0) json.append(",");
                            try {
                                String serializedRegion = RegionSerializer.serialize(regions.get(i));
                                if (serializedRegion == null) {
                                    throw new RuntimeException("RegionSerializer returned null for region at index " + i);
                                }
                                json.append(serializedRegion);
                            } catch (Exception e) {
                                throw new SerializationException("Serialize", entityClass, fieldName, "List<Region>",
                                        regions.get(i), "Failed to serialize region at index " + i, e);
                            }
                        }
                        json.append("]");
                        return json.toString();
                    }
                }
            }

            // Generic fallback
            return SerializationUtils.serializeObject(value);

        } catch (Exception e) {
            if (e instanceof SerializationException) {
                throw e;
            } else {
                throw new SerializationException("Serialize", entityClass, fieldName, "Collection", value,
                        "Failed to serialize collection", e);
            }
        }
    }

    /**
     * Enhanced collection deserialization with automatic empty collection creation
     */
    private static Object handleCollectionDeserialization(String stringValue, Field field, SerializationType serType) {
        String entityClass = field.getDeclaringClass().getSimpleName();
        String fieldName = field.getName();

        try {
            // Handle empty or null string values
            if (stringValue == null || stringValue.trim().isEmpty() || "[]".equals(stringValue.trim())) {
                return CollectionUtils.createEmptyCollection(field);
            }

            // Detect generic type
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType paramType) {
                Type[] typeArgs = paramType.getActualTypeArguments();

                if (typeArgs.length > 0) {
                    Class<?> elementType = (Class<?>) typeArgs[0];

                    // String list
                    if (elementType == String.class) {
                        List<String> result = SerializationUtils.deserializeStringList(stringValue);
                        if (result == null) {
                            return CollectionUtils.createEmptyCollection(field);
                        }
                        // Convert to proper collection type
                        if (Set.class.isAssignableFrom(field.getType())) {
                            return new HashSet<>(result);
                        }
                        return result;
                    }

                    // PotionEffect list
                    if (elementType == PotionEffect.class) {
                        List<PotionEffect> result = SerializationUtils.deserializePotionEffectsFromJson(stringValue);
                        if (result == null) {
                            return CollectionUtils.createEmptyCollection(field);
                        }
                        // Convert to proper collection type
                        if (Set.class.isAssignableFrom(field.getType())) {
                            return new HashSet<>(result);
                        }
                        return result;
                    }

                    // Region list
                    if (elementType == Region.class) {
                        try {
                            Collection<Region> regions;

                            if (Set.class.isAssignableFrom(field.getType())) {
                                regions = new HashSet<>();
                            } else {
                                regions = new ArrayList<>();
                            }

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

                                            try {
                                                Region region = RegionSerializer.deserialize(regionJson);
                                                if (region != null) {
                                                    regions.add(region);
                                                }
                                            } catch (Exception e) {
                                                throw new SerializationException("Deserialize", entityClass, fieldName,
                                                        "List<Region>", regionJson, "Failed to deserialize region from JSON", e);
                                            }

                                            currentRegion.setLength(0);
                                        }
                                    }
                                }
                            }
                            return regions;
                        } catch (Exception e) {
                            // Fallback to empty collection
                            return CollectionUtils.createEmptyCollection(field);
                        }
                    }
                }
            }

            // Generic fallback
            Object result = SerializationUtils.deserializeObject(stringValue, field.getType());
            if (result == null) {
                return CollectionUtils.createEmptyCollection(field);
            }
            return result;

        } catch (Exception e) {
            if (e instanceof SerializationException) {
                throw e;
            } else {
                // Always return empty collection on error instead of null
                return CollectionUtils.createEmptyCollection(field);
            }
        }
    }

    /**
     * Handles array serialization with enhanced error handling
     */
    private static String handleArraySerialization(Object value, SerializationType serType) {
        Class<?> componentType = value.getClass().getComponentType();

        try {
            if (componentType == PotionEffect.class) {
                PotionEffect[] effects = (PotionEffect[]) value;
                return SerializationUtils.serializePotionEffectsToJson(List.of(effects));
            }

            // Region array
            if (componentType == Region.class) {
                Region[] regions = (Region[]) value;
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < regions.length; i++) {
                    if (i > 0) json.append(",");
                    try {
                        String serializedRegion = RegionSerializer.serialize(regions[i]);
                        if (serializedRegion == null) {
                            throw new RuntimeException("RegionSerializer returned null for region at index " + i);
                        }
                        json.append(serializedRegion);
                    } catch (Exception e) {
                        throw new SerializationException("Serialize", "Unknown", "arrayField", "Region[]",
                                regions[i], "Failed to serialize region array at index " + i, e);
                    }
                }
                json.append("]");
                return json.toString();
            }

            // Primitive arrays and others - use object serialization
            return SerializationUtils.serializeObject(value);

        } catch (Exception e) {
            if (e instanceof SerializationException) {
                throw e;
            } else {
                throw new SerializationException("Serialize", "Unknown", "arrayField", componentType.getSimpleName() + "[]",
                        value, "Failed to serialize array", e);
            }
        }
    }

    /**
     * Handles array deserialization with enhanced error handling
     */
    private static Object handleArrayDeserialization(String stringValue, Class<?> arrayType, SerializationType serType) {
        Class<?> componentType = arrayType.getComponentType();

        try {
            if (componentType == PotionEffect.class) {
                List<PotionEffect> effects = SerializationUtils.deserializePotionEffectsFromJson(stringValue);
                return effects.toArray(new PotionEffect[0]);
            }

            // Region array (similar to List<Region> deserialization)
            if (componentType == Region.class) {
                try {
                    List<Region> regions = new ArrayList<>();
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

                                    try {
                                        Region region = RegionSerializer.deserialize(regionJson);
                                        if (region != null) {
                                            regions.add(region);
                                        }
                                    } catch (Exception e) {
                                        throw new SerializationException("Deserialize", "Unknown", "arrayField",
                                                "Region[]", regionJson, "Failed to deserialize region array from JSON", e);
                                    }

                                    currentRegion.setLength(0);
                                }
                            }
                        }
                    }
                    return regions.toArray(new Region[0]);
                } catch (Exception e) {
                    return new Region[0]; // Return empty array on error
                }
            }

            // Primitive arrays and others
            return SerializationUtils.deserializeObject(stringValue, arrayType);

        } catch (Exception e) {
            if (e instanceof SerializationException) {
                throw e;
            } else {
                // Return empty array on error
                return java.lang.reflect.Array.newInstance(componentType, 0);
            }
        }
    }

    /**
     * Checks if a type needs auto-serialization
     */
    public static boolean needsAutoSerialization(Class<?> type) {
        return type == Location.class ||
                type == Component.class ||
                type == ItemStack.class ||
                type == ItemStack[].class ||
                type == PotionEffect.class ||
                type == PotionEffect[].class ||
                type == Region.class ||
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

    /**
     * Creates a detailed error message for serialization issues
     */
    public static String createDetailedErrorMessage(String operation, String entityClass, String fieldName,
                                                    String serializationType, Object value, String message, Exception cause) {
        StringBuilder sb = new StringBuilder();
        sb.append("Serialization Error Details:\n");
        sb.append("  Operation: ").append(operation).append("\n");
        sb.append("  Entity Class: ").append(entityClass).append("\n");
        sb.append("  Field Name: ").append(fieldName).append("\n");
        sb.append("  Serialization Type: ").append(serializationType).append("\n");
        sb.append("  Value Type: ").append(value != null ? value.getClass().getSimpleName() : "null").append("\n");
        sb.append("  Value: ").append(value != null ? value.toString() : "null").append("\n");
        sb.append("  Error Message: ").append(message).append("\n");

        if (cause != null) {
            sb.append("  Root Cause: ").append(cause.getClass().getSimpleName())
                    .append(" - ").append(cause.getMessage()).append("\n");
        }

        return sb.toString();
    }
}