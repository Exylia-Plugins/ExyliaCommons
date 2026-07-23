# Configuration Standard

Configuration in Exylia plugins is **declared as schema classes** and **reloaded through the reload
system**. This gives one source of truth for defaults, comments, and live values, and keeps runtime
reloads coordinated.

Reference: `v2/config` (`ConfigSchemaRegistry`, `Configs`), `DebugDefaults`, `DatabaseDefaults`,
[../Configuration.md](../Configuration.md), [../Reload.md](../Reload.md).

---

## 1. Behavioral config → schema class

Declare behavioral configuration as a `@ConfigSchema` class. Static fields hold both the **default**
and the **live value**; `@ConfigSection` builds nested paths; `@Comment` documents the file.

```java
@ConfigSchema(file = "config", version = "1.0")
public class Settings {

    @ConfigSection("economy")
    @Comment("Economy tuning")
    public static class Economy {

        @ConfigValue("starting-balance")
        @Comment("Balance granted to new players")
        public static double STARTING_BALANCE = 100.0;

        @ConfigValue("daily-reward")
        public static int DAILY_REWARD = 500;
    }
}
```

```java
@Override
protected void onExyliaEnable() {
    ConfigSchemaRegistry.ensureDefaults(Settings.class);   // writes defaults + loads values
    double start = Settings.Economy.STARTING_BALANCE;      // read the live value
}
```

**Standard:** read behavior from the schema field, not `getConfig().getDouble("...", default)`
scattered across the codebase. The default lives in exactly one place — the field.

## 2. Structured data → dedicated files via `Configs`

Non-schema, structured data (shops, kits, arenas) goes in its own file, read with the typed
`Configs` facade and mapped into your models.

```java
Config shops = Configs.get("data/shops");
Map<String, ShopDef> defs = shops.map("shops", s ->
    new ShopDef(s.getName(), s.getString("owner"), s.getInt("price")));
```

## 3. Respect strict-file semantics

Strict schemas (`strict = true`) **delete keys the schema doesn't declare** ~30s after load, after
unioning all schemas that target the file. `config.yml` is co-owned by framework strict schemas
(`DebugDefaults`, `FormattersDefaults`).

**Standard:**
- Put free-form / user-authored keys in a **separate, non-strict file** — not `config.yml`.
- If you must add keys to a strict file, cover them with your own registered schema, or register a
  **preserved prefix**.

## 4. Content is data, not config

Menus, items, scoreboards, holograms, rewards, and sequences are **content**, defined in their own
resource files and loaded by their subsystems — not in `config.yml` and not in Java. See
[UI.md](UI.md) and [FolderStructure.md](FolderStructure.md).

## 5. Reload through the reload system

Don't reload config ad hoc from a command. Register a **reloadable system** so your config reloads
in the coordinated, priority-ordered, timeout-protected pass alongside the framework's.

```java
public class SettingsReloadAdapter extends ReloadableSystemAdapter {
    public SettingsReloadAdapter() { super("MyPluginSettings", ReloadPriority.NORMAL); }
    @Override public boolean isAvailable() { return true; }
    @Override protected void performCacheClear() {}
    @Override protected void performReload() throws Exception {
        ConfigSchemaRegistry.reloadSchema("config");
        Configs.reload("data/shops");
    }
}

// register in onExyliaEnable:
ReloadAPI.getInstance().registerReloadable("MyPluginSettings", new SettingsReloadAdapter());
```

- `performReload()` runs **async** — do **not** touch the Bukkit API there. Do Bukkit-thread reload
  work in the `ExyliaPlugin.onReload(ReloadContext)` hook (sync).
- Give the system a **unique name**; a colliding name silently overwrites a built-in. See
  [Naming.md](Naming.md) and [../Reload.md](../Reload.md).

## 6. Config conventions

- Keys follow the file's convention (framework schema keys are `kebab-case`).
- Every schema field gets a `@Comment` so the generated YAML is self-documenting.
- Version schemas (`version = "1.0"`) so migrations are trackable.
- Validate/clamp values at read time where a bad value would break gameplay (fail fast with a clear
  message).

---

## Checklist

- [ ] Behavioral config is a `@ConfigSchema` class; reads use the static field.
- [ ] Structured data in dedicated files via `Configs`, mapped to models.
- [ ] No custom keys in strict `config.yml` without a schema/preserved prefix.
- [ ] Content (menus/rewards/…) in resource files, not config or Java.
- [ ] Reload wired through a `ReloadableSystem` with a unique name; Bukkit work in `onReload`.
- [ ] Every schema field commented; schema versioned.

## Anti-patterns

- `getConfig().getX("path", default)` calls with hardcoded defaults spread across classes.
- Ad-hoc top-level keys in strict `config.yml` (pruned after 30s).
- Reloading config directly from a command instead of via the reload system.
- Bukkit API calls inside an async `performReload()`.
- Menus/rewards/effects embedded in Java instead of YAML.
