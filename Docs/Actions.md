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

> `ActionAPI.initialize` is a **no-op if already initialized** (it does not throw, unlike
> `CommandManager`). `ActionAPI.getManager()`/pipeline access throws `IllegalStateException` if
> used before init.

## Important: Action strings vs Sequence tokens

> The `[COMMAND]` / `[SOUND]` bracket tokens you see in effect YAML belong to the
> **[Sequence](Sequence.md)** engine, **not** the action subsystem. (There is **no `[MESSAGE]`
> token** anywhere — neither in the sequence engine nor as a built-in action.) The action
> subsystem executes **actions** identified by their registered id (optionally namespaced),
> parsed from an action string via `ActionAPI.execute(actionString, ActionContext)`. This is the
> single most common conceptual mix-up in ExyliaCommons.

## Public API (`ActionAPI`, static)

```java
void initialize(JavaPlugin plugin);   boolean isInitialized();

ActionBuilder create(String id [, JavaPlugin owner]);              // fluent action definition
ActionChainBuilder createChain(String id [, JavaPlugin owner]);    // chained actions

CompletableFuture<ActionResult> executeAsync(String actionString, ActionContext context);
ActionResult execute(String actionString, ActionContext context);

void register(Action action);   CompletableFuture<Void> registerAsync(Action action);
Optional<Action> get(String id);   Collection<Action> getAll();   Collection<Action> getAllByNamespace(String namespace);
void unregister(String actionId, JavaPlugin owner);   void unregisterAll(JavaPlugin owner);
void reload();   void shutdown();   ActionStats getStats();   ActionManager getManager();
```

## Execution Pipeline

The pipeline runs asynchronously. `PipelineStage` has exactly **four** stages
(`v2/action/pipeline/PipelineStage.java`): `PRE_VALIDATE`, `PRE_EXECUTE`, `POST_EXECUTE`, `ERROR`.

- Permission, cooldown, and rate-limit are **middlewares registered on `PRE_EXECUTE`** (by
  `ActionManager.initializeDefaultMiddlewares`), not separate stages.
- On exception, the pipeline enters the `ERROR` stage and returns an `ActionResult.failure`.
- `ActionException.isExpected()` distinguishes **blocked** (cooldown/permission) from **real
  errors**, so blocked actions are not logged as errors.
- Whether an action runs sync or async is chosen on the builder (`.sync()` / `.async()` /
  `.mode(ExecutionMode)`).

## Registering Custom Actions

Define an action with the fluent `ActionBuilder` (via `ActionAPI.create`) and register it. Pass an
owner plugin so it can be cleanly unregistered on disable.

```java
Action action = ActionAPI.create("do_thing", this)   // this = owning JavaPlugin
    .namespace("myplugin")                            // avoid id collisions
    .async()                                          // run handler off-thread (IO-safe)
    .permission("myplugin.do")
    .cooldownMillis(500)
    .handler((ctx, args) -> { /* perform the action */ })
    .build();

ActionAPI.register(action);
// ...
ActionAPI.unregisterAll(this); // on disable
```

Execute an action string against a context:

```java
ActionAPI.executeAsync("myplugin:do_thing arg1 arg2", context)
    .thenAccept(result -> { /* handle ActionResult */ });
```

## Extension Points

- **`ActionBuilder`** configuration: `namespace`, `permission`, `cooldown`/`cooldownMillis`,
  `rateLimit`, `auditable`, `priority`, `alias`/`aliases`, `sync`/`async`/`mode`, `handler`.
- **`ActionChainBuilder`** for multi-step chained actions.
- Custom `Action` implementations registered via `register`.

## Threading Considerations

- The pipeline runs off-thread; `.sync()` actions run on the sync thread, `.async()` actions on
  async pools.
- Put IO in `.async()` actions; keep `.sync()` actions to fast, Bukkit-touching work.

## Best Practices

- Always pass a `JavaPlugin owner` (via `create(id, owner)` or `.plugin(...)`) so `unregisterAll`
  cleans up on disable.
- **Namespace** action ids to avoid collisions.
- Use `.async()` for IO; keep sync actions minimal.
- Use builder-level `permission`/`cooldown`/`rateLimit`/`auditable` instead of inlining those
  concerns in the handler.

## Common Mistakes

- Confusing action strings with the [Sequence](Sequence.md) `[COMMAND]`/`[SOUND]` tokens.
- Assuming a `[MESSAGE]` token or action exists — it does not.
- Registering without an owner plugin → no clean unregistration path.
- Doing blocking IO in a `.sync()` action.

## Relationship With Other Systems

- Consumed by [Menus](Menus.md) (`ActionSource.MENU`), [Items](Items.md), and [Regions](Regions.md)
  (`ActionSource.REGION`/`ITEM_*`), and the reward-editor UI.
- Can execute commands via [CommandAPI](Commands.md).
- Resolves [Placeholders](Placeholders.md).
- Reloaded via the [Reload](Reload.md) system (`ActionAdapter`, registered as `"ActionManager"`).
