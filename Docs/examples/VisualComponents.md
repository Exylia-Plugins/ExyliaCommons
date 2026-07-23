# Example: Visual Components

Goal: send titles, action bars, boss bars, sounds, and particles the Exylia way — using config
objects, countdowns, and Folia-correct scheduling for world effects.

Related: [Visuals](../Visuals.md), [Formatting](../Formatting.md), [TaskAPI](../TaskAPI.md).

`TitleAPI`, `ActionBarAPI`, and `BossBarAPI` are initialized automatically in bootstrap. Text
supports `&` codes, MiniMessage, `{preset}` color presets ([Formatting](../Formatting.md)), and
placeholders.

---

## Titles

```java
TitleConfig title = TitleConfig.builder()
    .title("{primary}&lWELCOME")
    .subtitle("{muted}Enjoy your stay, %player_name%")
    .fadeIn(10).stay(60).fadeOut(20)  // ticks
    .build();

// send returns a CompletableFuture<String> (the visual id).
TitleAPI.send(player, title, PlaceholderContext.create().withPlayer(player));
// TitleAPI.cancelAll(player);  // clear
```

## Action bars

```java
ActionBarConfig bar = ActionBarConfig.builder("{success}+1 Kill &7(streak: %streak%)")
    .updateInterval(20L)
    .build();

ActionBarAPI.send(player, bar, PlaceholderContext.create().withPlayer(player).put("streak", streak));
```

## Boss bars (stateful — always cancel through the API)

```java
BossBarConfig bossBar = BossBarConfig.builder("{warning}Event starting soon")
    .color(BossBar.Color.YELLOW)
    .style(BossBar.Overlay.PROGRESS)
    .progress(1.0)
    .build();

// Keep it alive and update it by key rather than re-sending each tick:
BossBarAPI.sendUpdatable(player, "event-bar", bossBar, context);

// Later, hide it (do NOT rely on a generic manager cancel):
BossBarAPI.cancel(player, "event-bar");
// or BossBarAPI.cancelAll(player);
```

## Countdowns

Countdowns render only every update interval and clean themselves up. Use the global broadcast
variant for multi-viewer timers (parsed once, batch-sent).

```java
// Per-player 30s title countdown
TitleAPI.countdown(player, 30, title, context);

// Global (all viewers) boss-bar countdown, keyed by id
BossBarAPI.countdown(player, 30, bossBar, context);
TitleAPI.broadcastCountdown("event-start", 30, title); // returns a builder for all viewers
```

## Sounds

```java
SoundAPI.play(player, Sound.UI_BUTTON_CLICK);
SoundAPI.play(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f); // volume, pitch
SoundAPI.playAt(location, Sound.BLOCK_ANVIL_LAND);              // location-based
```

## Particles & fireworks (Folia-correct)

Particle/firework spawning is world work — on Folia it must run on the region thread owning the
location. Schedule with `Tasks.at(location, ...)` if you are not already on it.

```java
Tasks.at(location, () -> {
    ParticleAPI.spawn(location, Particle.FLAME, 30);
    FireworkAPI.launch(location, FireworkEffect.Type.BALL_LARGE, Color.RED, Color.ORANGE);
});
```

> For **choreographed** effect sequences (particles + sounds + delays), prefer the
> [Sequence engine](../Sequence.md) instead of hand-scheduling.

---

## Why this way

- **Config objects** (`TitleConfig`, `ActionBarConfig`, `BossBarConfig`) keep parameters explicit
  and reusable, and integrate with the config serializers so the same visuals can be loaded from
  YAML.
- **Updatable/countdown helpers** avoid spamming sends every tick and stay within the per-player
  [rate limiter](../Visuals.md#rate-limiting-visuallimiter).
- **`Tasks.at`** guarantees particles/fireworks run on the correct region thread on Folia.

## Common mistakes

- Passing a raw `String` to `TitleAPI.send` — it takes a `TitleConfig`.
- Re-sending a boss bar every tick instead of `sendUpdatable(..., key, ...)`.
- Cancelling a boss bar with anything other than `BossBarAPI.cancel/cancelAll` — the bar stays
  visible.
- Spawning particles off the region thread on Folia — wrap in `Tasks.at(location, ...)`.
- Using `FireworkAPI.spawn` / `MessageAPI.broadcastRadius` — the methods are `FireworkAPI.launch` /
  `MessageAPI.sendInRadius`.
