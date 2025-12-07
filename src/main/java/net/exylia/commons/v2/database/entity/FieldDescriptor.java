package net.exylia.commons.v2.database.entity;

import com.google.gson.Gson;
import lombok.Getter;
import net.exylia.commons.v2.database.annotation.Column;
import net.exylia.commons.v2.database.annotation.SerializationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.Base64;

@Getter
public class FieldDescriptor {

    private final String fieldName;
    private final String columnName;
    private final Field field;
    private final Class<?> type;

    private final boolean primaryKey;
    private final boolean autoIncrement;
    private final boolean nullable;
    private final boolean unique;
    private final int length;
    private final String defaultValue;
    private final boolean autoSerialize;
    private final SerializationType serializationType;
    private final boolean initializeEmpty;

    public FieldDescriptor(Field field, Column column) {
        this.field = field;
        this.fieldName = field.getName();
        this.type = field.getType();

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

        field.setAccessible(true);
    }

    public Object getValue(Entity entity) {
        try {
            Object value = field.get(entity);

            if (autoSerialize && value != null) {
                return serializeValue(value);
            }

            return value;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field " + fieldName, e);
        }
    }

    private String serializeValue(Object value) {
        if (value == null) {
            return null;
        }

        if (serializationType == SerializationType.BASE64) {
            if (type.isArray() && type.getComponentType() == ItemStack.class) {
                return serializeItemStackArray((ItemStack[]) value);
            }
        }

        if (serializationType == SerializationType.JSON) {
            return new Gson().toJson(value);
        }

        if (serializationType == SerializationType.AUTO) {
            if (type.isArray() && type.getComponentType() == ItemStack.class) {
                return serializeItemStackArray((ItemStack[]) value);
            }
        }

        return value.toString();
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

    private Object deserializeValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        if (serializationType == SerializationType.BASE64) {
            if (type.isArray() && type.getComponentType() == ItemStack.class) {
                return deserializeItemStackArray(value);
            }
        }

        if (serializationType == SerializationType.JSON) {
            return new Gson().fromJson(value, type);
        }

        if (serializationType == SerializationType.AUTO) {
            if (type.isArray() && type.getComponentType() == ItemStack.class) {
                return deserializeItemStackArray(value);
            }
        }

        return value;
    }

    private ItemStack[] deserializeItemStackArray(String value) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(value));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
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
