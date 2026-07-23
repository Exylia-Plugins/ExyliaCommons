# Exylia Development Standards

These documents define **how Exylia plugins are built on top of ExyliaCommons**. They describe the
conventions the framework itself follows so that every consuming plugin looks, behaves, and scales
the same way.

Wherever possible, each standard points at a **reference implementation inside ExyliaCommons** —
copy those patterns rather than inventing new ones.

## Who this is for

- **Developers** building new Exylia plugins or contributing to ExyliaCommons.
- **AI coding agents** generating code that must match the existing architecture and style.

Follow these standards by default. Deviate only with a concrete, documented reason.

## Documents

| Standard | Covers |
|----------|--------|
| [Architecture.md](Architecture.md) | The layered `API → Manager → builders/providers` pattern, lifecycle, singletons |
| [CodeOrganization.md](CodeOrganization.md) | How classes are split by responsibility within a subsystem |
| [FolderStructure.md](FolderStructure.md) | The canonical package layout (`api/core/model/config/...`) |
| [Naming.md](Naming.md) | Class/method/field/config naming conventions |
| [Threading.md](Threading.md) | Folia-aware scheduling, async pools, thread boundaries |
| [Performance.md](Performance.md) | Caching, diffing, batching, debounces, allocation discipline |
| [Configuration.md](Configuration.md) | Schema classes, strict files, reload integration |
| [UI.md](UI.md) | YAML-driven menus, actions, items, snapshots |
| [Messages.md](Messages.md) | Player-facing text, formatting, color presets |
| [DependencyUsage.md](DependencyUsage.md) | `compileOnly` posture, optional integrations, provider detection |
| [Reusability.md](Reusability.md) | Facades, builders, providers/bridges, data-driven design |

## The golden rules (summary)

1. **Extend `ExyliaPlugin`**, never override `onEnable`/`onDisable`. See [Architecture](Architecture.md).
2. **One facade per subsystem.** Consumers touch `XxxAPI`, never the internal managers directly.
3. **Schedule through `TaskAPI` / `Tasks`.** Never `Bukkit.getScheduler()`. See [Threading](Threading.md).
4. **Definitions are data.** Menus, items, rewards, scoreboards, holograms, sequences live in YAML.
5. **Config is a schema class.** One source of truth for defaults, comments, and live values.
6. **Player-facing text goes through `MessageAPI` + color presets**, not raw strings.
7. **Integrations are `compileOnly` and detected at runtime**, with a no-op fallback.
8. **Reload through the reload system**, not ad-hoc reload calls.
9. **Load session data on join, clear on quit.** Avoid TTL for session state.
10. **Mirror the framework's package layout** for every new subsystem.

## Reference subsystems

When in doubt, read these subsystems — they are the cleanest examples of the standards:

- **`v2/clan`** — textbook `api / core / provider / cache / config / listener / exception` layout
  with runtime provider detection and a dependency-free bridge (`clans-api`).
- **`v2/visual`** — facade family + `builder / config / renderer / instance / cache` separation.
- **`v2/database`** — `api / core / adapter / repository / entity / serialization / config` with a
  pluggable backend model.
- **`v2/tasks`** — the Folia-aware scheduling core every plugin should route through.
