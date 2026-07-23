# TaskAPI

## Overview

`TaskAPI` is the **single scheduling entry point** for all ExyliaCommons plugins. It abstracts
over Bukkit's scheduler **and Folia's region/entity schedulers**, plus a set of dedicated async
thread pools for IO/database/compute work. Every subsystem in the library schedules through it,
and consuming plugins should too.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `TaskAPI` | `v2/tasks/api/TaskAPI.java` | Static facade — your entry point |
| `TaskManager` | `v2/tasks/core/TaskManager.java` | Singleton, Folia detection, dispatch |
| `TaskExecutor` | `v2/tasks/core/TaskExecutor.java` | Async pools per category |
| `TaskBuilder` / `TaskChainBuilder` | `v2/tasks/builder/*` | Fluent task/chain construction |
| `ScheduledTask` | `v2/tasks/scheduler/ScheduledTask.java` | Handle to cancel a scheduled task |
| `TaskCategory` | `v2/tasks/model/TaskCategory.java` | `DATABASE`, `IO`, `COMPUTE`, ... |
| `TaskResult<T>` | `v2/tasks/model/TaskResult.java` | Async result wrapper |

## Purpose

Bukkit code has strict threading rules, and Folia removes the concept of a single main thread
entirely (work is scheduled per-region and per-entity). Writing plugins that work on both Paper
and Folia by hand is error-prone. `TaskAPI` provides one API that:

- Routes "sync" work to the correct thread (main thread on Paper; the owning region/entity thread
  on Folia).
- Offers dedicated async pools so blocking IO never touches a scheduler thread.
- Provides thread-affinity checks so you can assert you are on the right thread.

## Initialization

`TaskAPI.initialize(plugin)` is called automatically during bootstrap (see
[Lifecycle.md](Lifecycle.md)). You normally never call it yourself. It is shut down automatically
when the last `ExyliaPlugin` disables.

Check availability with `TaskAPI.isInitialized()`.

## Threading Model

| Concept | Paper | Folia |
|---------|-------|-------|
| "sync" | main server thread | region/entity thread |
| `at(location, ...)` | main thread | region thread owning that location |
| `at(entity, ...)` | main thread | entity's scheduler thread |
| async pools | thread pool | thread pool |

**Golden rule:** Any Bukkit API that touches a world, block, or entity must run on the owning
region/entity thread. On Folia, using plain `sync(...)` is not enough for location-bound work —
use `at(location, ...)` or `at(entity, ...)`.

## Public API

### Async work (returns `CompletableFuture<TaskResult<T>>`)

```java
CompletableFuture<TaskResult<T>> async(Supplier<T> supplier);
CompletableFuture<TaskResult<T>> async(TaskCategory category, Supplier<T> supplier);
CompletableFuture<TaskResult<Void>> async(Runnable runnable);

// Category shortcuts:
database(Supplier<T> | Runnable);   // TaskCategory.DATABASE pool
io(Supplier<T> | Runnable);         // TaskCategory.IO pool
compute(Supplier<T> | Runnable);    // TaskCategory.COMPUTE pool
```

Use the category that matches the workload so pools stay balanced: `database()` for DB queries,
`io()` for file/network IO, `compute()` for CPU-bound work.

### Sync (main / region-agnostic) scheduling

```java
ScheduledTask sync(Runnable task);
ScheduledTask syncLater(Runnable task, long ticks);
ScheduledTask syncLater(Runnable task, long delay, TimeUnit unit);
ScheduledTask syncTimer(Runnable task, long delayTicks, long periodTicks);
void runSync(Runnable task);   // fire-and-forget
void runAsync(Runnable task);  // fire-and-forget async
```

### Location / entity scheduling (Folia-correct)

```java
ScheduledTask at(Location location, Runnable task);
ScheduledTask atLater(Location location, Runnable task, long delay, TimeUnit unit);
ScheduledTask atTimer(Location location, Runnable task, long delay, long period, TimeUnit unit);

ScheduledTask at(Entity entity, Runnable task);
ScheduledTask atTimer(Entity entity, Runnable task, long delayTicks, long periodTicks);
ScheduledTask atTimer(Entity entity, Runnable task, Runnable onStop, long delayTicks, long periodTicks);
```

