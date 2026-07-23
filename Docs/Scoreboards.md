# Scoreboards

## Overview

The scoreboard subsystem renders per-player sidebar scoreboards defined in YAML, with placeholder
support, interval-based updates, smart line/title diffing, and optional TAB integration.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `ScoreboardAPI` | `v2/scoreboard/api/ScoreboardAPI.java` | Static facade |
| `ScoreboardManager` | `v2/scoreboard/...` | Singleton lifecycle, per-player boards |
| `ScoreboardBuilder` | `v2/scoreboard/builder/ScoreboardBuilder.java` | Programmatic scoreboards |
| `ScoreboardLoader` | `v2/scoreboard/config/...` | YAML → scoreboard model |
| `ScoreboardInstance` | `v2/scoreboard/...` | A live per-player board |
| exceptions | `v2/scoreboard/exception/...` | `ScoreboardException`, `ScoreboardLimitException`, `ScoreboardRenderException` |

## Purpose

Sidebar scoreboards are a common HUD element. This subsystem makes them declarative and
per-player, handles placeholder resolution and update scheduling efficiently, and integrates with
TAB where present so it does not fight other plugins.

## Initialization (opt-in)

`ScoreboardAPI.initialize(plugin)` → `ScoreboardManager.initialize(plugin)`. **This is NOT called
by the bootstrap** — you must call it in `onExyliaEnable`. `ScoreboardManager.getInstance()` throws
`IllegalStateException` if not initialized. Init registers `ScoreboardListener` and calls
`TabIntegration.register(...)`.

```java
@Override
protected void onExyliaEnable() {
    ScoreboardAPI.initialize(this);
}
```

## YAML Format

A scoreboard defines a `title`, a `lines` list, and a nested `update:` section. Text may contain
placeholders and color presets. Lines are limited: **an empty `lines`, a missing `title`, or more
than 15 lines throws `IllegalArgumentException`** (not a scoreboard-specific exception). The
verbatim message for the line cap is `"Scoreboard cannot have more than 15 lines"`.

This is the shipped `scoreboard/example.yml`, which is the authoritative key set:

```yaml
title: "&6&lMi Servidor"
enabled: true
lines:
  - "&7-----------------"
  - "&eJugadores: &f%online%"
  - "&eDinero: &f$%money%"
  - "&7-----------------"
update:
  interval: 20      # ticks
  smart: true       # smart updates (diff lines/title, only rewrite changes)
  cache: true       # cache enabled
```

> The update interval is **nested** under `update.interval` (not a top-level `update-interval`).

## Usage

`ScoreboardAPI` (static) API:

```java
void initialize(Plugin plugin);
ScoreboardBuilder builder();
Scoreboard load(ConfigurationSection section);
CompletableFuture<String> show(Player player, Scoreboard scoreboard [, PlaceholderContext context]);
boolean hide(Player player);   boolean has(Player player);
Optional<ScoreboardInstance> get(Player player);
void updateContext(Player player, PlaceholderContext context);
void forceUpdate(Player player);
void hideAll();   int getActiveCount();   ScoreboardStats getStats();
```

```java
Scoreboard board = ScoreboardAPI.load(Configs.get("scoreboard").section("."));
ScoreboardAPI.show(player, board);
```

`show` returns a `CompletableFuture<String>` (the board id). Placeholder and color computation runs
off-thread; line/title diffing minimizes packets so only changed lines are rewritten. Errors during
rendering are wrapped in `ScoreboardRenderException`.

## Lifecycle

- Boards are **per-player**.
- The registered `ScoreboardListener` handles reinitialization on world change automatically — you
  do not need to recreate boards on world change yourself.
- Updates are grouped by interval: one scheduler per distinct interval rather than one per board.

## Threading Considerations

- Placeholder/color computation is offloaded; scoreboard packet writes are scheduled on the
  correct thread.
- Interval-grouped schedulers reduce timer churn.

## Configuration

- Scoreboard `.yml` files (e.g. `scoreboard/example.yml`) with `title`, `lines`, `enabled`, and a
  nested `update:` section (`interval`, `smart`, `cache`).

## Best Practices

- Keep line count within limits (≤15) and use `update.interval` sensibly (frequent updates cost
  packets); enable `update.smart` and `update.cache`.
- Use placeholders + color presets for dynamic, themed content.
- Let the listener handle world-change reinit; do not manually recreate boards on respawn (double
  schedules).
- Reload scoreboards through the [Reload](Reload.md) system.

## Common Mistakes

- Forgetting `ScoreboardAPI.initialize(plugin)` (not auto-initialized) → `IllegalStateException`.
- Using a top-level `update-interval` — the key is nested `update.interval`.
- Empty `lines`, missing `title`, or >15 lines → `IllegalArgumentException` (not
  `ScoreboardLimitException`).
- Manually recreating boards on world change/respawn — the listener already handles it.

## Relationship With Other Systems

- Uses [Placeholders](Placeholders.md) and [ColorAPI](Formatting.md).
- Runs on [TaskAPI](TaskAPI.md) with interval-grouped scheduling.
- Reloaded via the [Reload](Reload.md) system (`ScoreboardAdapter`).
- Integrates with TAB when installed.
