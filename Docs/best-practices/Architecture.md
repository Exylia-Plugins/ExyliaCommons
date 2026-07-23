# Architecture Standard

## The core pattern

Every ExyliaCommons subsystem — and every subsystem you add to a consuming plugin — follows the
same shape:

```
Consumer code
     │  calls
     ▼
XxxAPI            (public static facade — the ONLY entry point consumers use)
     │  delegates
     ▼
XxxManager        (singleton: holds state, lifecycle, registries)
     │  uses
     ▼
builders / providers / adapters / caches / models
```

This layering is the single most important standard. It gives every subsystem a predictable
surface, keeps internals swappable, and makes the whole library learnable once.

**Reference implementations:** `v2/clan` (`ClanAPI` → `ClanManager` → `ClanProvider*` + caches),
`v2/visual` (`TitleAPI`/`BossBarAPI`/... → `VisualManager` → renderers/instances),
`v2/database` (`Database` → `DatabaseManager` → adapters/repositories).

---

## 1. The facade is the contract

- Each subsystem exposes **one `XxxAPI` facade**, a `final` class with a private constructor that
  throws `UnsupportedOperationException` (or a package-private constructor for singletons).
- Consumers call **only** the facade. They never touch `XxxManager`, adapters, or internal models
  directly.
- The facade is a thin delegator: it forwards to the manager singleton and validates
  initialization.

```java
public final class RewardAPI {
    private RewardAPI() {}                                    // no instances

    public static void initialize(JavaPlugin plugin) {
        RewardManager.getInstance().initialize(plugin);
    }

    public static CompletableFuture<List<RewardResult>> give(Player player, ConfigurationSection section) {
        return RewardManager.getInstance().giveFromConfig(player, section);
    }
}
```

> **Standard:** if a consumer has to import anything from a subsystem's `core`, `adapter`, or
> `provider` package to do a common task, the facade is incomplete. Add the method to the facade.

## 2. The manager owns state and lifecycle

- One singleton per subsystem (`XxxManager`), obtained via `getInstance()`.
- It exposes `initialize(plugin)` and (where it owns resources) `shutdown()`.
- `getInstance()` throws `IllegalStateException` when used before `initialize` — **except** where
  the framework deliberately lazy-constructs (e.g. `EconomyManager`). Prefer the throwing pattern
  for new subsystems so misuse fails loudly.

```java
public static ScoreboardManager getInstance() {
    if (instance == null) {
        throw new IllegalStateException("ScoreboardManager not initialized. Call initialize() first.");
    }
    return instance;
}
```

## 3. Initialization ownership is explicit

A small **core set** of subsystems is auto-initialized by `SystemBootstrapper` (Task, Config,
Debug, Color, Visual, Placeholders, Chat input, Reward, Action, Sequence). **Everything else is
opt-in** and must be `initialize(this)`-d by the consuming plugin in `onExyliaEnable`.

**Standard for consuming plugins:**

```java
public final class MyPlugin extends ExyliaPlugin {
    @Override
    protected void onExyliaEnable() {
        // Opt-in subsystems this plugin uses:
        Database.initialize(this);
        MenuAPI.initialize(this);        // also brings up CommandAPI + SkullAPI
        RegionAPI.initialize(this);
        // your own managers:
        ProfileService.get().initialize(this);
    }

    @Override
    protected void onExyliaDisable() {
        // Clean up anything YOU own (framework-shared systems are handled by the coordinator).
        ActionAPI.unregisterAll(this);
        ProfileService.get().shutdown();
    }
}
```

- **Never override `onEnable`/`onDisable`** — they are `final` on `ExyliaPlugin` and drive the
  framework lifecycle. Use `onExyliaEnable` / `onExyliaDisable` / `onPreExyliaEnable` / `onReload`.
- See [../Lifecycle.md](../Lifecycle.md) for the full bootstrap/shutdown order.

## 4. Build objects with fluent builders

Non-trivial objects are constructed with builders, not telescoping constructors. This matches the
framework everywhere (`TitleConfig.builder()`, `RegionBuilder`, `ActionAPI.create(id)`,
`ItemCooldownDefinition.builder()`).

```java
Action ability = ActionAPI.create("dash", this)
    .namespace("myplugin")
    .async()
    .cooldown(3, TimeUnit.SECONDS)
    .handler((ctx, args) -> dash(ctx.getPlayer()))
    .build();
```

## 5. Swap behavior with providers/adapters, not conditionals

When a subsystem must support multiple backends or integrations, define an SPI interface and select
an implementation at runtime — do not scatter `if (Bukkit.getPluginManager()...)` checks through
business logic.

- **Reference:** `v2/clan` detects `FactionsUUID / HuskTowns / SimpleClans / ...` via
  `ClanDetector`, exposing a uniform `Clan` model, with `NoClanProvider` as the fallback.
- **Reference:** `v2/database` selects an adapter (`SQLAdapter` / `MongoDBAdapter` /
  `YAMLFallbackAdapter`) behind the `DatabaseAdapter` SPI.

See [DependencyUsage.md](DependencyUsage.md) and [Reusability.md](Reusability.md).

## 6. Fail fast at boundaries

- Validate inputs at the facade/manager boundary with clear messages
  (`Objects.requireNonNull`, explicit `IllegalArgumentException`/`IllegalStateException`).
- Use domain-specific exceptions inside a subsystem
  (`ScoreboardException`, `HologramException`, `DatabaseException`), and preserve causes when
  wrapping.
- Restore interrupt status when catching `InterruptedException`
  (`Thread.currentThread().interrupt()`), as the framework does in its executors.

---

## Anti-patterns (do not do these)

- Consumers importing `*.core.*Manager` or `*.adapter.*` to perform a normal task.
- A subsystem with no facade, or with two competing entry points.
- Static "god" utility classes that mix state, IO, scheduling, and rendering.
- Overriding `onEnable`/`onDisable`.
- Runtime plugin checks (`isPluginEnabled(...)`) sprinkled through gameplay code instead of a
  provider selected once at init.

## Checklist for a new subsystem

- [ ] One `XxxAPI` facade; consumers never bypass it.
- [ ] One `XxxManager` singleton with `initialize`/`shutdown`.
- [ ] Builders for non-trivial objects.
- [ ] SPI + providers/adapters for pluggable behavior; a no-op fallback.
- [ ] Domain exceptions; fail fast at boundaries.
- [ ] Package layout per [FolderStructure.md](FolderStructure.md).
- [ ] Reload adapter registered if it holds reloadable state ([Configuration.md](Configuration.md)).
