# Effects

## Overview

The effect subsystem defines and plays **configurable feedback** — particles, sounds, potions,
fireworks, titles, action bars, messages, and full sequence choreographies — from YAML, with
probability, conditions, permissions, priority, delay, and audience scope.

It is the audio/visual counterpart to [Rewards](Rewards.md): same shape, same gating fields, same
editor UI. Use it whenever a plugin needs *"play something when X happens"* to be configurable
rather than hardcoded — mining a block, winning a duel, opening a crate, casting an ability.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `EffectAPI` | `v2/effect/api/EffectAPI.java` | Static facade |
| `EffectBuilder` | `v2/effect/api/EffectBuilder.java` | Fluent single-effect builder |
| `EffectManager` | `v2/effect/core/EffectManager.java` | Singleton lifecycle |
| `EffectExecutor` | `v2/effect/core/EffectExecutor.java` | Gating, ordering, delay, scope dispatch |
| `EffectConfigLoader` | `v2/effect/config/EffectConfigLoader.java` | Parses effect YAML |
| `EffectSerializer` | `v2/effect/config/EffectSerializer.java` | Writes entries back to YAML |
| `EffectEntry` | `v2/effect/model/EffectEntry.java` | Parsed effect payload + gating |
| `EffectContext` | `v2/effect/model/EffectContext.java` | Player / location / placeholders |
| `EffectResult` | `v2/effect/model/EffectResult.java` | Per-effect outcome |
| `EffectType` | `v2/effect/model/EffectType.java` | `PARTICLE`, `SOUND`, `POTION`, `FIREWORK`, `TITLE`, `ACTIONBAR`, `MESSAGE`, `SEQUENCE` |
| `EffectScope` | `v2/effect/model/EffectScope.java` | `PLAYER`, `NEARBY`, `LOCATION`, `RADIUS`, `GLOBAL` |

> **Name collision warning.** `net.exylia.commons.v2.effect.api.EffectAPI` is this subsystem.
> `net.exylia.commons.v2.visual.api.EffectAPI` applies **potion effects** and is a lower-level
> visual primitive. Import the right one.

## Purpose

Effects are content, and content should be data. Designers author feedback in YAML — with chance,
conditions, and delays — while the engine handles parsing, gating, audience resolution, and
thread-correct dispatch (Folia region threads).

## Initialization

`EffectManager` is initialized **automatically** during bootstrap, right after `RewardManager` and
before `ActionAPI`. The editor UI actions are registered afterward by `EffectEditorActionRegistrar`.
No opt-in initialization is required.

## Effect Types

| Type | Payload fields | Notes |
|------|---------------|-------|
| `PARTICLE` | `particle`, `count`, `offset-x/y/z`, `extra`, `color`, `dust-size`, `block-material` | `DUST` requires `color` |
| `SOUND` | `sound`, `volume`, `pitch` | `pitch` must be 0.5 – 2.0 |
| `POTION` | `potion`, `amplifier`, `duration` / `duration-ticks`, `ambient`, `particles`, `show-icon` | `amplifier: 0` is level I |
| `FIREWORK` | `firework-type`, `colors`, `fade-colors`, `flicker`, `trail`, `power` | Colors are hex or `r,g,b` |
| `TITLE` | `title`, `subtitle`, `fade-in`, `stay`, `fade-out` | Times are in ticks |
| `ACTIONBAR` | `actionbar` | |
| `MESSAGE` | `message` or `messages`, `centered` | |
| `SEQUENCE` | `sequence` (token list) | Delegates to the [Sequence](Sequence.md) engine |

## Shared Fields

Every entry accepts these regardless of type:

| Field | Default | Notes |
|-------|---------|-------|
| `chance` | `100.0` | Probability percent |
| `condition` | none | Boolean expression; placeholders allowed |
| `permission` | none | Permission node required by the receiving player |
| `priority` | `0` | Higher runs first |
| `delay` / `delay-ticks` | `0` | `delay` is in **seconds** (decimal), `delay-ticks` in ticks |
| `scope` | `PLAYER` | Who receives it — see below |
| `radius` | `16.0` | Only used when `scope: RADIUS` |
| `name` | none | Display name in editor menus |
| `icon` | none | Preview icon in editor menus |

### Scope

| Scope | Audience |
|-------|----------|
| `PLAYER` | Only the context player |
| `NEARBY` | Players around the context player |
| `LOCATION` | Everyone who can see/hear the effect location |
| `RADIUS` | Players within `radius` blocks of the location |
| `GLOBAL` | Every online player |

## Defining Effects in YAML

Effects are read from a config section. `EffectAPI.play(player, section)` reads the key `effects`;
`playFromKey(player, section, key)` reads a custom key. The loader accepts **three shapes**:

### 1. Grouped form — recommended

