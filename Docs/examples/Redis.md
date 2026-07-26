# Example: Redis

Goal: use `SimpleRedis` for cross-server key/value coordination and pub/sub — off the main thread.

> This is the **standalone** `SimpleRedis` stack (used by the [Channel](../Channel.md) system). The
> [Database](../Database.md) layer has its **own** Redis pool for L2 cache/invalidation — don't
> confuse the two. Jedis is `compileOnly`, so the server/plugin must provide it.

Related: [Redis](../Redis.md).

`SimpleRedis` is opt-in — initialize it in `onExyliaEnable`. The accessor is `SimpleRedis.get()`.

---

## Initialize

```java
@Override
protected void onExyliaEnable() {
    SimpleRedis.init(this);   // config from redis.yml (or init(this, SimpleRedisConfig))

    if (!SimpleRedis.isInitialized()) {
        DebugAPI.logPluginWarn("Redis is disabled in redis.yml — cross-server features disabled.");
        return;
    }
    if (!SimpleRedis.get().isConnected()) {
        DebugAPI.logPluginWarn("Redis not connected — cross-server features disabled.");
    }
}
```

## Key/value (typed convenience ops)

Prefer the typed helpers over raw `execute`. The **synchronous** ops run on the calling thread —
keep them off the main thread.

```java
SimpleRedis redis = SimpleRedis.get();

// Store a session token with a 5-minute TTL (SETEX), off-thread:
Tasks.io(() -> {
    redis.set("session:" + uuid, token, 300);
});

// Read it back off-thread, then use it on the main thread:
Tasks.io(() -> redis.get("session:" + uuid))
    .thenAccept(result -> {
        if (result.isSuccess() && result.getValue() != null) {
            String tokenValue = result.getValue();
            Tasks.at(player, () -> applyToken(player, tokenValue));
        }
    });

// Counters & sets (these synchronous calls also belong off-thread):
Tasks.io(() -> {
    long online = redis.incr("network:online");
    redis.sadd("party:" + partyId, player.getUniqueId().toString());
    return redis.smembers("party:" + partyId);
}).thenAccept(result -> {
    if (!result.isSuccess()) return;
    Tasks.sync(() -> useMembers(result.getValue()));
});
```

## Objects (serialized JSON)

```java
// Store/read a serializable object with a TTL:
Tasks.io(() -> redis.setObject("profile:" + uuid, profileSnapshot, 600));
Tasks.io(() -> redis.getObject("profile:" + uuid, ProfileSnapshot.class))
    .thenAccept(result -> {
        if (result.isSuccess() && result.getValue() != null) {
            Tasks.at(player, () -> applySnapshot(player, result.getValue()));
        }
    });
```

## Pub/Sub (cross-server events)

```java
// Publisher (async):
redis.publishObjectAsync("network:announce", new Announcement("Restart in 5m"));

// Subscriber — register during enable:
redis.pubSub().subscribeObject("network:announce", Announcement.class, msg ->
    Tasks.sync(() -> Bukkit.broadcastMessage("[Network] " + msg.text())));
```

## Raw access when you need a specific command

```java
Tasks.io(() -> redis.execute(jedis -> jedis.zremrangeByScore("leaderboard", 0, 100)))
    .thenAccept(result -> {
        if (result.isSuccess()) {
            Long removed = result.getValue();
            // Handle the result without touching Bukkit here.
        }
    });
```

---

## Why this way

- **Typed helpers** (`set`/`get`/`incr`/`sadd`/`setObject`) are clearer and safer than raw Jedis.
- **Async variants** (`executeAsync`, `publishObjectAsync`) and `Tasks.io(...)` keep Redis IO off
  the server thread.
- **Pub/sub** delivers cross-server events; apply Bukkit-side effects via `Tasks.sync`.
- **One pool.** Use `SimpleRedis` for coordination; use the [Database](../Database.md) layer for
  persistence + caching. Don't create a third Jedis pool.

## Common mistakes

- Calling `SimpleRedis.get()` when Redis is disabled — check `SimpleRedis.isInitialized()` first.
- Calling synchronous ops (`get`/`set`/...) on the main thread — they block on network IO. Use
  `Tasks.io(...)` or the async variants.
- Applying Bukkit changes directly from a pub/sub callback thread — bridge back with `Tasks.sync`.
- Calling `SimpleRedis.init` twice → `IllegalStateException`.
- Assuming Redis is present — check `isConnected()` and degrade gracefully.
