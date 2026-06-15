# SequenceAPI — ExyliaCommons

Centralized engine for executing sequences of visual/audio effects defined declaratively in YAML. Consumed by ExyliaArrows, ExyliaKillEffect, and any future Exylia plugin.

---

## Package

```
net.exylia.commons.v2.sequence
├── SequenceAPI.java       — static facade
├── SequenceContext.java   — execution context (location, player, target)
└── SequenceExecutor.java  — parser and dispatcher for all effect types
```

---

## Usage

### 1. Build a context

```java
SequenceContext ctx = SequenceContext.builder()
    .location(location)
    .sourcePlayer(killer)         // nullable
    .targetEntity(victim)         // nullable
    .particleFilter(filter)       // optional — see Particle Visibility below
    .build();
```

### 2. Execute a list of effects

```java
// Dispatches via TaskAPI.at(location, ...) — runs on the correct region thread
SequenceAPI.execute(ctx, effect.getEffects());

// Already on the correct thread (e.g. inside a timer task on the entity)
SequenceAPI.getExecutor().executeOnCurrentThread(ctx, effect.getEffects());
```

### 3. YAML key

Use the key `effects` in your YAML effect definitions:

```yaml
MY_EFFECT:
  category: elemental
  name: "My Effect"
  material: NETHER_STAR
  description: "A custom YAML effect"
  priority: 1
  effects:
    - '[PARTICLE] FLAME;count:30;offset:0.5,0.5,0.5;speed:0.1'
    - '[DELAY] 0.3'
    - '[LIGHTNING]'
```

---

## Effect Types

### `[PARTICLE]`

Spawn a vanilla particle at the effect location.

```
[PARTICLE] TYPE;count:N;offset:X,Y,Z;speed:F;y:F;color:R,G,B;size:F
```

| Param      | Description                                      | Default |
|------------|--------------------------------------------------|---------|
| `TYPE`     | Bukkit `Particle` enum name (required)           | —       |
| `count`    | Number of particles                              | `1`     |
| `offset`   | Random spread X,Y,Z                              | `0,0,0` |
| `speed`    | Particle speed / extra                           | `0`     |
| `y`        | Y offset added to the base location              | `0`     |
| `color`    | RGB color for DUST particles (e.g. `255,0,0`)    | —       |
| `size`     | Dust particle size                               | `1.0`   |

**Examples:**
```yaml
- '[PARTICLE] FLAME;count:50;offset:0.5,0.5,0.5;speed:0.1'
- '[PARTICLE] DUST;count:80;offset:0.5,0.8,0.5;color:139,0,0;size:2.0;y:1.0'
- '[PARTICLE] END_ROD;count:40;offset:0.5,0.5,0.5;speed:0.2'
- '[PARTICLE] SOUL;count:60;offset:0.3,1.5,0.3;speed:0.1;y:0.5'
```

> **Note:** When using `DUST`, always provide `color`. The `y` param shifts the particle origin upward from the kill/hit location.

---

### `[SOUND]`

Play a sound at the effect location.

```
[SOUND] SOUND_NAME;volume;pitch
```

| Param        | Description                 | Default |
|--------------|-----------------------------|---------|
| `SOUND_NAME` | Bukkit `Sound` enum name    | —       |
| `volume`     | Volume (0.0 – 2.0)         | `1.0`   |
| `pitch`      | Pitch (0.5 – 2.0)          | `1.0`   |

**Examples:**
```yaml
- '[SOUND] ENTITY_LIGHTNING_BOLT_THUNDER;2.0;1.0'
- '[SOUND] ENTITY_BLAZE_DEATH;1.5;0.8'
- '[SOUND] BLOCK_AMETHYST_BLOCK_CHIME;1.5;2.0'
```

---

### `[LIGHTNING]`

Strike a visual-only lightning bolt at the location (no fire, no damage).

```
[LIGHTNING]
```

---

### `[EXPLOSION]`

Visual explosion at the location (no damage, no block destruction).

```
[EXPLOSION]
```

---

### `[FIREWORK]`

Spawn a firework and immediately detonate it.

