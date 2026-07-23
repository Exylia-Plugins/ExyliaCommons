# Example: TaskAPI

Goal: use the Folia-aware scheduler correctly — offload IO/compute to async pools, bridge results
back to the main/region thread, and schedule world/entity work on the right thread.

Related: [TaskAPI](../TaskAPI.md).

`TaskAPI` is initialized automatically in bootstrap. Both `TaskAPI` (descriptive names) and `Tasks`
(short names) exist; pick one per plugin. These examples use `TaskAPI`.

---

## Load-then-apply (the canonical pattern)

Do blocking work off-thread, apply the result on the main/region thread.

```java
// databaseThenSync = run on the DATABASE pool, then hand the result to the main thread.
TaskAPI.databaseThenSync(
    () -> profileRepository.findById(uuid),          // async (DATABASE pool)
    optProfile -> optProfile.ifPresent(this::applyToPlayer)  // sync (main/region thread)
);
```

Equivalent explicit form:

```java
TaskAPI.database(() -> profileRepository.findById(uuid))
    .thenAccept(result -> {
        if (result.isSuccess()) {
            TaskAPI.runSync(() -> result.getValue().ifPresent(this::applyToPlayer));
        }
    });
```

## Choosing the right pool

```java
TaskAPI.database(() -> repo.findAll());   // DB queries
TaskAPI.io(() -> readConfigFile());       // file/network IO
TaskAPI.compute(() -> heavyPathfind());   // CPU-bound work
```

Using the correct category keeps pools balanced and prevents a slow DB query from starving IO work.

## Sync (main-thread) scheduling

```java
TaskAPI.sync(() -> player.setHealth(20.0));               // next tick, main/region thread
TaskAPI.syncLater(() -> announceWinner(), 100L);          // after 100 ticks
ScheduledTask timer = TaskAPI.syncTimer(this::tick, 0L, 20L); // every second
// timer.cancel();
```

## Folia-correct world / entity work

On Folia, world and entity work must run on the owning region/entity thread. Use `at(...)`, **not**
`sync(...)`, for location/entity-bound Bukkit calls.

```java
// Location-bound work → region thread owning that location.
TaskAPI.at(location, () -> location.getWorld().strikeLightningEffect(location));

// Entity-bound repeating task with an onStop cleanup callback.
ScheduledTask trail = TaskAPI.atTimer(
    entity,
    () -> entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 5),
    () -> DebugAPI.logPluginDebug("Trail stopped (entity gone)"),  // onStop
    0L, 1L                                                          // delay/period ticks
);
```

## Thread checks

```java
if (!TaskAPI.isRegionThread(location)) {
    TaskAPI.at(location, () -> mutateBlocks(location));
    return;
}
mutateBlocks(location); // already on the correct thread
```

---

## Why this way

- **Folia compatibility for free** — `at(...)` routes to the correct region/entity thread; plain
  `Bukkit.getScheduler()` does not.
- **Pool separation** prevents one workload from starving another.
- **`databaseThenSync` / `asyncThenSync`** encode the load-then-apply pattern so you don't manage
  `CompletableFuture` + manual sync hops by hand.

## Common mistakes

- Calling `Bukkit.getScheduler()` directly — breaks Folia. Always use `TaskAPI` / `Tasks`.
- Using `sync(...)` for entity/world work on Folia — use `at(entity/location, ...)`.
- Doing blocking IO/DB on a scheduler thread — use `io()` / `database()`.
- Touching the Bukkit API inside an `async`/`io`/`database` block — bridge back with `runSync` /
  `databaseThenSync`.
