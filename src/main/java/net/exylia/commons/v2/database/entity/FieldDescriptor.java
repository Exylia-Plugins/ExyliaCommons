package net.exylia.commons.v2.database.entity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import net.exylia.commons.v2.database.annotation.Column;
import net.exylia.commons.v2.database.annotation.SerializationType;
import net.exylia.commons.v2.database.serialization.SerializationRegistry;
import net.exylia.commons.v2.utils.SlugUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;

@Getter
public class FieldDescriptor {

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = Logger.getLogger(FieldDescriptor.class.getName());

    private final String fieldName;
    private final String columnName;
    private final Field field;
    private final Class<?> type;
    private final Class<?> elementType;

    private final boolean primaryKey;
    private final boolean autoIncrement;
    private final boolean nullable;
    private final boolean unique;
    private final int length;
    private final String defaultValue;
    private final boolean autoSerialize;
    private final SerializationType serializationType;
    private final boolean initializeEmpty;
    private final boolean sanitize;

    public FieldDescriptor(Field field, Column column) {
        this.field = field;
        this.fieldName = field.getName();
        this.type = field.getType();
        this.elementType = extractElementType(field);

        String colName = column.name();
        this.columnName = colName.isEmpty() ? fieldName : colName;

        this.primaryKey = column.primaryKey();
        this.autoIncrement = column.autoIncrement();
        this.nullable = column.nullable();
        this.unique = column.unique();
        this.length = column.length();
        this.defaultValue = column.defaultValue();
        this.autoSerialize = column.autoSerialize();
        this.serializationType = column.serializationType();
        this.initializeEmpty = column.initializeEmpty();
        this.sanitize = column.sanitize();

        field.setAccessible(true);
    }

