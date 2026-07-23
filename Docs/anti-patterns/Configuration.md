# Anti-Pattern: Incorrect Configuration Usage

Configuration misuse in ExyliaCommons usually produces silent data loss or stale values. These are
the concrete traps.

Reference: [../Configuration.md](../Configuration.md),
[../best-practices/Configuration.md](../best-practices/Configuration.md), [../Reload.md](../Reload.md).

---

## 1. Adding custom keys to a strict file (they get deleted)

**Anti-pattern:** writing your own top-level keys into `config.yml`.

**Why:** `config.yml` is co-owned by **strict** framework schemas (`DebugDefaults`,
`FormattersDefaults`). Strict finalization runs **~30 seconds after load**, unions all schema paths
for the file, and **deletes every key no schema declares** (except preserved prefixes). Your keys
vanish after startup.

```yaml
# BAD — added to config.yml without a covering schema; pruned ~30s later
my-feature:
  enabled: true
```

**Preferred:**
- Put custom config in a **separate, non-strict file** (`Configs.get("data/myfeature")`), **or**
- Cover the keys with **your own `@ConfigSchema` class** (register via
  `ConfigSchemaRegistry.ensureDefaults(...)`), **or**
- Register a **preserved prefix** on the `Config`.

---

## 2. Reading a `@ConfigValue` field before loading it

**Anti-pattern:** reading the static schema field before `ensureDefaults`/`load` ran.

```java
// BAD — returns the compile-time default, not the config value
double start = Settings.Economy.STARTING_BALANCE; // schema not loaded yet
```

**Why:** the field holds the **default** until the schema is loaded into it.

**Preferred:** call `ConfigSchemaRegistry.ensureDefaults(Settings.class)` in `onExyliaEnable`
**before** reading fields.

---

## 3. Using `Configs.string(path)` for a non-main file

**Anti-pattern:**

```java
// BAD — path methods on Configs read config.yml, not "shops.yml"
String currency = Configs.string("currency"); // reads config.yml
```

**Why:** the `Configs.string/integer/bool/...` path methods operate on the **main `config.yml`**.

**Preferred:** get the file first.

```java
String currency = Configs.get("data/shops").string("currency", "coins");
```

---

## 4. Editing config at runtime without reloading the cache

**Anti-pattern:** changing `debug.*`, `colors.yml`, or a schema value on disk and expecting it to
take effect immediately.

**Why:** debug level/categories, color presets, and schema fields are **cached**. Nothing re-reads
them until a reload.

**Preferred:** reload through the reload system (`ReloadAPI.getInstance().reloadAll(...)` /
`reloadSystem(...)`), or the specific refresh (`DebugConfig.reload()`, `ColorAPI.reloadPresets()`,
`ConfigSchemaRegistry.reloadSchema(...)`). Prefer the coordinated reload pass.

---

## 5. Ad-hoc reload from a command instead of a reload adapter

**Anti-pattern:** wiring `/myplugin reload` to call `config.reload()` directly, out of order with
the framework's reload.

**Why:** you lose priority ordering, timeout protection, critical short-circuiting, and the
`onReload` sync hook.

**Preferred:** register a `ReloadableSystem` (extend `ReloadableSystemAdapter`) with a **unique
name**; do Bukkit-thread reload work in `ExyliaPlugin.onReload(ReloadContext)`, not in the async
`performReload()`.

```java
public class MyReload extends ReloadableSystemAdapter {
    public MyReload() { super("MyPluginSettings", ReloadPriority.NORMAL); }
    @Override protected void performCacheClear() {}
    @Override protected void performReload() throws Exception { ConfigSchemaRegistry.reloadSchema("config"); }
}
ReloadAPI.getInstance().registerReloadable("MyPluginSettings", new MyReload());
```

---

## 6. Bukkit API calls inside `performReload()`

**Anti-pattern:** touching worlds/entities/inventories inside an adapter's `performReload()`.

**Why:** `performReload()` runs **async** (`CompletableFuture.supplyAsync`). Bukkit calls there are
off-thread.

**Preferred:** do async work (re-read config, rebuild caches) in `performReload()`; do Bukkit-thread
work in the `onReload(ReloadContext)` hook. Pass state between them via `ReloadContext.put/get`.

---

## 7. Colliding reload-system names

**Anti-pattern:** registering a reloadable with a name that matches a built-in (`"Config"`,
`"RewardManager"`, ...).

**Why:** registration is `systems.put(name, system)` — a colliding name **silently overwrites** the
built-in system.

**Preferred:** namespace your reload system name (e.g. `"MyPlugin:Settings"`), unique across the
registry.

---

## 8. Hardcoding content that should be config

**Anti-pattern:** building menus, rewards, scoreboards, holograms, or effect sequences in Java.

**Preferred:** define them in their YAML resources and load via the subsystem. See
[Menus](Menus.md) and [../best-practices/Configuration.md](../best-practices/Configuration.md).

---

## Checklist

- [ ] No custom keys in strict `config.yml` without a schema/preserved prefix.
- [ ] Schema fields read only after `ensureDefaults`/`load`.
- [ ] `Configs.get(name)` for non-main files; path methods are for `config.yml`.
- [ ] Runtime config edits followed by a reload.
- [ ] Reload via a uniquely-named `ReloadableSystem`; Bukkit work in `onReload`.
- [ ] No Bukkit API inside `performReload()`.
- [ ] Content lives in YAML, not Java.
