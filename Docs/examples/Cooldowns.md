# Example: Cooldowns

ExyliaCommons offers two production-grade cooldown mechanisms:

1. **Action cooldowns** — declared on an [action](../Actions.md) via the builder; the pipeline
   enforces them automatically. Best for ability/button behaviors.
2. **Item cooldowns** (`ItemCooldownAPI`) — durable, per-item cooldowns with a visual item overlay,
   region/world-specific durations, and use limits. Best for consumables/gadgets.

Related: [Actions](../Actions.md), [PlayerInteraction](../PlayerInteraction.md).

---

## 1. Action cooldown (recommended for abilities)

The action pipeline enforces cooldowns and permissions as middleware — you don't write cooldown
bookkeeping. A blocked action returns a failure `ActionResult` (logged as *expected*, not an
error).

```java
Action fireball = ActionAPI.create("fireball", this)
    .namespace("myplugin")
    .async()
    .permission("myplugin.ability.fireball")
    .cooldown(5, TimeUnit.SECONDS)       // enforced by the pipeline
    .handler((ctx, args) -> {
        Player player = ctx.getPlayer();
        Tasks.at(player.getLocation(), () -> launchFireball(player)); // world work on region thread
    })
    .build();

// build() already registered fireball.
```

Trigger it (e.g. from a menu button `actions: ["myplugin:fireball"]`, or directly):

```java
ActionAPI.executeAsync("myplugin:fireball",
    ActionContext.builder().player(player).source(ActionSource.CUSTOM).build())
    .thenAccept(result -> {
        if (!result.isSuccess()) {
            MessageAPI.send(player, "{error}Fireball is on cooldown!");
        }
    });
```

## 2. Item cooldowns (`ItemCooldownAPI`)

Item cooldowns are part of the **client** system. Initialize it once (this also registers the
required listeners), then register definitions.

```java
@Override
protected void onExyliaEnable() {
    ClientAPI.initialize(this);   // brings up ItemCooldownAPI + listeners

    // A gadget that can be used once every 30s, with a shorter duration inside "arena".
    ItemCooldownAPI.register(ItemCooldownDefinition.builder()
        .id("grappling_hook")
        .material(Material.FISHING_ROD)
        .displayName("&bGrappling Hook")
        .durationMs(30_000L)
        .trigger(CooldownTrigger.USE)
        .regionDurationsMs(Map.of("arena", 10_000L))   // per-region override
        .build());
}
```

Apply / query cooldowns:

```java
// Put the player on cooldown (with the item overlay shown on the given material):
ItemCooldownAPI.set(player, "grappling_hook", 30_000L, Material.FISHING_ROD);

// Guard usage:
if (ItemCooldownAPI.isOnCooldown(player, "grappling_hook")) {
    MessageAPI.send(player, "{error}On cooldown: &f"
        + ItemCooldownAPI.getFormattedRemaining(player, "grappling_hook"));
    return;
}
useGrapplingHook(player);
```

`CooldownTrigger` values: `USE`, `USE_ON_ELYTRA`, `ELYTRA_BOOST`, `CONSUME`, `LAUNCH`,
`BOW_SHOOT`, `RESURRECT`. Definitions also support `regionMaxUses` and `worldDurationsMs`.

---

## Which to use

| Need | Use |
|------|-----|
| Ability/button with a fixed cooldown | **Action `.cooldown(...)`** |
| Consumable/gadget with a visual item overlay, region/world tuning, or use limits | **`ItemCooldownAPI`** |

## Why this way

- **Action cooldowns** are declarative and enforced by the pipeline — no manual maps or timestamps.
- **Item cooldowns** persist, show the vanilla cooldown overlay, and support per-region/world rules
  out of the box.

## Common mistakes

- Hand-rolling `Map<UUID, Long>` cooldown tracking — use `.cooldown(...)` or `ItemCooldownAPI`.
- Using `ItemCooldownAPI` without `ClientAPI.initialize(this)` first — the listeners won't be
  registered.
- Blocking the main thread in an action handler — mark the action `.async()` and do world work via
  `Tasks.at(...)`.
