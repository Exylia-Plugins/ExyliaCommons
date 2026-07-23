# Lifecycle

## Overview

The lifecycle system is the backbone of ExyliaCommons. It defines how a consuming plugin boots
the framework, in what order core subsystems initialize, and how everything is torn down cleanly
on disable. Understanding it removes almost all "why is X not initialized?" confusion.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `ExyliaPlugin` | `net/exylia/commons/ExyliaPlugin.java` | Base class consuming plugins extend |
| `LifecycleManager` | `v2/lifecycle/LifecycleManager.java` | Drives bootstrap → enable → shutdown |
| `SystemBootstrapper` | `v2/lifecycle/SystemBootstrapper.java` | Initializes the core subsystem set |
| `ShutdownCoordinator` | `v2/lifecycle/ShutdownCoordinator.java` | Ordered teardown |
| `LifecycleStage` | `v2/lifecycle/LifecycleStage.java` | Enum of stages |

## Purpose

Consuming plugins must never manually wire up ExyliaCommons' 40+ subsystems. Instead they extend
`ExyliaPlugin`, and the lifecycle system guarantees a deterministic init order (config before
tasks before placeholders before rewards/actions, etc.) and a safe shutdown order (player state
cleanup before pool shutdown before database close).

## How Plugins Use It

Extend `ExyliaPlugin` and implement the abstract hooks. **Do not override `onEnable`/`onDisable`;
they are `final`.**

```java
public final class MyPlugin extends ExyliaPlugin {

    @Override
    protected void onPreExyliaEnable() {
        // Optional. Runs BEFORE the framework bootstraps.
        // Use only for things that must exist before ExyliaCommons initializes.
    }

    @Override
    protected void onExyliaEnable() {
        // Required. Runs AFTER all core systems are initialized.
        // This is your normal onEnable equivalent.
    }

    @Override
    protected void onExyliaDisable() {
        // Required. Your cleanup. Runs BEFORE the framework's ordered shutdown.
    }

    @Override
    protected void onReload(ReloadContext context) {
        // Optional. Invoked by the reload system (see Reload.md).
    }
}
```

### Available hooks

| Hook | When | Required |
|------|------|----------|
| `onPreExyliaEnable()` | Before framework bootstrap | No |
| `onExyliaEnable()` | After core systems initialized | **Yes** |
| `onExyliaDisable()` | Before framework shutdown | **Yes** |
| `onReload(ReloadContext)` | On reload pass | No |

### Utility accessors on `ExyliaPlugin`

- `ExyliaPlugin.getInstance()` — the first registered plugin instance.
- `ExyliaPlugin.getExyliaPlugin(Class<T>)` — fetch a specific plugin by type.
- `adventure()` — the plugin's `BukkitAudiences` (throws if unavailable).
- `ExyliaPlugin.isPlaceholderAPIEnabled()` — convenience check.

## Internal Workflow

### Enable (`onEnable`, final)

```
onEnable()
 ├─ onPreExyliaEnable()                         // your pre-hook
 ├─ new LifecycleManager(this)
 ├─ BukkitAudiences.create(this)                // adventure() becomes available
 ├─ register plugin instance (first = static instance)
 ├─ lifecycleManager.executeBootstrap()
 │   ├─ SystemBootstrapper.initializeCoreSystemsAsync(plugin)
 │   │   ├─ delete stale menus/admin folder
 │   │   ├─ ConfigInitializer.initConfigs(plugin)
 │   │   ├─ ConfigSchemaRegistry.ensureDefaults(DebugDefaults, FormattersDefaults)
 │   │   ├─ TaskAPI.initialize(plugin)          // ← task engine online
 │   │   ├─ ConfigInitializer.initMessages()
 │   │   ├─ DebugConfig.reload()
 │   │   ├─ ColorAPI.initialize(plugin)
 │   │   ├─ VisualManager.getInstance().initialize(plugin)
 │   │   ├─ PlayerUtils.initialize(plugin)
 │   │   ├─ Placeholders.initialize(plugin)
 │   │   ├─ ChatInputManager.init(plugin)
 │   │   ├─ RewardManager.initialize(plugin)
 │   │   ├─ ActionAPI.initialize(plugin)
 │   │   ├─ RewardEditorActionRegistrar.register(plugin)
 │   │   ├─ EffectPreview.init(plugin)
 │   │   └─ register SequenceListener
 │   └─ checkOptionalDependencies()             // probes Jedis, H2, MySQL, Mongo, Hikari
 ├─ ReloadAPI.initialize(this)                  // reload system + adapters online
 └─ lifecycleManager.executePluginEnable()
     └─ onExyliaEnable()                        // your enable logic
```

