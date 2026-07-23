# Holograms

## Overview

The hologram subsystem renders floating text (using vanilla `TextDisplay` entities) defined in
YAML or built programmatically, with placeholder support, per-player content, async update loops,
spatial indexing for visibility, and optional YAML persistence.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `HologramAPI` | `v2/hologram/api/HologramAPI.java` | Static facade |
| `HologramManager` | `v2/hologram/...` | Singleton lifecycle, registry |
| `HologramBuilder` | `v2/hologram/...` | Fluent construction |
| exceptions | `v2/hologram/exception/...` | `HologramException`, `HologramSpawnException` |

## Purpose

Holograms are a common decorative/informational element (spawn points, leaderboards, shop labels).
This subsystem manages their lifecycle, placeholder-driven updates, per-player visibility, and
Folia-correct spawning, so plugins don't have to manage `TextDisplay` entities by hand.

## Initialization (opt-in)

Holograms are managed by `HologramManager` and are **not** auto-initialized by bootstrap. Bring up
the manager and use `HologramAPI` from `onExyliaEnable`. Persistence is silently disabled unless
the YAML layer is initialized.

## YAML Format

A hologram defines a location, text lines (with placeholders/color), a view distance, and flags
like `perPlayer` and `persistent`. See the shipped `hologram/example.yml` for the authoritative
key set.

```yaml
example:
  world: world
  x: 100.5
  y: 65.0
  z: 100.5
  view-distance: 48
  per-player: false
  persistent: true
  lines:
    - "{primary}&lLeaderboard"
    - "{highlight}#1 %top_player%"
```

## Building & Spawning (threading is strict)

- `HologramBuilder.build()` **throws `IllegalStateException` if called on the primary thread**
  (it would deadlock on an internal `.join()`). On the main thread, use `buildAsync()`.
- `Hologram.spawn()` guards: it **throws `IllegalStateException` unless you are on the region
  thread** owning the hologram's location:
  `if (!Tasks.isRegionThread(location)) throw new IllegalStateException(...)`.

So the correct pattern is:

```java
HologramAPI.buildAsync(definition).thenAccept(holo ->
    TaskAPI.at(location, holo::spawn) // spawn on the owning region thread
);
```

## Per-Player & Updates

- `per-player: true` renders player-specific content (use only when placeholders differ per
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

- Set a realistic `view-distance`.
- Use `per-player` only when content actually differs per viewer.
- Mark `persistent` only when you want YAML durability.
- Reload holograms through the [Reload](Reload.md) system (`HologramManager.reload()`).

## Common Mistakes

- Calling `HologramBuilder.build()` on the main thread → deadlock guard `IllegalStateException`.
- Spawning/mutating a hologram off its region thread → `IllegalStateException`.
- Expecting persistence without initializing the YAML layer → silently disabled.

## Relationship With Other Systems

- Uses [Placeholders](Placeholders.md) + [ColorAPI](Formatting.md).
- Strictly uses [TaskAPI](TaskAPI.md) region scheduling (`Tasks.isRegionThread` / `at`).
- Reloaded via the [Reload](Reload.md) system (`HologramAdapter`).
