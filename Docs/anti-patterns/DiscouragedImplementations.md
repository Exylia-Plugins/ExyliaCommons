# Anti-Pattern: Discouraged Implementations

Patterns that compile and "work" but violate the framework's design. Each has a preferred Exylia
approach.

---

## 1. Bypassing the facade to reach internal managers/adapters

**Anti-pattern:** importing `*.core.*Manager`, `*.adapter.*`, or `*.provider.*` to do a normal task.

```java
// BAD
VisualManager.getInstance().someInternalMethod(...);
DatabaseManager.getInstance().getRepository(Profile.class); // internal
```

**Why:** the internal managers are implementation detail. They can change; the `XxxAPI` facade is
the stable contract.

**Preferred:** call the facade.

```java
// GOOD
TitleAPI.send(player, config);
Database.getRepository(Profile.class);
```

> Rule of thumb: if you had to import from `core`/`adapter`/`provider` for a common task, you took a
> wrong turn. If the facade lacks a method, that's a framework gap to fill — not a reason to bypass.

---

## 2. Overriding `onEnable` / `onDisable`

**Anti-pattern:** trying to override Bukkit's lifecycle methods.

**Why:** on `ExyliaPlugin` they are `final` and drive the framework's bootstrap/shutdown. Overriding
is impossible; working around them skips core-system init.

**Preferred:** use `onPreExyliaEnable` / `onExyliaEnable` / `onExyliaDisable` / `onReload`. See
[../Lifecycle.md](../Lifecycle.md).

---

## 3. Treating `SkullAPI.fromPlayer(...)` as a live fetch

**Anti-pattern:**

```java
// BAD — expecting a real skin synchronously
ItemStack head = SkullAPI.fromPlayer(name);
```

**Why:** the sync `fromPlayer` / `fromPlayerCached` return a **cached or default (Steve)** texture;
they never block to fetch from Mojang.

**Preferred:** preload, then use sync in hot paths; use async when you must have the real texture.

```java
SkullAPI.preloadPlayers(names);                 // at startup / before opening a menu
SkullAPI.fromPlayerAsync(name, head -> apply(head)); // when a real texture is required
```

See [../Skulls.md](../Skulls.md).

---

## 4. Relying on `Configs.set(...)` being fluent

**Anti-pattern:**

```java
// BAD — set() always returns null, even on success
Configs.set("a.b", 1).save();   // NullPointerException
```

`v2/config/Configs.java`:

```java
public static Configs set(String path, Object value) {
    if (mainConfig == null) return null;
    mainConfig.set(path, value);
    return null;   // always null
}
```

**Preferred:** call them separately, or use the `Config` object (whose `set` **is** fluent):

```java
Configs.set("a.b", 1);
Configs.save();
// or
Configs.get("config").set("a.b", 1).save();
```

---

## 5. Doing your own PvP/combat checks assuming `CombatAPI.canAttack` is authoritative

Under the DeluxeCombat provider, `canAttack` is a stub returning `true`
([DeprecatedAPIs](DeprecatedAPIs.md)). Don't build protection logic on it yet.

---

## 6. Manual integration checks instead of a provider

**Anti-pattern:** `if (Bukkit.getPluginManager().isPluginEnabled("Vault")) { ... }` scattered
through gameplay code.

**Why:** duplicated, fragile, and un-testable. The framework detects providers **once** at init and
exposes a uniform facade with a no-op fallback (clans/combat/economy).

**Preferred:** call `EconomyAPI` / `ClanAPI` / `CombatAPI`; they resolve the backend and fall back
safely. To add a new integration, implement the SPI (`EconomyProvider`, `ClanProvider`, ...). See
[../best-practices/DependencyUsage.md](../best-practices/DependencyUsage.md).

---

## 7. Hand-rolling infrastructure the framework provides

**Anti-pattern:** custom `BukkitRunnable` pools, bespoke `ItemStack` builders, manual Mojang fetch +
cache, raw JDBC, `Map<UUID, Long>` cooldowns, manual placeholder string-replace.

**Preferred:** `TaskAPI`, `ItemsAPI`, `SkullAPI`, `Database`, action `.cooldown(...)` /
`ItemCooldownAPI`, `Placeholders`. See
[../best-practices/Reusability.md](../best-practices/Reusability.md).

---

## 8. Ignoring `IllegalStateException` from opt-in facades

**Anti-pattern:** calling `MenuAPI`, `Database`, `RegionAPI`, `ScoreboardAPI`, `ClanAPI`, etc.
before initializing them.

**Why:** most managers throw `IllegalStateException` when used before `initialize`. (Note:
`EconomyManager` is the exception — it lazily constructs and will **NPE** instead; guard with
`EconomyAPI.isAvailable()`.)

**Preferred:** `initialize(this)` opt-in subsystems in `onExyliaEnable`. See the ownership table in
[../README.md](../README.md#initialization-ownership-read-this-first).
