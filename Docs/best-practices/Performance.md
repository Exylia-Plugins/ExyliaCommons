# Performance Standard

Exylia plugins run on busy networks. The framework is built for it — cache, diff, batch, debounce.
Reuse those mechanisms instead of hand-rolling slower ones.

---

## 1. Cache with Caffeine, load on join, clear on quit

- Use **Caffeine** for plugin-facing caches (the framework does everywhere: clan, combat, skull,
  database, visual). Configure `maximumSize` + `expireAfterWrite` and enable `recordStats()` when
  you need visibility.
- For **player session data**, load on join and clear on quit — **do not use TTL** for data whose
  lifecycle is the player's presence. This is a project data convention and what
  `@PlayerSession` + your session cache implement.

```java
// Session cache: load on join, clear on quit (see Listeners example).
private final Map<UUID, Profile> cache = new ConcurrentHashMap<>();
// onJoin: load off-thread → cache.put(...)
// onQuit: cache.remove(...)   (framework flushes @PlayerSession rows automatically)
```

- Deduplicate concurrent expensive fetches with a pending-request map, as `SkullCache`'s
  `pendingRequests` does, so N callers trigger one Mojang lookup.

## 2. Keep IO off the tick loop

- All DB/network/file work runs on async pools (`Tasks.db/io`), never on the main/region thread.
  A single blocking query on the tick loop is a server-wide stall. See [Threading.md](Threading.md).
- Use **write-behind** persistence (the `Database` default) so saves buffer and flush periodically
  instead of hitting the backend on every mutation.

## 3. Batch and preload

- **Preload** what a menu will render so it displays from cache: `SkullAPI.preloadPlayers(...)` /
  `batchPlayers(...)` avoids per-slot Mojang stalls.
- Batch DB reads (one ordered query for a leaderboard, not N `findById`s).
- Register entities/serializers once at startup, not per operation.

## 4. Render by diffing, not rebuilding

- The menu, scoreboard, and hologram systems **diff old vs new state and rewrite only what
  changed**. When you drive them, feed changes (via pagination suppliers / `PlaceholderContext`),
  don't reopen/recreate on every update.
- Use `RefreshMode.SMART` / `SLOT_ONLY` so only dynamic items are reprocessed.
- Prefer keyed updatables over re-sending: `TitleAPI.sendUpdatable(player, key, ...)`,
  `BossBarAPI.sendUpdatable(...)` instead of spamming `send` each tick.

## 5. Debounce and rate-limit

- User-driven actions are **debounced** by the framework (menu clicks/navigation ~150ms). Don't
  fight it with your own tick-fast handlers.
- Visual sends are **rate-limited per player** (`VisualLimiter`). Respect the caps; check the
  returned future on future-returning sends.
- For your own hot paths, apply cooldowns via **action `.cooldown(...)`** or **`ItemCooldownAPI`**
  rather than unbounded event handling. See [Reusability.md](Reusability.md).

## 6. Timers: coarse intervals, grouped schedulers

- Run periodic work at the **largest acceptable interval**. Scoreboards/holograms group work by
  interval (one scheduler per distinct interval), and countdowns render only every
  `updateInterval` tick even though they tick every tick — model your timers the same way.
- Cancel timers you own on disable (`TaskAPI.shutdown()` cancels framework-scheduled work at
  unload, but cancel long-lived `ScheduledTask`s you started).

## 7. Allocation discipline in hot loops

- Avoid per-tick allocations in tight loops (particle spawners, per-player render loops). Reuse
  builders/config objects (`TitleConfig` built once, reused per viewer).
- Prefer primitive-friendly structures and avoid boxing in inner loops.
- Copy `PlaceholderContext` (`.copy()`) per render only when necessary — the framework copies per
  tick where isolation is required, but don't allocate contexts you can reuse.

## 8. Measure before optimizing

- Use `TaskAPI.getStats()` / `getPoolStatus(category)` to spot pool saturation.
- Enable Caffeine `recordStats()` / `hitRate()` on your caches when tuning.
- Turn on debug categories ([../Debug.md](../Debug.md)) to trace hot paths rather than guessing.

---

## Checklist

- [ ] Player data: Caffeine/`ConcurrentHashMap`, loaded on join, cleared on quit (no TTL).
- [ ] No blocking IO on the tick loop; write-behind for saves.
- [ ] Preload/batch skulls and DB reads; register serializers once.
- [ ] Drive menus/scoreboards/holograms by change, not rebuild; use `SMART` refresh + updatables.
- [ ] Cooldowns/rate-limits on hot user actions.
- [ ] Timers at coarse intervals; owned timers cancelled on disable.
- [ ] No needless per-tick allocations; reuse config objects.

## Anti-patterns

- `findById` in a loop instead of one ordered/batch query.
- Reopening a menu every tick to "refresh" it.
- Re-sending titles/bars every tick instead of `sendUpdatable`.
- TTL-expiring per-session player data.
- Blocking Mojang/DB/HTTP calls on the main thread.
- Creating ad-hoc thread pools instead of using `TaskAPI` categories.
