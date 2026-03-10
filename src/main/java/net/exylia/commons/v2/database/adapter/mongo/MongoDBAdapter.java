package net.exylia.commons.v2.database.adapter.mongo;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import net.exylia.commons.v2.database.adapter.DatabaseAdapter;
import net.exylia.commons.v2.database.config.AdapterConfig;
import net.exylia.commons.v2.database.entity.Entity;
import net.exylia.commons.v2.database.entity.EntityMetadata;
import net.exylia.commons.v2.database.entity.FieldDescriptor;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bson.Document;

import java.util.*;

public class MongoDBAdapter implements DatabaseAdapter {

    private MongoClient mongoClient;
    private MongoDatabase database;
    private final AdapterConfig config;

    public MongoDBAdapter(AdapterConfig config) {
        this.config = config;
    }

    @Override
    public void connect() throws Exception {
        try {
            String uri = config.getUri().isEmpty() ?
                    "mongodb://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase() :
                    config.getUri();
            mongoClient = MongoClients.create(uri);
            database = mongoClient.getDatabase(config.getDatabase());
            DebugAPI.logLibInfo("Connected to MongoDB");
        } catch (Exception e) {
            DebugAPI.logLibError("Failed to connect to MongoDB: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void disconnect() throws Exception {
        if (mongoClient != null) {
            mongoClient.close();
            DebugAPI.logLibInfo("Disconnected from MongoDB");
        }
    }

    @Override
    public boolean isConnected() {
        return mongoClient != null && database != null;
    }

    @Override
    public void createTable(EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        if (collection == null) {
            database.createCollection(metadata.getTableName());
        }
    }

    @Override
    public void updateTable(EntityMetadata metadata) throws Exception {
        // MongoDB is schema-less, no need to update
    }

    @Override
    public boolean tableExists(EntityMetadata metadata) throws Exception {
        return database.listCollectionNames().into(new ArrayList<>()).contains(metadata.getTableName());
    }

    @Override
    public <T extends Entity> void insert(T entity, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        Document doc = entityToDocument(entity, metadata);
        collection.insertOne(doc);
    }

    @Override
    public <T extends Entity> void update(T entity, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        Document doc = entityToDocument(entity, metadata);
        collection.replaceOne(Filters.eq("_id", entity.getId()), doc);
    }

    @Override
    public <T extends Entity> void delete(T entity, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        collection.deleteOne(Filters.eq("_id", entity.getId()));
    }

    @Override
    public <T extends Entity> Optional<T> findById(Object id, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        Document doc = collection.find(Filters.eq("_id", id)).first();
        if (doc != null) {
            return Optional.of(documentToEntity(doc, entityClass, metadata));
        }
        return Optional.empty();
    }

    @Override
    public <T extends Entity> List<T> findAll(Class<T> entityClass, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        List<T> results = new ArrayList<>();
        for (Document doc : collection.find()) {
            results.add(documentToEntity(doc, entityClass, metadata));
        }
        return results;
    }

    @Override
    public <T extends Entity> List<T> findByField(String fieldName, Object value, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        List<T> results = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq(fieldName, value))) {
            results.add(documentToEntity(doc, entityClass, metadata));
        }
        return results;
    }

    @Override
    public <T extends Entity> long count(Class<T> entityClass, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        return collection.countDocuments();
    }

    @Override
    public <T extends Entity> long countByField(String fieldName, Object value, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        return collection.countDocuments(Filters.eq(fieldName, value));
    }

    @Override
    public <T extends Entity> List<T> executeQuery(String query, List<Object> params, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        // MongoDB doesn't support raw queries like SQL
        throw new UnsupportedOperationException("Raw query execution not supported in MongoDB");
    }

