# Reload

## Overview

The reload subsystem is a **priority-ordered, asynchronous, timeout-protected orchestration
engine** that reloads all registered "systems" (config, database caches, redis, clan/scoreboard/
hologram managers, etc.) in a single coordinated pass — without restarting the plugin. It supports
player progress feedback, per-system metrics, critical-system short-circuiting, exclusions, and a
plugin reload hook.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `ReloadAPI` | `v2/reload/api/ReloadAPI.java` | Public singleton facade |
| `ReloadableSystem` | `v2/reload/api/ReloadableSystem.java` | SPI a reloadable unit implements |
| `ReloadContext` | `v2/reload/api/ReloadContext.java` | Shared mutable state for a pass |
| `ReloadManagerV2` | `v2/reload/core/ReloadManagerV2.java` | Engine + system registry |
| `ReloadOrchestrator` | `v2/reload/core/ReloadOrchestrator.java` | Per-system timeout + error mapping |
| `ReloadPriority` | `v2/reload/core/ReloadPriority.java` | Ordering weights (CRITICAL/HIGH/NORMAL/LOW) |
| `ReloadableSystemAdapter` | `v2/reload/adapter/ReloadableSystemAdapter.java` | Base class for adapters |
| `SystemReloadMetrics` | `v2/reload/stats/SystemReloadMetrics.java` | Per-system result |

## Purpose

Server owners run `/reload`-style commands constantly. Reloading a whole plugin's configuration
safely — in the right order, without blocking the main thread, and reporting what succeeded — is
non-trivial. This engine standardizes it: every subsystem registers a `ReloadableSystem`, and one
pass reloads them all in priority order with per-system timeouts.

## Initialization

`ReloadAPI.initialize(plugin)` is called by `ExyliaPlugin.onEnable` **right after bootstrap**. It
constructs `ReloadManagerV2`, which registers all built-in adapters. `ReloadAPI.getInstance()`
throws `IllegalStateException` if called before enable.

## Public API

```java
// Reload everything (optionally reporting to a player/sender)
CompletableFuture<ReloadStats> reloadAll();
CompletableFuture<ReloadStats> reloadAll(Player player);
CompletableFuture<ReloadStats> reloadAll(CommandSender sender); // prints detailed stats to console senders
CompletableFuture<ReloadStats> reloadAll(long timeoutSeconds);  // returns null on timeout (does NOT throw)

// Reload everything except named systems
CompletableFuture<ReloadStats> reloadAllExcept(String... excludedSystems);
CompletableFuture<ReloadStats> reloadAllExcept(Player player, String... excludedSystems);

// Reload a single system
CompletableFuture<ReloadStats> reloadSystem(String systemName);

// Register / unregister custom reloadable systems
void registerReloadable(String name, ReloadableSystem system);
void unregisterReloadable(String name);

// Introspection
SystemAvailability getAvailability();
List<String> getRegisteredSystems();
List<String> getAvailableSystems();
```

Typical usage from a command:

```java
ReloadAPI.getInstance().reloadAll(sender)
    .thenAccept(stats -> { /* stats is null on timeout/error */ });
```

## The Plugin Reload Hook

The recommended place for a consuming plugin to do its **own** reload work (especially anything
that touches the Bukkit API) is the `onReload(ReloadContext)` hook on `ExyliaPlugin` — it runs on
the sync thread, unlike adapter `performReload()` which runs async. Use `ReloadContext.put/get` to
pass computed state from an adapter into the hook.

## Priority & Ordering

Systems declare a `ReloadPriority`. The engine reloads in ascending priority order so dependencies
come first:

| Priority | Examples |
|----------|----------|
| `CRITICAL` | Config, ConfigSchema, ConfigSystem, DebugConfig |
| `HIGH` | Messages |
| `NORMAL` | Clan, database caches, most managers |
| `LOW` | FormatterRegistry |

**Critical short-circuit:** in `executeReloadAll`, if a `CRITICAL` system's metrics are not
successful — or an exception is thrown during its reload — the loop **breaks** and remaining
systems are **not** reloaded. Non-critical failures are recorded but the pass continues.

## Per-System Timeout

Each system's reload is bounded by `ReloadOrchestrator.executeReload` using
`.orTimeout(getTimeoutSeconds(), SECONDS)` (default **30s**). A timeout becomes a
`SystemReloadMetrics.failure(..., "Timeout after Ns")`.

## Writing a Custom Reloadable System

Extend `ReloadableSystemAdapter` — it runs `performCacheClear()` then `performReload()` on a
`CompletableFuture.supplyAsync`, wrapping into success/failure metrics:

```java
public class ShopReloadAdapter extends ReloadableSystemAdapter {
    @Override public String getName() { return "Shops"; }
    @Override public boolean isAvailable() { return ShopManager.isInitialized(); }
    @Override protected void performCacheClear() { ShopManager.get().clearCache(); }
    @Override protected void performReload() throws Exception { ShopManager.get().loadFromConfig(); }
    // optionally override getPriority() and getTimeoutSeconds()
}

// Register (e.g. in onExyliaEnable):
ReloadAPI.getInstance().registerReloadable("Shops", new ShopReloadAdapter());
```

> **`performReload()` runs async** — do **not** touch the Bukkit API there. Do Bukkit-thread work
> in `ExyliaPlugin.onReload` (sync) instead.

Implement `isAvailable()` accurately: unavailable systems are cleanly **skipped** (recorded as
skipped), not failed.

## Threading Considerations

- The whole pass runs **async**; each system is timeout-bounded (default 30s).
- Adapter cache-clear/reload run off-thread. Bukkit-touching reload work belongs in `onReload`.
- Player/console feedback is scheduled back to the sync thread for console senders.

## Best Practices

- Keep `performReload()` pure/off-thread; set realistic `getTimeoutSeconds()`.
- Use `ReloadContext.put/get` to pass state to the `onReload` hook.
- Prefer `reloadAllExcept(...)` over unregistering systems for one-off exclusions.
- Give custom systems **unique names** — registering with a built-in name silently overwrites it.

## Common Mistakes

- Calling `ReloadAPI.getInstance()` before enable → `IllegalStateException`.
- Assuming `reloadAll(timeout)` **throws** on timeout — it returns a **null** `ReloadStats`.
- Expecting a critical failure to still reload the rest — it **aborts** the remainder.
- Touching the Bukkit API inside `performReload()` (it is async) — use `onReload` instead.
- Name-colliding a custom system with a built-in → silent overwrite.

## Relationship With Other Systems

The reload engine is the top-level runtime lifecycle tie-point for nearly every v2 subsystem. It
drives config (`Configs.reloadAll`), schema finalization, `FormatterRegistry`, database local
cache, `SimpleRedis.reload`, and the clan/scoreboard/hologram/action/region/placeholder/command/
reward/skull/visual/color/discord managers. See each subsystem doc for its specific adapter.

> **Note:** the [Combat](Integrations.md) subsystem has **no default reload adapter** — register
> your own `ReloadableSystem` calling `CombatAPI.reload()` if you need it. The
> [Economy](Integrations.md) subsystem is likewise not in the default reload set.
