package net.exylia.commons.databaseV2.entity;

import lombok.Getter;
import net.exylia.commons.databaseV2.annotation.Column;
import net.exylia.commons.databaseV2.annotation.SerializationType;

import java.lang.reflect.Field;

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
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field " + fieldName, e);
        }
    }

    public void setValue(Entity entity, Object value) {
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot set field " + fieldName, e);
        }
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
