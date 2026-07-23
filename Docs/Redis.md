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
(`Class.forName("redis.clients.jedis.Jedis")`); it does not connect.

```java
SimpleRedis.init(plugin);                    // default config
SimpleRedis.init(plugin, simpleRedisConfig); // explicit config
boolean ok = SimpleRedis.reload(plugin);     // reload connection/config
```

## Public API

```java
// Lifecycle
public static void init(Plugin plugin);
public static void init(Plugin plugin, SimpleRedisConfig config);
public static boolean reload(Plugin plugin);

// Execute against a Jedis connection
public <T> T execute(Function<Jedis, T> action);
public <T> CompletableFuture<T> executeAsync(Function<Jedis, T> action);

// Pub/Sub
public SimpleRedisPubSub pubSub();
```

## Usage

```java
// Synchronous command
String value = SimpleRedis.getInstance().execute(jedis -> jedis.get("key"));

// Async command (offloaded)
SimpleRedis.getInstance().executeAsync(jedis -> jedis.get("key"))
    .thenAccept(v -> { /* handle */ });

// Publish / subscribe
SimpleRedis.getInstance().pubSub().publish("channel", "message");
```

## Threading Considerations

- `execute(...)` runs on the calling thread — **do not call it on the main thread** for network
  round-trips; use `executeAsync(...)` or wrap in [TaskAPI](TaskAPI.md) `io()`.
- Pub/sub subscribers run on their own connection/thread.

## Best Practices

- Prefer `executeAsync` for anything on a hot path.
- Use `SimpleRedis` for messaging/coordination; use the [Database](Database.md) layer (which has
  its own pool) for persistence + caching. Do not create a third ad-hoc Jedis pool.
- Ship Jedis with the server or shade it in the consuming plugin (it is `compileOnly` here).

## Common Mistakes

- Confusing `SimpleRedis` with the database Redis pool — they are separate connections/configs.
- Calling `execute(...)` on the main thread and blocking on network IO.
- Forgetting that Jedis is not bundled by ExyliaCommons.

## Relationship With Other Systems

- Powers the [Channel](Channel.md) cross-server messaging system.
- The [Reload](Reload.md) system reloads it via `RedisAdapter`.
- Independent from the [Database](Database.md) Redis pool used by [Teleport](Teleport.md).
