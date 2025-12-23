package net.exylia.commons.v2.yaml.util;

import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.entity.FieldDescriptor;
import net.exylia.commons.v2.yaml.exception.YamlParseException;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class YamlSerializer {

    public static Map<String, Object> entityToYaml(Entity entity, EntityMetadata metadata) {
        Map<String, Object> yamlData = new HashMap<>();

        for (FieldDescriptor field : metadata.getFields()) {
            Object value = field.getValue(entity);
            if (value != null) {
                yamlData.put(field.getColumnName(), value);
            }
        }

        return yamlData;
    }

    public static <T extends Entity> T yamlToEntity(Map<String, Object> yamlData, Class<T> entityClass, EntityMetadata metadata) {
        try {
            Constructor<T> constructor = entityClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T entity = constructor.newInstance();

            for (FieldDescriptor field : metadata.getFields()) {
                Object value = yamlData.get(field.getColumnName());
                if (value != null) {
                    field.setValue(entity, value);
                } else if (field.isInitializeEmpty()) {
                    field.setValue(entity, null);
                }
            }

            return entity;

        } catch (Exception e) {
            throw new YamlParseException("Failed to deserialize entity from YAML: " + entityClass.getName(), e);
        }
    }
}
