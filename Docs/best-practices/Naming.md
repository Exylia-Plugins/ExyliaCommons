# Naming Standard

Consistent names make the whole codebase navigable. These conventions match ExyliaCommons and
`AGENTS.md`.

## Language & casing

- **English only**, everywhere (identifiers, comments, log messages, config keys/comments).
- Packages: `lowercase` (`net.exylia.commons.v2.reward`).
- Classes / interfaces / enums: `PascalCase`.
- Methods / fields / locals / parameters: `camelCase`.
- Constants and `@ConfigValue` static fields: `UPPER_SNAKE_CASE`
  (e.g. `DebugDefaults.Debug.LEVEL`, `DatabaseDefaults.Database.WriteBehind.FLUSH_INTERVAL`).

## Class name suffixes (use the framework's vocabulary)

Pick the suffix that describes the class's role. These are the exact suffixes the framework uses:

| Suffix | Role | Reference |
|--------|------|-----------|
| `API` | Public static facade | `TaskAPI`, `MenuAPI`, `RewardAPI` |
| `Manager` | Singleton owning state/lifecycle | `VisualManager`, `DatabaseManager` |
| `Registry` | Registration store | `ClanProviderRegistry`, `SerializationRegistry` |
| `Factory` | Creates objects from input | `MenuFactory`, `SkullFactory` |
| `Builder` | Fluent construction | `RegionBuilder`, `CommandBuilder` |
| `Provider` | SPI implementation for a backend/integration | `VaultProvider`, `FactionsUUIDProvider` |
| `Adapter` | Backend behind an SPI / reload adapter | `SQLAdapter`, `ClanAdapter` |
| `Detector` | Chooses an implementation at runtime | `ClanDetector`, `CombatDetector` |
| `Executor` | Runs a pipeline/work | `ActionExecutor`, `RewardExecutor` |
| `Listener` | Bukkit event listener | `PlayerCacheListener` |
| `Config` / `Defaults` | Config POJO / schema class | `ClanConfig`, `DatabaseDefaults` |
| `Context` | Per-call carried state | `ActionContext`, `PlaceholderContext` |
| `Result` | Operation outcome | `RewardResult`, `CommandResult`, `TaskResult` |
| `Exception` | Domain error | `DatabaseException`, `HologramException` |

**Standard:** if you name a class `XxxManager`, it must be a singleton owning state. If it only
builds objects, it's a `Builder` or `Factory`. Don't misuse the vocabulary.

## Method naming

- **Async methods end in `Async`** and return a `CompletableFuture`
  (`findByIdAsync`, `createAsync`, `processFromConfigAsync`). The sync twin drops the suffix.
- **Boolean queries** read as questions: `isInitialized`, `isOnCooldown`, `hasPagination`,
  `canAttack`.
- **Getters** use `getX` (or Lombok `@Getter`); **fluent builder setters** use the bare property
  name (`.title(...)`, `.cooldown(...)`), not `setX`.
- **Lifecycle**: `initialize` / `shutdown` (not `start`/`stop`/`enable`).
- **Facade entry verbs** mirror intent: `send`, `broadcast`, `give`, `execute`, `open`, `register`.

## Config key naming

- Config keys are **`kebab-case`** or **`snake_case`** matching the surrounding file; follow the
  neighbours. The framework uses `kebab-case` for schema keys (`starting-balance`, `flush-interval`,
  `pool-size`) and `snake_case` in some menu/item keys (`hide_attributes`, `restore_on_close`).
- Nested config uses `@ConfigSection` names as path segments (`economy.starting-balance`).
- Placeholder names are `snake_case`, conventionally namespaced by plugin
  (`%myplugin_kills%`).

## Action / reward / channel ids

- Action ids: lowercase with a plugin namespace → `myplugin:open_shop`. Namespacing is required to
  avoid collisions ([DependencyUsage.md](DependencyUsage.md)).
- Channel ids must match `[a-z0-9-]+`.
- Reward/menu keys in YAML: `kebab-case` (`common-crate`, `join-commands`).

## Reload system names

When registering a reloadable system, the **registration key** and the adapter's `getName()`
should match (e.g. `"RewardManager"`), and must be unique — a colliding name silently overwrites a
built-in. See [Configuration.md](Configuration.md).

## Anti-patterns

- `Utils` / `Helper` / `Manager2` / `MyMenuThing` — non-descriptive or role-mismatched names.
- An async method without the `Async` suffix (or a sync method with it).
- `setX` on a fluent builder.
- Mixed languages in identifiers or config comments.
