# Database

## Overview

`net.exylia.commons.v2.database` is a lightweight, annotation-driven persistence layer for
Bukkit/Paper/Folia plugins. It offers a backend-agnostic adapter model, an entity/repository
abstraction with async and sync CRUD, a layered cache (Caffeine L1 + optional Redis L2) with
cross-server invalidation, write-behind buffering, `@PlayerSession` semantics for per-player data,
and a serialization registry for complex types.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `Database` | `v2/database/api/Database.java` | Static facade — init/shutdown/repositories |
| `DatabaseAdapter` | `v2/database/adapter/DatabaseAdapter.java` | Backend SPI |
| `SQLAdapter` / `MongoDBAdapter` / `YAMLFallbackAdapter` | `v2/database/adapter/...` | Backends |
| `RedisConnectionPool` | `v2/database/redis/RedisConnectionPool.java` | L2 cache / pub-sub |
| `CacheStrategy` / `InvalidationBus` | `v2/database/redis/...` | Cache + cross-server sync |
| serializers | `v2/database/serialization/...` | Type serialization registry |
| `DatabaseTransferAPI` | `v2/database/transfer/api/DatabaseTransferAPI.java` | JSON import/export |
| `DatabaseException` (+ subclasses) | `v2/database/exception/...` | Domain exceptions |

## Purpose

Plugins that persist data (economy balances, player profiles, shops) need consistent storage that
works across single servers and multi-server networks, without blocking the main thread. This
layer provides that with a repository API, automatic caching, and network-wide cache invalidation.

## Initialization

`Database` is **opt-in** — it is **not** started by the bootstrap. Initialize it in
`onExyliaEnable` with a connection configuration. `Database.initialize(...)` throws
`IllegalStateException` if called twice and wraps failures in `ConnectionException`. On plugin
disable, `Database.shutdown()` is called by the `ShutdownCoordinator` (an `IllegalStateException`
is swallowed if it was never initialized).

```java
@Override
protected void onExyliaEnable() {
    Database.initialize(/* config selecting H2 / MYSQL / MONGODB / YAML */);
    // register entities / repositories
}
```

## Supported Backends & Limitations

| Backend | Transactions | Raw queries | Notes |
|---------|-------------|-------------|-------|
| **H2** (default) | No* | Yes | Embedded; default choice. Not declared in root deps — supply the driver. |
| **MySQL** | No* | Yes | Via HikariCP; supply connector. |
| **MongoDB** | No | **`executeQuery` throws** `UnsupportedOperationException` | Schema-less; uses `_id` as PK. |
| **YAML** (fallback) | No | **`executeQuery` throws** `UnsupportedOperationException` | In-memory + file; no indexes. `findAllSorted` is **not implemented** (returns unsorted); `findAllSortedPaged` can throw `IndexOutOfBoundsException` on out-of-range skip. Dev/fallback only. |

> \* `SQLAdapter.supportsTransactions()` returns `false`. `upsertBatch` runs multiple statements
> **without a transaction** — a partial failure is **not** rolled back. Do not rely on atomic
> multi-row writes.

**SQL PK note:** UUID primary keys are stored as `VARCHAR(36)`. `insert`/`delete`/`findById`
convert UUIDs to `String`, but `findByField`/`countByField` bind the raw value
(`stmt.setObject(1, value)`) — passing a `UUID` there may not match the stored string. Pass a
`String` when querying by a UUID field.

## Repositories & CRUD

Entities are annotated POJOs; repositories provide typed CRUD with both async
(`CompletableFuture`) and sync variants, plus paging and sorting. The idiomatic pattern is:

- **Reads/writes off-thread** via the async repository methods (they use the `DATABASE`
  [TaskAPI](TaskAPI.md) pool).
- Apply results back on the main/region thread when touching Bukkit (`databaseThenSync`).

```java
repository.findByIdAsync(uuid)
    .thenAccept(opt -> TaskAPI.runSync(() -> opt.ifPresent(this::apply)));
```

