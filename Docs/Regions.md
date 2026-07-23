# Regions

## Overview

The region subsystem defines cuboid regions with flags, enter/exit callbacks, cancellable events,
and optional schematic support via WorldEdit/FastAsyncWorldEdit. It is used for safe zones,
member-only areas, event arenas, and any location-based rule.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `RegionAPI` | `v2/region/api/RegionAPI.java` | Singleton facade (instance methods via `getInstance()`) |
| `RegionManager` | `v2/region/core/...` | Registry, queries |
| `RegionBuilder` | `v2/region/api/RegionBuilder.java` | Fluent construction + presets |
| `Region` / `RegionFlag` / `RegionPriority` | `v2/region/model/...` | Model + flags |
| `RegionCallback` | `v2/region/...` | onEnter/onExit hooks |
| selection | `v2/region/selection/...` | `SelectionSession`, `Selection`, wands |
| schematic | `v2/region/schematic/...` | `SchematicManager`, WE/FAWE integration |

> **`RegionAPI` is a singleton with instance methods**, not a pure static facade. Only
> `initialize`, `getInstance`, and `isInitialized` are static; everything else is called on
> `RegionAPI.getInstance()`.

## Purpose

Location-based rules are common (protection, arenas, hubs). This subsystem centralizes region
definition, spatial queries, enter/exit detection, and rule enforcement via cancellable events, so
plugins don't reimplement region tracking.

## Initialization (opt-in)

`RegionAPI.initialize(plugin)` must be called explicitly (it is **not** bootstrapped).
`RegionAPI.getInstance()` throws `IllegalStateException` if used before init.

```java
@Override protected void onExyliaEnable() { RegionAPI.initialize(this); }
```

## Public API (selected, all on `RegionAPI.getInstance()`)

```java
RegionBuilder createRegion(String id);
boolean registerRegion(Region region);   boolean unregisterRegion(String regionId);
Optional<Region> getRegion(String regionId);   Collection<Region> getAllRegions();
List<Region> getRegionsAt(Location location);
Optional<Region> getHighestPriorityRegionAt(Location location);
Set<Region> getPlayerRegions(Player player);
boolean isPlayerInRegion(Player player, Region region);   boolean isPlayerInAnyRegion(Player player);

// Selection / wands
SelectionSession showSelector(Player player, Region region [, Color color]);
ItemStack giveWand(Player player);   ItemStack createWand([String selectionId]);   boolean isWand(ItemStack item);
Optional<Selection> getPlayerSelection(Player player);
void setSelectionPos1(Player, Location);   void setSelectionPos2(Player, Location);
void setSelectionCallback(Player, Consumer<Selection>);   void clearPlayerSelection(Player);
Region createRegionFromSelection(String regionId, Player player);
SchematicManager getSchematicManager();
```

## Defining Regions

Build with `RegionAPI.getInstance().createRegion(id)`. The builder offers **no-arg preset
methods** (`safeZone()`, `membersOnly()`, `playerBuildOnly()`) plus flags, owners/members,
priority, and enter/exit callbacks. Build via `.build()`, then register:

```java
RegionAPI regions = RegionAPI.getInstance();

Region hub = regions.createRegion("hub")
    .selection(corner1, corner2)          // or .selection(Selection)
    .safeZone()                           // preset (no args)
    .displayName("&aHub")
    .priority(RegionPriority.HIGH)
    .flag(RegionFlag.PVP, false)
    .onEnter(ctx -> { /* ... */ })
    .onExit(ctx -> { /* ... */ })
    .build();

regions.registerRegion(hub);
```

Available builder methods include: `selection`, `displayName`, `description`, `priority`,
`flag(RegionFlag, boolean | RegionFlagState)`, `owners(UUID...)`, `members(UUID...)`,
`metadata(key, value)`, `allowedBlocks`, `breakableBlocks`, `onEnter`, `onExit`,
`temporaryBlocks`/`temporaryBlocksSeconds`, and the presets `safeZone()`, `membersOnly()`,
`playerBuildOnly()`.

## Events & Access Control

Region events are provided (enter/exit and related), and **pre-events are cancellable** — use them
for custom access control. `RegionCallback` provides `onEnter`/`onExit` hooks for side effects.
Region actions integrate with the [Action](Actions.md) subsystem (`ActionSource.REGION`).

## Schematics (optional)

`SchematicType` selects a WorldEdit/FAWE-backed schematic operation. WorldEdit/FAWE are
`compileOnly` — ship them if you need large-region `.schem` performance; region logic works
without them, but schematic features require them.

## Threading Considerations

- Region enter/exit detection and block/world operations run on the correct region thread; on
  Folia, location-bound work must reach the owning region thread (use [TaskAPI](TaskAPI.md) if you
  spawn work yourself).
- Schematic paste/save via FAWE is designed to be async-capable; large operations should not block
  the main thread.

## Best Practices

- `initialize` in `onExyliaEnable`.
- Use the builder presets (`safeZone()`, `membersOnly()`, `playerBuildOnly()`) for common cases.
- Use the **cancellable pre-events** for custom access control rather than polling.
- Use `createRegionFromSelection` + the wand flow for admin-driven region creation.
- Ship WorldEdit/FAWE only if you use `.schem` features.

## Common Mistakes

- Calling instance methods statically — only `initialize`/`getInstance`/`isInitialized` are static;
  the rest are on `RegionAPI.getInstance()`.
- Using `RegionAPI.getInstance()` before `initialize()` → `IllegalStateException`.
- Expecting schematic features without WorldEdit/FAWE installed.

## Extension Points

- `RegionCallback` (onEnter/onExit), the six region events, and `SchematicType` selection.

## Relationship With Other Systems

- Fires [Actions](Actions.md) (`ActionSource.REGION`).
- Selection wands are used by the [Wizard](PlayerInteraction.md) system.
- Optional WorldEdit/FAWE integration for schematics.
- Reloaded via the [Reload](Reload.md) system (`RegionAdapter`).