    private Class<?> extractElementType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        return null;
    }

    private boolean isCollection() {
        return Collection.class.isAssignableFrom(type);
    }

    public Object getValue(Entity entity) {
        try {
            Object value = field.get(entity);

            if (sanitize && value instanceof String) {
                value = SlugUtils.sanitize((String) value).orElse((String) value);
            }

            if (autoSerialize && value != null) {
                return serializeValue(value);
            }

            return value;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field " + fieldName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private String serializeValue(Object value) {
        if (value == null) {
            return null;
        }

        if (isCollection() && elementType != null) {
            return serializeCollection((Collection<?>) value);
        }

        if (SerializationRegistry.getInstance().hasSerializer(type)) {
            return SerializationRegistry.getInstance().serialize(value, (Class) type);
        }

        if (serializationType == SerializationType.BASE64) {
            if (type.isArray() && type.getComponentType() == ItemStack.class) {
                return serializeItemStackArray((ItemStack[]) value);
            }
        }

        if (serializationType == SerializationType.JSON) {
            return GSON.toJson(value);
        }

        if (serializationType == SerializationType.AUTO) {
            if (type.isArray() && type.getComponentType() == ItemStack.class) {
                return serializeItemStackArray((ItemStack[]) value);
            }
            if (!type.isPrimitive() && !type.isEnum() && type != String.class) {
                return GSON.toJson(value);
            }
        }

        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private String serializeCollection(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }

        SerializationRegistry registry = SerializationRegistry.getInstance();
        if (!registry.hasSerializer(elementType)) {
            return GSON.toJson(collection);
        }

        JsonArray jsonArray = new JsonArray();
        for (Object element : collection) {
            String serialized = registry.serialize(element, (Class) elementType);
            if (serialized != null) {
                jsonArray.add(serialized);
            }
        }
        return GSON.toJson(jsonArray);
    }

    private String serializeItemStackArray(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error serializing ItemStack array", e);
        }
    }

    public void setValue(Entity entity, Object value) {
        try {
            if (value == null) {
                field.set(entity, null);
                return;
            }

            if (sanitize && value instanceof String) {
                value = SlugUtils.sanitize((String) value).orElse((String) value);
            }

            if (autoSerialize && value instanceof String) {
                Object deserialized = deserializeValue((String) value);
                field.set(entity, deserialized);
                return;
            }

            Object convertedValue = convertValue(value);
            field.set(entity, convertedValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot set field " + fieldName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object deserializeValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        if (isCollection() && elementType != null) {
            return deserializeCollection(value);
        }

        if (SerializationRegistry.getInstance().hasDeserializer(type)) {
            return SerializationRegistry.getInstance().deserialize(value, (Class) type);
        }

        if (serializationType == SerializationType.BASE64) {
            if (type.isArray() && type.getComponentType() == ItemStack.class) {
                return deserializeItemStackArray(value);
            }
        }

        if (serializationType == SerializationType.JSON) {
            return GSON.fromJson(value, type);
        }

        if (serializationType == SerializationType.AUTO) {
            if (type.isArray() && type.getComponentType() == ItemStack.class) {
                return deserializeItemStackArray(value);
            }
            if (!type.isPrimitive() && !type.isEnum() && type != String.class) {
                try {
                    return GSON.fromJson(value, type);
                } catch (Exception e) {
                    return null;
                }
            }
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    private Collection<?> deserializeCollection(String value) {
        SerializationRegistry registry = SerializationRegistry.getInstance();

        if (!registry.hasDeserializer(elementType)) {
            return GSON.fromJson(value, (Type) type);
        }

        Collection<Object> collection = createCollectionInstance();

        try {
            JsonArray jsonArray = GSON.fromJson(value, JsonArray.class);
            for (JsonElement element : jsonArray) {
                String elementStr = element.getAsString();
                Object deserialized = registry.deserialize(elementStr, (Class) elementType);
                if (deserialized != null) {
                    collection.add(deserialized);
                }
            }
        } catch (Exception e) {
            return collection;
        }

        return collection;
    }

    private Collection<Object> createCollectionInstance() {
        if (List.class.isAssignableFrom(type)) {
            return new ArrayList<>();
        } else if (Set.class.isAssignableFrom(type)) {
            return new HashSet<>();
        } else {
            return new ArrayList<>();
        }
    }

    private ItemStack[] deserializeItemStackArray(String value) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(value));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                try {
                    items[i] = (ItemStack) dataInput.readObject();
                } catch (Exception e) {
                    LOGGER.warning("[ExyliaCommons] Failed to deserialize ItemStack at index " + i
                            + " in field '" + fieldName + "' — using STONE as fallback. Cause: " + e.getMessage());
                    items[i] = new ItemStack(org.bukkit.Material.STONE);
                }
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing ItemStack array", e);
        }
    }

    private Object convertValue(Object value) {
        if (value == null) {
            return null;
        }

        if (type == java.util.UUID.class && value instanceof String) {
            return java.util.UUID.fromString((String) value);
        }

        if (type == String.class && value instanceof java.util.UUID) {
            return value.toString();
        }

        if (type.isEnum() && value instanceof String) {
            return Enum.valueOf((Class<Enum>) type, (String) value);
        }

        if ((type == int.class || type == Integer.class) && value instanceof Number) {
            return ((Number) value).intValue();
        }

        if ((type == long.class || type == Long.class) && value instanceof Number) {
            return ((Number) value).longValue();
        }

        if ((type == double.class || type == Double.class) && value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if ((type == float.class || type == Float.class) && value instanceof Number) {
            return ((Number) value).floatValue();
        }

        if ((type == boolean.class || type == Boolean.class) && value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }

        if (type == java.math.BigDecimal.class) {
            if (value instanceof java.math.BigDecimal) return value;
            if (value instanceof Number) return new java.math.BigDecimal(value.toString());
            if (value instanceof String) return new java.math.BigDecimal((String) value);
        }

        if (type == java.math.BigInteger.class) {
            if (value instanceof java.math.BigInteger) return value;
            if (value instanceof Number) return java.math.BigInteger.valueOf(((Number) value).longValue());
            if (value instanceof String) return new java.math.BigInteger((String) value);
        }

        return value;
    }

    public boolean isNumeric() {
        return type == int.class || type == Integer.class ||
                type == long.class || type == Long.class ||
                type == double.class || type == Double.class ||
                type == float.class || type == Float.class ||
                type == short.class || type == Short.class ||
                type == byte.class || type == Byte.class;
    }

    public boolean isBoolean() {
        return type == boolean.class || type == Boolean.class;
    }

    public boolean isString() {
        return type == String.class;
    }

    public boolean isDateTime() {
        return type == long.class || type == Long.class ||
                type == java.util.Date.class ||
                type == java.time.LocalDateTime.class;
    }
}
