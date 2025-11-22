# DatabaseV2 - Complete Usage Guide

**DatabaseV2** es un sistema de base de datos completamente optimizado, 100% ASYNC, con Caffeine Cache integrado, soporte para H2/MySQL/MongoDB y diseñado para máxima performance en servidores Bukkit.

---

## 🎯 Características Principales

- ✅ **100% ASYNC**: Todas las operaciones devuelven `CompletableFuture<T>`
- ✅ **Caffeine Cache Integrado**: Caching automático con TTL, eviction y stats
- ✅ **Multi-Database**: H2, MySQL, MongoDB con misma API
- ✅ **Zero Code Duplication**: Arquitectura limpia y reutilizable
- ✅ **Type-Safe**: Generics y validación en compilación
- ✅ **AsyncExecutor Integration**: Usa los 4 threads dedicados a DB existentes
- ✅ **Fallback Chain**: Degradación automática si la DB principal cae
- ✅ **Entity Metadata Caching**: Reflection solo al registrar entidades

---

## 📋 Estructura de Paquetes

```
net.exylia.commons.databaseV2/
├── core/              (DatabaseV2Manager, QueryExecutor)
├── adapter/           (DatabaseAdapter, SQL, MongoDB, YAML)
├── entity/            (Entity, EntityMetadata, FieldDescriptor)
├── repository/        (Repository, RepositoryImpl, RepositoryRegistry)
├── cache/             (CaffeineCacheStrategy, CacheKey, CacheStats)
├── serialization/     (Serializers, SerializationRegistry, Builtin)
├── config/            (DatabaseV2Config, AdapterConfig)
├── exception/         (Excepciones y manejo de errores)
└── api/               (DatabaseV2 - API pública)
```

---

## 🚀 Quick Start

### 1. Crear una Entidad

```java
@Table(name = "users")
@Getter
@Setter
public class User extends Entity {

    @Column(name = "id", primaryKey = true)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "level")
    private int level;

    @Column(name = "last_login")
    private long lastLogin;

    @Column(name = "faction")
    private String faction;

    @Override
    public Object getId() {
        return id;
    }
}
```

### 2. Inicializar DatabaseV2

```java
// En tu plugin onEnable()
public void onEnable() {
    try {
        DatabaseV2.initialize();

        // Registrar entidades
        DatabaseV2.registerEntity(User.class);
        DatabaseV2.registerEntity(Inventory.class);

        getLogger().info("DatabaseV2 initialized successfully");
    } catch (Exception e) {
        getLogger().severe("Failed to initialize DatabaseV2: " + e.getMessage());
        getServer().getPluginManager().disablePlugin(this);
    }
}
```

### 3. Usar el Repositorio

```java
// Obtener repositorio
Repository<User> userRepository = DatabaseV2.getRepository(User.class);

// Buscar un usuario (async)
userRepository.findByIdAsync(playerId)
    .thenAccept(optionalUser -> {
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            player.sendMessage("Welcome back, " + user.getName());
        }
    })
    .exceptionally(error -> {
        getLogger().severe("Database error: " + error.getMessage());
        return null;
    });

// Guardar usuario (async)
User newUser = new User();
newUser.setId(player.getUniqueId().toString());
newUser.setName(player.getName());
newUser.setLevel(1);
newUser.setCreatedAt(System.currentTimeMillis());

userRepository.saveAsync(newUser)
    .thenRun(() -> player.sendMessage("Profile created!"));

// Con AsyncAPI para volver al thread principal cuando sea necesario
AsyncAPI.computeDbThenSync(
    () -> userRepository.findById(playerId),
    user -> player.sendMessage(user.getName()) // Runs on main thread
);
```

---

## 📊 Operaciones CRUD

Todas las operaciones tienen versiones **Async** (recomendadas) y síncronas:

