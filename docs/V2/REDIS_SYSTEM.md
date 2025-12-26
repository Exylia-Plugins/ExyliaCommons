# REDIS SYSTEM

## Descripción
Sistema de integración con Redis usando Jedis que proporciona operaciones key-value, hashes, sets, pub/sub messaging, serialización automática de objetos con Gson, pool de conexiones optimizado, operaciones async, y configuración desde YAML. Ideal para comunicación entre servidores, cache distribuido, y sincronización de datos.

## Inicialización
```java
SimpleRedis.init(Plugin plugin)  // Auto-carga desde redis.yml
SimpleRedis.init(Plugin plugin, SimpleRedisConfig config)  // Con config custom
```

## API Principal

### String Operations
- `set(String key, String value)` - Guarda string
- `set(String key, String value, int seconds)` - Con TTL
- `get(String key)` → `String` - Obtiene string
- `exists(String key)` → `boolean` - Verifica existencia
- `delete(String key)` - Elimina key
- `expire(String key, int seconds)` - Define TTL
- `ttl(String key)` → `long` - Obtiene TTL restante

### Object Operations (JSON)
- `setObject(String key, T object)` - Guarda objeto como JSON
- `setObject(String key, T object, int seconds)` - Con TTL
- `getObject(String key, Class<T> type)` → `T` - Obtiene objeto

### Hash Operations
- `hset(String key, String field, String value)` - Guarda en hash
- `hget(String key, String field)` → `String` - Obtiene de hash
- `hgetAll(String key)` → `Map<String, String>` - Obtiene hash completo
- `hdel(String key, String... fields)` - Elimina fields de hash
- `hexists(String key, String field)` → `boolean` - Verifica field en hash
- `hsetObject(String key, String field, T object)` - Guarda objeto en hash
- `hgetObject(String key, String field, Class<T> type)` → `T` - Obtiene objeto de hash

### Set Operations
- `sadd(String key, String... members)` - Añade a set
- `smembers(String key)` → `Set<String>` - Obtiene miembros del set
- `sismember(String key, String member)` → `boolean` - Verifica membresía
- `srem(String key, String... members)` - Remueve de set

### Counter Operations
- `incr(String key)` → `long` - Incrementa contador
- `incrBy(String key, long value)` → `long` - Incrementa por valor
- `decr(String key)` → `long` - Decrementa contador
- `decrBy(String key, long value)` → `long` - Decrementa por valor

### Pub/Sub Operations
- `publish(String channel, String message)` - Publica mensaje
- `publishObject(String channel, T object)` - Publica objeto
- `publishAsync(String channel, String message)` → `CompletableFuture<Void>` - Async
- `publishObjectAsync(String channel, T object)` → `CompletableFuture<Void>` - Async
- `pubSub()` → `SimpleRedisPubSub` - Manager de pub/sub

### Advanced Operations
- `execute(Function<Jedis, T> action)` → `T` - Ejecuta operación custom
- `executeAsync(Function<Jedis, T> action)` → `CompletableFuture<T>` - Async custom

### Gestión
- `isConnected()` → `boolean` - Verifica conexión
- `reload(Plugin plugin)` → `boolean` - Recarga configuración
- `shutdown()` - Cierra conexiones
- `isInitialized()` → `boolean` - Verifica inicialización
- `get()` → `SimpleRedis` - Obtiene instancia

### Scheduler
- `runSync(Runnable task)` - Ejecuta en main thread
- `runAsync(Runnable task)` - Ejecuta async

## SimpleRedisPubSub

Sistema de mensajería pub/sub:

### Suscripción
- `subscribe(String channel, Consumer<String> handler)` → `Subscription` - Suscribe a canal
- `subscribeObject(String channel, Class<T> type, Consumer<T> handler)` → `Subscription` - Objetos
- `subscribePattern(String pattern, Consumer<PatternMessage> handler)` → `Subscription` - Pattern
- `unsubscribe(String channel)` - Desuscribe de canal
- `unsubscribeAll()` - Desuscribe de todos