```
[FIREWORK] color:R,G,B;fade:R,G,B;type:TYPE;trail:true;power:N
```

| Param   | Description                                                 | Default      |
|---------|-------------------------------------------------------------|--------------|
| `color` | Main firework color (RGB)                                   | `255,0,0`    |
| `fade`  | Fade color (RGB)                                            | `255,165,0`  |
| `type`  | `BALL`, `BALL_LARGE`, `STAR`, `BURST`, `CREEPER`           | `BALL_LARGE` |
| `trail` | Firework trail                                              | `true`       |
| `power` | Firework flight power (0 = instant)                         | `0`          |

**Examples:**
```yaml
- '[FIREWORK] color:255,0,0;fade:255,200,0;type:BALL_LARGE;trail:true'
- '[FIREWORK] color:0,200,255;fade:255,255,255;type:STAR;trail:false'
```

---

### `[COMMAND]`

Execute a console command. Supports placeholders.

```
[COMMAND] your command here
```

| Placeholder | Replaced with                        |
|-------------|--------------------------------------|
| `{player}`  | Source player name (killer/shooter)  |
| `{world}`   | World name                           |
| `{x}`       | Block X of effect location           |
| `{y}`       | Block Y of effect location           |
| `{z}`       | Block Z of effect location           |

**Example:**
```yaml
- '[COMMAND] broadcast {player} got a kill!'
- '[COMMAND] give {player} diamond 1'
```

---

### `[DELAY]`

Pause the sequence and reschedule the remaining effects after N seconds. Uses `TaskAPI.atLater`.

```
[DELAY] seconds
```

**Example:**
```yaml
- '[PARTICLE] FLAME;count:30;offset:0.5,0.5,0.5;speed:0.1'
- '[DELAY] 0.5'
- '[LIGHTNING]'
- '[DELAY] 0.3'
- '[EXPLOSION]'
```

---

### `[POTION]`

Apply a potion effect to `targetEntity` (the hit entity / victim). No-op if target is null or not a `LivingEntity`.

```
[POTION] effect_type;duration;amplifier
```

| Param         | Description                            | Default |
|---------------|----------------------------------------|---------|
| `effect_type` | Minecraft effect key (e.g. `slowness`) | —       |
| `duration`    | Duration in ticks                      | `100`   |
| `amplifier`   | Level (0 = level I)                    | `0`     |

**Examples:**
```yaml
- '[POTION] slowness;60;1'
- '[POTION] blindness;40;0'
- '[POTION] poison;100;2'
```

---

### `[BLOCK_BREAK]`

Spawn block-crack particles at the location (visual only, no block is broken).

```
[BLOCK_BREAK] MATERIAL;count:N;offset:X,Y,Z;y:F
```

| Param      | Default    |
|------------|------------|
| `MATERIAL` | required   |
| `count`    | `20`       |
| `offset`   | `0.3,0.3,0.3` |
| `y`        | `0`        |

**Example:**
```yaml
- '[BLOCK_BREAK] OBSIDIAN;count:40;y:1.0'
- '[BLOCK_BREAK] REDSTONE_BLOCK;count:60;offset:0.5,0.5,0.5'
```

---

### `[TITLE]`

Send a title to `sourcePlayer`. No-op if sourcePlayer is null. Supports `&` color codes.

```
[TITLE] title;subtitle;fadeIn;stay;fadeOut
```

| Param      | Description         | Default |
|------------|---------------------|---------|
| `title`    | Title text          | `""`    |
| `subtitle` | Subtitle text       | `""`    |
| `fadeIn`   | Fade-in ticks       | `10`    |
| `stay`     | Stay ticks          | `70`    |
| `fadeOut`  | Fade-out ticks      | `20`    |

**Example:**
```yaml
- '[TITLE] &c&lKILL!;&7You eliminated your enemy;5;40;15'
```

---

### `[ACTION_BAR]`

Send an action bar message to `sourcePlayer`. Supports `&` color codes.

```
[ACTION_BAR] text
```

**Example:**
```yaml
- '[ACTION_BAR] &a+1 Kill'
```

---

### `[CIRCLE]`

