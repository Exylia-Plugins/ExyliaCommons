package net.exylia.commons.database.adapters;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOneModel;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.database.annotations.Column;
import net.exylia.commons.database.annotations.Table;
import net.exylia.commons.database.serialization.SerializationHelper;
import net.exylia.commons.utils.DebugUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Field;
import java.util.*;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.DebugUtils.logInternalDebug;

public class MongoDBAdapter implements DatabaseAdapter {

    private final FileConfiguration config;
    private final ExyliaPlugin plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBAdapter(FileConfiguration config, ExyliaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public void connect() throws Exception {
        String host = config.getString("database.mongodb.host", "localhost");
        int port = config.getInt("database.mongodb.port", 27017);
        String databaseName = config.getString("database.mongodb.database", "minecraft");
        String username = config.getString("database.mongodb.username", "");
        String password = config.getString("database.mongodb.password", "");
        String authDatabase = config.getString("database.mongodb.auth-database", "admin");

        String connectionString;

        if (!username.isEmpty() && !password.isEmpty()) {
            connectionString = String.format("mongodb://%s:%s@%s:%d/%s?authSource=%s",
                    username, password, host, port, databaseName, authDatabase);
        } else {
            connectionString = String.format("mongodb://%s:%d", host, port);
        }

        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase(databaseName);

        // Probar conexión
        database.runCommand(new Document("ping", 1));
    }

    @Override
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public boolean isConnected() {
        if (mongoClient == null || database == null) {
            return false;
        }

        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public <T> void save(T entity) throws Exception {
        String collectionName = getTableName(entity.getClass());
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Document document = entityToDocument(entity);
        collection.insertOne(document);

        // Si tiene un campo _id y es ObjectId, establecerlo de vuelta en la entidad
        setIdFromDocument(entity, document);
    }

    @Override
    public <T> void saveOrUpdateAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String collectionName = getTableName(entities.get(0).getClass());
        MongoCollection<Document> collection = database.getCollection(collectionName);

        List<ReplaceOneModel<Document>> operations = new ArrayList<>();

        for (T entity : entities) {
            Object id = getEntityId(entity);
            Document document = entityToDocument(entity);

            if (id != null) {
                // Si tiene ID, hacer upsert (actualizar o insertar)
                Document filter = new Document("_id", convertToObjectId(id));
                ReplaceOptions options = new ReplaceOptions().upsert(true);
                operations.add(new ReplaceOneModel<>(filter, document, options));
            } else {
                // Si no tiene ID, es una inserción (MongoDB generará el _id)
                operations.add(new ReplaceOneModel<>(
                        new Document("_id", new ObjectId()), // Filtro que nunca coincidirá
                        document,
                        new ReplaceOptions().upsert(true)
                ));
            }
        }

        if (!operations.isEmpty()) {
            BulkWriteResult result = collection.bulkWrite(operations);
            DebugUtils.logInternalInfo("saveOrUpdateAll completado: " +
                    result.getInsertedCount() + " insertadas, " +
                    result.getModifiedCount() + " actualizadas, " +
                    result.getUpserts().size() + " upserts");
        }
    }

    @Override
    public <T> void updateAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String collectionName = getTableName(entities.get(0).getClass());
        MongoCollection<Document> collection = database.getCollection(collectionName);

        List<UpdateOneModel<Document>> operations = new ArrayList<>();

        for (T entity : entities) {
            Object id = getEntityId(entity);

            if (id == null) {
                DebugUtils.logInternalError("No se puede actualizar entidad sin ID en updateAll");
                continue;
            }

            Document filter = new Document("_id", convertToObjectId(id));
            Document updateDocument = entityToDocument(entity);

            // Remover el _id del documento de actualización para evitar errores
            updateDocument.remove("_id");

            Document update = new Document("$set", updateDocument);
            operations.add(new UpdateOneModel<>(filter, update));
        }

        if (!operations.isEmpty()) {
            BulkWriteResult result = collection.bulkWrite(operations);
            DebugUtils.logInternalInfo("updateAll completado: " +
                    result.getModifiedCount() + " de " + entities.size() + " entidades actualizadas");
        }
    }

    @Override
    public <T> void update(T entity) throws Exception {
        String collectionName = getTableName(entity.getClass());
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Object id = getEntityId(entity);
        if (id == null) {
            throw new IllegalArgumentException("No se puede actualizar una entidad sin ID");
        }

        Document document = entityToDocument(entity);
        Document filter = new Document("_id", convertToObjectId(id));

        collection.replaceOne(filter, document, new ReplaceOptions().upsert(false));
    }

