# Regions

## Overview

The region subsystem defines cuboid regions with flags, enter/exit callbacks, cancellable events,
and optional schematic support via WorldEdit/FastAsyncWorldEdit. It is used for safe zones,
member-only areas, event arenas, and any location-based rule.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `RegionAPI` | `v2/region/api/RegionAPI.java` | Static facade |
| `RegionManager` | `v2/region/...` | Singleton lifecycle, registry, queries |
| `RegionBuilder` | `v2/region/...` | Fluent construction + presets |
| region events | `v2/region/event/...` | Enter/exit/etc. (cancellable) |
| `RegionCallback` | `v2/region/...` | onEnter/onExit hooks |
| selection | `v2/region/selection/...` | `SelectionManager`, `Selection`, wands |
| schematic | `v2/region/schematic/...` | `SchematicType`, WE/FAWE integration |

## Purpose

Location-based rules are common (protection, arenas, hubs). This subsystem centralizes region
definition, spatial queries, enter/exit detection, and rule enforcement via cancellable events, so
plugins don't reimplement region tracking.

## Initialization (opt-in)

`RegionAPI.initialize(plugin)` / `RegionManager.initialize` must be called explicitly (it is
**not** bootstrapped). Call it in `onExyliaEnable` and `cleanup()` in `onExyliaDisable`.
`RegionManager.getInstance()` throws `IllegalStateException` if used before init.

```java
@Override protected void onExyliaEnable()  { RegionAPI.initialize(this); }
@Override protected void onExyliaDisable() { RegionManager.getInstance().cleanup(); }
```

## Defining Regions

Build regions with `RegionBuilder`, including presets like `safeZone` and `membersOnly`:

```java
Region hub = RegionBuilder.safeZone("hub", corner1, corner2)
    .onEnter(ctx -> VisualAPIsWelcome(ctx.getPlayer()))
    .onExit(ctx -> {})
    .build();
RegionAPI.register(hub);
```

## Events & Access Control

Six region events are provided (enter/exit/etc.), and the **pre-events are cancellable** — use
them to implement custom access control. `RegionCallback` provides `onEnter`/`onExit` hooks for
side effects. Region actions integrate with the [Action](Actions.md) subsystem
(`ActionSource.REGION`).

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

- `initialize` in `onEnable`, `cleanup()` in `onDisable`.
- Use `RegionBuilder` presets (`safeZone`, `membersOnly`) for common cases.
- Use the **cancellable pre-events** for custom access control rather than polling.
- Ship WorldEdit/FAWE only if you use `.schem` features.

## Common Mistakes

- Using `RegionManager.getInstance()` before `initialize()` → `IllegalStateException`.
- Forgetting `cleanup()` on disable.
- Expecting schematic features without WorldEdit/FAWE installed.

## Extension Points

- `RegionCallback` (onEnter/onExit), the six region events, and `SchematicType` selection.

## Relationship With Other Systems

- Fires [Actions](Actions.md) (`ActionSource.REGION`).
- Selection wands are used by the [Wizard](PlayerInteraction.md) system.
- Optional WorldEdit/FAWE integration for schematics.
- Reloaded via the [Reload](Reload.md) system (`RegionAdapter`).