Spawn particles in a horizontal circle around the location.

```
[CIRCLE] PARTICLE;radius:F;points:N;y:F;color:R,G,B;size:F;count:N
```

| Param    | Description                      | Default |
|----------|----------------------------------|---------|
| `radius` | Circle radius in blocks          | `1.0`   |
| `points` | Number of particles in the ring  | `16`    |
| `y`      | Height offset above location     | `0`     |
| `color`  | Dust color (for DUST type)       | —       |
| `size`   | Dust size                        | `1.0`   |
| `count`  | Particles per point              | `1`     |

**Examples:**
```yaml
- '[CIRCLE] DUST;radius:1.5;points:24;y:0.1;color:255,0,0;size:2.0'
- '[CIRCLE] END_ROD;radius:2.0;points:32;y:1.0'
- '[CIRCLE] FLAME;radius:1.0;points:16;y:0.5;count:2'
```

---

### `[SPHERE]`

Spawn particles uniformly distributed over a sphere using golden-angle distribution. Center is at location + 1 block up.

```
[SPHERE] PARTICLE;radius:F;points:N;color:R,G,B;size:F;count:N
```

| Param    | Default |
|----------|---------|
| `radius` | `1.0`   |
| `points` | `32`    |
| `color`  | —       |
| `size`   | `1.0`   |
| `count`  | `1`     |

**Example:**
```yaml
- '[SPHERE] DUST;radius:1.2;points:48;color:0,200,255;size:1.5'
```

---

### `[BEAM]`

Spawn particles in a vertical line going upward.

```
[BEAM] PARTICLE;height:F;points:N;y:F;color:R,G,B;size:F;count:N
```

| Param    | Description              | Default |
|----------|--------------------------|---------|
| `height` | Total height in blocks   | `3.0`   |
| `points` | Number of particles      | `20`    |
| `y`      | Starting Y offset        | `0`     |
| `color`  | Dust color               | —       |
| `size`   | Dust size                | `1.0`   |
| `count`  | Particles per point      | `1`     |

**Example:**
```yaml
- '[BEAM] END_ROD;height:4.0;points:25'
- '[BEAM] DUST;height:3.0;points:20;color:255,215,0;size:1.5'
```

---

### `[SPIRAL]`

Spawn particles in a helical spiral going upward (all particles rendered at once).

```
[SPIRAL] PARTICLE;height:F;radius:F;turns:N;points:N;y:F;color:R,G,B;size:F;count:N
```

| Param    | Description                    | Default |
|----------|--------------------------------|---------|
| `height` | Total height of the spiral     | `3.0`   |
| `radius` | Horizontal radius              | `1.0`   |
| `turns`  | Number of full rotations       | `2`     |
| `points` | Total particle count           | `40`    |
| `y`      | Starting Y offset              | `0`     |
| `color`  | Dust color                     | —       |
| `size`   | Dust size                      | `1.0`   |
| `count`  | Particles per point            | `1`     |

**Example:**
```yaml
- '[SPIRAL] FLAME;height:3.0;radius:0.8;turns:2;points:40'
- '[SPIRAL] DUST;height:4.0;radius:1.0;turns:3;points:60;color:200,0,255;size:1.5'
```

---

## Particle Visibility Filter

ExyliaArrows supports per-player particle visibility preferences. Pass a `particleFilter` to `SequenceContext` to control who sees each particle:

```java
SequenceContext ctx = SequenceContext.builder()
    .location(loc)
    .sourcePlayer(shooter)
    .particleFilter((observer, sourceId) -> {
        PlayerData data = playerManager.getPlayerData(observer.getUniqueId());
        ParticleVisibility vis = data != null ? data.getVisibilityMode() : ParticleVisibility.ALL;
        boolean isOwn = observer.getUniqueId().equals(sourceId);
        return switch (vis) {
            case ALL         -> true;
            case NONE        -> false;
            case SELF_ONLY   -> isOwn;
            case OTHERS_ONLY -> !isOwn;
        };
    })
    .build();
```

When `particleFilter` is `null` (default), particles are spawned via `world.spawnParticle()` normally.

