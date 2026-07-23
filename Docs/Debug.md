# Debug

## Overview

`DebugAPI` is the framework's central logging utility. It provides **level-gated**,
**category-filterable** logging split into two audiences: **library-internal** logs (`logLib*`)
and **plugin-facing** logs (`logPlugin*`). It also renders the startup MOTD banner.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `DebugAPI` | `v2/debug/api/DebugAPI.java` | Static logging facade |
| `DebugConfig` | `v2/debug/config/DebugConfig.java` | Cached level + category set |
| `DebugDefaults` | `v2/debug/config/DebugDefaults.java` | Schema on `config.yml` |
| `DebugCategory` | `v2/debug/core/DebugCategory.java` | Enum of categories |
| `DebugType` | `v2/debug/core/DebugType.java` | DEBUG/ERROR/WARN/INFO/SUCCESS + colors |
| `DebugManager` | `v2/debug/core/DebugManager.java` | MOTD/prefix |

## Purpose

A single logging surface so that verbose diagnostic output can be turned on/off globally by level
and narrowed by category, without recompiling. Library noise and plugin noise are separated so a
plugin developer can enable their own debug output without drowning in framework internals.

## Debug Levels

Configured in `config.yml` under `debug.level` (mapped by `DebugDefaults.Debug.LEVEL`):

| Level | Meaning | What DEBUG output appears |
|-------|---------|---------------------------|
| `0` | DISABLED | No DEBUG logs (default) |
| `1` | PLUGIN | `logPluginDebug` only |
| `2` | LIB | `logLibDebug` + plugin debug |
| `3` | ALL | Everything |

> Only `DebugType.DEBUG` messages are gated by level. `INFO`, `WARN`, `ERROR`, and `SUCCESS`
> **always** log regardless of level.

## Categories

`debug.categories` in `config.yml` is a list of allowed category names. When non-empty, only
categorized DEBUG logs matching a listed category are shown. **Uncategorized DEBUG logs (no
`DebugCategory`) bypass the category filter** and still appear (subject to level). Categories are
defined by the `DebugCategory` enum; there is no runtime registration API — extend the enum to add
one.

## Configuration

```yaml
# config.yml
debug:
  # Debug level: 0=DISABLED, 1=PLUGIN, 2=LIB, 3=ALL
  level: 0
  # Allowed debug categories (empty = all)
  categories: []
```

This section is owned by the strict `DebugDefaults` schema. After editing at runtime, the cache
must be refreshed — the [Reload](Reload.md) system does this via `DebugConfigAdapter` (priority
`CRITICAL`), or you can call `DebugConfig.reload()` directly.

## Public API

```java
// DEBUG (level-gated)
DebugAPI.logPluginDebug(String);
DebugAPI.logPluginDebug(DebugCategory, String);
DebugAPI.logLibDebug(String);
DebugAPI.logLibDebug(DebugCategory, String);

// INFO / SUCCESS / WARN (always log)
DebugAPI.logPluginInfo(String);   DebugAPI.logLibInfo(String);
DebugAPI.logPluginSuccess(String); DebugAPI.logLibSuccess(String);
DebugAPI.logPluginWarn(String);   DebugAPI.logLibWarn(String);

// ERROR (always log; Throwable + category overloads)
DebugAPI.logPluginError(String);
DebugAPI.logPluginError(String, Throwable);
DebugAPI.logPluginError(DebugCategory, String);
DebugAPI.logPluginError(DebugCategory, String, Throwable);
DebugAPI.logLibError(String);
DebugAPI.logLibError(String, Throwable);
DebugAPI.logLibError(DebugCategory, String);
DebugAPI.logLibError(DebugCategory, String, Throwable);
```

**Choose the audience:**
- Use `logPlugin*` for output meant for the plugin's operators/developers.
- Use `logLib*` for framework-internal diagnostics (mostly used by ExyliaCommons itself).

## Examples

```java
// Verbose diagnostic, only shown at level >= 1
DebugAPI.logPluginDebug("Loaded " + count + " shops");

// Categorized diagnostic, filterable via debug.categories
DebugAPI.logPluginDebug(DebugCategory.DATABASE, "Query took " + ms + "ms");

// Always-visible error with stack trace
try {
    risky();
} catch (Exception e) {
    DebugAPI.logPluginError(DebugCategory.DATABASE, "Failed to load shop", e);
}
```

## MOTD Banner

`DebugManager.sendPluginMOTD(plugin)` renders an ASCII banner via JFiglet
(`FigletFont.convertOneLine(plugin.getName())`), logs version, whether plugin debug is enabled,
and a "Powered by Exylia" footer. On any exception it falls back to a simple banner. This is
invoked by the higher-level plugin loader wrapper.

## Threading Considerations

- Logging is **synchronous** (no async logging path is currently enabled).
- The level/category set is cached (`DebugConfig`); reads are cheap and thread-safe. After config
  edits you must call `DebugConfig.reload()` (or reload the plugin) for changes to take effect.

## Best Practices

- Pass a `DebugCategory` to scope noisy DEBUG output so operators can filter it.
- Use `logLib*` only for framework/library internals; use `logPlugin*` for your plugin.
- Reserve `ERROR` for genuine failures; use `WARN` for recoverable issues.
- Keep DEBUG messages cheap to construct (they are gated, but the argument string is still built).

## Common Mistakes

- Expecting `logLibDebug`/`logPluginDebug` to appear at `level: 0` — they are suppressed. Raise
  the level.
- Editing `debug.*` at runtime and not reloading — the category/level set is cached.
- Assuming categories hide *uncategorized* logs — they do not; only categorized logs are filtered.

## Relationship With Other Systems

- Configured via the [Configuration](Configuration.md) schema layer (`DebugDefaults` on
  `config.yml`).
- Refreshed by the [Reload](Reload.md) system (`DebugConfigAdapter`, `CRITICAL` priority).
- Used pervasively by every other subsystem for diagnostics.
