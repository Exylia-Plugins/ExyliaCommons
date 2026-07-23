# ExyliaCommons Documentation

ExyliaCommons is a large, cohesive Spigot/Paper/Folia **library** (not a standalone plugin)
used as the shared foundation for every Exylia plugin. It provides a Folia-aware task engine,
a schema-driven configuration layer, a category-based debug logger, a rich UI/menu framework,
visual effect APIs, a database + cache layer, a declarative effect-sequence engine, and a large
set of integration facades (clans, combat, economy, teleport, etc.).

This documentation is the **primary source of truth**. It is written so that a developer or an
AI coding agent can use ExyliaCommons correctly for common tasks **without reading the source**.

---

## What ExyliaCommons Is

- A **library artifact** (`net.exylia:ExyliaCommons:1.0.0`), shaded into consuming plugins or
  provided as a dependency. It has **no `plugin.yml`** and is never enabled directly.
- Consuming plugins **extend `ExyliaPlugin`** (which extends `JavaPlugin`). Extending
  `ExyliaPlugin` is what wires the entire framework into the plugin lifecycle.
- Everything lives under the package family `net.exylia.commons.v2.*`. The only class outside
  `v2` is the `ExyliaPlugin` base class itself (`net.exylia.commons.ExyliaPlugin`).
- Target platform: **Java 21**, Paper compile baseline **1.20.4**, with **Folia awareness**
  built into the task engine and all region/entity-scheduled work.

> **v2 only.** Although `AGENTS.md` mentions a legacy `net.exylia.commons.async` package, it
> does not exist in the current tree. **Always use the `v2` APIs.**

---

## The Architectural Pattern

Almost every subsystem follows the same shape. Recognizing it makes the whole library
predictable:

```
<subsystem>/
├── api/        → Static facade class (e.g. TaskAPI, MenuAPI, VisualManager). Your entry point.
├── core/       → Manager singleton holding state and lifecycle (initialize/shutdown).
├── builder/    → Fluent builders for the subsystem's objects.
├── model/      → Data/enum types.
├── config/     → Config/schema for the subsystem.
├── provider/   → Pluggable backends (integration detection, adapters).
└── adapter/    → Backend implementations or reload adapters.
```

**You almost always interact with the `api/*API` facade.** Facades are `final` utility classes
with private constructors; they delegate to a manager singleton. Most managers must be
`initialize(plugin)`-d before use and throw `IllegalStateException` otherwise.

---

## Initialization Ownership (Read This First)

This is the single most important thing to understand. When a plugin extends `ExyliaPlugin` and
enables, the framework auto-initializes a **core set** of subsystems in
`SystemBootstrapper.initializeCoreSystemsAsync`. Everything else is **opt-in** and must be
initialized explicitly by the consuming plugin.

### Auto-initialized by the framework (no action required)

| Subsystem | Initializer called by bootstrap |
|-----------|---------------------------------|
| Config / Messages | `ConfigInitializer.initConfigs`, `initMessages` |
| Config schema defaults | `ConfigSchemaRegistry.ensureDefaults(DebugDefaults, FormattersDefaults)` |
| **TaskAPI** | `TaskAPI.initialize(plugin)` |
| Debug | `DebugConfig.reload()` |
| Color / Visual | `ColorAPI.initialize`, `VisualManager.initialize` |
| PlayerUtils | `PlayerUtils.initialize` |
| Placeholders | `Placeholders.initialize` |
| Chat input | `ChatInputManager.init` |
| Reward | `RewardManager.initialize` |
| Action | `ActionAPI.initialize` |
| Sequence | `SequenceListener` registered, `EffectPreview.init` |
| Reload | `ReloadAPI.initialize` (called in `ExyliaPlugin.onEnable` after bootstrap) |

### NOT auto-initialized — you must call `initialize()` yourself

`MenuAPI`, `SkullAPI`*, `CommandAPI`*, `RegionAPI`, `ScoreboardAPI`, `Database`, `TeleportAPI`,
`ChannelAPI`, `ClanAPI`, `CombatAPI`, `EconomyAPI`, `SnapshotAPI`, `ConversationAPI`,
`WizardAPI`, `SimpleRedis`, `HologramManager`.

> \* `CommandAPI` and `SkullAPI` are lazily initialized by `MenuAPI.initialize`, so if you open
> menus they come up automatically. If you use them independently, initialize them yourself.

See [Lifecycle.md](Lifecycle.md) for the full bootstrap/shutdown sequence.

---

## Documentation Index

### Core

