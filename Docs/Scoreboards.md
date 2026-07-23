# Scoreboards

## Overview

The scoreboard subsystem renders per-player sidebar scoreboards defined in YAML, with placeholder
support, interval-based updates, smart line/title diffing, and optional TAB integration.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `ScoreboardAPI` | `v2/scoreboard/api/ScoreboardAPI.java` | Static facade |
| `ScoreboardManager` | `v2/scoreboard/...` | Singleton lifecycle, per-player boards |
| serializers | `v2/scoreboard/config/serializer/...` | YAML ↔ scoreboard model |
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

A scoreboard defines a title and a list of lines. Lines may contain placeholders and color
presets, and the board can specify an update interval. Lines are limited (**more than 15 lines
throws**).

```yaml
title: "{primary}&lMY SERVER"
update-interval: 20
lines:
  - "{muted}&m----------------"
  - "Player: {highlight}%player_name%"
  - "Rank: {accent}%vault_rank%"
  - "Online: {success}%server_online%"
  - "{muted}&m----------------"
```

(See the shipped `scoreboard/example.yml` for the authoritative key set.)

## Usage

Show, update, hide, and swap boards through `ScoreboardAPI` (per-player). Placeholder and color
computation runs off-thread; line/title diffing minimizes packets so only changed lines are
rewritten. Errors during rendering are wrapped in `ScoreboardRenderException`.

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

- Scoreboard `.yml` files (e.g. `scoreboard/example.yml`), with `title`, `lines`, and
  `update-interval`.

## Best Practices

- Keep line count within limits (≤15) and use `update-interval` sensibly (frequent updates cost
  packets).
- Use placeholders + color presets for dynamic, themed content.
- Let the listener handle world-change reinit; do not manually recreate boards on respawn (double
  schedules).
- Reload scoreboards through the [Reload](Reload.md) system.

## Common Mistakes

- Forgetting `ScoreboardAPI.initialize(plugin)` (not auto-initialized) → `IllegalStateException`.
- Exceeding 15 lines → throws.
- Manually recreating boards on world change/respawn — the listener already handles it.

## Relationship With Other Systems

- Uses [Placeholders](Placeholders.md) and [ColorAPI](Formatting.md).
- Runs on [TaskAPI](TaskAPI.md) with interval-grouped scheduling.
- Reloaded via the [Reload](Reload.md) system (`ScoreboardAdapter`).
- Integrates with TAB when installed.
