# ExyliaCommons Anti-Patterns

This section catalogs patterns you should **not** use with ExyliaCommons, grounded in the actual
implementation. Each entry explains **why** it is wrong and points to the **preferred Exylia
approach**.

Use this as a review checklist. If a change matches an anti-pattern here, fix it before merging.

## Documents

| Document | Covers |
|----------|--------|
| [DeprecatedAPIs.md](DeprecatedAPIs.md) | `@Deprecated` methods, stubbed/unimplemented APIs (do not call) |
| [DiscouragedImplementations.md](DiscouragedImplementations.md) | Bypassing facades, misnamed calls, bad init/shutdown |
| [DuplicatedFunctionality.md](DuplicatedFunctionality.md) | Two Redis stacks, two bootstrap bases, overlapping cooldown packages, `TaskAPI` vs `Tasks` |
| [Performance.md](Performance.md) | Blocking IO, reopen-to-refresh, TTL session data, per-tick allocation |
| [Threading.md](Threading.md) | `Bukkit.getScheduler`, `.join()` on the main thread, region/entity-thread violations |
| [Configuration.md](Configuration.md) | Strict-file key pruning, reading fields before load, ad-hoc reload |
| [Menus.md](Menus.md) | `slot`+`slots`, sync skulls, global handlers, reopen loops, hardcoded layouts |

## The short list (most common mistakes)

1. **Calling `EconomyManager.transfer(...)`** — it is a **stub that always fails**. Use explicit
   `withdraw` + `deposit`. See [DeprecatedAPIs](DeprecatedAPIs.md).
2. **`Bukkit.getScheduler()`** instead of `TaskAPI`/`Tasks` — breaks Folia. See [Threading](Threading.md).
3. **Blocking `.join()`/`.get()` on the main thread** (e.g. `HologramBuilder.build()`,
   `SnapshotAPI.restoreRegistered`) — stalls or deadlocks. Use the async variants.
4. **Adding custom keys to strict `config.yml`** — pruned ~30s after load. See [Configuration](Configuration.md).
5. **Specifying both `slot` and `slots`** on a menu item — throws `IllegalArgumentException`. See
   [Menus](Menus.md).
6. **Expecting `SkullAPI.fromPlayer(...)` (sync) to fetch a skin** — it returns cache/default only.
7. **Confusing the two Redis stacks** (`SimpleRedis` vs the database Redis pool). See
   [DuplicatedFunctionality](DuplicatedFunctionality.md).
8. **Reaching into `*.core`/`*.adapter` internals** instead of the `XxxAPI` facade.

## How these were identified

Every entry references real source (file + line/behavior) verified in the current tree — deprecated
annotations, `TODO`/stub returns, `UnsupportedOperationException` throws, blocking `.join()` calls,
init-order requirements, and duplicated subsystems. Where the framework provides a correct
alternative, it is named explicitly.