Consult the repository interface for the exact method set (`save`, `findById`, `findByField`,
`findAll`, `findAllSortedPaged`, `count`, `delete`, `executeQuery`, batch variants).

## Caching

A two-level cache sits in front of the adapter:

- **L1**: Caffeine in-process cache.
- **L2 (optional)**: Redis via `RedisConnectionPool`, storing serialized wrapper JSON.

`CacheStrategy.get(...)`: L1 → L2 (`jedis.get`, deserialize) → loader (DB) → populate both. On L2
read failure it logs a warning and falls through to the loader. **Write-behind** buffering makes
saves appear instant and flushes periodically.

`InvalidationBus` publishes cache invalidations over Redis pub/sub so other servers drop stale
entries — this is how a multi-server network stays consistent.

## `@PlayerSession`

`@PlayerSession` entities have per-player lifecycle semantics aligned to a network:

- **Loaded on join.**
- **Auto-flushed on quit** (write-behind buffer flushed for that player).

This is the recommended model for session data (profiles, stats) because it avoids TTL-based
invalidation for data that has a natural lifecycle bound to the player's presence.

## Serialization Registry

Complex Bukkit/Adventure types are (de)serialized via a registry. `serialize(value, type)` /
`deserialize(value, type)` fall back to enum handling for enums; **unregistered types throw
`SerializationException`**. Register a serializer for any custom type you persist before using it.

## Import / Export

`DatabaseTransferAPI` supports JSON import/export of entity data — useful for backups and
migrations between backends.

## Redis Relationship

The database layer uses its **own** Redis pool (`v2/database/redis/RedisConnectionPool`) for L2
cache and invalidation. This is **distinct** from the standalone `SimpleRedis`
([Redis.md](Redis.md)) used by the [Channel](Channel.md) system. The [Teleport](Teleport.md)
cross-server feature uses the **database** Redis pool. Do not confuse the two stacks.

## Threading Considerations

- All DB IO belongs off the main thread — use the async repository methods (they run on the
  `DATABASE` pool) and bridge back with `databaseThenSync`.
- The connection health monitor runs its own daemon scheduler + reconnect thread with exponential
  backoff, correctly restoring interrupt status on `InterruptedException`.
- Caches use concurrent structures and are thread-safe.

## Stubbed / Unimplemented (do not rely on)

- `YAMLFallbackAdapter.executeQuery` → throws `UnsupportedOperationException`.
- `YAMLFallbackAdapter.findAllSorted` → returns **unsorted** (TODO); paged variant may throw.
- `MongoDBAdapter.executeQuery` → throws `UnsupportedOperationException`.
- `EconomyManager.transfer` (economy subsystem) → **stub, always returns failure**
  (see [Integrations.md](Integrations.md)).

## Best Practices

- Choose the backend deliberately; use **YAML only for development/fallback**, never production.
- Register serializers for all custom persisted types up front.
- Use **`@PlayerSession`** for player-bound data; let auto-flush handle quit.
- Do all DB access async; never block the main thread.
- On multi-server networks, configure L2 Redis so `InvalidationBus` keeps caches consistent.

## Common Mistakes

- Expecting transactions/rollback — **none exist**; `upsertBatch` is not atomic.
- Calling `executeQuery` on Mongo/YAML adapters — throws.
- Passing a `UUID` to `findByField`/`countByField` on SQL — pass a `String`.
- Calling `Database.initialize` twice → `IllegalStateException`.
- Relying on `EconomyManager.transfer` — it is a stub.
- Sorting with the YAML adapter — not implemented.

## Relationship With Other Systems

- Uses [TaskAPI](TaskAPI.md) `DATABASE` pool for async work.
- Shares the database Redis pool with [Teleport](Teleport.md).
- Cache clears are wired into the [Reload](Reload.md) system (`DatabaseV2Adapter`, `RedisAdapter`).
- Distinct from [Redis](Redis.md) (`SimpleRedis`) used by [Channel](Channel.md).
