# AGENTS.md

Concise guidance for AI agents developing Exylia plugins with ExyliaCommons. Use the linked
documentation for complete API details and examples.

## Read First

- [Docs/README.md](Docs/README.md): framework map and initialization ownership.
- [Docs/best-practices/README.md](Docs/best-practices/README.md): development standards.
- [Docs/anti-patterns/README.md](Docs/anti-patterns/README.md): patterns to reject.
- [Docs/examples/README.md](Docs/examples/README.md): practical workflows.

## Architecture

- Extend `net.exylia.commons.ExyliaPlugin`; do not override its final `onEnable`/`onDisable`.
  Use `onPreExyliaEnable`, `onExyliaEnable`, `onExyliaDisable`, and `onReload`.
- Use `net.exylia.commons.v2.*` APIs. Consumers call `XxxAPI` facades, not managers, adapters, or
  providers directly.
- Follow `API -> Manager -> builder/model/provider/adapter/cache/listener` separation. Keep
  business logic out of the plugin main class.
- Core systems are bootstrapped automatically: config/messages, tasks, debug, color/visual,
  placeholders, chat input, rewards, actions, and sequence support. Opt-in systems such as
  `Database`, `MenuAPI`, `RegionAPI`, `ScoreboardAPI`, `ClanAPI`, `CombatAPI`, and `TeleportAPI`
  require explicit initialization in `onExyliaEnable`.
- Use provider/bridge SPIs with safe fallbacks for optional integrations instead of scattered
  plugin checks. See [Architecture](Docs/best-practices/Architecture.md) and
  [DependencyUsage](Docs/best-practices/DependencyUsage.md).

## Philosophy

- Reuse Exylia primitives before creating infrastructure: `TaskAPI`, `Database`, `MenuAPI`,
  `ItemsAPI`, `SkullAPI`, `Placeholders`, visual APIs, rewards, regions, and sequences.
- Keep variation in YAML. Menus, items, rewards, scoreboards, holograms, and effects are content,
  not Java code.
- Use builders for non-trivial objects, facades for stable contracts, and domain exceptions for
  invalid state/input.
- Keep third-party server/plugin dependencies `compileOnly`; detect optional integrations safely.
- Use Lamp for slash-command registration. `CommandAPI` executes commands; it does not register them.

## TaskAPI and Threading

- Schedule through `TaskAPI` or `Tasks`; never use `Bukkit.getScheduler()` or ad-hoc gameplay pools.
- Use `database`/`db` for DB work, `io` for file/network IO, and `compute` for CPU work.
- Use `sync` for general server-thread work. Use `at(location, ...)` or `at(entity, ...)` for
  world/block/entity work, especially on Folia.
- Use `databaseThenSync`/`asyncThenSync` for load-then-apply workflows.
- Never touch Bukkit from async callbacks. Pub/sub, database, wizard, and chat callbacks may run
  off-thread; bridge with `sync`/`at` before applying effects.
- Never call blocking `.join()`/`.get()` on the main/region thread. Use async variants such as
  `HologramBuilder.buildAsync()` and `SnapshotAPI.restoreRegisteredAsync()`.
- Restore interrupt status after `InterruptedException`; shut down any executor you own.
- Details: [Docs/best-practices/Threading.md](Docs/best-practices/Threading.md) and
  [Docs/TaskAPI.md](Docs/TaskAPI.md).

## UI Philosophy

- Initialize `MenuAPI` explicitly; it also initializes `CommandAPI` and `SkullAPI`.
- Define layouts in YAML and open with `MenuAPI.openAsync`. Attach per-item `actions` and
  `commands`; register actions with an owning plugin and unregister them on disable.
- Use `type: PAGINATION`, `pagination.item_template`, and framework navigation for paged menus.
  Refresh with `SMART`/dynamic updates; never reopen a menu every tick.
- Build items through `ItemsAPI`; preload/batch player heads before opening menus.
- Inventory writes are synchronous; item processing may be async. Do not mutate inventories/items
  from async code.
- Use snapshots for full-inventory menus: `snapshot.enabled: true` and
  `restore_on_close: true`.
- Never specify both `slot` and `slots`; menu sizes must be 9–54 and divisible by 9.
- See [Docs/best-practices/UI.md](Docs/best-practices/UI.md) and
  [Docs/anti-patterns/Menus.md](Docs/anti-patterns/Menus.md).

