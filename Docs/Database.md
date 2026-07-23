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

`Database` is **opt-in** — it is **not** started by the bootstrap. Its facade signature is simply:

```java
public static void initialize(Plugin plugin);   // Database.java
```

There is **no config-object parameter** — `initialize(plugin)` reads all settings from the
`database.yml` config file (`Configs.get("database")`, backed by the
`@ConfigSchema(file = "database")` `DatabaseDefaults` schema). The backend is selected by the
`database.type` key (default `"h2"`). Double-init throws `IllegalStateException` (thrown before the
try/catch, so it is **not** wrapped); other init failures are wrapped in `ConnectionException`. On
disable, `Database.shutdown()` is called by the `ShutdownCoordinator` (`IllegalStateException`
swallowed if never initialized) and flushes/stops all write-behind repositories.

```java
@Override
protected void onExyliaEnable() {
    Database.initialize(this);           // reads database.yml
    Database.registerEntity(Profile.class);
    Repository<Profile> repo = Database.getRepository(Profile.class);
}
```

### `database.yml` (defaults from `DatabaseDefaults`)

```yaml
database:
  type: h2                 # h2 | mysql | mongodb | yaml
  server-id: server-1
  settings:
    pool-size: 10
    minimum-idle: 2
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
  write-behind:
    enabled: true          # ON by default — saves buffer and flush periodically
    flush-interval: 30     # seconds
  cache:
    enabled: true
    ttl-minutes: 30
    max-entries: 10000
  h2:
    file: database/h2
    username: sa
    password: ""
    auto-server: false
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: ""
    ssl: false
  mongodb:
    host: localhost
    # ...
```

### Facade API (`Database`, static)

```java
void initialize(Plugin plugin);   void shutdown();
<T extends Entity> void registerEntity(Class<T> entityClass);
<T extends Entity> CompletableFuture<Void> registerEntityAsync(Class<T> entityClass);
<T extends Entity> Repository<T> getRepository(Class<T> entityClass);   // write-behind-wrapped when enabled
CompletableFuture<Void> flushPlayerSession(UUID playerUuid);
CompletableFuture<TransferResult> export([Player] [, String filename]);
CompletableFuture<TransferResult> importData([Player,] String filename);
DatabaseManager getManager();
```

> `getRepository` returns a **write-behind-wrapped** repository when `write-behind.enabled` is true
> (the default) — saves are buffered and flushed on the `flush-interval`. When disabled it returns
> the direct repository.

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
convert UUIDs to `String`, but the internal by-field binds the raw value — so when querying a
field that stores a UUID, **pass a `String`**, not a `UUID`.

## Repositories & CRUD

Entities are annotated POJOs (extending `Entity`); `Repository<T>`
(`v2/database/repository/Repository.java`) provides typed CRUD with both async
(`CompletableFuture`) and sync variants, plus ordering and paging. The real method set is:

```java
// Reads
Optional<T> findById(Object id);                 CompletableFuture<Optional<T>> findByIdAsync(Object id);
List<T> findAll();                               CompletableFuture<List<T>> findAllAsync();
Optional<T> findBy(String field, Object value);  CompletableFuture<Optional<T>> findByAsync(...);
List<T> findAllBy(String field, Object value);   CompletableFuture<List<T>> findAllByAsync(...);
long count();                                    long countBy(String field, Object value);
boolean exists(Object id);
List<T> findAllOrderedBy(String field, boolean ascending, int limit);
List<T> findAllByOrderedBy(String whereField, Object whereValue, String orderField, boolean ascending, int limit);
List<T> findAllPaged(int page, int pageSize);
List<T> findAllPagedOrderedBy(String field, boolean ascending, int page, int pageSize);
// (each read has an ...Async twin)

// Writes
void save(T entity);        void saveAll(List<T> entities);
void delete(T entity);      void deleteAll(List<T> entities);
// (each write has an ...Async twin)

// Cache control
void putToCache(T entity);  void invalidateCache();  void invalidateCache(Object id);
```

