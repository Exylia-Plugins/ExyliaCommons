# Skulls

## Overview

The skull subsystem resolves player-head textures (by name, UUID, or base64 texture) with an
aggressive multi-layer cache, async Mojang fetching, rate-limit/back-pressure handling, and
persistent disk caching. It exists so menus can render heads instantly from cache without stalling
the main thread on network calls.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `SkullAPI` | `v2/skull/api/SkullAPI.java` | Static facade |
| `SkullBuilder` | `v2/skull/builder/SkullBuilder.java` | Fluent head construction |
| `SkullManager` | `v2/skull/core/SkullManager.java` | Singleton lifecycle |
| `SkullCache` | `v2/skull/core/SkullCache.java` | Caffeine caches + in-flight dedup |
| `SkullExecutor` | `v2/skull/core/SkullExecutor.java` | Fetch thread pool + cleanup |
| `MojangFetcher` / `TextureFetcher` | `v2/skull/fetcher/...` | HTTP texture fetch |
| `SkullPersistence` | `v2/skull/persistence/...` | Persistent disk cache |
| `SkullConfig` | `v2/skull/config/SkullConfig.java` | Tunables |

## Purpose

Fetching a player skin from Mojang is a blocking network call and is rate-limited. Doing it on the
main thread stalls the server; doing it naively hits rate limits. This subsystem centralizes
fetching, caches results (memory + disk), deduplicates concurrent requests, and applies back-off.

## Initialization

`SkullAPI` is lazily initialized by `MenuAPI.initialize` (so opening menus is enough). If you use
skulls independently, initialize it yourself. It is shut down by the `ShutdownCoordinator`
(`SkullAPI.shutdown()` is called on plugin disable).

## Sync vs Async (critical)

- **Synchronous** skull building returns a **cached or default (Steve) texture only** — it never
  blocks to fetch. Use it when you have already preloaded the head.
- **Asynchronous** fetching performs the real Mojang lookup off-thread and completes a future.

```java
// Preload at startup so later sync builds hit cache (varargs)
SkullAPI.preloadPlayers("Notch", "jeb_");
// batch: CompletableFuture<List<ItemStack>> SkullAPI.batchPlayers(String...)

// Async fetch (real texture)
SkullAPI.fromPlayerAsync(name).thenAccept(head -> { /* apply on main thread */ });
SkullAPI.fromPlayerAsync(name, head -> { /* consumer overload */ });

// Sync (cache/default only — safe on main thread, no network)
ItemStack head = SkullAPI.fromPlayer(name);       // or fromPlayerCached(name)

// From texture / URL:
ItemStack t = SkullAPI.fromTexture(base64);       // fromTextureAsync(...) for real fetch
ItemStack u = SkullAPI.fromTextureURL(url);
SkullBuilder b = SkullAPI.player(name);           // fluent builder
```

## Caching & Back-Pressure

- **`SkullCache`**: two Caffeine caches, `maximumSize = 100000`, `expireAfterWrite = 24h`, with
  stats. `pendingRequests` deduplicates in-flight futures per key. Player keys are lowercased.
- **Rate limiting**: `rateLimitBackoff` (60s on HTTP 429) and `networkErrorBackoff` (5s on IO
  error) gate fetches.
- **`SkullExecutor`**: fetch pool core=4, max=8, 60s keep-alive, queue 1000, daemon threads,
  `CallerRunsPolicy` (under heavy load a fetch may run on the caller thread as back-pressure). A
  daemon cleanup thread clears the cache every 8h. `shutdown()` awaits 5s then `shutdownNow`.
- **Persistence**: `SkullPersistence` stores textures on disk with a 7-day TTL, surviving
  restarts.

## `MojangFetcher` Behavior

Uses `java.net.http.HttpClient` (HTTP/1.1). `fetchPlayerUUID(name)` maps status `200`→parse id,
`404`→`"NOT_FOUND"`, `429`→set rate-limit backoff, other→error. `fetchPlayerTexture(uuid)` parses
the `textures` property base64. On `IOException` sets network back-off; on `InterruptedException`
restores interrupt status.

## `SkullConfig` Tunables

`threadPoolSize=4`, `maxCacheSize=100000`, `cacheExpiration=24h`, `rateLimitBackoff=60s`,
`networkErrorBackoff=5s`, `httpTimeout=10s`, `cleanupInterval=8h`, `batchDelay=150`,
`preloadDelay=50`, `persistentCacheTtl=7d`, `defaultTexture` (Steve), plus Mojang endpoint URLs.

## Threading Considerations

- Async fetch runs on the `SkullExecutor` pool; the sync path never blocks.
- Apply resolved heads to inventories on the **main/region thread**.
- `CallerRunsPolicy` means an over-saturated fetch queue can execute a fetch on the calling
  thread — avoid calling async fetch in tight loops on the main thread.

## Best Practices

- **Preload/batch** heads at startup (`SkullAPI.preloadPlayers(String...)` /
  `SkullAPI.batchPlayers(String...)`) so menus render from cache.
- Use the **sync** path in menu item building (fast, cache/default) and rely on preloading for
  real textures.
- Use the **async** path only when you need a guaranteed real texture and can wait.
- Let the framework shut the executor down on disable.

## Common Mistakes

- Using the sync path and expecting a real skin without preloading → you get Steve/cached.
- Calling async fetch in a hot loop on the main thread → back-pressure may run fetches inline.
- Mutating the resulting item into an inventory off the main thread.

## Relationship With Other Systems

- Initialized by [Menus](Menus.md) (`MenuAPI.initialize`) and used to build head items via
  [Items](Items.md).
- Uses [TaskAPI](TaskAPI.md) conventions for off-thread work.
- Cache is cleared by the [Reload](Reload.md) system (`SkullAdapter`).
