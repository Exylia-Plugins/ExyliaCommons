# Visuals

## Overview

The visual subsystem is a unified, thread-aware facade for all transient client-facing effects:
titles, action bars, boss bars, chat messages, sounds, particles, potion effects, and fireworks —
plus the color/formatting engine ([ColorAPI](Formatting.md)). It supports one-shot ("simple"),
self-updating ("continuous"), per-player countdowns, and global (multi-viewer) countdowns, with a
per-player rate limiter.

**Key classes** (all facades under `v2/visual/api/`, `final`, private constructors)

| API | Responsibility |
|-----|----------------|
| `TitleAPI` | Titles + countdown + `sendUpdatable` + global countdown broadcast |
| `ActionBarAPI` | Action bars + countdown + updatable |
| `BossBarAPI` | Boss bars (stateful) + countdown |
| `MessageAPI` | Chat text: broadcast / filter / radius / centered / route |
| `SoundAPI` | Play sounds |
| `ParticleAPI` | Spawn particles |
| `FireworkAPI` | Spawn/detonate fireworks |
| `EffectAPI` | Potion/visual effects |
| `ColorAPI` | Formatting/preset engine (see [Formatting.md](Formatting.md)) |

Managed by `VisualManager` (`v2/visual/core/VisualManager.java`).

## Purpose

Client feedback (titles, bars, sounds, particles) is used everywhere. Centralizing it gives a
consistent, thread-safe, rate-limited API that works on Paper and Folia, with caching so repeated
identical renders are cheap, and countdown/animation helpers so timers don't require boilerplate.

## Initialization

`VisualManager.initialize(plugin)` and `ColorAPI.initialize(plugin)` are called **automatically**
during bootstrap. Every public send validates initialization (`validateInitialized()`) and throws
`IllegalStateException` if not initialized.

Re-initialization semantics: same plugin → `softReset()` (clears registries/caches, hides boss
bars, stays initialized); different plugin → full `shutdown()` then re-init.

## Usage Patterns

### Titles / Action Bars

Sends take a config object (e.g. `TitleConfig`) plus an optional `PlaceholderContext`. Updatable
sends are keyed by an identifier so you refresh a value instead of spamming new sends:

```java
// Self-updating title keyed by "score" — verbatim signature:
// TitleAPI.sendUpdatable(Player player, String key, TitleConfig config, PlaceholderContext context)
TitleAPI.sendUpdatable(player, "score", titleConfig, context);

// Cancel:
TitleAPI.cancelAll(player);
```

Build the `TitleConfig`/`ActionBarConfig` via their builders (or load them from config via the
registered serializers). Text supports color presets and placeholders.

### Boss Bars (stateful — always cancel through the API)

Boss bars are stateful; cancel them through `BossBarAPI` so the bar is actually hidden. Verbatim:

```java
boolean BossBarAPI.cancel(Player player, String bossBarId);
void    BossBarAPI.cancelAll(Player player);      // hides all bars for the player
boolean BossBarAPI.cancelGlobalCountdown(String id);
```

### Countdowns

Per-player countdowns are keyed; global multi-viewer countdowns parse the component once and
batch-send to all viewers:

```java
TitleAPI.broadcastCountdown(...);   // one component parse, batched send
```

### Messages

```java
MessageAPI.send(player, "{primary}Hello");
MessageAPI.broadcast("{info}Server restarting soon");
MessageAPI.broadcastRadius(location, radius, "{warning}Boss spawned");
// filtered / centered / routed variants also available
```

### Sounds / Particles / Fireworks

```java
SoundAPI.play(player, Sound.UI_BUTTON_CLICK);
ParticleAPI.spawn(location, Particle.FLAME, count);
FireworkAPI.spawn(location, ...); // spawn + detonate
```

For complex choreographed effects, prefer the [Sequence](Sequence.md) engine.

## Rate Limiting (`VisualLimiter`)

Per-player caps prevent spam. Total default `DEFAULT_MAX_PER_PLAYER = 20`; per-type: `ACTIONBAR=5`,
`BOSSBAR=10`, `TITLE=3`, `PARTICLE=100`, `SOUND=50`, `MESSAGE=50`, `EFFECT=20`, `FIREWORK=10`
(unlisted default 10). Over-limit sends return
`CompletableFuture.failedFuture(new LimitExceededException(...))` — check the returned future if
you must know.

## Configuration

- `colors.yml` — color/gradient presets (see [Formatting.md](Formatting.md)).
- A disabled config (`enabled: false`), a null player, or an offline player causes a silent no-op
  via `validateParameters`.

## Threading Considerations

- All sends validate initialization and are safe to call; the manager routes work appropriately.
- Countdown timers run at 1-tick granularity but only **render** every `updateInterval` ticks.
- Particle/firework spawning is location/entity work — on Folia it must reach the owning region
  thread; use [TaskAPI](TaskAPI.md) `at(...)` if you are not already on it.

## Best Practices

- Use `sendUpdatable`/keyed countdowns for live values instead of spamming `send`.
- For multi-viewer timers use `broadcastCountdown` (single parse, batched send).
- Always cancel boss bars through `BossBarAPI.cancel/cancelAll` so the bar is hidden.
- Define colors/gradients as presets and reference `{preset}`; reload with
  `ColorAPI.reloadPresets()`.
- Check the returned future when a send might exceed the per-player limit.

## Common Mistakes

- Calling any `send*` before `VisualManager.initialize` → `IllegalStateException` (only relevant
  in unusually early code; bootstrap initializes it).
- Using a generic manager cancel for boss bars instead of `BossBarAPI.cancel*` → the bar stays
  visible.
- Spamming `send` every tick instead of using updatable/countdown helpers → hits the limiter.
- Spawning particles off the owning region thread on Folia.

## Performance Considerations

- Caffeine-backed strip cache + per-player render cache short-circuit re-parsing unchanged text.
- Global countdowns parse the component once and batch-send.
- Countdown timers render only every `updateInterval` ticks.

## Relationship With Other Systems

- Uses [ColorAPI](Formatting.md) for all text and [Placeholders](Placeholders.md) via
  `PlaceholderContext`.
- Runs timers on [TaskAPI](TaskAPI.md).
- Reset/reloaded by the [Reload](Reload.md) system (`VisualAdapter`).
- The [Sequence](Sequence.md) engine and [Scoreboards](Scoreboards.md)/[Holograms](Holograms.md)
  build on the same visual primitives.