| Document | Subsystem |
|----------|-----------|
| [Lifecycle.md](Lifecycle.md) | Plugin bootstrap, `ExyliaPlugin`, init ownership, shutdown order |
| [TaskAPI.md](TaskAPI.md) | Folia-aware scheduling, async pools, thread checks |
| [Configuration.md](Configuration.md) | `Configs`, schema binding, strict finalization, messages |
| [Debug.md](Debug.md) | Category logging, debug levels, MOTD |
| [Formatting.md](Formatting.md) | `FormatterAPI`, number/time formatting, color/gradient presets |
| [Reload.md](Reload.md) | `ReloadAPI`, adapters, priorities, custom reloadable systems |

### Data

| Document | Subsystem |
|----------|-----------|
| [Database.md](Database.md) | Adapters (H2/MySQL/Mongo/YAML), repositories, caching, `@PlayerSession` |
| [Redis.md](Redis.md) | `SimpleRedis` vs database Redis pool, pub/sub, invalidation |

### UI & Visuals

| Document | Subsystem |
|----------|-----------|
| [UIFramework.md](UIFramework.md) | Overview of menus + items + skulls |
| [Menus.md](Menus.md) | `MenuAPI`, YAML menus, pagination, navigation, packet titles |
| [Items.md](Items.md) | `ItemsAPI`, item YAML format |
| [Skulls.md](Skulls.md) | `SkullAPI`, async texture fetching, caching |
| [Visuals.md](Visuals.md) | Titles, action bars, boss bars, messages, sounds, particles, fireworks |
| [Scoreboards.md](Scoreboards.md) | `ScoreboardAPI`, YAML scoreboards |
| [Holograms.md](Holograms.md) | `HologramAPI`, YAML holograms |

### Gameplay & Content

| Document | Subsystem |
|----------|-----------|
| [Sequence.md](Sequence.md) | Declarative YAML effect sequence engine |
| [Commands.md](Commands.md) | `CommandAPI`, YAML command execution |
| [Placeholders.md](Placeholders.md) | Placeholder registration and PlaceholderAPI bridging |
| [Actions.md](Actions.md) | Action pipeline, action tokens, custom actions |
| [Rewards.md](Rewards.md) | Reward definitions, providers, granting |
| [Regions.md](Regions.md) | `RegionAPI`, region events, WorldEdit/FAWE |

### Integrations & Misc

| Document | Subsystem |
|----------|-----------|
| [Integrations.md](Integrations.md) | Clans, Combat, Economy provider detection |
| [Teleport.md](Teleport.md) | Local + cross-server teleport |
| [Channel.md](Channel.md) | Cross-server messaging over Redis |
| [PlayerInteraction.md](PlayerInteraction.md) | Chat input, conversations, wizards, snapshots |

---

## Quick Start

```java
public final class MyPlugin extends ExyliaPlugin {

    @Override
    protected void onExyliaEnable() {
        // TaskAPI, Placeholders, VisualManager, etc. are already initialized here.

        // Opt-in systems you use must be initialized by you:
        MenuAPI.initialize(this);       // also brings up CommandAPI + SkullAPI

        getLogger().info("MyPlugin enabled on top of ExyliaCommons");
    }

    @Override
    protected void onExyliaDisable() {
        // Framework shutdown (TaskAPI, Database, player cleanup) runs automatically after this.
    }

    @Override
    protected void onReload(ReloadContext context) {
        // Called by the reload system. Reload your own config here.
    }
}
```

> **Do not override `onEnable`/`onDisable`** — they are `final` in `ExyliaPlugin`. Use
> `onExyliaEnable`, `onExyliaDisable`, `onPreExyliaEnable`, and `onReload` instead.

---

## Global Conventions & Common Mistakes

- **Never override `onEnable`/`onDisable`.** They are final and drive the framework lifecycle.
- **Respect init ownership.** Calling an opt-in facade before `initialize()` throws
  `IllegalStateException`. See the table above.
- **Keep Bukkit API on the correct thread.** Use [TaskAPI](TaskAPI.md) for all scheduling; on
  Folia, region/entity work must run on the owning region/entity thread.
- **Prefer YAML-driven definitions** (menus, items, scoreboards, holograms, rewards, sequences)
  over hardcoding. The framework is built around declarative configuration.
- **`[COMMAND]` / `[SOUND]` (and the many particle-shape) tokens belong to the
  [Sequence](Sequence.md) engine**, not the [Action](Actions.md) subsystem. There is **no
  `[MESSAGE]` token** anywhere — use `[TITLE]`/`[ACTION_BAR]`. This is the most common conceptual
  mix-up.
- **`EconomyManager.transfer` is a stub** and always fails. Use explicit `withdraw` + `deposit`.
- Strict config schemas **delete unregistered keys** ~30s after load. See
  [Configuration.md](Configuration.md).