### Gestión
- `getActiveSubscriptions()` → `Set<String>` - Canales suscritos
- `isSubscribed(String channel)` → `boolean` - Verifica suscripción

## SimpleRedisConfig

Configuración del sistema:

```java
SimpleRedisConfig.builder()
    .host(String host)
    .port(int port)
    .password(String password)
    .database(int database)
    .timeout(int timeout)
    .poolSize(int poolSize)
    .keyPrefix(String prefix)
    .build()
```

**Propiedades:**
- `host` - Host de Redis (default: localhost)
- `port` - Puerto (default: 6379)
- `password` - Contraseña (opcional)
- `database` - Número de database (default: 0)
- `timeout` - Timeout en ms (default: 2000)
- `poolSize` - Tamaño del pool (default: 8)
- `keyPrefix` - Prefijo para todas las keys (opcional)

## Configuración YAML

```yaml
redis:
  host: "localhost"
  port: 6379
  password: ""
  database: 0
  timeout: 2000
  pool-size: 8
  key-prefix: "myplugin:"
```

## Pool de Conexiones

El sistema usa JedisPool con:
- Pool size base + 10 conexiones extra para PubSub
- Test on borrow/return/idle
- Max wait: 2 segundos
- Block when exhausted: true
- Min idle: 2 conexiones

## Características Principales
- **Jedis Integration**: Usa Jedis como cliente Redis
- **Connection Pooling**: Pool optimizado de conexiones
- **Object Serialization**: Gson para objetos automáticamente
- **Pub/Sub Messaging**: Sistema completo de mensajería
- **Async Operations**: Operaciones async con CompletableFuture
- **Key Prefix**: Prefijo automático para todas las keys
- **Auto-Reconnect**: Reconexión automática
- **Thread-Safe**: Seguro para uso concurrente
- **YAML Configuration**: Configuración fácil desde archivo

## Pub/Sub Pattern

### Publicar Mensajes
```java
// String simple
SimpleRedis.get().publish("chat", "Hola mundo");

// Objeto
MyData data = new MyData();
SimpleRedis.get().publishObject("updates", data);

// Async
SimpleRedis.get().publishAsync("events", "evento").thenRun(() -> {
    // Completado
});
```

### Suscribirse a Mensajes
```java
// String
SimpleRedis.get().pubSub().subscribe("chat", message -> {
    System.out.println("Mensaje: " + message);
});

// Objeto
SimpleRedis.get().pubSub().subscribeObject("updates", MyData.class, data -> {
    // Procesar data
});

// Pattern (múltiples canales)
SimpleRedis.get().pubSub().subscribePattern("chat:*", msg -> {
    String channel = msg.getChannel();
    String message = msg.getMessage();
});
```

## Key Prefix

El key prefix se aplica automáticamente:
```java
// Config: key-prefix: "myplugin:"
SimpleRedis.get().set("player:123", "data");
// Guarda en: "myplugin:player:123"
```

## Notas Importantes
- Redis debe estar ejecutándose antes de inicializar
- El sistema crea redis.yml automáticamente si no existe
- Los objetos se serializan con Gson (requiere serializables)
- Las operaciones async no bloquean el main thread
- El pool se dimensiona automáticamente (base + 10 para pubsub)
- Las suscripciones de PubSub usan conexiones dedicadas
- El key prefix facilita multi-tenant en mismo Redis
- Los timeouts previenen bloqueos indefinidos
- El reload cierra conexiones anteriores y crea nuevas
- Las publicaciones retornan número de suscriptores alcanzados
- El sistema detecta desconexiones y reporta en logs
- Use database numbers para separar datos de diferentes plugins
- Los patrones de PubSub soportan wildcards (* y ?)
- El shutdown es automático al deshabilitar el plugin
- Los handlers de PubSub se ejecutan async automáticamente

## Ver También
- DATABASE_SYSTEM - Alternativa para persistencia
- PLACEHOLDERS_SYSTEM - Sincronización de datos entre servidores
- CONFIG_SYSTEM - Configuración de Redis
