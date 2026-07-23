# Reusability Standard

ExyliaCommons exists so plugins don't reinvent menus, tasks, config, persistence, or integrations.
The reusability standard is about **not duplicating what the framework already provides**, and
building your own features so *they* are reusable too.

## 1. Use the framework primitive, don't rebuild it

Before writing infrastructure, check whether a subsystem already covers it:

| Need | Use this | Don't build |
|------|----------|-------------|
| Scheduling | [`TaskAPI` / `Tasks`](../TaskAPI.md) | your own `BukkitRunnable` wrappers / thread pools |
| Menus | [`MenuAPI`](../Menus.md) | custom `InventoryHolder` frameworks |
| Item building | [`ItemsAPI`](../Items.md) | hand-rolled `ItemStack` builders |
| Player heads | [`SkullAPI`](../Skulls.md) | Mojang fetch + cache code |
| Config + defaults | [schema classes](../Configuration.md) | scattered `getConfig().getX(...)` |
| Persistence + cache | [`Database`](../Database.md) | raw JDBC / manual Caffeine |
| Cross-server | [`SimpleRedis`](../Redis.md) / channels | ad-hoc Jedis pools |
| Titles/bars/sounds | [visual APIs](../Visuals.md) | manual packet/adventure code |
| Effects | [Sequence engine](../Sequence.md) | hand-scheduled particle chains |
| Cooldowns | [action cooldowns / `ItemCooldownAPI`](Performance.md) | `Map<UUID, Long>` timestamps |
| Placeholders | [`Placeholders`](../Placeholders.md) | manual string replace |
| Rewards | [`RewardAPI`](../Rewards.md) | bespoke give-loops |

> **Standard:** duplicating a framework primitive is a code-review blocker. If the primitive is
> missing a capability, extend the framework (or file it), don't fork it into your plugin.

## 2. Facades make your features reusable

Expose your own features through a single facade/service, exactly like the framework's `XxxAPI`.
Other parts of your plugin (and sister plugins) then depend on the contract, not the internals.

```java
// One entry point for the whole feature.
ProfileService.get().load(uuid);
ShopService.get().open(player, "weapons");
```

See [CodeOrganization.md](CodeOrganization.md#consuming-plugin-services) for the service pattern.

## 3. Data-driven design = reuse by configuration

The most reusable code is the code you don't rewrite per case. Push variation into **YAML**:

- One menu definition → many themed menus by editing YAML.
- One reward loader → any drop table via `chance`/`condition`/`priority`.
- One sequence engine → unlimited effects authored in `effects:` lists.

**Reference:** ExyliaArrows and ExyliaKillEffect share the same `SequenceExecutor` — different
content, zero duplicated effect code. Attach behavior to menu items via **actions/commands**, not
per-menu Java.

## 4. Reuse across plugins via providers and bridges

When a capability must integrate with *other* plugins, define an **SPI** and let implementations
plug in — the pattern the framework uses for clans/combat/economy.

- **Provider SPI:** `ClanProvider`, `CombatProvider`, `EconomyProvider`, `DatabaseAdapter`.
- **Dependency-free bridge:** `clans-api` exposes `ClanProviderBridge` using only JDK types + a
  `ClanSnapshot` record, so a third-party plugin can integrate **without depending on Bukkit or
  ExyliaCommons**. Provide a bridge module when you want external plugins to extend your feature
  cheaply.

```java
// Register a runtime integration without touching core logic:
ClanAPI.registerBridge(new MyClanBridge());
```

## 5. Reusable objects: builders + config objects

Prefer builders that yield reusable, immutable config objects. A `TitleConfig`, `MenuData`, or
`ItemCooldownDefinition` is built once and reused across players/ticks.

```java
// Build once, reuse for every viewer.
private static final TitleConfig ROUND_START = TitleConfig.builder()
    .title("{primary}&lFIGHT")
    .fadeIn(5).stay(30).fadeOut(10)
    .build();
```

## 6. Share via config serializers

If you persist or configure a custom type, register a **serializer** once (`SerializationRegistry`
for DB, `ConfigSchemaRegistry.registerSerializer` for config). Then every repository/menu/scoreboard
can use it — reuse instead of per-site conversion code.

## Anti-patterns

- Re-implementing scheduling, menus, config, or caching that the framework provides.
- Copy-pasting a feature into three plugins instead of extracting a facade/bridge.
- Hardcoding content (menus/rewards/effects) that should be YAML.
- Exposing internal managers/models across plugin boundaries instead of a stable facade.