## Configuration and Messages

- Behavioral settings belong in `@ConfigSchema` classes with `@ConfigValue`, `@ConfigSection`,
  defaults, comments, and a version. Call `ensureDefaults` before reading static fields.
- `config.yml` is strict/co-owned by framework schemas; undeclared keys can be deleted about 30s
  after load. Use a separate non-strict file for free-form plugin data.
- `Configs.string(...)` and similar path methods read the main `config.yml`; use
  `Configs.get("file").string(...)` for other files.
- Player-facing copy belongs in `messages.yml`/`Messages`; send through `MessageAPI` or visual
  APIs. Use `ColorAPI` presets, placeholders, and `FormatterAPI`; do not hardcode colors or format
  currency/durations manually.
- Reload config through a uniquely named `ReloadableSystem`. Its `performReload()` is async;
  Bukkit work belongs in `onReload(ReloadContext)`.
- See [Docs/best-practices/Configuration.md](Docs/best-practices/Configuration.md) and
  [Docs/best-practices/Messages.md](Docs/best-practices/Messages.md).

## Performance Rules

- No blocking DB/network/file/Mojang work on tick or event threads.
- Batch/ordered database queries; avoid `findById` loops.
- Use concurrent/Caffeine session caches; load player data on join and clear on quit. Do not use TTL
  as the lifecycle mechanism for session data. `@PlayerSession` auto-flushes on quit but does not
  auto-load on join.
- Preload/batch skulls, reuse config objects, use diff-based UI refreshes, grouped/coarse timers,
  and action/item cooldowns.
- Use keyed visual updatables/countdowns instead of per-tick sends. Measure with TaskAPI stats and
  cache hit rates before optimizing.
- See [Docs/best-practices/Performance.md](Docs/best-practices/Performance.md).

## Common Workflows

1. **New plugin:** extend `ExyliaPlugin`, initialize only the opt-in systems you use in
   `onExyliaEnable`, register listeners/actions, and clean up plugin-owned state in
   `onExyliaDisable`. See [Docs/examples/Listeners.md](Docs/examples/Listeners.md).
2. **Player data:** initialize Database, register `Entity` classes, load on join asynchronously,
   apply on the correct thread, and clear the session cache on quit. See
   [Docs/examples/Database.md](Docs/examples/Database.md).
3. **Menu:** put layout in YAML, register owned actions, load data asynchronously, then
   `MenuAPI.openAsync`. See [Docs/examples/SimpleMenu.md](Docs/examples/SimpleMenu.md).
4. **Interaction flow:** use `ChatInputAPI`, `WizardAPI`, `ConversationAPI`, or `ChannelAPI`
   instead of manually parsing chat or world events.
5. **Runtime reload:** register a uniquely named reload adapter; keep async cache/config work in
   the adapter and Bukkit work in `onReload`.

## Common Mistakes

- Overriding `onEnable`/`onDisable` or using an opt-in API before initialization.
- Calling `EconomyAPI.transfer` — it is currently a stub that always fails. Use explicit
  withdraw/deposit with rollback.
- Confusing `SimpleRedis` with the database Redis pool, or mixing the three cooldown systems.
- Using deprecated `Configs.debug()`, `NBTManager.getNBTValue(...)`, or no-arg
  `SystemDetector.detectAll()`.
- Assuming sync skull rendering fetches skins; relying on DeluxeCombat `canAttack` for authoritative
  PvP; using unsupported raw queries on Mongo/YAML adapters.
- Using the nonexistent `[MESSAGE]` sequence token; use `[TITLE]`/`[ACTION_BAR]`.
- Adding ad-hoc keys to strict `config.yml`, using both menu `slot` and `slots`, reopening menus to
  refresh, or mutating inventories off-thread.
- Calling `Configs.set(...).save()` — `Configs.set` returns `null`; call `Configs.save()` separately
  or use the `Config` object.

## Verification

- Use Java 21 and preserve the repository's Gradle/shadow/dependency posture.
- Run `./gradlew check` after changes. There is currently no test suite, so explain any limitation.
- Keep changes small, preserve local style, and document intentional deviations from these standards.