---

## Full Effect Examples

### Fire Kill Effect
```yaml
FIRE_KILL:
  category: elemental
  name: "&c&lFire Kill"
  material: BLAZE_POWDER
  description: "Flames engulf the fallen"
  priority: 1
  effects:
    - '[CIRCLE] FLAME;radius:1.2;points:20;y:0.1'
    - '[BEAM] FLAME;height:3.5;points:22'
    - '[SOUND] ENTITY_BLAZE_DEATH;1.5;0.8'
    - '[DELAY] 0.3'
    - '[EXPLOSION]'
    - '[FIREWORK] color:255,69,0;fade:255,200,0;type:BALL_LARGE'
```

### Void Portal Kill Effect
```yaml
VOID_PORTAL:
  category: cosmic
  name: "&5&lVoid Portal"
  material: END_CRYSTAL
  description: "A portal to the void opens"
  priority: 2
  effects:
    - '[SPHERE] DUST;radius:1.0;points:40;color:75,0,130;size:1.5'
    - '[CIRCLE] DUST;radius:1.5;points:28;y:0.1;color:138,43,226;size:2.0'
    - '[SOUND] ENTITY_ENDERMAN_TELEPORT;1.5;0.5'
    - '[DELAY] 0.5'
    - '[SPIRAL] DUST;height:3.0;radius:0.8;turns:2;points:40;color:75,0,130;size:1.5'
    - '[SOUND] ENTITY_WARDEN_HEARTBEAT;1.0;0.5'
```

### Ice Crystal Kill Effect
```yaml
ICE_CRYSTAL:
  category: magical
  name: "&b&lIce Crystal"
  material: PACKED_ICE
  description: "Ice shatters the fallen"
  priority: 3
  effects:
    - '[BLOCK_BREAK] ICE;count:60;offset:0.5,0.5,0.5;y:0.5'
    - '[CIRCLE] DUST;radius:1.8;points:32;y:0.1;color:135,206,250;size:2.0'
    - '[BEAM] DUST;height:3.0;points:20;color:200,240,255;size:1.5'
    - '[SOUND] BLOCK_GLASS_BREAK;2.0;2.0'
    - '[DELAY] 0.2'
    - '[SOUND] BLOCK_GLASS_BREAK;1.5;1.5'
    - '[TITLE] &b&lFROZEN!;;5;30;10'
```

### Holy Light Kill Effect
```yaml
HOLY_LIGHT:
  category: premium
  name: "&e&lHoly Light"
  material: BEACON
  description: "Divine light descends"
  priority: 4
  effects:
    - '[BEAM] END_ROD;height:5.0;points:30;y:0.0'
    - '[CIRCLE] DUST;radius:2.0;points:36;y:0.1;color:255,255,200;size:2.0'
    - '[SOUND] BLOCK_BELL_USE;2.0;1.8'
    - '[DELAY] 0.4'
    - '[SPHERE] DUST;radius:1.5;points:48;color:255,255,150;size:1.2'
    - '[FIREWORK] color:255,255,200;fade:255,255,255;type:STAR;trail:true'
    - '[TITLE] &e&l✦ RIGHTEOUS ✦;;5;40;15'
    - '[ACTION_BAR] &eDivine judgement delivered'
```

---

## Version Compatibility

| Particle type constant | ExyliaCommons resolves at runtime via `Particle.valueOf()` with fallbacks |
|------------------------|---------------------------------------------------------------------------|
| `EXPLOSION`            | Tries: `EXPLOSION` → `EXPLOSION_LARGE` → `EXPLOSION_EMITTER`             |
| `BLOCK`                | Tries: `BLOCK` → `BLOCK_CRACK`                                            |

All other particle names are passed as-is to `Particle.valueOf()`. Use the enum name matching your server version.

---

## Consumers

| Plugin            | Context built in                       | YAML key  |
|-------------------|----------------------------------------|-----------|
| ExyliaArrows      | `ProjectileListener.buildContext()`    | `effects` |
| ExyliaKillEffect  | `KillEffectListener.applyKillEffect()` | `effects` |