> **Naming caveat:** `initializeCoreSystemsAsync` is **not** async — it runs synchronously on the
> server's enable thread. The name is historical. All the listed initializations happen before
> your `onExyliaEnable()` is called.

### Multiple plugins

ExyliaCommons tracks all registered `ExyliaPlugin`s in a static set. The first to enable becomes
the static `instance`. Shared singletons (`TaskAPI`, etc.) are initialized once. Re-initializing
a subsystem from a *different* plugin generally triggers a soft reset or full re-init (see the
individual subsystem docs, e.g. `VisualManager`).

### Disable (`onDisable`, final)

```
onDisable()
 ├─ unregister this plugin
 ├─ close BukkitAudiences
 ├─ lifecycleManager.executeShutdown()
 │   ├─ onExyliaDisable()                       // your cleanup
 │   └─ ShutdownCoordinator.executeOrderedShutdown(plugin)
 │       ├─ ChatInputManager.shutdown()
 │       ├─ SkullAPI.shutdown()
 │       ├─ cleanupPlayerState()                // closeInventory + cancel Title/ActionBar/BossBar for all players
 │       └─ shutdownManagers()
 │           ├─ TaskAPI.shutdown()  (if initialized)
 │           └─ Database.shutdown() (IllegalStateException swallowed if never initialized)
 └─ if last plugin unregistered: reset static state + TaskAPI.shutdown()
```

## Lifecycle Stages

`LifecycleStage` transitions: `PRE_INIT → CORE_INIT → PLUGIN_INIT → POST_INIT → RUNNING` on the
way up, and `PRE_SHUTDOWN → SHUTDOWN` on the way down. Available via
`lifecycleManager.getCurrentStage()`.

## Threading Considerations

- The entire enable/disable path runs on the **server main thread** (Bukkit calls
  `onEnable`/`onDisable` synchronously).
- After bootstrap, `TaskAPI` is available for offloading async work. **Do not do heavy IO in
  `onExyliaEnable`** — schedule it via [TaskAPI](TaskAPI.md).

## Best Practices

- Put startup registrations (listeners, commands, opt-in `initialize()` calls) in
  `onExyliaEnable`.
- Put your teardown in `onExyliaDisable`. You do **not** need to shut down framework subsystems
  yourself — the `ShutdownCoordinator` handles the shared ones. You **do** shut down anything you
  initialized that is not on the coordinator's list (e.g. custom Redis, custom repositories) if
  it owns resources.
- Use `onPreExyliaEnable` sparingly; most things belong in `onExyliaEnable`.

## Common Mistakes

- **Overriding `onEnable`/`onDisable`** — impossible (final) and would bypass the framework. Use
  the Exylia hooks.
- **Using an opt-in facade before initializing it.** The bootstrap only initializes the core set
  (see [README](README.md#initialization-ownership-read-this-first)). `MenuAPI`, `Database`,
  `RegionAPI`, `ScoreboardAPI`, etc. must be initialized in `onExyliaEnable`.
- **Assuming `initializeCoreSystemsAsync` is async.** It is synchronous.
- **Relying on `adventure()` in `onPreExyliaEnable`** — audiences are created after the pre-hook.

## Relationship With Other Systems

The lifecycle system is the init/teardown hub for every other subsystem. The reload system
([Reload.md](Reload.md)) is initialized right after bootstrap and provides a runtime re-init path
that does not restart the plugin.