> There is **no `findByField` / `findAllSortedPaged` / `executeQuery` / `upsertBatch` on the
> `Repository` interface** — those are adapter-internal. Use `findBy`/`findAllBy`,
> `findAllPagedOrderedBy`, `saveAll`, etc.

Idiomatic pattern — do IO off-thread, apply on the main/region thread:

```java
repo.findByIdAsync(uuid)
    .thenAccept(opt -> TaskAPI.runSync(() -> opt.ifPresent(this::apply)));
```

Sync repository methods block the calling thread; only call them from an async context.

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

`@PlayerSession` (`v2/database/annotation/PlayerSession.java`) marks per-player entities. When such
an entity is registered, the framework **automatically registers a `PlayerQuitEvent` listener**
that calls `Database.flushPlayerSession(uuid)` — flushing that player's write-behind buffer on
quit.

- **Auto-flushed on quit.** ✅ (built-in quit listener)
- **There is NO automatic load-on-join.** You must load the entity yourself (e.g. on join via your
  own listener). The annotation only wires up the quit flush.

`@PlayerSession` optionally takes `playerField` — when set, the flush targets entities matching
that field (`flushEntitiesBy(playerField, value)`); when empty, it resolves the player-session id
directly. This is the recommended model for session data (profiles, stats) because it avoids
TTL-based invalidation for data bound to the player's presence — but remember to **load on join
yourself**.

## Serialization Registry

Complex Bukkit/Adventure types are (de)serialized via `SerializationRegistry.getInstance()`.
Built-in serializers are auto-registered at manager start (via
`SerializerFactory.registerBuiltinSerializers()`) covering `Location`, `Component`, `ItemStack`,
`ExyliaLocation`, `Region`, `Selection`, `BossBar`, `ActionBar`, `Scoreboard`, `Hologram`, and
`SnapshotData`. Enums are auto-handled; **unregistered non-enum types throw
`SerializationException`** on serialize (deserializing an unknown enum value returns `null` rather
than throwing).

Register a serializer for any custom type you persist before using it:

```java
SerializationRegistry.getInstance().registerSerializer(MyType.class, myType -> /* → String */);
SerializationRegistry.getInstance().registerDeserializer(MyType.class, str -> /* → MyType */);
// or registerSerializationPair(...) for both at once
```

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

- Choose the backend deliberately via `database.type`; use **YAML only for development/fallback**,
  never production.
- Register serializers for all custom persisted types up front.
- Use **`@PlayerSession`** for player-bound data — it auto-flushes on quit, but **load it yourself
  on join** (there is no auto-load).
- Do all DB access async; never block the main thread.
- On multi-server networks, configure L2 Redis so `InvalidationBus` keeps caches consistent.
- Keep `write-behind.enabled` on for instant saves, but call `Database.flushPlayerSession(uuid)` /
  rely on the quit listener before critical reads on another server.

## Common Mistakes

- Passing a config object to `Database.initialize` — it takes **only `Plugin`** and reads
  `database.yml`.
- Assuming `@PlayerSession` **loads** data on join — it only flushes on quit.
- Expecting transactions/rollback — **none exist**; batch writes are not atomic.
- Calling a raw query on Mongo/YAML adapters — throws `UnsupportedOperationException`.
- Passing a `UUID` (instead of `String`) when querying a UUID-valued field on SQL.
- Calling `Database.initialize` twice → `IllegalStateException` (not wrapped).
- Relying on `EconomyManager.transfer` — it is a stub.
- Sorting with the YAML adapter — not implemented.
- Looking for `findByField`/`executeQuery` on `Repository` — use `findBy`/`findAllBy`/ordered/paged
  methods.

## Relationship With Other Systems

- Uses [TaskAPI](TaskAPI.md) `DATABASE` pool for async work.
- Shares the database Redis pool with [Teleport](Teleport.md).
- Cache clears are wired into the [Reload](Reload.md) system (`DatabaseV2Adapter`, `RedisAdapter`).
- Distinct from [Redis](Redis.md) (`SimpleRedis`) used by [Channel](Channel.md).
