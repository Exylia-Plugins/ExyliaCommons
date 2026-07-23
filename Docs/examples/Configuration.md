# Example: Configuration

Goal: declare configuration as a **schema class** (the single source of truth for defaults,
comments, and live values) and read arbitrary config files with the typed `Configs` facade.

Related: [Configuration](../Configuration.md), [Reload](../Reload.md).

The config layer is initialized during bootstrap; `config.yml` and `messages.yml` are available in
`onExyliaEnable`.

---

## Schema config (recommended)

Declare a schema class. Static fields hold both the **default** and, after loading, the **live
config value**. Nested `@ConfigSection` classes create nested YAML paths; `@Comment` emits YAML
comments.

```java
@ConfigSchema(file = "config", version = "1.0")   // targets config.yml
public class Settings {

    @ConfigSection("economy")
    @Comment("Economy tuning")
    public static class Economy {

        @ConfigValue("starting-balance")
        @Comment("Balance granted to new players")
        public static double STARTING_BALANCE = 100.0;

        @ConfigValue("daily-reward")
        @Comment("Amount granted by /daily")
        public static int DAILY_REWARD = 500;

        @ConfigValue("enabled-worlds")
        @Comment("Worlds where the economy is active (empty = all)")
        public static List<String> WORLDS = List.of();
    }
}
```

Register and read it:

```java
@Override
protected void onExyliaEnable() {
    ConfigSchemaRegistry.ensureDefaults(Settings.class); // writes missing defaults + loads values

    double start = Settings.Economy.STARTING_BALANCE;    // live config value
    List<String> worlds = Settings.Economy.WORLDS;
}
```

`ensureDefaults` writes any missing keys/comments (existing values are preserved) and loads the
file into the fields.

> **Strict schemas** (`strict = true`) delete keys the schema doesn't declare ~30s after load.
> `config.yml` is co-owned by framework strict schemas, so if you add custom top-level keys to
> `config.yml`, cover them with your own schema or use a preserved prefix — otherwise they are
> pruned. See [Configuration](../Configuration.md#strict-finalization-important). Prefer a
> **separate, non-strict file** for free-form data (below).

## Reading a separate file with `Configs`

Use a dedicated file for structured, non-schema data:

```java
Config shops = Configs.get("data/shops");   // data/shops.yml

String currency = shops.string("currency", "coins");
int maxShops    = shops.integer("limits.per-player", 3);
boolean pvp     = shops.bool("flags.pvp");
List<String> ids = shops.stringList("featured");

// Map a section into your own objects:
Map<String, ShopDef> defs = shops.map("shops", section -> new ShopDef(
    section.getName(),
    section.getString("owner"),
    section.getInt("price")
));
```

## Writing and reloading

```java
Configs.get("data/shops").set("limits.per-player", 5).save();
Configs.reload("data/shops");   // reload one file
```

For runtime reloads triggered by an admin command, prefer wiring a
[reload adapter](../Reload.md#writing-a-custom-reloadable-system) so your config reloads in the
coordinated pass rather than calling `reload` ad hoc.

---

## Why this way

- **Schema = one source of truth.** Defaults, comments, YAML paths, and current values all live in
  one class; you read a typed static field instead of scattering `getConfig().getX(path, default)`.
- **Typed `Configs` access** for structured data files, with `map`/`list` helpers to build your
  own models.
- **Reload-friendly** — schema values refresh through the reload system.

## Common mistakes

- Adding ad-hoc keys to strict `config.yml` without a covering schema → pruned after ~30s. Use a
  separate non-strict file.
- Reading a `@ConfigValue` field before `ensureDefaults`/`load` → you get the compile-time default,
  not the config value.
- Using `Configs.string(path)` for a non-main file — that reads `config.yml`. Use
  `Configs.get(name).string(path)`.
