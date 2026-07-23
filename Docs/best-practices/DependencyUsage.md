# Dependency Usage Standard

ExyliaCommons integrates with many third-party plugins (Vault, PlaceholderAPI, WorldGuard/FAWE,
Redis, MongoDB, clan/combat plugins, PacketEvents, Lunar/Feather, Geyser/Floodgate…) **without
hard-depending on any of them**. Consuming plugins must preserve that posture.

Reference: `build.gradle`, `v2/clan`, `v2/combat`, `v2/economy`, `v2/lifecycle/SystemBootstrapper`.

---

## 1. Third-party server/plugin deps are `compileOnly`

Almost every integration in `build.gradle` is `compileOnly`: Paper, PlaceholderAPI, HikariCP,
SQLite/MySQL, Jedis, MongoDB, Vault, FAWE, WorldGuard, PacketEvents, Caffeine, ItemsAdder, the
clan/combat/economy plugins, Lunar/Feather, Geyser/Floodgate.

**Standard:** integrations are `compileOnly`. The server (or the consuming plugin) provides them at
runtime. Do **not** convert an integration to `implementation` without a concrete reason — it would
bundle another plugin's classes into your jar.

```groovy
compileOnly 'me.clip:placeholderapi:2.12.2'
compileOnly 'com.github.MilkBowl:VaultAPI:1.7'
compileOnly 'redis.clients:jedis:5.1.0'
```

Only genuinely-bundled runtime libraries are `implementation` (in the framework: the `clans-api`
module, the local ExyliaClans API jar, and JFiglet).

## 2. Never assume a dependency is present at runtime

Because deps are `compileOnly`, they may be absent. **Guard every optional integration** and
degrade gracefully.

- Detect once, at init, and cache the result — don't probe `isPluginEnabled(...)` in hot paths.
- Wrap construction against `NoClassDefFoundError`/`Exception` so a missing class can't crash you.

**Reference:** `SystemBootstrapper.checkOptionalDependencies()` probes Jedis / H2 / MySQL / Mongo /
Hikari via `Class.forName` and silently continues if absent. `ClientAPI` only wires PacketEvents
features `if (Bukkit.getPluginManager().isPluginEnabled("packetevents"))`.

```java
// Detect once; wrap construction so an absent class never crashes detection.
private static Provider tryCreate(Supplier<Provider> factory) {
    try {
        Provider p = factory.get();
        return p.isEnabled() ? p : null;
    } catch (NoClassDefFoundError | Exception e) {
        return null;   // dependency absent → skip, don't crash
    }
}
```

## 3. Abstract integrations behind a provider SPI with a no-op fallback

Don't scatter plugin checks through gameplay code. Define an SPI, detect the best implementation at
init, and fall back to a no-op when nothing is available. This is exactly how the framework does
clans/combat/economy.

- **Clans:** `ClanDetector` → `FactionsUUID / HuskTowns / SimpleClans / ...` → `NoClanProvider`.
- **Combat:** `CombatDetector` → `DeluxeCombat / PvPManager` → `NoCombatProvider`.
- **Economy:** `EconomyManager` resolves `Vault → PlayerPoints → DummyProvider`.

Consumers call the uniform facade (`ClanAPI.getPlayerClan(...)`) and get sensible behavior whether
or not the backend exists. See [Reusability.md](Reusability.md).

## 4. Prefer the framework's chosen libraries

Use what the framework already standardizes on, rather than adding a competing library:

| Concern | Standard library | Notes |
|---------|------------------|-------|
| Caching | **Caffeine** | Used across the framework |
| DB pooling | **HikariCP** (SQL) | Behind the `Database` layer |
| Redis | **Jedis** via `SimpleRedis` / DB pool | Don't add a second client |
| Text | **Adventure / MiniMessage** via `ColorAPI` | Don't hand-serialize components |
| ASCII banner | **JFiglet** | Bundled |
| Commands (consumer plugins) | **Lamp** | For slash-command registration |

Adding a redundant library is a review blocker.

## 5. Consuming-plugin build hygiene

- Keep server/plugin integrations `compileOnly`; let them be provided at runtime.
- If you shade libraries, **relocate** them to avoid classpath clashes, and keep shading minimal.
- **Java 21**; match the framework's toolchain.
- **Never commit secrets/tokens** (review `gradle.properties` and any publish config).
- Don't break the framework's relocation/shadow behavior when depending on it.

## 6. PlaceholderAPI specifically

PAPI is optional. Locally registered placeholders always resolve; PAPI-provided ones (`%vault_*%`)
resolve only when PAPI is installed. Check with `ExyliaPlugin.isPlaceholderAPIEnabled()` and expose
your placeholders to PAPI only via `Placeholders.registerPapiExpander(...)`. See
[../Placeholders.md](../Placeholders.md).

---

## Checklist

- [ ] All third-party server/plugin deps are `compileOnly`.
- [ ] Every optional integration is detected once and guarded against `NoClassDefFoundError`.
- [ ] Integrations sit behind an SPI with a no-op fallback, not inline plugin checks.
- [ ] No redundant libraries; reuse the framework's (Caffeine, Hikari, Jedis, Adventure, Lamp).
- [ ] Java 21; no committed secrets; minimal, relocated shading.

## Anti-patterns

- `implementation` on Vault/PAPI/WorldGuard/etc. (bundling another plugin's classes).
- `Bukkit.getPluginManager().isPluginEnabled(...)` checks sprinkled through gameplay logic.
- Assuming Jedis/Mongo/PAPI is on the classpath and NPE/`NoClassDefFoundError`-ing when it isn't.
- Adding a second cache/redis/text library alongside the framework's.
