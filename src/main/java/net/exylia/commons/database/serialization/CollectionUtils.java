package net.exylia.commons.database.serialization;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for initializing empty collections and handling collection defaults
 */
public class CollectionUtils {

    /**
     * Checks if a field type is a collection that should be initialized as empty
     */
    public static boolean isCollectionType(Class<?> type) {
        return Collection.class.isAssignableFrom(type) ||
                Map.class.isAssignableFrom(type) ||
                type.isArray();
    }

    /**
     * Creates an empty instance of the appropriate collection type
     */
    public static Object createEmptyCollection(Field field) {
        Class<?> fieldType = field.getType();

        // Handle Sets
        if (Set.class.isAssignableFrom(fieldType)) {
            if (fieldType.isInterface() || fieldType == Set.class) {
                return new HashSet<>(); // Default implementation
            }
            if (fieldType == LinkedHashSet.class) {
                return new LinkedHashSet<>();
            }
            if (fieldType == TreeSet.class) {
                return new TreeSet<>();
            }
            // For other concrete Set implementations, try to instantiate
            try {
                return fieldType.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return new HashSet<>(); // Fallback
            }
        }

        // Handle Lists
        if (List.class.isAssignableFrom(fieldType)) {
            if (fieldType.isInterface() || fieldType == List.class) {
                return new ArrayList<>(); // Default implementation
            }
            if (fieldType == LinkedList.class) {
                return new LinkedList<>();
            }
            if (fieldType == Vector.class) {
                return new Vector<>();
            }
            // For other concrete List implementations, try to instantiate
            try {
                return fieldType.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return new ArrayList<>(); // Fallback
            }
        }

        // Handle Maps
        if (Map.class.isAssignableFrom(fieldType)) {
            if (fieldType.isInterface() || fieldType == Map.class) {
                return new HashMap<>(); // Default implementation
            }
            if (fieldType == LinkedHashMap.class) {
                return new LinkedHashMap<>();
            }
            if (fieldType == TreeMap.class) {
                return new TreeMap<>();
            }
            if (fieldType == ConcurrentHashMap.class) {
                return new ConcurrentHashMap<>();
            }
            // For other concrete Map implementations, try to instantiate
            try {
                return fieldType.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return new HashMap<>(); // Fallback
            }
        }

        // Handle Collections (general)
        if (Collection.class.isAssignableFrom(fieldType)) {
            if (fieldType.isInterface() || fieldType == Collection.class) {
                return new ArrayList<>(); // Default implementation
            }
            // Try to instantiate the specific collection type
            try {
                return fieldType.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return new ArrayList<>(); // Fallback
            }
        }

        // Handle Arrays
        if (fieldType.isArray()) {
            Class<?> componentType = fieldType.getComponentType();
            return java.lang.reflect.Array.newInstance(componentType, 0);
        }

        return null; // Not a collection type
    }

    /**
     * Gets the element type of a collection field using generics
     */
    public static Class<?> getCollectionElementType(Field field) {
        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }

        return Object.class; // Fallback
    }

    /**
     * Gets the key and value types for Map fields
     */
    public static Class<?>[] getMapTypes(Field field) {
        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length >= 2) {
                Class<?> keyType = typeArgs[0] instanceof Class ? (Class<?>) typeArgs[0] : Object.class;
                Class<?> valueType = typeArgs[1] instanceof Class ? (Class<?>) typeArgs[1] : Object.class;
                return new Class<?>[] { keyType, valueType };
            }
        }

        return new Class<?>[] { Object.class, Object.class }; // Fallback
    }

    /**
     * Checks if a collection is null or empty
     */
    public static boolean isNullOrEmpty(Object collection) {
        if (collection == null) return true;

        if (collection instanceof Collection) {
            return ((Collection<?>) collection).isEmpty();
        }
        if (collection instanceof Map) {
            return ((Map<?, ?>) collection).isEmpty();
        }
        if (collection.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(collection) == 0;
        }

        return false;
    }

    /**
     * Gets the size of a collection, map, or array safely
     */
    public static int getSize(Object collection) {
        if (collection == null) return 0;

        if (collection instanceof Collection) {
            return ((Collection<?>) collection).size();
        }
        if (collection instanceof Map) {
            return ((Map<?, ?>) collection).size();
        }
        if (collection.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(collection);
        }

        return 0;
    }

    /**
     * Creates a default empty collection for serialization when the field is null
     */
    public static Object getDefaultForSerialization(Field field) {
        if (isCollectionType(field.getType())) {
            return createEmptyCollection(field);
        }
        return null;
    }
}