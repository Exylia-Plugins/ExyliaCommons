# ExyliaCommons Examples

Production-grade, copy-adaptable examples for the most important ExyliaCommons subsystems. Every
example demonstrates the **recommended Exylia way** — no shortcuts, no simplified stubs that you
would not ship.

All examples assume your plugin extends `ExyliaPlugin` (see [../Lifecycle.md](../Lifecycle.md)) and
that you call the relevant opt-in `initialize(this)` in `onExyliaEnable`.

## Index

| Example | Subsystem | Doc |
|---------|-----------|-----|
| [SimpleMenu.md](SimpleMenu.md) | YAML-driven menu + custom click action | [Menus](../Menus.md) |
| [PaginatedMenu.md](PaginatedMenu.md) | Paginated menu with a live data supplier | [Menus](../Menus.md) |
| [ConfirmationMenu.md](ConfirmationMenu.md) | Reusable confirm/deny dialog | [Menus](../Menus.md) |
| [Commands.md](Commands.md) | Executing commands (player/console/proxy) | [Commands](../Commands.md) |
| [Listeners.md](Listeners.md) | Listeners + Folia-safe scheduling | [TaskAPI](../TaskAPI.md) |
| [TaskAPI.md](TaskAPI.md) | Async pools, sync bridging, region tasks | [TaskAPI](../TaskAPI.md) |
| [Database.md](Database.md) | Entities, repositories, `@PlayerSession` | [Database](../Database.md) |
| [Redis.md](Redis.md) | `SimpleRedis` key/value + pub/sub | [Redis](../Redis.md) |
| [Placeholders.md](Placeholders.md) | Registering + resolving placeholders | [Placeholders](../Placeholders.md) |
| [Configuration.md](Configuration.md) | Schema config + raw file access | [Configuration](../Configuration.md) |
| [Regions.md](Regions.md) | Building regions + enter/exit callbacks | [Regions](../Regions.md) |
| [Rewards.md](Rewards.md) | YAML + programmatic rewards | [Rewards](../Rewards.md) |
| [Wizards.md](Wizards.md) | Interaction / location / selection wizards | [PlayerInteraction](../PlayerInteraction.md) |
| [Cooldowns.md](Cooldowns.md) | Item cooldowns + action cooldowns | [Actions](../Actions.md) |
| [ChatComponents.md](ChatComponents.md) | Chat input + messages | [PlayerInteraction](../PlayerInteraction.md) / [Visuals](../Visuals.md) |
| [VisualComponents.md](VisualComponents.md) | Titles, action bars, boss bars, sounds | [Visuals](../Visuals.md) |

## Conventions used in every example

- **Never override `onEnable`/`onDisable`** — they are `final` on `ExyliaPlugin`. Use
  `onExyliaEnable` / `onExyliaDisable`.
- **Schedule through [TaskAPI](../TaskAPI.md) / `Tasks`** — never `Bukkit.getScheduler()` directly
  (breaks Folia).
- **Do IO/DB off the main thread** and apply results back on the main/region thread.
- **Prefer YAML-driven definitions** (menus, rewards, items) and attach behavior via registered
  **actions** / commands rather than hardcoding.
- **Guard opt-in facades** with `isInitialized()` where relevant, and `initialize(this)` them in
  `onExyliaEnable`.

## `TaskAPI` vs `Tasks`

Both facades exist. `TaskAPI` uses descriptive names (`async`, `syncLater`, `atTimer`); `Tasks`
uses short names (`run`, `db`, `io`, `sync`, `later`, `timer`, `at`). They are equivalent; the
examples use whichever reads best. Pick one convention per plugin for consistency.
