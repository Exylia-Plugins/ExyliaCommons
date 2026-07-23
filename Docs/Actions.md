# Actions

## Overview

The action subsystem is a pluggable, middleware-driven pipeline for executing named actions —
typically the behaviors attached to menu items, region triggers, and config-driven interactions.
Actions are identified by tokens (with optional namespacing) and run through validation →
permission → execute stages with audit logging and cooldown/rate-limit support.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `ActionAPI` | `v2/action/api/ActionAPI.java` | Static facade |
| `ActionExecutor` | `v2/action/...` | Runs the pipeline |
| `ActionPipeline` | `v2/action/...` | Stage orchestration + middleware |
| `Action` / `SyncAction` / `AsyncAction` | `v2/action/...` | Action implementations (thread affinity) |
| `ActionResult` | `v2/action/...` | Success/failure result |
| `ActionException` | `v2/action/exception/...` | Blocked vs error distinction |
| audit / namespace / cache | `v2/action/{audit,namespace,cache}/...` | Cross-cutting |

## Purpose

Config-driven behaviors ("when this item is clicked, do X") need a consistent, safe execution
model: permission checks, cooldowns, error handling, audit trails, and thread affinity. The action
pipeline provides all of that so each subsystem doesn't reinvent it.

## Initialization

`ActionAPI.initialize(plugin)` is called **automatically** during bootstrap. Built-in
reward-editor actions are registered afterward by `RewardEditorActionRegistrar.register(plugin)`.

## Important: Action tokens vs Sequence tokens

> The `[COMMAND]`, `[MESSAGE]`, `[SOUND]` bracket tokens you see in effect YAML belong to the
> **[Sequence](Sequence.md)** engine, **not** the action subsystem. The action subsystem executes
> **actions** (e.g. `[open_menu]`, custom registered IDs) through its pipeline. This is the single
> most common conceptual mix-up in ExyliaCommons.

## Execution Pipeline

`ActionExecutor.executeAsync` runs the whole pipeline inside `Tasks.run(...)`. Stage order
(`ActionPipeline.execute`):

```
PRE_VALIDATE → action.canExecute() → PRE_EXECUTE → action.executeDirect() → POST_EXECUTE
                                                                          (exception → ERROR)
```

- On exception, the pipeline enters the `ERROR` stage and returns `ActionResult.failure`.
- `ActionException.isExpected()` distinguishes **blocked** (cooldown/permission) from **real
  errors**, so blocked actions are not logged as errors.
- `SyncAction` vs `AsyncAction` determines the thread affinity of the handler.

## Registering Custom Actions

Implement an `Action` (`SyncAction` or `AsyncAction`) and register it with an owner plugin so it
can be cleanly unregistered on disable:

```java
ActionAPI.register(owningPlugin, new MyAction("do_thing"));
// ...
ActionAPI.unregisterAll(owningPlugin); // on disable
```

Use **namespacing** to avoid simple-ID collisions across plugins.

## Extension Points

- **Middleware**: `ActionPipeline.registerMiddleware(stage, middleware)` for cross-cutting
  concerns (metrics, extra validation).
- **PermissionProvider**: custom permission resolution.
- **RateLimiter**: custom cooldown/rate-limiting.
- **Custom `Action` implementations**: the core extension mechanism.

## Threading Considerations

- The pipeline runs via the task system; `SyncAction` handlers run on the sync thread,
  `AsyncAction` handlers on async pools.
- Put IO in `AsyncAction`s; keep `SyncAction`s to Bukkit-touching, fast work.

## Best Practices

- Always pass a `JavaPlugin owner` on registration so `unregisterAll` cleans up on disable.
- **Namespace** action IDs to avoid collisions.
- Put IO in `async()` actions; keep sync actions minimal.
- Use middleware / custom `PermissionProvider` / `RateLimiter` for cross-cutting logic instead of
  inlining it in each action.

## Common Mistakes

- Confusing action tokens with [Sequence](Sequence.md) `[COMMAND]`/`[MESSAGE]`/`[SOUND]`.
- Registering without an owner plugin → no clean unregistration path.
- Doing blocking IO in a `SyncAction`.

## Relationship With Other Systems

- Consumed by [Menus](Menus.md) (`ActionSource.MENU`), [Items](Items.md), and [Regions](Regions.md)
  (`ActionSource.REGION`/`ITEM_*`), and the reward-editor UI.
- Can execute commands via [CommandAPI](Commands.md).
- Resolves [Placeholders](Placeholders.md).
- Reloaded via the [Reload](Reload.md) system (`ActionAdapter`).
