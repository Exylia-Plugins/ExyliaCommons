# Example: Wizards

Goal: guide a player through an interaction-driven, multi-step flow (pick a block, pick N points,
or select a cuboid region) with title/action-bar HUD feedback. Each wizard returns a
`CompletableFuture<T>` that completes when the player finishes.

Related: [PlayerInteraction](../PlayerInteraction.md#wizards-wizardapi), [Regions](Regions.md).

Initialize once in `onExyliaEnable`:

```java
WizardAPI.init(this);
```

Handlers run on the **main thread** (Bukkit interaction events) — keep them fast and offload heavy
work. Starting a wizard cancels any existing one for that player (one active per player).

---

## Interaction wizard (click a block)

Return a terminal `WizardResult` to complete the future; return `continueWizard()` to keep going.

```java
WizardAPI.<Location>interaction(player, (p, type, event) -> {
    Block clicked = event.getClickedBlock();
    if (clicked == null) {
        return WizardResult.continueWizard();          // ignore, keep waiting
    }
    return WizardResult.complete(clicked.getLocation()); // finish with the value
})
.thenAccept(loc -> {
    // Runs when the wizard completes. Bridge to the region thread for world work.
    Tasks.at(loc, () -> setSpawnBlock(loc));
    MessageAPI.send(player, "{success}Spawn block set!");
});
```

## Location wizard (pick N points)

```java
WizardAPI.<List<Location>>location(player, 2, (p, loc, all, remaining) -> {
    MessageAPI.send(p, "{muted}Point set. Remaining: " + remaining);
    if (remaining == 0) {
        return WizardResult.complete(all);              // both points chosen
    }
    return WizardResult.continueWizard();
})
.thenAccept(points -> createPortal(points.get(0), points.get(1)));
```

## Selection wizard (cuboid region, with wand)

Pass `giveWand = true` so the manager gives and manages the selection wand. SHIFT+LEFT-CLICK
confirms multi-area selections.

```java
WizardAPI.<Selection>selection(player, 1, /* giveWand */ true, (p, selection, all, remaining) -> {
    MessageAPI.send(p, "{muted}Area selected.");
    return WizardResult.complete(selection);
})
.thenAccept(selection -> {
    Region arena = RegionAPI.getInstance()
        .createRegion("arena")
        .selection(selection.getMinimumPoint(), selection.getMaximumPoint())
        .membersOnly()
        .build();
    RegionAPI.getInstance().registerRegion(arena);
    MessageAPI.send(player, "{success}Arena region created!");
});
```

## Custom HUD text

```java
WizardConfig config = WizardConfig.defaults()
    .withTitle("&eSelect Spawn", "&7Right-click a block")
    .withActionBar("&7Points remaining: %remaining%");

WizardAPI.<Location>interaction(player, config, (p, type, event) -> { /* ... */ });
```

## Cancellation

```java
if (WizardAPI.hasActive(player)) {
    WizardAPI.cancel(player);   // also happens automatically on quit
}
```

---

## Why this way

- **Interaction-driven** — the player uses the world to provide input; you return `WizardResult`
  values, never parse raw events.
- **`CompletableFuture<T>`** gives you a clean completion point per wizard.
- **Selection wizards integrate with [Regions](Regions.md)** — the wand + `Selection` feed straight
  into `createRegion`.
- **HUD feedback** via `WizardConfig` keeps the player oriented.

## Common mistakes

- Never returning a terminal `WizardResult` — the future stays pending until cancel/quit.
- Nesting wizards — starting one cancels the other (one active per player).
- Doing heavy work in the handler (main thread) — offload with `Tasks.io/db`, and do world work via
  `Tasks.at(location, ...)`.
- Forgetting `WizardAPI.init(this)` in `onExyliaEnable`.
