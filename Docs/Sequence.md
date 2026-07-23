# Sequence Engine

## Overview

The sequence engine executes **declarative lists of visual/audio effects** defined in YAML. It is
the recommended way to build choreographed effects (kill effects, arrow trails, ability casts)
rather than hand-scheduling particles, sounds, and delays. Consumed by ExyliaArrows,
ExyliaKillEffect, and any future Exylia plugin.

> **Effect grammar reference:** see [`../SEQUENCE_API.md`](../SEQUENCE_API.md) at the repo root. It
> documents the core tokens (`[PARTICLE]`, `[SOUND]`, `[LIGHTNING]`, `[EXPLOSION]`, `[FIREWORK]`,
> `[COMMAND]`, `[DELAY]`, `[POTION]`, `[BLOCK_BREAK]`, `[TITLE]`, `[ACTION_BAR]`, `[CIRCLE]`,
> `[SPHERE]`, `[BEAM]`, `[SPIRAL]`) with every parameter and default, plus full example effects.
> This document covers how the engine fits into the framework.

### Full token set

The `SequenceExecutor` dispatch (`v2/sequence/SequenceExecutor.java`) supports **30 tokens** —
more than `SEQUENCE_API.md` documents. In addition to the 15 core tokens above, these particle
**shape** tokens exist and take the same `PARTICLE;...` style parameters:

`[DOUBLE_HELIX]`, `[TORNADO]`, `[STAR]`, `[CAGE]`, `[DISC]`, `[VORTEX]`, `[WAVE]`, `[CROSS]`,
`[GALAXY]`, `[TORUS]`, `[BURST]`, `[PYRAMID]`, `[RING_PULSE]`, `[WINGS]`, `[ARCH]`, `[CLAW]`.

There is **no `[MESSAGE]` token** — use `[TITLE]` or `[ACTION_BAR]` for player-facing text.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `SequenceAPI` | `v2/sequence/SequenceAPI.java` | Static facade |
| `SequenceContext` | `v2/sequence/SequenceContext.java` | Execution context (location, source/target, filter) |
| `SequenceExecutor` | `v2/sequence/SequenceExecutor.java` | Parser + dispatcher for all effect tokens |
| `SequenceListener` | `v2/sequence/SequenceListener.java` | Registered by bootstrap |
| `EffectPreview` | `v2/sequence/preview/EffectPreview.java` | Admin preview support |

## Purpose

Effects are content, and content should be data, not code. The sequence engine lets designers
author rich effects in YAML (with delays, potions, commands, particle shapes) while the engine
handles parsing, thread-correct dispatch (Folia region threads), and rescheduling of delayed
steps.

## Initialization

The engine is wired up in bootstrap: `SequenceListener` is registered and `EffectPreview.init` is
called. `SequenceAPI` is ready to use from `onExyliaEnable` onward. No opt-in initialization is
required.

## Usage

### 1. Build a context

```java
SequenceContext ctx = SequenceContext.builder()
    .location(location)
    .sourcePlayer(killer)      // nullable — target of [TITLE]/[ACTION_BAR], {player} in [COMMAND]
    .targetEntity(victim)      // nullable — target of [POTION]
    .particleFilter(filter)    // optional per-player particle visibility
    .build();
```

### 2. Execute a list of effects

```java
// Dispatches via TaskAPI.at(location, ...) — runs on the correct region thread
SequenceAPI.execute(ctx, effect.getEffects());

// Already on the correct thread (e.g. inside an entity timer task)
SequenceAPI.getExecutor().executeOnCurrentThread(ctx, effect.getEffects());
```

### 3. YAML key

Use the key `effects` (a list of effect strings) in your effect definitions. Example:

```yaml
FIRE_KILL:
  category: elemental
  name: "&c&lFire Kill"
  material: BLAZE_POWDER
  effects:
    - '[CIRCLE] FLAME;radius:1.2;points:20;y:0.1'
    - '[SOUND] ENTITY_BLAZE_DEATH;1.5;0.8'
    - '[DELAY] 0.3'
    - '[EXPLOSION]'
    - '[FIREWORK] color:255,69,0;fade:255,200,0;type:BALL_LARGE'
```

## Delays & Rescheduling

`[DELAY] seconds` pauses the sequence and reschedules the **remaining** effects after the delay
via `TaskAPI.atLater`, on the correct region thread. This is what lets a single YAML list express a
timed choreography.

## Per-Player Particle Visibility

`SequenceContext` accepts a `particleFilter` `(observer, sourceId) -> boolean` so plugins can
honor per-player particle-visibility preferences (ALL / NONE / SELF_ONLY / OTHERS_ONLY). When
`null`, particles spawn normally via `world.spawnParticle()`. See `SEQUENCE_API.md` for the full
filter example.

## `[COMMAND]` runs on the console

The `[COMMAND]` token executes a **console** command directly (with `{player}`, `{world}`,
`{x/y/z}` placeholders). It does **not** go through [CommandAPI](Commands.md). Do not confuse the
sequence `[COMMAND]`/`[SOUND]` tokens with the [Action](Actions.md) subsystem — they are unrelated
systems with similar-looking bracket syntax. (There is **no `[MESSAGE]` token**; use `[TITLE]` or
`[ACTION_BAR]` for player-facing text.)

## Threading Considerations

- `SequenceAPI.execute` dispatches to the owning **region thread** via `TaskAPI.at`, so effects
  are Folia-safe.
- Use `executeOnCurrentThread` only when you are certain you are already on the correct region
  thread (e.g. inside an `atTimer` on the entity).

## Version Compatibility

Particle names resolve at runtime via `Particle.valueOf()` with fallbacks (e.g.
`EXPLOSION → EXPLOSION_LARGE → EXPLOSION_EMITTER`, `BLOCK → BLOCK_CRACK`). Use the enum name
matching your server version.

## Best Practices

- Author effects in YAML under the `effects` key; keep them data-driven.
- Build a `SequenceContext` with the correct source/target so `[TITLE]`, `[POTION]`, `[COMMAND]`
  placeholders resolve.
- Use `[DELAY]` for choreography instead of manual scheduling.
- Always dispatch via `SequenceAPI.execute` unless you are already on the region thread.

## Common Mistakes

- Confusing sequence `[COMMAND]`/`[SOUND]` tokens with the [Action](Actions.md) system.
- Referencing a `[MESSAGE]` token — it does not exist; use `[TITLE]`/`[ACTION_BAR]`.
- Calling `executeOnCurrentThread` from the wrong thread.
- Using `DUST` particles without a `color` param.
- Assuming a particle enum name exists on all versions (use the correct name for your version).

## Relationship With Other Systems

- Dispatches through [TaskAPI](TaskAPI.md) region scheduling.
- Overlaps conceptually with [Visuals](Visuals.md) primitives but is the choreographed,
  data-driven layer.
- `[COMMAND]` uses the Bukkit console directly, **not** [CommandAPI](Commands.md).
