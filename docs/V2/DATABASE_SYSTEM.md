# DATABASE SYSTEM

## Descripción
Sistema ORM completo para persistencia de datos con soporte para múltiples adapters (MySQL, H2, MongoDB, YAML). Implementa el patrón Repository para acceso a datos, cache inteligente con Caffeine, serialización automática de tipos complejos, y anotaciones para definir estructura de tablas. Todas las operaciones tienen versiones síncronas y asíncronas.

## Inicialización

```java
Database.initialize()
Database.initialize(Config config)
Database.initialize(Plugin plugin)
Database.initialize(Config config, Plugin plugin)
```

Por defecto usa `Configs.get("database")` si no se especifica config.

## API Principal

### Registro de Entities
- `registerEntity(Class<T> entityClass)` - Registra entity sync (crea tabla)
- `registerEntityAsync(Class<T> entityClass)` → `CompletableFuture<Void>` - Registra async

### Obtención de Repositories
- `getRepository(Class<T> entityClass)` → `Repository<T>` - Obtiene repository para entity

### Gestión
- `shutdown()` - Cierra conexiones y libera recursos
- `getManager()` → `DatabaseManager` - Obtiene manager para acceso avanzado

## Repository<T> Interface

Cada entity registrada tiene un Repository para operaciones CRUD.

### Operaciones de Lectura (Sync)
- `findById(Object id)` → `Optional<T>` - Busca por ID
- `findAll()` → `List<T>` - Obtiene todas las entities
- `findBy(String field, Object value)` → `List<T>` - Busca por campo específico
- `count()` → `long` - Cuenta total de registros
- `exists(Object id)` → `boolean` - Verifica si existe

### Operaciones de Lectura (Async)
- `findByIdAsync(Object id)` → `CompletableFuture<Optional<T>>`
- `findAllAsync()` → `CompletableFuture<List<T>>`
- `findByAsync(String field, Object value)` → `CompletableFuture<List<T>>`
- `countAsync()` → `CompletableFuture<Long>`

### Operaciones de Escritura (Sync)
- `save(T entity)` → `T` - Guarda o actualiza entity
- `saveAll(List<T> entities)` → `List<T>` - Guarda múltiples entities
- `delete(T entity)` → `boolean` - Elimina entity
- `deleteById(Object id)` → `boolean` - Elimina por ID

### Operaciones de Escritura (Async)
- `saveAsync(T entity)` → `CompletableFuture<T>`
- `saveAllAsync(List<T> entities)` → `CompletableFuture<List<T>>`
- `deleteAsync(T entity)` → `CompletableFuture<Boolean>`

### Operaciones Avanzadas
- `findAllOrderedBy(String field, boolean asc, int limit)` → `List<T>` - Busca ordenado con límite
- `findAllPaged(int page, int pageSize)` → `List<T>` - Paginación
- `invalidateCache()` - Invalida cache del repository
- `getCacheStats()` → `CacheStats` - Estadísticas de cache

## Anotaciones

### @Table
Define la tabla de la entity:
```java
@Table(name = "my_table", version = "1.0")
```
- `name` - Nombre de la tabla en la base de datos
- `version` - Versión de la tabla (default: "1.0")

### @Column
Define una columna:
```java
@Column(
    name = "column_name",
    primaryKey = false,
    autoIncrement = false,
    nullable = true,
    unique = false,
    length = 255,
    defaultValue = "",
    autoSerialize = false,
    serializationType = SerializationType.GSON
)
```

**Parámetros:**
- `name` - Nombre de la columna
- `primaryKey` - Si es primary key
- `autoIncrement` - Auto incremento (solo para números)
- `nullable` - Permite valores NULL
- `unique` - Constraint de unicidad
- `length` - Longitud máxima (para strings)
- `defaultValue` - Valor por defecto
- `autoSerialize` - Serializar automáticamente objetos complejos
- `serializationType` - Tipo de serialización (GSON, JSON, CUSTOM)

### @Index
Define índices para optimizar consultas:
```java
@Index(name = "idx_name", columns = {"column1", "column2"})
```

## Clase Base Entity

Todas las entities deben extender `Entity`:

```java
public abstract class Entity {
    protected Long createdAt;
    protected Long updatedAt;

    public abstract Object getId();
    public void updateTimestamp() { ... }
}
```

**Métodos:**
- `getId()` - Retorna el ID de la entity (debe ser implementado)
- `createdAt()` → `Long` - Timestamp de creación
- `updatedAt()` → `Long` - Timestamp de última actualización
- `updateTimestamp()` - Actualiza `updatedAt` al tiempo actual

## Serialización Automática

El sistema incluye serializers para tipos complejos:
- **ItemStack** - Items de Minecraft
- **Location** - Ubicaciones
- **Component** - Adventure components
- **Region** - Regiones del sistema
- **HologramData** - Datos de hologramas
- **PotionEffect** - Efectos de poción
- **Atributos** - Y más...

Los serializers se usan automáticamente cuando `autoSerialize = true`.

## Adapters Soportados

1. **MySQL/MariaDB** - Base de datos relacional con pool de conexiones
2. **H2** - Base de datos embebida para desarrollo/testing
3. **MongoDB** - Base de datos NoSQL document-based
4. **YAML** - Persistencia en archivos YAML (fallback)

## Características Principales
- **Múltiples Adapters**: MySQL, H2, MongoDB, YAML
- **Cache con Caffeine**: Cache configurable con TTL y estadísticas
- **Async Operations**: Versiones async de todas las operaciones
- **Serialización Automática**: Soporte para tipos complejos de Minecraft
- **Pattern Repository**: Abstracción clean para acceso a datos
- **Anotaciones Declarativas**: Definición de schema con @Table/@Column
- **Auto-creación de Tablas**: Las tablas se crean automáticamente
- **Batch Operations**: `saveAll()` para operaciones en lote
- **Paginación**: Soporte built-in para paginación
- **Thread-Safe**: Seguro para uso concurrente

## Notas Importantes
- Debe llamarse `Database.initialize()` antes de registrar entities
- Las entities deben extender `Entity` y tener `@Table`
- Los campos deben usar `@Column` o no serán persistidos
- El ID debe ser único y definido con `@Column(primaryKey = true)`
- `autoSerialize` funciona solo para tipos con serializer registrado
- El cache se invalida automáticamente en operaciones de escritura
- Las operaciones async usan un pool de threads dedicado
- La conexión se mantiene abierta hasta llamar `shutdown()`
- Si cambias el schema, considera versionar con `@Table(version = "2.0")`

## Ver También
- YAML_SYSTEM - Alternativa de persistencia en archivos
- CONFIG_SYSTEM - Configuración del sistema de database
- SNAPSHOT_SYSTEM - Usa Database para guardar snapshots
- HOLOGRAM_SYSTEM - Persistencia de hologramas