    @Override
    public <T> void delete(T entity) throws Exception {
        String collectionName = getTableName(entity.getClass());
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Object id = getEntityId(entity);
        if (id == null) {
            throw new IllegalArgumentException("No se puede eliminar una entidad sin ID");
        }

        Document filter = new Document("_id", convertToObjectId(id));
        collection.deleteOne(filter);
    }

    @Override
    public <T> Optional<T> findById(Class<T> entityClass, Object id) throws Exception {
        String collectionName = getTableName(entityClass);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Document filter = new Document("_id", convertToObjectId(id));
        Document document = collection.find(filter).first();

        if (document != null) {
            return Optional.of(documentToEntity(document, entityClass));
        }

        return Optional.empty();
    }

    @Override
    public <T> List<T> findAll(Class<T> entityClass) throws Exception {
        String collectionName = getTableName(entityClass);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        List<T> results = new ArrayList<>();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                results.add(documentToEntity(document, entityClass));
            }
        }

        return results;
    }

    @Override
    public <T> List<T> findBy(Class<T> entityClass, String field, Object value) throws Exception {
        String collectionName = getTableName(entityClass);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        List<T> results = new ArrayList<>();
        Document filter = new Document(field, value);

        try (MongoCursor<Document> cursor = collection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                results.add(documentToEntity(document, entityClass));
            }
        }

        return results;
    }

    @Override
    public <T> List<T> executeQuery(Class<T> entityClass, String query, Object... params) throws Exception {
        // Para MongoDB, interpretamos el query como un filtro JSON
        String collectionName = getTableName(entityClass);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Document filter = Document.parse(query);
        List<T> results = new ArrayList<>();

        try (MongoCursor<Document> cursor = collection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                results.add(documentToEntity(document, entityClass));
            }
        }

        return results;
    }

    @Override
    public int executeUpdate(String query, Object... params) throws Exception {
        // Para operaciones de actualización en masa en MongoDB
        throw new UnsupportedOperationException("executeUpdate no está implementado para MongoDB. Use métodos específicos del repositorio.");
    }

    @Override
    public void createTable(Class<?> entityClass) throws Exception {
        // MongoDB crea colecciones automáticamente, pero podemos crear índices aquí
        String collectionName = getTableName(entityClass);

        // Verificar si la colección ya existe
        boolean exists = false;
        for (String name : database.listCollectionNames()) {
            if (name.equals(collectionName)) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            database.createCollection(collectionName);
        }

        // Crear índices para campos únicos
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Field[] fields = entityClass.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.unique() && !column.primaryKey()) {
                    String fieldName = column.name().isEmpty() ? field.getName() : column.name();
                    collection.createIndex(new Document(fieldName, 1));
                }
            }
        }
    }

    @Override
    public void updateTable(Class<?> entityClass) throws Exception {
        // MongoDB es schema-less, así que solo necesitamos asegurar que la colección existe
        createTable(entityClass);
    }

    @Override
    public boolean tableExists(Class<?> entityClass) throws Exception {
        String collectionName = getTableName(entityClass);

        for (String name : database.listCollectionNames()) {
            if (name.equals(collectionName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> getTableColumns(Class<?> entityClass) throws Exception {
        // En MongoDB, retornamos los campos definidos en la entidad
        List<String> fields = new ArrayList<>();

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                String fieldName = column.name().isEmpty() ? field.getName() : column.name();
                fields.add(fieldName);
            }
        }

        return fields;
    }

    @Override
    public void beginTransaction() throws Exception {
        // MongoDB soporta transacciones solo en replica sets
        // Para simplificar, no implementamos transacciones por ahora
        throw new UnsupportedOperationException("Transacciones no implementadas para MongoDB en esta versión");
    }

    @Override
    public void commit() throws Exception {
        // No implementado
    }

    @Override
    public void rollback() throws Exception {
        // No implementado
    }

    @Override
    public String getTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    // ===== MÉTODOS ACTUALIZADOS CON AUTO-SERIALIZACIÓN =====

    @Override
    public Map<String, Object> entityToMap(Object entity) throws Exception {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String fieldName = column.name().isEmpty() ? field.getName() : column.name();

                Object value = field.get(entity);

                // AUTO-SERIALIZACIÓN: Si está habilitada, serializar automáticamente
                if (value != null && column.autoSerialize()) {
                    try {
                        value = SerializationHelper.autoSerializeValue(value, field, column.serializationType());
                    } catch (Exception e) {
                        DebugUtils.logInternalError("Error auto-serializando campo MongoDB " + fieldName + ": " + e.getMessage());
                        // Fallback: usar el valor original
                    }
                }

                if (value != null) {
                    map.put(fieldName, value);
                }
            }
        }

        return map;
    }

    @Override
    public <T> T mapToEntity(Map<String, Object> map, Class<T> entityClass) throws Exception {
        T entity = entityClass.getDeclaredConstructor().newInstance();
        Field[] fields = entityClass.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String fieldName = column.name().isEmpty() ? field.getName() : column.name();

                Object value = map.get(fieldName);
                if (value != null) {
                    // AUTO-DESERIALIZACIÓN: Si está habilitada, deserializar automáticamente
                    if (column.autoSerialize()) {
                        try {
                            value = SerializationHelper.autoDeserializeValue(value, field, column.serializationType());
                        } catch (Exception e) {
                            DebugUtils.logInternalError("Error auto-deserializando campo MongoDB " + fieldName + ": " + e.getMessage());
                            // Fallback: intentar conversión normal
                            value = convertValue(value, field.getType());
                        }
                    } else {
                        value = convertValue(value, field.getType());
                    }

                    field.set(entity, value);
                }
            }
        }

        return entity;
    }

    // Métodos específicos de MongoDB actualizados

    private Document entityToDocument(Object entity) throws Exception {
        Document document = new Document();
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String fieldName;

                if (column.primaryKey()) {
                    fieldName = "_id";
                } else {
                    fieldName = column.name().isEmpty() ? field.getName() : column.name();
                }

                Object value = field.get(entity);

                // AUTO-SERIALIZACIÓN para MongoDB
                if (value != null && column.autoSerialize()) {
                    try {
                        value = SerializationHelper.autoSerializeValue(value, field, column.serializationType());
                    } catch (Exception e) {
                        DebugUtils.logInternalError("Error auto-serializando campo documento MongoDB " + fieldName + ": " + e.getMessage());
                    }
                }

                if (value != null) {
                    if (fieldName.equals("_id") && value instanceof String) {
                        // Convertir String a ObjectId si es necesario
                        try {
                            document.put(fieldName, new ObjectId(value.toString()));
                        } catch (IllegalArgumentException e) {
                            // Si no es un ObjectId válido, usar como String
                            document.put(fieldName, value);
                        }
                    } else {
                        document.put(fieldName, value);
                    }
                }
            }
        }

        return document;
    }

    private <T> T documentToEntity(Document document, Class<T> entityClass) throws Exception {
        T entity = entityClass.getDeclaredConstructor().newInstance();
        Field[] fields = entityClass.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                String fieldName;

                if (column.primaryKey()) {
                    fieldName = "_id";
                } else {
                    fieldName = column.name().isEmpty() ? field.getName() : column.name();
                }

                Object value = document.get(fieldName);
                if (value != null) {
                    // AUTO-DESERIALIZACIÓN para MongoDB
                    if (column.autoSerialize()) {
                        try {
                            value = SerializationHelper.autoDeserializeValue(value, field, column.serializationType());
                        } catch (Exception e) {
                            DebugUtils.logInternalError("Error auto-deserializando campo documento MongoDB " + fieldName + ": " + e.getMessage());
                            // Fallback a conversión normal
                            if (fieldName.equals("_id") && value instanceof ObjectId && field.getType() == String.class) {
                                value = value.toString();
                            } else {
                                value = convertValue(value, field.getType());
                            }
                        }
                    } else {
                        if (fieldName.equals("_id") && value instanceof ObjectId && field.getType() == String.class) {
                            // Convertir ObjectId a String
                            value = value.toString();
                        } else {
                            value = convertValue(value, field.getType());
                        }
                    }

                    field.set(entity, value);
                }
            }
        }

        return entity;
    }

    private Object getEntityId(Object entity) throws Exception {
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey()) {
                    field.setAccessible(true);
                    return field.get(entity);
                }
            }
        }

        return null;
    }

    private void setIdFromDocument(Object entity, Document document) throws Exception {
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.primaryKey()) {
                    field.setAccessible(true);
                    Object id = document.get("_id");

                    if (id != null) {
                        if (field.getType() == String.class && id instanceof ObjectId) {
                            field.set(entity, id.toString());
                        } else {
                            field.set(entity, id);
                        }
                    }
                    break;
                }
            }
        }
    }

    private Object convertToObjectId(Object id) {
        if (id instanceof String) {
            try {
                return new ObjectId(id.toString());
            } catch (IllegalArgumentException e) {
                return id;
            }
        }
        return id;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == int.class || targetType == Integer.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.valueOf(value.toString());
        } else if (targetType == long.class || targetType == Long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.valueOf(value.toString());
        } else if (targetType == double.class || targetType == Double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.valueOf(value.toString());
        } else if (targetType == float.class || targetType == Float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.valueOf(value.toString());
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.valueOf(value.toString());
        }

        return value;
    }
}