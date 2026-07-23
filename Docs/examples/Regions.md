# Example: Regions

Goal: define a cuboid region with flags and enter/exit callbacks, and query regions at a location.

Related: [Regions](../Regions.md), [Visuals](../Visuals.md).

`RegionAPI` is a **singleton** — only `initialize`/`getInstance`/`isInitialized` are static; all
other methods are called on `RegionAPI.getInstance()`. It is opt-in.

---

## Initialize

```java
@Override
protected void onExyliaEnable() {
    RegionAPI.initialize(this);
}
```

## Build and register a region

Build with `createRegion(id)`. Presets (`safeZone()`, `membersOnly()`, `playerBuildOnly()`) are
no-arg builder methods. Callbacks are `RegionCallback` = `(player, region) -> ...`.

```java
RegionAPI regions = RegionAPI.getInstance();

Region hub = regions.createRegion("hub")
    .selection(corner1, corner2)                 // two opposite corners
    .displayName("&aHub")
    .priority(RegionPriority.HIGH)
    .safeZone()                                  // preset: no PvP, no damage, etc.
    .flag(RegionFlag.BUILD, false)               // explicit flag override
    .onEnter((player, region) -> {
        TitleAPI.send(player, TitleConfig.builder()
            .title("{primary}Hub")
            .subtitle("{muted}Welcome!")
            .build());
        SoundAPI.play(player, Sound.ENTITY_PLAYER_LEVELUP);
    })
    .onExit((player, region) ->
        MessageAPI.send(player, "{muted}Leaving the hub."))
    .build();

regions.registerRegion(hub);
```

`RegionFlag` values include `PVP`, `BUILD`, `BREAK`, `INTERACT`, `PLAYER_BUILD_ONLY`,
`ALLOWED_BLOCKS_ONLY`, `BREAKABLE_BLOCKS_ONLY`, `REGION_MEMBERS_ONLY`, `ENTRY`, `EXIT`,
`ITEM_DROP`, `ITEM_PICKUP`, `FALL_DAMAGE`, and more.

## Query regions

```java
RegionAPI regions = RegionAPI.getInstance();

// Highest-priority region at a location (respects priority ordering):
regions.getHighestPriorityRegionAt(location)
    .ifPresent(region -> DebugAPI.logPluginDebug("At region: " + region.getId()));

// All regions a player is currently inside:
Set<Region> inside = regions.getPlayerRegions(player);

boolean inHub = regions.getRegion("hub")
    .map(r -> regions.isPlayerInRegion(player, r))
    .orElse(false);
```

## Admin region creation with a wand

Give an admin the selection wand, then create a region from their selection:

```java
RegionAPI regions = RegionAPI.getInstance();
regions.giveWand(player);   // player selects two corners with the wand

// Later, on a confirm command/button:
Region arena = regions.createRegionFromSelection("arena-" + name, player);
```

---

## Why this way

- **Declarative flags + presets** cover common protection rules without custom event handling.
- **Cancellable pre-events** (see [Regions](../Regions.md#events--access-control)) let you enforce
  custom access control; the `onEnter`/`onExit` callbacks handle side effects like titles/sounds.
- **Priority-aware queries** return the correct region when several overlap.

## Common mistakes

- Calling instance methods statically — only `initialize`/`getInstance`/`isInitialized` are static.
- Using `RegionAPI.getInstance()` before `initialize()` → `IllegalStateException`.
- Passing a lambda of the wrong shape to `onEnter`/`onExit` — `RegionCallback` is
  `(player, region) -> ...`.
- Expecting schematic features without WorldEdit/FAWE installed.
