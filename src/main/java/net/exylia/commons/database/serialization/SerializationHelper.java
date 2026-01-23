package net.exylia.commons.database.serialization;

import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.annotations.SerializationType;
import net.exylia.commons.database.exceptions.SerializationException;
import net.exylia.commons.region.model.Region;
import net.exylia.commons.region.serialization.RegionSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.lang.reflect.*;
import java.util.*;

@Deprecated
public class SerializationHelper {

    @SuppressWarnings("unchecked")
    public static Object autoSerializeValue(Object value, Field field, SerializationType serType) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Class<?> type = field.getType();
        String entityClass = field.getDeclaringClass().getSimpleName();
        String fieldName = field.getName();

        try {
             
            if (value == null && CollectionUtils.isCollectionType(type)) {
                Column column = field.getAnnotation(Column.class);
                if (column != null && column.initializeEmpty()) {
                    value = CollectionUtils.createEmptyCollection(field);
                    if (value == null) {
                        return "[]";  
                    }
                }
            }

            if (value == null) return null;

            SerializationType actualType = serType == SerializationType.AUTO ? detectSerializationType(type, field) : serType;

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

            else if (Collection.class.isAssignableFrom(type)) {
                return handleCollectionSerialization(value, field, actualType);
            }

            else if (Map.class.isAssignableFrom(type)) {
                String result = SerializationUtils.serializeMap((Map<String, Object>) value);
                if (result == null) {
                    throw new RuntimeException("Map serialization returned null");
                }
                return result;
            }

            else if (type.isArray()) {
                return handleArraySerialization(value, actualType);
            } else if (actualType == SerializationType.STRING && !type.isPrimitive() && type != String.class) {
                try {
                    String result;

                    if (value instanceof Keyed) {
                        result = ((Keyed) value).getKey().toString();
                    }
                     
                    else if (value.getClass().isEnum()) {
                        result = ((Enum<?>) value).name();
                    }
                     
                    else if (hasMethod(value.getClass(), "name")) {
                        Method nameMethod = value.getClass().getMethod("name");
                        Object nameResult = nameMethod.invoke(value);
                        result = nameResult != null ? nameResult.toString() : null;
                    }
                     
                    else if (hasMethod(value.getClass(), "getName")) {
                        Method getNameMethod = value.getClass().getMethod("getName");
                        Object nameResult = getNameMethod.invoke(value);
                        result = nameResult != null ? nameResult.toString() : null;
                    }
                     
                    else {
                        result = value.toString();
                    }

                    if (result == null || result.trim().isEmpty()) {
                        throw new RuntimeException("Generic object serialization returned null or empty result");
                    }
                    return result;

                } catch (Exception e) {
                    if (e instanceof SerializationException) {
                        throw e;
                    } else {
                        throw new SerializationException("Serialize", entityClass, fieldName, "Generic", value,
                                "Failed to serialize object as string", e);
                    }
                }
            }

            else {
                if (actualType == SerializationType.JSON) {
                    try {
                        return SerializationUtils.serializeMap((Map<String, Object>) value);
                    } catch (Exception e) {
                         
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

    public static Object autoDeserializeValue(Object serializedValue, Field field, SerializationType serType) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        if (serializedValue == null) {
             
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

            else if (Collection.class.isAssignableFrom(type)) {
                Object result = handleCollectionDeserialization(stringValue, field, actualType);
                 
                if (result == null) {
                    Column column = field.getAnnotation(Column.class);
                    if (column != null && column.initializeEmpty()) {
                        return CollectionUtils.createEmptyCollection(field);
                    }
                }
                return result;
            }

            else if (Map.class.isAssignableFrom(type)) {
                Map<String, Object> result = SerializationUtils.deserializeMap(stringValue);
                 
                if (result == null) {
                    Column column = field.getAnnotation(Column.class);
                    if (column != null && column.initializeEmpty()) {
                        return CollectionUtils.createEmptyCollection(field);
                    }
                }
                return result;
            }

            else if (type.isArray()) {
                Object result = handleArrayDeserialization(stringValue, type, actualType);
                 
                if (result == null) {
                    return CollectionUtils.createEmptyCollection(field);
                }
                return result;
            }

            else if (actualType == SerializationType.STRING && !type.isPrimitive() && type != String.class) {
                try {
                     
                    if (Keyed.class.isAssignableFrom(type)) {
                        return deserializeKeyedObject(type, stringValue, entityClass, fieldName);
                    }
                     
                    else if (type.isEnum()) {
                        @SuppressWarnings("unchecked")
                        Class<Enum> enumClass = (Class<Enum>) type;
                        return Enum.valueOf(enumClass, stringValue);
                    }
                     
                    else if (hasMethod(type, "valueOf", String.class)) {
                        Method valueOfMethod = type.getMethod("valueOf", String.class);
                        if (java.lang.reflect.Modifier.isStatic(valueOfMethod.getModifiers())) {
                            return valueOfMethod.invoke(null, stringValue);
                        }
                    }
                     
                    else if (hasMethod(type, "getByName", String.class)) {
                        Method getByNameMethod = type.getMethod("getByName", String.class);
                        if (java.lang.reflect.Modifier.isStatic(getByNameMethod.getModifiers())) {
                            return getByNameMethod.invoke(null, stringValue);
                        }
                    }
                     
                    else {
                        try {
                            return type.getConstructor(String.class).newInstance(stringValue);
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException("No suitable deserialization method found for type: " + type.getSimpleName());
                        }
                    }

                    return null;

                } catch (Exception e) {
                    if (e instanceof SerializationException) {
                        throw e;
                    } else {
                        throw new SerializationException("Deserialize", entityClass, fieldName, "Generic", stringValue,
                                "Failed to deserialize object from string", e);
                    }
                }
            }

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

    private static SerializationType detectSerializationType(Class<?> type, Field field) {
         
        if (type == Region.class) {
            return SerializationType.JSON;
        }

        if (type.isEnum()) {
            return SerializationType.STRING;
        }

        if (type == Location.class || type == Component.class) {
            return SerializationType.STRING;
        }

        if (type == ItemStack.class || type == ItemStack[].class) {
            return SerializationType.BASE64;
        }

        if (type == PotionEffect.class || (type.isArray() && type.getComponentType() == PotionEffect.class)) {
            return SerializationType.JSON;
        }

        if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            return SerializationType.JSON;
        }

        if (type.isArray() && (type.getComponentType().isPrimitive() || type.getComponentType() == String.class)) {
            return SerializationType.JSON;
        }

        return SerializationType.BASE64;
    }

    @SuppressWarnings("unchecked")
    private static String handleCollectionSerialization(Object value, Field field, SerializationType serType) {
        if (value == null) {
            return "[]";  
        }

        Collection<?> collection = (Collection<?>) value;
        String entityClass = field.getDeclaringClass().getSimpleName();
        String fieldName = field.getName();

        try {
             
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType paramType) {
                Type[] typeArgs = paramType.getActualTypeArguments();

                if (typeArgs.length > 0) {
                    Class<?> elementType = (Class<?>) typeArgs[0];

                    if (elementType == String.class) {
                        return SerializationUtils.serializeStringList((List<String>) collection);
                    }

                    if (elementType == PotionEffect.class) {
                        return SerializationUtils.serializePotionEffectsToJson((List<PotionEffect>) collection);
                    }

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

    private static Object handleCollectionDeserialization(String stringValue, Field field, SerializationType serType) {
        String entityClass = field.getDeclaringClass().getSimpleName();
        String fieldName = field.getName();

        try {
             
            if (stringValue == null || stringValue.trim().isEmpty() || "[]".equals(stringValue.trim())) {
                return CollectionUtils.createEmptyCollection(field);
            }

            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType paramType) {
                Type[] typeArgs = paramType.getActualTypeArguments();

                if (typeArgs.length > 0) {
                    Class<?> elementType = (Class<?>) typeArgs[0];

                    if (elementType == String.class) {
                        List<String> result = SerializationUtils.deserializeStringList(stringValue);
                        if (result == null) {
                            return CollectionUtils.createEmptyCollection(field);
                        }
                         
                        if (Set.class.isAssignableFrom(field.getType())) {
                            return new HashSet<>(result);
                        }
                        return result;
                    }

                    if (elementType == PotionEffect.class) {
                        List<PotionEffect> result = SerializationUtils.deserializePotionEffectsFromJson(stringValue);
                        if (result == null) {
                            return CollectionUtils.createEmptyCollection(field);
                        }
                         
                        if (Set.class.isAssignableFrom(field.getType())) {
                            return new HashSet<>(result);
                        }
                        return result;
                    }

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
                             
                            return CollectionUtils.createEmptyCollection(field);
                        }
                    }
                }
            }

            Object result = SerializationUtils.deserializeObject(stringValue, field.getType());
            if (result == null) {
                return CollectionUtils.createEmptyCollection(field);
            }
            return result;

        } catch (Exception e) {
            if (e instanceof SerializationException) {
                throw e;
            } else {
                 
                return CollectionUtils.createEmptyCollection(field);
            }
        }
    }

    private static String handleArraySerialization(Object value, SerializationType serType) {
        Class<?> componentType = value.getClass().getComponentType();

        try {
            if (componentType == PotionEffect.class) {
                PotionEffect[] effects = (PotionEffect[]) value;
                return SerializationUtils.serializePotionEffectsToJson(List.of(effects));
            }

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

    private static Object handleArrayDeserialization(String stringValue, Class<?> arrayType, SerializationType serType) {
        Class<?> componentType = arrayType.getComponentType();

        try {
            if (componentType == PotionEffect.class) {
                List<PotionEffect> effects = SerializationUtils.deserializePotionEffectsFromJson(stringValue);
                return effects.toArray(new PotionEffect[0]);
            }

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
                    return new Region[0];  
                }
            }

            return SerializationUtils.deserializeObject(stringValue, arrayType);

        } catch (Exception e) {
            if (e instanceof SerializationException) {
                throw e;
            } else {
                 
                return java.lang.reflect.Array.newInstance(componentType, 0);
            }
        }
    }

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

    private static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            clazz.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Object deserializeKeyedObject(Class<?> type, String keyString, String entityClass, String fieldName) {
        try {
            NamespacedKey key = NamespacedKey.fromString(keyString);
            if (key == null) {
                throw new RuntimeException("Invalid NamespacedKey format: " + keyString);
            }

            java.lang.reflect.Field[] fields = type.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.getType() == type &&
                        java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                        java.lang.reflect.Modifier.isPublic(field.getModifiers())) {

                    try {
                        Object constant = field.get(null);
                        if (constant instanceof Keyed &&
                                ((Keyed) constant).getKey().equals(key)) {
                            return constant;
                        }
                    } catch (Exception e) {
                         
                    }
                }
            }

            throw new RuntimeException("Keyed object not found for key: " + keyString);

        } catch (Exception e) {
            throw new SerializationException("Deserialize", entityClass, fieldName, "Keyed", keyString,
                    "Failed to deserialize Keyed object", e);
        }
    }
}