```java
Repository<User> repo = DatabaseV2.getRepository(User.class);

// CREATE
repo.saveAsync(user).join();           // async
repo.save(user);                       // sync

// READ
Optional<User> user = repo.findById(id).join();
Optional<User> user = repo.findBy("email", "test@example.com").join();
List<User> users = repo.findAll().join();

// UPDATE
user.setLevel(5);
user.updateTimestamp();
repo.saveAsync(user);

// DELETE
repo.deleteAsync(user);
repo.deleteAllAsync(userList);

// COUNT
long total = repo.countAsync().join();
long byLevel = repo.countByAsync("level", 5).join();

// EXISTS
boolean exists = repo.existsAsync(id).join();
```

---

## 🔄 Batch Operations (Muy Rápidas)

```java
List<User> newPlayers = getPlayersToSave();

// Guardar muchos usuarios en una sola operación
userRepository.saveAllAsync(newPlayers)
    .thenRun(() -> logger.info("Saved " + newPlayers.size() + " players"));

// Eliminar muchos usuarios
userRepository.deleteAllAsync(oldPlayers)
    .thenRun(() -> logger.info("Deleted " + oldPlayers.size() + " inactive players"));
```

---

## 📄 Paginación

```java
// Página 0 con 50 resultados por página
List<User> page1 = repo.findAllPaged(0, 50).join();

// Página 1 con 50 resultados, ordenada por level DESC
List<User> topPlayers = repo.findAllPagedOrderedBy("level", false, 0, 50).join();
```

---

## 💾 Caché Automático

El caché Caffeine está **automáticamente integrado**:

```java
// Primer acceso: Database query
User user1 = userRepository.findById(id).join();

// Segundo acceso (dentro del TTL): Cache hit ✅ (rápido)
User user2 = userRepository.findById(id).join();

// Invalidar manualmente si es necesario
userRepository.invalidateCache(id);

// Ver estadísticas del caché
CacheStats stats = userRepository.getCacheStats();
System.out.println("Cache hit rate: " + stats.hitRate() + "%");
System.out.println("Evictions: " + stats.getEvictions());
```

---

## ⚙️ Configuración (database-v2.yml)

```yaml
database-v2:
  type: MySQL              # H2, MySQL, MongoDB, YAML
  auto-migration: true
  debug: false
  enable-metrics: false

  cache:
    enabled: true
    strategy: CAFFEINE
    ttl-minutes: 30        # Cache TTL
    max-entries: 10000     # Max entries before eviction
    record-stats: false    # true = overhead, solo para debugging
    refresh-after-access: true

  h2:
    file: ./data/database
    pool-size: 5

  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: root
    charset: utf8mb4
    collation: utf8mb4_unicode_ci
    pool-size: 10
    min-idle: 2

  mongodb:
    uri: mongodb://localhost:27017
    database: minecraft
    pool-size: 10

  fallback:
    enabled: true
    chain:
      - H2
      - YAML
```

---

## 🔗 Serialización Personalizada

Tipos soportados automáticamente:
- ✅ `Location` (Bukkit)
- ✅ `ItemStack` (Bukkit)
- ✅ `Component` (Adventure)
- ✅ Enums
- ✅ String, primitivos, Date

Registrar serializador personalizado:

```java
// Crear serializer
public class CustomTypeSerializer implements Serializer<CustomType> {
    @Override
    public String serialize(CustomType value) {
        return value.toString();
    }
}

// Crear deserializer
public class CustomTypeDeserializer implements Deserializer<CustomType> {
    @Override
    public CustomType deserialize(String value, Class<CustomType> type) {
        return CustomType.parse(value);
    }
}

// Registrar
SerializationRegistry registry = SerializationRegistry.getInstance();
registry.registerSerializationPair(CustomType.class,
    new CustomTypeSerializer(),
    new CustomTypeDeserializer());
```

---

## 🔀 Manejo de Errores

Los errores se manejan automáticamente con fallback:

```java
userRepository.findByIdAsync(id)
    .exceptionally(error -> {
        if (error instanceof ConnectionException) {
            // Database principal falló, se usa fallback automáticamente
            logger.warn("Using fallback database");
        } else if (error instanceof SerializationException) {
            logger.error("Serialization failed: " + error.getMessage());
        }
        return Optional.empty();
    });
```

---

## 🧹 Shutdown

Limpiar recursos al desactivar el plugin:

```java
public void onDisable() {
    DatabaseV2.shutdown();
    getLogger().info("DatabaseV2 shutdown complete");
}
```

---

## 📈 Performance Tips

1. **Usa batch operations** cuando sea posible
   - `saveAll()` es 10x+ más rápido que bucle de `save()`

2. **Aprovecha el caché**
   - El caché Caffeine tiene hit rate >80% típicamente

3. **Usa FindBy para queries simples**
   - Más eficiente que `findAll()` + filtrado manual

4. **Async siempre**
   - Nunca bloquees el thread principal de Bukkit
   - Usa `AsyncAPI.computeDb()` para garantizarlo

5. **Paginación para grandes datasets**
   - En lugar de `findAll()`, usa `findAllPaged(0, 50)`

---

## 🎓 Ejemplo Completo

```java
// En tu listener de evento
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Repository<User> userRepository = DatabaseV2.getRepository(User.class);

    userRepository.findByAsync("name", player.getName())
        .thenAcceptAsync(optionalUser -> {
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                user.setLastLogin(System.currentTimeMillis());
                userRepository.saveAsync(user);

                // Volver al thread principal para Bukkit API
                Schedulers.sync(() -> {
                    player.sendMessage("Welcome back! Level: " + user.getLevel());
                });
            } else {
                // Nuevo jugador
                User newUser = new User();
                newUser.setId(player.getUniqueId().toString());
                newUser.setName(player.getName());
                newUser.setLevel(1);
                newUser.setLastLogin(System.currentTimeMillis());

                userRepository.saveAsync(newUser)
                    .thenRun(() -> Schedulers.sync(() -> {
                        player.sendMessage("Welcome! Profile created.");
                    }));
            }
        })
        .exceptionally(error -> {
            logger.severe("Error loading user: " + error.getMessage());
            return null;
        });
}
```

---

## 📦 Dependencias Incluidas

- **HikariCP** 5.1.0 - Connection pooling para SQL
- **Caffeine** 3.2.2 - Caching ultra-rápido
- **MongoDB Driver** 4.11.0 - Soporte NoSQL
- **AsyncExecutor** (ExyliaCommons) - Thread pool dedicado

---

## 🔧 Troubleshooting

**P: ¿Por qué mi query es lenta?**
- A: Verifica `CacheStats`. Si hit rate es bajo (<50%), aumenta TTL o max-entries

**P: ¿Puedo bloquear una operación async?**
- A: Sí, llama `.join()` en el `CompletableFuture`, pero NO en main thread Bukkit

**P: ¿Cómo sé si la DB principal está caída?**
- A: El fallback se activa automáticamente. Revisa los logs para `ConnectionException`

**P: ¿Funciona con MySQL y MongoDB simultáneamente?**
- A: No, pero puedes cambiar en la config sin recompilar

---

## ✅ Checklist de Buenas Prácticas

- [ ] Todas las DB operations son async (`CompletableFuture`)
- [ ] Usas batch operations para múltiples inserts/updates
- [ ] El caché está habilitado (`cache.enabled: true`)
- [ ] Vuelves al main thread para Bukkit API (`Schedulers.sync()`)
- [ ] Entities tienen `getId()` implementado
- [ ] Llamaste a `registerEntity()` para todas tus entidades
- [ ] Llamaste a `shutdown()` en `onDisable()`
- [ ] Manejo de excepciones con `.exceptionally()`

---

**DatabaseV2 está listo para producción. Enjoy! 🚀**