```yaml
effects:
  particles:
    - particle: HAPPY_VILLAGER
      count: 12
      offset-x: 0.3
      offset-y: 0.5
      offset-z: 0.3
      scope: LOCATION
  sounds:
    - sound: BLOCK_STONE_BREAK
      volume: 1.0
      pitch: 1.2
    - sound: ENTITY_PLAYER_LEVELUP
      pitch: 1.8
      chance: 10.0
      delay: 0.25
  messages:
    - "{success}You mined a block!"
  sequence:
    - '[CIRCLE] FLAME;radius:1.2;points:20'
    - '[DELAY] 0.3'
    - '[SOUND] ENTITY_BLAZE_DEATH;1.5;0.8'
```

Group keys: `particles`, `sounds`, `potions`, `fireworks`, `titles`, `actionbars`, `messages`,
`sequence`. A `sequence:` group is a **single** entry holding a token list, not one entry per token.

Each group also accepts a shorthand string using `|` separators:

```yaml
effects:
  sounds:
    - "BLOCK_STONE_BREAK|1.0|1.2"     # sound|volume|pitch
  particles:
    - "FLAME|10|0.3|0.5|0.3"          # particle|count|offsetX|offsetY|offsetZ
```

### 2. List of typed entries

```yaml
effects:
  - type: SOUND
    sound: BLOCK_STONE_BREAK
    pitch: 1.2
  - type: PARTICLE
    particle: DUST
    color: "#8a51c4"
    count: 20
    scope: RADIUS
    radius: 12.0
  - type: TITLE
    title: "{primary}&lRARE DROP"
    subtitle: "{letters}You found something valuable"
    chance: 5.0
```

`type` may be omitted — it is inferred from whichever payload field is present.

### 3. Inline string list

```yaml
effects:
  - "sound: BLOCK_STONE_BREAK|1.0|1.2"
  - "particle: FLAME|10"
  - "[CIRCLE] FLAME;radius:1.2"        # leading [TOKEN] → sequence step
  - "{success}Nice find!"              # no prefix → message
```

## Global vs Per-Variant Effects

`playVariant` resolves effects with **override** semantics: if the variant declares its own
`effects`, they **fully replace** the global list; otherwise the global list is used.

```yaml
# mines.yml
effects:                          # global fallback for every block
  sounds:
    - sound: BLOCK_STONE_BREAK
      pitch: 1.2
  particles:
    - particle: CRIT
      count: 6
      scope: LOCATION

blocks:
  DIAMOND_ORE:                    # this block overrides the global list entirely
    effects:
      sounds:
        - sound: ENTITY_PLAYER_LEVELUP
          pitch: 1.6
      particles:
        - particle: DUST
          color: "#83d8ff"
          count: 24
          scope: RADIUS
          radius: 10.0
      titles:
        - title: "{highlight}&lDIAMOND!"
          chance: 100.0
  COAL_ORE: {}                    # no effects key → falls back to the global list
```

```java
// Mining listener — one call covers both global and per-block
EffectAPI.playVariant(player, mineSection, "blocks", block.getType().name(), block.getLocation());
```

Variant lookup is **case-insensitive**, so `diamond_ore` matches `DIAMOND_ORE`.

## Playing Effects

`EffectAPI` returns `EffectResult` (or a list) describing what happened per entry. Bukkit work is
dispatched on the region thread owning the effect location, so these calls are safe from async code.

```java
// From a config section (reads the "effects" key)
EffectAPI.play(player, section);

// From a custom key
EffectAPI.playFromKey(player, questSection, "completion-effects");

// Variant-or-global resolution
EffectAPI.playVariant(player, mineSection, "blocks", material.name());

// At a location with no player
EffectAPI.playAt(location, entries);

// Inspect outcomes
for (EffectResult result : EffectAPI.play(player, section)) {
    if (!result.isSuccess() && !result.isSkipped()) {
        getLogger().warning(result.getMessage());
    }
}
```

### Programmatic building

```java
EffectAPI.builder()
    .sound("BLOCK_STONE_BREAK", 1.0f, 1.2f)
    .radius(12)
    .chance(50)
    .delay(4)
    .play(player);
```

### Preloading

Parse once at load time instead of re-reading YAML per event:

```java
List<EffectEntry> breakEffects = EffectAPI.load(mineSection, "effects");
// later, per block break:
EffectAPI.play(player, breakEffects);
```

## Editor UI

`SelectorAPI.effectEditor(player)` opens an in-game editor for a list of effects — the same shape
as the reward editor. Every field above is editable, with a live preview button.

```java
SelectorAPI.effectEditor(player)
    .title("{primary}&lMINE EFFECTS")
    .entries(EffectAPI.load(mineSection, "effects"))
    .onSave((p, entries) -> {
        EffectSerializer.write(mineSection, "effects", entries);
        config.save();
    })
    .onCancel(() -> MineMenu.open(player))
    .open();
```

