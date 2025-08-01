package net.exylia.commons.database.serialization;

import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.exceptions.DatabaseErrorHandler;

import java.lang.reflect.Field;

public class EnumSafetyHandler {

    /**
     * Maneja la deserialización segura de enums, retornando null o valor por defecto
     * cuando el valor no existe en el enum actual
     */
    @SuppressWarnings("unchecked")
    public static Object handleEnumDeserialization(Object value, Field field,
                                                   String entityClassName, String fieldName,
                                                   DatabaseErrorHandler errorHandler) {
        if (!field.getType().isEnum() || !(value instanceof String)) {
            return value;
        }

        Class<Enum> enumClass = (Class<Enum>) field.getType();
        String stringValue = (String) value;

        try {
            return Enum.valueOf(enumClass, stringValue);
        } catch (IllegalArgumentException e) {
            // El valor del enum no existe más
            Column column = field.getAnnotation(Column.class);

            if (column != null && !column.nullable()) {
                // Campo no nullable - usar valor por defecto
                Enum<?>[] enumConstants = enumClass.getEnumConstants();
                if (enumConstants.length > 0) {
                    Object defaultValue = enumConstants[0];
                    errorHandler.logWarning("EnumDeserialization", entityClassName,
                            String.format("Enum value '%s' not found for field '%s', using default: %s",
                                    stringValue, fieldName, defaultValue));
                    return defaultValue;
                }
            }

            // Campo nullable o no hay constantes - usar null
            errorHandler.logWarning("EnumDeserialization", entityClassName,
                    String.format("Enum value '%s' not found for field '%s', setting to null",
                            stringValue, fieldName));
            return null;
        }
    }
}