The `onStop` overloads run a callback when the entity-bound timer stops (e.g. entity removed),
useful for cleanup.

### Async → sync bridging

```java
<T> void asyncThenSync(Supplier<T> asyncSupplier, Consumer<T> syncConsumer);
<T> void asyncThenSync(TaskCategory category, Supplier<T> asyncSupplier, Consumer<T> syncConsumer);
<T> void databaseThenSync(Supplier<T> asyncSupplier, Consumer<T> syncConsumer);
```

This is the canonical pattern: do blocking work off-thread, then apply the result on the main
thread. Prefer it over manual `CompletableFuture` chaining.

### Thread checks

```java
boolean isMainThread();
boolean isFolia();
boolean isRegionThread(Location | (World, chunkX, chunkZ) | Chunk);
boolean isEntityThread(Entity);
```

### Builders

```java
TaskAPI.task()...   // TaskBuilder<T> for complex single tasks
TaskAPI.chain()...  // TaskChainBuilder<Void> for sequenced steps
```

### Diagnostics

```java
TaskStats getStats();
TaskExecutor.PoolStatus getPoolStatus(TaskCategory category);
void resetStats();
void cancelAll();
```

## Examples

### Load player data off-thread, apply on the main thread

```java
TaskAPI.databaseThenSync(
    () -> repository.load(uuid),      // runs on DATABASE pool
    data -> applyToPlayer(player, data) // runs on main / region thread
);
```

### Folia-correct particle burst at a location

```java
TaskAPI.at(location, () -> location.getWorld().spawnParticle(Particle.FLAME, location, 30));
```

### Repeating entity-bound task with cleanup

```java
ScheduledTask task = TaskAPI.atTimer(
    entity,
    () -> tickTrail(entity),
    () -> DebugAPI.logPluginDebug("Trail stopped"), // onStop
    0L, 1L                                          // delay/period ticks
);
// later:
task.cancel();
```

### Plain async result handling

```java
TaskAPI.io(() -> readFile()).thenAccept(result -> {
    if (result.isSuccess()) use(result.getValue());
});
```

## Best Practices

- **Always schedule through `TaskAPI`.** Never call `Bukkit.getScheduler()` directly in Exylia
  plugins — it breaks Folia compatibility.
- Use the **category shortcuts** (`database`/`io`/`compute`) so pools are used appropriately.
- For any location/entity Bukkit call, use **`at(...)`**, not `sync(...)`, so it works on Folia.
- Use **`asyncThenSync` / `databaseThenSync`** for the load-then-apply pattern.
- Keep sync tasks short; offload heavy work to async pools.

## Common Mistakes

- Using `sync(...)` for entity/world work on Folia → wrong thread, `IllegalStateException` or
  data races. Use `at(entity/location, ...)`.
- Doing blocking IO/DB calls on a scheduler thread → server stalls. Use `io()`/`database()`.
- Assuming `isMainThread()` is meaningful on Folia — prefer `isRegionThread`/`isEntityThread`.
- Forgetting to cancel long-lived timers on disable (though `TaskAPI.shutdown()` cancels
  everything at plugin unload).

## Performance Considerations

- Async pools are shared per-category; long-running blocking tasks can starve a category — split
  heavy jobs or use the correct category.
- `cancelAll()` and `shutdown()` stop everything; use targeted `ScheduledTask.cancel()` for
  individual tasks.
- Use `getStats()` / `getPoolStatus()` to diagnose saturation.

## Relationship With Other Systems

`TaskAPI` underpins essentially every other subsystem: the [Sequence](Sequence.md) engine
dispatches effects via `at(...)`, [Database](Database.md) uses the `DATABASE` pool,
[Skulls](Skulls.md) fetch textures off-thread, [Holograms](Holograms.md) enforce region-thread
spawning, and [Visuals](Visuals.md) run countdown timers. Because it is initialized early in
bootstrap, it is safe to use anywhere from `onExyliaEnable` onward.
