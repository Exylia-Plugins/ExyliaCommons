# Threading Standard

Threading correctness is non-negotiable in Exylia plugins because the framework targets **Paper and
Folia**. On Folia there is no single "main thread" — work is scheduled per-region and per-entity.
Following this standard makes your plugin correct on both.

Reference: [`TaskAPI`](../TaskAPI.md) / `Tasks` (`v2/tasks`).

---

## 1. Schedule only through `TaskAPI` / `Tasks`

**Never call `Bukkit.getScheduler()` directly.** It is not Folia-safe. Route every scheduled unit of
work through the framework.

```java
// GOOD
Tasks.sync(() -> player.setHealth(20.0));
Tasks.at(location, () -> location.getWorld().strikeLightningEffect(location));

// BAD — breaks Folia
Bukkit.getScheduler().runTask(plugin, () -> ...);
```

Both facades exist: `TaskAPI` (descriptive names) and `Tasks` (short names). Pick **one per
plugin** for consistency.

## 2. Know the three execution contexts

| Context | Use for | How |
|---------|---------|-----|
| **Sync (main/region)** | Bukkit API not tied to a specific location | `Tasks.sync`, `Tasks.later`, `Tasks.timer` |
| **Region/entity thread** | World, block, or entity access | `Tasks.at(location, ...)`, `Tasks.at(entity, ...)` |
| **Async pool** | IO, DB, network, heavy compute | `Tasks.io`, `Tasks.db`, `Tasks.compute` |

**Golden rule:** any Bukkit call touching a world/block/entity must run on the **owning
region/entity thread**. On Folia, plain `sync(...)` is not enough for location-bound work — use
`at(...)`.

```java
// Correct: mutate blocks on the region thread that owns the location.
if (!Tasks.isRegionThread(location)) {
    Tasks.at(location, () -> placeStructure(location));
} else {
    placeStructure(location);
}
```

The framework enforces this itself: `Hologram.spawn()` throws unless
`Tasks.isRegionThread(location)`, and `HologramBuilder.build()` throws on the primary thread
(it would deadlock). Follow the same discipline in your code.

## 3. The load-then-apply pattern (the default for IO)

Do blocking work off-thread, then apply the result on the main/region thread. This is the single
most-used threading pattern.

```java
// databaseThenSync = DATABASE pool → then main thread.
TaskAPI.databaseThenSync(
    () -> repository.findById(uuid),           // async
    opt -> opt.ifPresent(this::applyToPlayer)  // sync
);
```

Never:
- do DB/network/file IO on the main/region thread, or
- touch the Bukkit API from an async pool.

## 4. Pick the right async category

```java
Tasks.db(() -> repo.findAll());     // DB queries → DATABASE pool
Tasks.io(() -> readFile());         // file/network → IO pool
Tasks.compute(() -> pathfind());    // CPU-bound → COMPUTE pool
```

Using the correct category keeps pools balanced; a slow DB query must not starve IO work.

## 5. Thread-safety of shared state

- Session/registry state that multiple threads read/write uses **concurrent structures**
  (`ConcurrentHashMap`), as the framework does throughout (`Configs` cache, skull `pendingRequests`,
  wizard/session maps).
- Prefer **immutable** config/model objects (built once, shared freely).
- Guard lazy singletons with double-checked locking or a static holder — mirror `EconomyManager` /
  `SnapshotStore`.

## 6. `InterruptedException` and executors

- Restore interrupt status when catching `InterruptedException`
  (`Thread.currentThread().interrupt()`), as the skull/database executors do.
- If you create an executor (rare — prefer `TaskAPI`), make threads **daemon** and **shut it down**
  on plugin disable.

## 7. Callbacks land where the handler completes

- Chat input / wizard / region callbacks run on the thread the triggering event uses (usually
  main). Keep them fast; offload heavy work with `Tasks.io/db` and bridge back with `Tasks.sync`.
- Pub/sub (`SimpleRedis`) callbacks run on a Redis listener thread — apply Bukkit effects via
  `Tasks.sync`.

---

## Checklist

- [ ] No direct `Bukkit.getScheduler()` usage.
- [ ] World/block/entity work uses `Tasks.at(...)`.
- [ ] All IO/DB/network is on `io`/`db`/`compute`, applied back via `sync`.
- [ ] Shared mutable state is concurrent; config/models are immutable.
- [ ] Any custom executor uses daemon threads, restores interrupts, and is shut down.

## Anti-patterns

- `Bukkit.getScheduler().runTask(...)` — Folia-unsafe.
- Blocking IO on the main/region thread (menu build, join handler, command).
- Touching Bukkit from an async block.
- Using `sync(...)` for location-bound work on Folia instead of `at(...)`.
- Sharing mutable models across threads without synchronization.