`EffectSerializer.write` emits the typed-list shape and omits default values, so the generated YAML
stays readable and round-trips through `EffectConfigLoader`.

### Registry pickers

Fields backed by a Minecraft registry (particle, sound, potion effect, firework shape) do **not**
ask the admin to type an id. They open a native Minecraft dialog listing every valid value as a
button — paged, searchable, and labelled in plain English (`HAPPY_VILLAGER` shows as
"Happy Villager").

That matters at scale: there are ~107 particles and ~1539 sounds. Sounds alone span 35 pages, but
typing `block stone break` narrows it to a single result.

```java
SelectorAPI.registry().particle(player)
    .onPick(name -> entry.setParticle(name))   // receives the raw id, e.g. HAPPY_VILLAGER
    .onCancel(() -> EffectEditMenu.open(player, entry))
    .open();
```

Available: `particle`, `sound`, `potionEffect`, `fireworkShape`, `material`, `block`. Each accepts
`.title(...)`, `.columns(n)`, `.pageSize(n)` and `.filter(predicate)` to narrow the list.

Entries that do not resolve on the running server version are dropped at collection time, so the
list can only ever offer values that actually work. Lists are cached after first use, since
registry contents do not change at runtime.

> On clients too old for dialogs this falls back to a paged inventory automatically — see
> [PlayerInteraction](PlayerInteraction.md).

### What the editor exposes

| Screen | Contents |
|--------|----------|
| List | Paginated effects; add, save, cancel, copy/paste one or all |
| Type picker | The 8 effect types |
| Edit | Payload + the shared gating fields + a live **preview** button |

Per-type buttons on the edit screen:

| Type | Extra buttons |
|------|--------------|
| `PARTICLE` | count, offset, color, speed |
| `SOUND` | volume, pitch |
| `POTION` | amplifier, duration, show particles, show icon |
| `FIREWORK` | colors, fade colors, power, flicker, trail |
| `TITLE` | subtitle, times |
| `MESSAGE` | centered |
| `SEQUENCE` | add step, clear steps |
| `ACTIONBAR` | — |

Color inputs accept hex (`#ff6b9d`) or `r,g,b` (`255,215,0`), comma separated for firework lists.

The **preview** button plays the effect on yourself with chance, condition, permission and delay
bypassed, so you always see it regardless of gating.

The editor is backed by the `commons:effect_*` actions. The potion-effect editor
(`SelectorAPI.potionEffectEditor`) is a **separate** system backed by `commons:potion_*`.

> A `FIREWORK` with no colors set renders **white** rather than failing validation, so an effect
> created in the editor is always playable before you customize it.

## Threading Considerations

- Gating (chance, permission, condition) is evaluated on the calling thread; it does no Bukkit work.
- Dispatch goes through `TaskAPI.at(location, ...)`, so effects are Folia-safe.
- `delay-ticks` reschedules via `TaskAPI.atLater` on the correct region thread.
- A delayed effect returns `EffectResult.delayed(...)` immediately; it does not report the eventual
  outcome of the delayed dispatch.

## Relationship With Other Systems

- Mirrors [Rewards](Rewards.md) in structure — same gating fields, same editor pattern.
- `SEQUENCE` entries delegate to the [Sequence](Sequence.md) engine, so all 30 tokens
  (`[CIRCLE]`, `[SPIRAL]`, `[DELAY]`, …) are available inside a configured effect.
- Dispatches through [TaskAPI](TaskAPI.md) region scheduling.
- Built on the [Visuals](Visuals.md) primitives (`ParticleAPI`, `SoundAPI`, `TitleAPI`, …); this is
  the declarative, data-driven layer above them.
- Reuses the reward `ConditionProcessor` and `ProbabilityProcessor`, so condition syntax is
  identical to rewards.

## Best Practices

- Author effects in YAML under an `effects` key; keep them data-driven.
- Use `playVariant` for per-block/per-tier feedback instead of `if/else` chains in Java.
- Preload with `EffectAPI.load` when the same effects fire on a hot path (block break, tick).
- Use `scope: LOCATION` or `RADIUS` for world events; `PLAYER` for personal feedback.
- Use `delay` for choreography instead of manual scheduling, or a `SEQUENCE` entry when the
  choreography is complex.
- Keep player-facing copy in `messages.yml` and reference it, rather than inlining strings.

## Common Mistakes

- Importing `visual.api.EffectAPI` (potions) when you meant `effect.api.EffectAPI` (this subsystem).
- Expecting a per-block `effects` list to **merge** with the global one — it **overrides** it.
- Using `DUST` particles without a `color`, which fails validation.
- Setting `pitch` outside 0.5 – 2.0.
- Treating `delay` as ticks — `delay` is in seconds; use `delay-ticks` for ticks.
- Assuming a delayed effect's `EffectResult` reports the final outcome; it reports scheduling only.
- Confusing `commons:effect_*` (this editor) with `commons:potion_*` (potion-effect editor).
