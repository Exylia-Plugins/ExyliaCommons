# Configuration

## Overview

The configuration subsystem is a layered facade over Bukkit's `YamlConfiguration`. It provides
three cooperating layers:

1. **Raw typed file access** (`Configs`, `Config`) — cached, typed getters over `.yml` files.
2. **Annotation-driven schema binding** (`schema/*`) — declarative Java classes whose static
   fields map to YAML paths, with automatic defaults, comments, and optional strict cleanup.
3. **Messages** (`Messages`) — a fluent builder over `messages.yml` with prefix/placeholder and
   Adventure `Component` support.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `Configs` | `v2/config/Configs.java` | Static facade for file access |
| `Config` | `v2/config/Config.java` | A single cached `.yml` file |
| `ConfigInitializer` | `v2/config/ConfigInitializer.java` | Bootstrap init |
| `Messages` | `v2/config/Messages.java` | `messages.yml` builder |
| `@ConfigSchema` | `v2/config/schema/ConfigSchema.java` | Marks a schema class → target file |
| `@ConfigSection` | `v2/config/schema/ConfigSection.java` | Nested section mapping |
| `@ConfigValue` | `v2/config/schema/ConfigValue.java` | Field → YAML path |
| `@Comment` | `v2/config/schema/Comment.java` | Emits YAML comments |
| `ConfigSchemaRegistry` | `v2/config/schema/ConfigSchemaRegistry.java` | Binds/loads/finalizes schemas |

## Purpose

Plugins need typed, cached, comment-annotated config with defaults that self-populate and stay in
sync with code. The schema layer lets you declare configuration **as a Java class** — the single
source of truth for defaults, paths, comments, and current values — instead of scattering
`getConfig().getString(...)` calls with hardcoded defaults everywhere.

## Initialization

`ConfigInitializer.initConfigs(plugin)` runs during bootstrap and calls `Configs.init(plugin)`,
which loads `config.yml` as the main config. Messages are initialized right after via
`ConfigInitializer.initMessages()`. Both happen **before** `onExyliaEnable`, so config is
available immediately in your enable logic.

`Configs.get(...)` throws `IllegalStateException` if called before initialization.

## Layer 1: Raw Typed File Access (`Configs`)

`Configs` is a static facade. Methods with a `path` argument operate on the **main `config.yml`**;
use `Configs.get(name)` / `Configs.file(name)` to get a `Config` for any other file.

```java
// Main config (config.yml)
String name   = Configs.string("server.name", "Default");
int max       = Configs.integer("limits.players", 100);
boolean flag  = Configs.bool("features.pvp");
double rate   = Configs.decimal("economy.rate", 1.0);
long ms       = Configs.longValue("timers.cooldown");
List<String> motd = Configs.stringList("motd");
ConfigurationSection sec = Configs.section("rewards");
Set<String> keys = Configs.getKeys("rewards");

// Arbitrary file (data/shop.yml → Configs.get("data/shop"))
Config shop = Configs.get("data/shop");
String currency = shop.string("currency");
```

### Mapping helpers

```java
Map<String, T> map(String path, Function<ConfigurationSection, T> mapper);
List<T> list(String path, Function<ConfigurationSection, T> mapper);
```

### Lifecycle helpers

```java
Configs.reload("config");   Configs.reloadAll();
Configs.save("config");     Configs.saveAll();
Configs.unload("config");   Configs.unloadAll();
boolean Configs.exists(path);
```

Files are cached in a `ConcurrentHashMap`; the first `get(name)` creates and loads the file (from
the plugin data folder, copying a bundled resource if present).

## Layer 2: Schema Binding (recommended)

Declare a schema class annotated with `@ConfigSchema(file = "...")`. Inner static classes marked
`@ConfigSection` create nested paths; static fields marked `@ConfigValue` map to leaf paths and
hold both the **default** and the **loaded current value**. `@Comment` emits YAML comments.

```java
@ConfigSchema(file = "config", strict = true, version = "1.0")
public class MyDefaults {

    @ConfigSection("gameplay")
    @Comment("Gameplay tuning")
    public static class Gameplay {

        @ConfigValue("max-health")
        @Comment("Maximum player health")
        public static int MAX_HEALTH = 20;

        @ConfigValue("allowed-worlds")
        @Comment("Worlds where the feature is active (empty = all)")
        public static List<String> WORLDS = List.of();
    }
}
```

This binds to:

```yaml
# Gameplay tuning
gameplay:
  # Maximum player health
  max-health: 20
  # Worlds where the feature is active (empty = all)
  allowed-worlds: []
```