    @Override
    public <T extends Entity> List<T> findByFieldPaged(String fieldName, Object value, int page, int pageSize, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        List<T> results = new ArrayList<>();
        int skip = page * pageSize;
        FindIterable<Document> query;
        if (fieldName == null) {
            query = collection.find().skip(skip).limit(pageSize);
        } else {
            query = collection.find(Filters.eq(fieldName, value)).skip(skip).limit(pageSize);
        }
        for (Document doc : query) {
            results.add(documentToEntity(doc, entityClass, metadata));
        }
        return results;
    }

    @Override
    public <T extends Entity> void insertBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        List<Document> docs = new ArrayList<>();
        for (T entity : entities) {
            docs.add(entityToDocument(entity, metadata));
        }
        collection.insertMany(docs);
    }

    @Override
    public <T extends Entity> void updateBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        for (T entity : entities) {
            Document doc = entityToDocument(entity, metadata);
            collection.replaceOne(Filters.eq("_id", entity.getId()), doc);
        }
    }

    @Override
    public <T extends Entity> void upsertBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        ReplaceOptions options = new ReplaceOptions().upsert(true);
        List<WriteModel<Document>> operations = new ArrayList<>();
        for (T entity : entities) {
            Document doc = entityToDocument(entity, metadata);
            operations.add(new ReplaceOneModel<>(Filters.eq("_id", entity.getId()), doc, options));
        }
        collection.bulkWrite(operations);
    }

    @Override
    public <T extends Entity> void deleteBatch(List<T> entities, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        for (T entity : entities) {
            collection.deleteOne(Filters.eq("_id", entity.getId()));
        }
    }

    @Override
    public <T extends Entity> List<T> findAllSorted(String orderByField, boolean ascending, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        List<T> results = new ArrayList<>();
        FindIterable<Document> query = ascending ?
                collection.find().sort(new Document(orderByField, 1)) :
                collection.find().sort(new Document(orderByField, -1));
        for (Document doc : query) {
            results.add(documentToEntity(doc, entityClass, metadata));
        }
        return results;
    }

    @Override
    public <T extends Entity> List<T> findAllSortedPaged(String orderByField, boolean ascending, int page, int pageSize, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        List<T> results = new ArrayList<>();
        int skip = page * pageSize;
        FindIterable<Document> query = ascending ?
                collection.find().sort(new Document(orderByField, 1)).skip(skip).limit(pageSize) :
                collection.find().sort(new Document(orderByField, -1)).skip(skip).limit(pageSize);
        for (Document doc : query) {
            results.add(documentToEntity(doc, entityClass, metadata));
        }
        return results;
    }

    @Override
    public <T extends Entity> List<T> findByFieldSorted(String whereField, Object whereValue, String orderField, boolean ascending, int limit, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        MongoCollection<Document> collection = database.getCollection(metadata.getTableName());
        List<T> results = new ArrayList<>();
        FindIterable<Document> query = collection.find(Filters.eq(whereField, whereValue))
                .sort(new Document(orderField, ascending ? 1 : -1))
                .limit(limit);
        for (Document doc : query) {
            results.add(documentToEntity(doc, entityClass, metadata));
        }
        return results;
    }

    @Override
    public String getAdapterName() {
        return "MongoDB";
    }

    @Override
    public boolean supportsBatchOperations() {
        return true;
    }

    @Override
    public boolean supportsIndexes() {
        return true;
    }

    @Override
    public boolean supportsTransactions() {
        return false;
    }

    private Document entityToDocument(Entity entity, EntityMetadata metadata) {
        Document doc = new Document("_id", entity.getId());
        for (FieldDescriptor field : metadata.getFields()) {
            if (!field.getColumnName().equals("_id")) {
                Object value = field.getValue(entity);
                doc.append(field.getColumnName(), value);
            }
        }
        return doc;
    }

    private <T extends Entity> T documentToEntity(Document doc, Class<T> entityClass, EntityMetadata metadata) throws Exception {
        T entity = entityClass.getDeclaredConstructor().newInstance();
        for (FieldDescriptor field : metadata.getFields()) {
            Object value = doc.get(field.getColumnName());
            if (value != null) {
                field.setValue(entity, value);
            }
        }
        return entity;
    }
}
