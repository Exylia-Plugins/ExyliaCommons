# Anti-Pattern: Performance Issues

These patterns work in a test world and fall over on a live network. Each has a framework-provided
alternative.

Reference: [../best-practices/Performance.md](../best-practices/Performance.md),
[../Database.md](../Database.md), [../Menus.md](../Menus.md).

---

## 1. Blocking IO on the tick loop

**Anti-pattern:** synchronous DB/network/file calls in join handlers, commands, menu building, or
timers.

```java
// BAD — stalls the whole server per call
Profile p = repository.findById(uuid).orElseThrow();
```

**Preferred:** async pools + apply-on-main. Use write-behind (the `Database` default) so saves
buffer instead of hitting the backend per mutation. See [Threading](Threading.md).

```java
TaskAPI.databaseThenSync(() -> repository.findById(uuid), opt -> opt.ifPresent(this::apply));
```

---

## 2. `findById` in a loop instead of one query

**Anti-pattern:** looping over players and fetching each row individually to build a leaderboard.

```java
// BAD — N queries
for (UUID id : ids) top.add(repo.findById(id).orElse(null));
```

**Preferred:** one ordered query.

```java
repo.findAllOrderedByAsync("balance", false, 100).thenAccept(this::render);
```

---

## 3. Reopening a menu every tick to "refresh" it

**Anti-pattern:**

```java
// BAD — rebuilds the whole inventory each tick
Tasks.timer(() -> MenuAPI.openAsync(player, section), 0L, 1L);
```

**Why:** the menu framework already diffs old vs new items and rewrites only changed slots.
Reopening throws that away and flickers.

**Preferred:** mark dynamic items `dynamic_update` with an `update_interval`, use
`RefreshMode.SMART`, and feed live data via a pagination supplier / `PlaceholderContext`. See
[Menus](Menus.md).

---

## 4. Re-sending titles/bars every tick

**Anti-pattern:** calling `TitleAPI.send(...)` / `ActionBarAPI.send(...)` on a 1-tick timer.

**Why:** it hammers the per-player [rate limiter](../Visuals.md#rate-limiting-visuallimiter) and
re-parses text every tick.

**Preferred:** keyed updatables / countdowns — one render, updated in place.

```java
ActionBarAPI.sendUpdatable(player, "cooldown", config, context);
```

---

## 5. TTL-expiring per-session player data

**Anti-pattern:** a Caffeine cache with `expireAfterWrite` for data whose lifecycle is the player's
session.

**Why:** the data can expire mid-session, causing reloads/loss; it doesn't clear promptly on quit.

**Preferred:** load on join, clear on quit. Use `@PlayerSession` (auto-flush on quit) plus a
`ConcurrentHashMap` session cache you populate on join and `remove` on quit. See
[../examples/Listeners.md](../examples/Listeners.md).

---

## 6. Synchronous skull fetching / per-slot Mojang calls

**Anti-pattern:** building a head-heavy menu and letting each head fetch from Mojang on demand.

**Why:** blocks and hits rate limits; the sync path returns cache/default anyway.

**Preferred:** **preload/batch** before opening.

```java
SkullAPI.preloadPlayers(names); // then sync fromPlayer(...) renders from cache
```

See [../Skulls.md](../Skulls.md).

---

## 7. Hand-rolled cooldown maps

**Anti-pattern:** `Map<UUID, Long>` timestamp bookkeeping for abilities.

**Preferred:** action `.cooldown(...)` (pipeline-enforced) or `ItemCooldownAPI`. See
[DuplicatedFunctionality](DuplicatedFunctionality.md).

---

## 8. Fine-grained timers and per-tick allocation

**Anti-pattern:** a 1-tick timer per board/hologram, or allocating new builders/contexts every tick
in a render loop.

**Why:** the framework groups periodic work by interval (one scheduler per distinct interval) and
reuses config objects; per-entity 1-tick timers and per-tick allocations don't scale.

**Preferred:**
- Run periodic work at the **largest acceptable interval**; let scoreboards/holograms group by
  interval.
- Build config objects **once** and reuse them per viewer/tick.

```java
private static final TitleConfig ROUND_START = TitleConfig.builder().title("{primary}FIGHT").build();
```

---

## 9. Not measuring

**Anti-pattern:** guessing at hotspots.

**Preferred:** `TaskAPI.getStats()` / `getPoolStatus(category)` for pool saturation, Caffeine
`recordStats()`/`hitRate()` for caches, and debug categories ([../Debug.md](../Debug.md)) to trace
hot paths.

---

## Checklist

- [ ] No blocking IO on tick/event threads; write-behind for saves.
- [ ] Batch/ordered queries, not per-entity `findById`.
- [ ] Menus refresh by diff (`SMART` + `dynamic_update`), never reopen loops.
- [ ] Titles/bars use `sendUpdatable`, not per-tick `send`.
- [ ] Session data loaded on join / cleared on quit (no TTL).
- [ ] Skulls preloaded/batched.
- [ ] Cooldowns via action/`ItemCooldownAPI`, not manual maps.
- [ ] Coarse timers, grouped; config objects reused; minimal per-tick allocation.