### Registering and reading a schema

```java
// In onExyliaEnable (or wherever appropriate):
ConfigSchemaRegistry.ensureDefaults(MyDefaults.class);

// Later, read the CURRENT value straight from the static field:
int hp = MyDefaults.Gameplay.MAX_HEALTH;   // reflects the loaded config value
```

`ensureDefaults(schemaClass)`:

1. Registers the class under its target file.
2. Reloads the file, writes any missing defaults + comments (existing values are preserved).
3. Saves the file.
4. **Loads** the file back into the static fields (fields now hold live config values).
5. Schedules strict finalization if applicable (see below).

`ConfigSchemaRegistry.load(schemaClass)` re-reads values into fields without rewriting defaults.
`ConfigSchemaRegistry.reloadSchema(file)` / `reloadAll()` re-process all schemas for a file.

> The framework's own `DebugDefaults` and `FormattersDefaults` both target `file = "config"`
> with `strict = true`. Multiple schemas can co-own a single file.

### Strict Finalization (important)

When any schema targeting a file has `strict = true`, ExyliaCommons schedules a **finalization
pass 30 seconds after** `ensureDefaults` (only if `TaskAPI` is initialized). During finalization
it:

- Unions the declared paths of **all** schemas registered for that file.
- **Deletes every key in the file that no schema declares** (orphan removal), except keys under
  "preserved prefixes" (see `Config.getPreservedPrefixes()`).
- Saves and reloads.

**Implications:**

- Strict files are effectively **owned** by their schemas. Do not put ad-hoc keys in a strict
  file — they will be deleted ~30s after startup.
- If you need a mixed file (schema + free-form user keys), either use a **non-strict** schema or
  register the free-form section as a preserved prefix.
- Because the framework's `DebugDefaults`/`FormattersDefaults` make `config.yml` strict, any
  custom top-level keys you add to `config.yml` must be covered by your own registered schema (or
  preserved), otherwise they will be pruned.

The 30s delay + per-file union exists precisely so all co-owning schemas can register before
orphan cleanup runs.

## Layer 3: Messages

`Messages` (initialized in bootstrap) reads from `messages.yml` and returns formatted strings /
Adventure components with prefix, placeholders, and color support. Missing keys resolve to a
visible error marker (`{error}<path> not found`) so problems are obvious in-game.

Use `Messages` for player-facing text and `Configs`/schema for behavioral configuration. For
color/gradient formatting details see [Formatting.md](Formatting.md).

## Threading Considerations

- File caches and the schema registry use concurrent structures and are safe to read from any
  thread.
- **File IO (load/save) should not be done on the main thread in hot paths.** Bootstrap does it
  once at startup; for runtime reloads prefer the [Reload](Reload.md) system, which runs async.
- Strict finalization runs **async** 30s after registration via `TaskAPI.asyncScheduledLater`.

## Configuration Files & Defaults

- `config.yml` — main config (strict, co-owned by framework + your schemas).
- `messages.yml` — player-facing messages.
- Any other file via `Configs.get("name")` (or subpaths like `data/shop`).
- Bundled resources with matching names are copied to the data folder on first access.

## Best Practices

- **Prefer schema classes** for behavioral config: one source of truth for defaults, comments,
  and live values. Read the static field directly.
- Use **`strict = true`** only when you want the file to be exclusively schema-owned and
  self-cleaning; otherwise leave it non-strict.
- Keep player-facing text in `messages.yml` via `Messages`, not in behavioral config.
- For runtime config changes, wire a [reload adapter](Reload.md) instead of manual reload calls.

## Common Mistakes

- **Adding custom keys to a strict file** (like `config.yml`) without a covering schema → keys
  silently deleted after 30s. Register a schema or use a preserved prefix.
- Calling `Configs.get(...)` before initialization → `IllegalStateException` (only relevant in
  very early `onPreExyliaEnable` code).
- Reading a `@ConfigValue` field before calling `ensureDefaults`/`load` for its class → you get
  the compile-time default, not the config value.
- Expecting `Configs.string(path)` to read a non-main file — it only reads `config.yml`. Use
  `Configs.get(name).string(path)`.

## Relationship With Other Systems

- [Debug](Debug.md) and [Formatting](Formatting.md) are configured through schema classes on
  `config.yml`.
- The [Reload](Reload.md) system reloads config/schemas at high priority (config before
  dependents).
- [Menus](Menus.md), [Scoreboards](Scoreboards.md), [Holograms](Holograms.md), [Rewards](Rewards.md),
  and [Sequences](Sequence.md) all read their definitions through `Configs`.
