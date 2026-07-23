# Redis

## Overview

ExyliaCommons contains **two independent Redis stacks**. Knowing which is which prevents a lot of
confusion:

| Stack | Class | Used by | Purpose |
|-------|-------|---------|---------|
| **Standalone Redis** | `v2/redis/SimpleRedis.java` | [Channel](Channel.md) | General-purpose Redis + pub/sub for cross-server messaging |
| **Database Redis pool** | `v2/database/redis/RedisConnectionPool.java` | [Database](Database.md), [Teleport](Teleport.md) | L2 cache, cache invalidation, cross-server teleport |

This document covers **`SimpleRedis`**. For the database pool see [Database.md](Database.md).

## Purpose

`SimpleRedis` is a thin, opt-in wrapper around Jedis for plugins that need direct Redis access
and pub/sub without pulling in the full database layer. It powers the [Channel](Channel.md)
cross-server messaging system.

## Initialization

`SimpleRedis` is **opt-in**. Jedis is a `compileOnly` dependency, so the server/consuming plugin
must provide it at runtime. The bootstrap only *probes* for Jedis presence
(`Class.forName("redis.clients.jedis.Jedis")`); it does not connect. Configuration comes from
`redis.yml` (or an explicit `SimpleRedisConfig`).

```java
SimpleRedis.init(plugin);                    // config from redis.yml
SimpleRedis.init(plugin, simpleRedisConfig); // explicit config
boolean ok = SimpleRedis.reload(plugin);     // reload; initializes on first call, else rebuilds
```

`init(...)` throws `IllegalStateException("SimpleRedis already initialized")` if called twice.
`reload(plugin)` returns `false` on failure and will initialize if not yet initialized.

## Public API

```java
// Lifecycle (static)
static void init(Plugin plugin [, SimpleRedisConfig config]);
static boolean reload(Plugin plugin);
static SimpleRedis get();          // accessor is get(), NOT getInstance()
static boolean isInitialized();
void shutdown();   boolean isConnected();

// Raw execution
<T> T execute(Function<Jedis, T> action);              // runs on the calling thread
<T> CompletableFuture<T> executeAsync(Function<Jedis, T> action);

// Typed data operations (convenience over execute)
void set(String key, String value [, int seconds]);
String get(String key);   boolean exists(String key);   void delete(String key);
void expire(String key, int seconds);   long ttl(String key);
<T> void setObject(String key, T object [, int seconds]);   <T> T getObject(String key, Class<T> type);
void hset(...);  String hget(...);  Map<String,String> hgetAll(String key);  void hdel(...);  boolean hexists(...);
<T> void hsetObject(...);  <T> T hgetObject(...);
void sadd(...);  Set<String> smembers(String key);  boolean sismember(...);  void srem(...);
long incr(String key);  long incrBy(String key, long v);  long decr(String key);  long decrBy(String key, long v);

// Pub/Sub
SimpleRedisPubSub pubSub();
void publish(String channel, String message);          CompletableFuture<Void> publishAsync(...);
<T> void publishObject(String channel, T object);       <T> CompletableFuture<Void> publishObjectAsync(...);
```

## Usage

```java
// Typed convenience ops (recommended over raw execute)
SimpleRedis.get().set("session:" + uuid, data, 300);   // SETEX 300s
String data = SimpleRedis.get().get("session:" + uuid);

// Raw command
String value = SimpleRedis.get().execute(jedis -> jedis.get("key"));

// Async command (offloaded)
SimpleRedis.get().executeAsync(jedis -> jedis.get("key")).thenAccept(v -> { /* handle */ });

// Publish / subscribe
SimpleRedis.get().pubSub().publish("channel", "message");
```

## Threading Considerations

- `execute(...)` and the **synchronous typed ops** (`get`/`set`/`hget`/...) run on the **calling
  thread** — do not call them on the main thread for network round-trips; use `executeAsync(...)`,
  `publishAsync`, or wrap in [TaskAPI](TaskAPI.md) `io()`.
- `executeAsync` / `publishAsync` complete on a background executor.
- Pub/sub subscribers run on their own connection/thread.

## Best Practices

- Prefer the typed ops for readability; prefer async variants on hot paths.
- Use `SimpleRedis` for messaging/coordination; use the [Database](Database.md) layer (which has
  its own pool) for persistence + caching. Do not create a third ad-hoc Jedis pool.
- Ship Jedis with the server or shade it in the consuming plugin (it is `compileOnly` here).
- Check `isConnected()` before relying on Redis-dependent features.

## Common Mistakes

- Using `SimpleRedis.getInstance()` — the accessor is `SimpleRedis.get()`.
- Confusing `SimpleRedis` with the database Redis pool — they are separate connections/configs.
- Calling `execute`/synchronous typed ops on the main thread and blocking on network IO.
- Calling `init(...)` twice → `IllegalStateException`.
- Forgetting that Jedis is not bundled by ExyliaCommons.

## Relationship With Other Systems

- Powers the [Channel](Channel.md) cross-server messaging system.
- The [Reload](Reload.md) system reloads it via `RedisAdapter`.
- Independent from the [Database](Database.md) Redis pool used by [Teleport](Teleport.md).
