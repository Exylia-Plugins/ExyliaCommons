package net.exylia.commons.v2.database.entity;

import lombok.Getter;
import net.exylia.commons.v2.database.annotation.Column;
import net.exylia.commons.v2.database.annotation.Index;
import net.exylia.commons.v2.database.annotation.Table;

import java.lang.reflect.Field;
import java.util.*;

@Getter
public class EntityMetadata {

    private final Class<?> entityClass;
    private final String tableName;
    private final String version;
    private final List<FieldDescriptor> fields;
    private final Map<String, FieldDescriptor> fieldsByName;
    private final FieldDescriptor primaryKeyField;
    private final List<FieldDescriptor> indexedFields;
    private final List<Index> indexes;

    public EntityMetadata(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Table.class)) {
            throw new IllegalArgumentException("Class " + entityClass.getName() + " is not annotated with @Table");
        }

        this.entityClass = entityClass;
        Table table = entityClass.getAnnotation(Table.class);
        this.tableName = table.name();
        this.version = table.version();
        this.fields = new ArrayList<>();
        this.fieldsByName = new HashMap<>();
        this.indexedFields = new ArrayList<>();

        scanFields();

        FieldDescriptor primaryKey = null;
        for (FieldDescriptor field : fields) {
            if (field.isPrimaryKey()) {
                if (primaryKey != null) {
                    throw new IllegalArgumentException("Multiple primary keys in " + entityClass.getName());
                }
                primaryKey = field;
            }
            if (!field.getColumnName().equals("created_at") && !field.getColumnName().equals("updated_at")) {
                fieldsByName.put(field.getFieldName(), field);
            }
        }
        this.primaryKeyField = primaryKey;

        this.indexes = scanIndexes();
    }

    private void scanFields() {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                FieldDescriptor descriptor = new FieldDescriptor(field, column);
                fields.add(descriptor);

                if (field.isAnnotationPresent(net.exylia.commons.v2.database.annotation.Index.class)) {
                    indexedFields.add(descriptor);
                }
            }
        }

        fields.add(new FieldDescriptor(
                getFieldByName("createdAt", entityClass),
                createDefaultColumn("created_at", false)
        ));

        fields.add(new FieldDescriptor(
                getFieldByName("updatedAt", entityClass),
                createDefaultColumn("updated_at", false)
        ));
    }

    private List<Index> scanIndexes() {
        List<Index> indexList = new ArrayList<>();
        if (entityClass.isAnnotationPresent(Index.class)) {
            indexList.add(entityClass.getAnnotation(Index.class));
        }
        if (entityClass.isAnnotationPresent(net.exylia.commons.v2.database.annotation.Indexes.class)) {
            net.exylia.commons.v2.database.annotation.Indexes indexes = entityClass.getAnnotation(net.exylia.commons.v2.database.annotation.Indexes.class);
            indexList.addAll(Arrays.asList(indexes.value()));
        }
        return indexList;
    }

    private Field getFieldByName(String name, Class<?> clazz) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
                return getFieldByName(name, clazz.getSuperclass());
            }
            throw new RuntimeException("Field " + name + " not found in " + clazz.getName());
        }
    }

    private Column createDefaultColumn(String name, boolean pk) {
        return new Column() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean primaryKey() {
                return pk;
            }

            @Override
            public boolean autoIncrement() {
                return false;
            }

            @Override
            public boolean nullable() {
                return false;
            }

            @Override
            public boolean unique() {
                return false;
            }

            @Override
            public int length() {
                return 255;
            }

            @Override
            public String defaultValue() {
                return "";
            }

            @Override
            public boolean autoSerialize() {
                return false;
            }

            @Override
            public net.exylia.commons.v2.database.annotation.SerializationType serializationType() {
                return net.exylia.commons.v2.database.annotation.SerializationType.AUTO;
            }

            @Override
            public boolean initializeEmpty() {
                return false;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Column.class;
            }
        };
    }

    public FieldDescriptor getField(String fieldName) {
        return fieldsByName.get(fieldName);
    }

    public boolean hasField(String fieldName) {
        return fieldsByName.containsKey(fieldName);
    }
}
