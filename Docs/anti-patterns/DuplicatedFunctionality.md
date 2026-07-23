# Anti-Pattern: Duplicated Functionality

ExyliaCommons contains a few overlapping systems. Using the wrong one, or mixing them, causes subtle
bugs. Know which is which and pick deliberately.

---

## 1. Two Redis stacks

There are **two independent Redis implementations**:

| Stack | Class | Owner / users | Config |
|-------|-------|---------------|--------|
| Standalone | `v2/redis/SimpleRedis` | [Channel](../Channel.md) system, general use | `redis.yml` |
| Database pool | `v2/database/redis/RedisConnectionPool` | [Database](../Database.md) L2 cache + invalidation, [Teleport](../Teleport.md) | `database.yml` (`redis:` section) |

**Anti-pattern:** assuming they share a connection/config, or picking the wrong one.

- The **database** pool backs L2 caching, `RedisInvalidationBus`, and cross-server **teleport**.
- **`SimpleRedis`** backs cross-server **channels** and is what you use for your own
  key/value + pub/sub coordination.

**Preferred:**
- Cross-server messaging / your own Redis usage → **`SimpleRedis.get()`** ([../Redis.md](../Redis.md)).
- Persistence + cache + teleport → the **Database** layer (it manages its own pool).
- **Never create a third Jedis pool** — reuse one of these.

---

## 2. Two plugin base classes / bootstrap paths

There are two ways a plugin boots the framework:

| Base | File | Notes |
|------|------|-------|
| `ExyliaPlugin` (`net.exylia.commons`) | standard `JavaPlugin` subclass | The normal path |
| `ExyliaLoaderPlugin` (`v2/loader`) | Lukittu `LoaderPlugin` | For protected/loader-based plugins |

Both call `SystemBootstrapper.initializeCoreSystemsAsync(...)` and `ReloadAPI.initialize(...)`, so
bootstrap logic is **duplicated** and can drift.

**Important behavioral difference:** `ExyliaPlugin.onDisable` calls `TaskAPI.shutdown()` when the
last plugin unregisters; **`ExyliaLoaderPlugin.shutdown` does not call `TaskAPI.shutdown()`** — it
only runs the `ShutdownCoordinator` (which shuts down TaskAPI only if it's still initialized). Don't
assume identical teardown between the two.

**Preferred:** use **`ExyliaPlugin`** unless you specifically need the Lukittu loader. Whichever you
use, put logic in the `onExyliaEnable`/`onExyliaDisable` hooks — not in a duplicated bootstrap of
your own.

---

## 3. Three "cooldown" concepts

`cooldown` appears in three unrelated packages:

| Package | Purpose |
|---------|---------|
| `v2/cooldown` (`ItemCooldownAPI`) | Durable **item** cooldowns with vanilla overlay, region/world tuning |
| `v2/action/cooldown` | The **action pipeline's** cooldown middleware (`.cooldown(...)` on an action) |
| `v2/clientapi/cooldown` (`CooldownAPI`) | **Client-side** cooldown *display* (Lunar/Feather) |

Plus `v2/channel/core/CooldownManager` for per-channel chat cooldowns.

**Anti-pattern:** importing the wrong `CooldownAPI`/manager, or rolling your own `Map<UUID, Long>`.

**Preferred:**
- Ability/button cooldown → **action `.cooldown(duration, unit)`** ([../Actions.md](../Actions.md)).
- Consumable/gadget with an item overlay → **`ItemCooldownAPI`** (needs `ClientAPI.initialize`).
- Client HUD cooldown rendering → **`clientapi.cooldown.CooldownAPI`** (Lunar/Feather only).
- Never hand-roll cooldown maps. See [Performance](Performance.md).

---

## 4. `TaskAPI` vs `Tasks`

Two scheduling facades exist over the same engine:

- `TaskAPI` — descriptive names (`async`, `syncLater`, `atTimer`, `databaseThenSync`).
- `Tasks` — short names (`run`, `db`, `io`, `sync`, `later`, `timer`, `at`).

They are **equivalent** — not a bug, but a consistency hazard.

**Anti-pattern:** mixing both randomly across a plugin so readers can't tell them apart.

**Preferred:** pick **one** convention per plugin and use it consistently. (Both are fine; just
don't interleave.) See [../TaskAPI.md](../TaskAPI.md).

---

## 5. Async formatter vs sync formatter

`FormatterAPI` (sync) and `AsyncFormatterAPI` (async) mirror each other.

**Anti-pattern:** calling `FormatterAPI` inside a heavy async batch (fine, but pointless double
work) or calling `AsyncFormatterAPI` on the main thread for a single value (needless future).

**Preferred:** `FormatterAPI` for normal (main-thread) formatting; `AsyncFormatterAPI` only inside
async pipelines (e.g. formatting a large leaderboard off-thread). See [../Formatting.md](../Formatting.md).

---

## General rule

When two systems overlap, the correct one is determined by **who owns the resource** and **what
thread/lifecycle it belongs to**. Choose deliberately, document the choice, and never introduce a
third variant of something the framework already provides.
