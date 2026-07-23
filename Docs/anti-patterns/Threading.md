# Anti-Pattern: Threading Mistakes

Threading bugs are the most damaging class of error in Exylia plugins because the framework targets
**Paper and Folia**. These are the concrete mistakes the framework guards against — and how to avoid
them.

Reference: [../TaskAPI.md](../TaskAPI.md), [../best-practices/Threading.md](../best-practices/Threading.md).

---

## 1. `Bukkit.getScheduler()` instead of `TaskAPI`/`Tasks`

```java
// BAD — not Folia-safe
Bukkit.getScheduler().runTask(plugin, () -> ...);
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> ...);
```

**Why:** on Folia there is no global main thread; the Bukkit scheduler doesn't route to the correct
region/entity thread.

**Preferred:**

```java
Tasks.sync(() -> ...);                 // main/region
Tasks.at(location, () -> ...);         // region thread owning the location
Tasks.at(entity, () -> ...);           // entity's scheduler
Tasks.io(() -> ...);                   // async pool
```

---

## 2. Blocking `.join()` / `.get()` on the main thread

The framework has explicit guards here — respect them.

### `HologramBuilder.build()` on the primary thread

`v2/hologram/api/HologramBuilder.java`:

```java
if (Bukkit.isPrimaryThread()) {
    throw new IllegalStateException(...); // would deadlock on the internal join()
}
return buildAsync().join();
```

**Why:** `build()` blocks on `buildAsync().join()`; on the main thread that deadlocks, so it throws
instead.

**Preferred:** on the main thread use `buildAsync()`; or use `HologramAPI.createAsync(...)` which
builds and spawns on the right thread for you.

### `SnapshotAPI.restoreRegistered(...)`

`v2/snapshot/core/SnapshotManager.java`:

```java
public boolean restoreRegistered(Player player, String snapshotId) {
    return restoreRegisteredAsync(player, snapshotId).join();  // blocks the caller
}
```

**Why:** it `.join()`s — calling it on the main thread blocks the server.

**Preferred:** use `SnapshotAPI.restoreRegisteredAsync(...)` and handle the future.

**General:** never call `.join()` / `.get()` on a `CompletableFuture` from the main/region thread.
Chain with `.thenAccept(...)` and bridge back with `Tasks.sync`.

---

## 3. Bukkit world/entity work off the owning region thread (Folia)

```java
// BAD on Folia — wrong region thread
Tasks.io(() -> block.setType(Material.STONE));
```

The framework enforces this: `Hologram.spawn()` throws unless
`Tasks.isRegionThread(location)`:

```java
if (!Tasks.isRegionThread(location)) {
    throw new IllegalStateException("Hologram must be spawned on the region thread for its location");
}
```

**Preferred:** wrap location/entity work in `Tasks.at(...)`.

```java
Tasks.at(location, () -> block.setType(Material.STONE));
```

Note: plain `Tasks.sync(...)` is **not** sufficient for location-bound work on Folia — use
`at(...)`.

---

## 4. Blocking IO on a scheduler / event thread

```java
// BAD — blocks the tick / event thread
Profile p = repository.findById(uuid).orElseThrow(); // sync DB call on main thread
String skin = fetchSkinFromMojang(name);             // network on main thread
```

**Preferred:** load off-thread, apply on the main thread.

```java
TaskAPI.databaseThenSync(
    () -> repository.findById(uuid),
    opt -> opt.ifPresent(this::apply)
);
```

Do all DB/network/file work on `db`/`io`/`compute` pools. See [Performance](Performance.md).

---

## 5. Touching Bukkit from an async pool

```java
// BAD — Bukkit API from an async pool
Tasks.db(() -> {
    Profile p = repo.findById(uuid).get();
    player.teleport(p.getLastLocation()); // WRONG thread
});
```

**Preferred:** compute async, apply sync (or on the entity thread for teleports):

```java
Tasks.db(() -> repo.findById(uuid))
    .thenAccept(res -> Tasks.at(player, () -> res.getValue()
        .ifPresent(p -> player.teleportAsync(p.getLastLocation()))));
```

---

## 6. Callbacks that assume the wrong thread

- Chat input / wizard / region callbacks run on the **event thread** (usually main) — keep them
  fast; offload heavy work.
- `SimpleRedis` pub/sub callbacks run on a **Redis listener thread** — apply Bukkit effects via
  `Tasks.sync`.

```java
redis.pubSub().subscribeObject("announce", Announcement.class, msg ->
    Tasks.sync(() -> Bukkit.broadcastMessage(msg.text())));   // bridge back
```

---

## 7. Ad-hoc thread pools / raw threads

**Anti-pattern:** `new Thread(...)`, `Executors.newFixedThreadPool(...)` for gameplay work.

**Why:** duplicates the framework's Folia-aware pools and leaks threads on reload if not shut down.

**Preferred:** use `Tasks.io/db/compute`. If you genuinely need a custom executor, use **daemon**
threads, restore interrupt status on `InterruptedException`, and shut it down in `onExyliaDisable`.

---

## Checklist

- [ ] No `Bukkit.getScheduler()`.
- [ ] No `.join()`/`.get()` on the main/region thread (use `buildAsync`, `restoreRegisteredAsync`).
- [ ] Location/entity work via `Tasks.at(...)`, not `sync(...)`.
- [ ] All IO on `db`/`io`/`compute`, applied back with `sync`.
- [ ] No Bukkit calls inside async blocks.
- [ ] Pub/sub and async callbacks bridge to the main thread before touching Bukkit.
- [ ] No raw threads / ad-hoc pools for gameplay.
