package net.exylia.commons.database.serialization;

import net.exylia.commons.database.annotations.SerializationType;
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

        // Tipos específicos de Bukkit
        if (type == Location.class) {
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

        // Tipos específicos de Bukkit
        if (type == Location.class) {
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