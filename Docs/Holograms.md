# Holograms

## Overview

The hologram subsystem renders floating text (using vanilla `TextDisplay` entities) defined in
YAML or built programmatically, with placeholder support, per-player content, async update loops,
spatial indexing for visibility, and optional YAML persistence.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `HologramAPI` | `v2/hologram/api/HologramAPI.java` | Static facade |
| `HologramManager` | `v2/hologram/core/...` | Singleton lifecycle, registry |
| `HologramBuilder` | `v2/hologram/api/HologramBuilder.java` | Fluent construction |
| `Hologram` / `HologramTemplate` | `v2/hologram/model/...` | Live hologram + reusable styling |
| exceptions | `v2/hologram/exception/...` | `HologramException`, `HologramSpawnException` |

## Purpose

Holograms are a common decorative/informational element (spawn points, leaderboards, shop labels).
This subsystem manages their lifecycle, placeholder-driven updates, per-player visibility, and
Folia-correct spawning, so plugins don't have to manage `TextDisplay` entities by hand.

## Initialization (opt-in)

Holograms are **not** auto-initialized by bootstrap. Call `HologramAPI.initialize(plugin)` in
`onExyliaEnable`. `HologramAPI.getManager()` / `getInstance()` access throws if used before init.
Persistence is silently disabled unless the YAML layer is initialized.

## YAML Format

A hologram uses a **nested `location:` section** (a flat `world/x/y/z` throws
`IllegalArgumentException("Location section is required")`), a `lines` list, a `properties:`
section (display styling), a `config:` section (update behavior), and the top-level flags
`persistent`, `perPlayer`, and `viewDistance`. This is the shipped `hologram/example.yml`, the
authoritative key set:

```yaml
location:
  world: world
  x: 0.5
  y: 100.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0
lines:
  - "&6Welcome to &eServer"
  - "&7Online: &f%players_online%"
  - "&7Rank: &f%player_rank%"
properties:
  billboard: CENTER          # TextDisplay billboard mode
  alignment: CENTER
  scale: { x: 1.5, y: 1.5, z: 1.5 }
  shadow: true
  seeThrough: false
  lineWidth: 200
  backgroundColor: { r: 0, g: 0, b: 0 }
  lineSpacing: 0.25
  brightness: -1
config:
  updateInterval: 20         # ticks
  autoUpdate: true
  spawnOnChunkLoad: true
  removeOnChunkUnload: true
persistent: true
perPlayer: false
viewDistance: 50.0
```

> Note the camelCase keys: `viewDistance`, `perPlayer`, `updateInterval` — **not** `view-distance`
> / `per-player` / `update-interval`.

## Public API (`HologramAPI`, static)

```java
void initialize(JavaPlugin plugin);   boolean isInitialized();

HologramBuilder create(String id, Location location);
CompletableFuture<Hologram> createAsync(String id, Location location, String... lines);
Optional<Hologram> get(String id);   Collection<Hologram> getAll();
List<Hologram> getNearby(Location location, double radius);
CompletableFuture<Boolean> remove(String id);   void removeAll();   void removeAllSync();

Hologram loadFromConfig(String id, ConfigurationSection section [, Location location]);
CompletableFuture<Hologram> loadFromConfigAsync(String id, ConfigurationSection section [, Location location]);
void saveToConfig(Hologram hologram, ConfigurationSection section);

// Templates (reusable styling)
HologramTemplate loadTemplateFromConfig(ConfigurationSection section);
HologramBuilder createFromTemplate(String id, Location location, HologramTemplate template);

void reload();   void shutdown([boolean isServerShutdown]);   HologramManager getManager();
```

## Building & Spawning (threading is strict)

- `HologramBuilder.build()` **throws `IllegalStateException` if called on the primary thread**
  (it would deadlock on an internal `.join()`). On the main thread, use `buildAsync()`.
- `Hologram.spawn()` guards: it **throws `IllegalStateException("Hologram must be spawned on the
  region thread for its location")` unless `Tasks.isRegionThread(location)` is true.**

The correct pattern (using the API, which handles this for you):

```java
// createAsync builds and spawns on the correct region thread internally:
HologramAPI.createAsync("leaderboard", location, "&6Leaderboard", "&e#1 %top_player%");

// If you build manually, spawn on the owning region thread yourself:
HologramAPI.create("id", location).lines("&aHi").buildAsync().thenAccept(holo ->
    TaskAPI.at(location, holo::spawn)
);
```

## Per-Player & Updates

- `perPlayer: true` renders player-specific content (use only when placeholders differ per
  viewer — it is more expensive).
- An async update loop refreshes lines; `UpdateScheduler` groups holograms by interval.
- Spatial chunk indexing limits visibility recomputation to nearby holograms; movement is
  throttled (~3 blocks) and per-player display cleanup runs every ~10s.

## Persistence

Set `persistent: true` (with the YAML layer initialized) to have holograms survive restarts via
YAML durability. Without YAML init, persistence is silently disabled.

## Threading Considerations

- **Never** call `build()` on the main thread — use `buildAsync()`.
- **Always** spawn/mutate a hologram's `TextDisplay` on its owning region thread (`TaskAPI.at`).
- Update loops run async; entity mutations are scheduled onto the correct thread.

## Best Practices

- Set a realistic `viewDistance`.
- Use `perPlayer` only when content actually differs per viewer.
- Mark `persistent` only when you want YAML durability.
- Prefer `createAsync` / `loadFromConfigAsync` so the framework handles region-thread spawning.
- Reload holograms through the [Reload](Reload.md) system (`HologramAdapter` → `reload()`).

## Common Mistakes

- Using a flat `world/x/y/z` instead of a nested `location:` section → `IllegalArgumentException`.
- Using `view-distance`/`per-player`/`update-interval` instead of `viewDistance`/`perPlayer`/
  `config.updateInterval`.
- Calling `HologramBuilder.build()` on the main thread → deadlock guard `IllegalStateException`.
- Spawning/mutating a hologram off its region thread → `IllegalStateException`.
- Expecting persistence without initializing the YAML layer → silently disabled.

## Relationship With Other Systems

- Uses [Placeholders](Placeholders.md) + [ColorAPI](Formatting.md).
- Strictly uses [TaskAPI](TaskAPI.md) region scheduling (`Tasks.isRegionThread` / `at`).
- Reloaded via the [Reload](Reload.md) system (`HologramAdapter`).
