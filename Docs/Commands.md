# Commands

## Overview

`CommandAPI` is an **execution** layer for running commands as the player or the console, driven
by config, with a proxy sender for cross-server (BungeeCord) command dispatch. It is **not** a
slash-command registration framework.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `CommandAPI` | `v2/command/api/CommandAPI.java` | Static facade |
| `CommandManager` | `v2/command/core/...` | Singleton, executor, proxy sender |
| `CommandExecutor` | `v2/command/core/...` | Runs commands |
| `CommandConfigLoader` | `v2/command/config/...` | Parses YAML command lists |
| `ProxyCommandSender` | `v2/command/...` | Cross-server (BungeeCord/`exylia:commands`) dispatch |

## Purpose vs Lamp

> **Important:** For registering real slash commands in a consuming plugin, ExyliaCommons expects
> you to use **Lamp** (per `AGENTS.md`). `CommandAPI` here is about **executing** commands (as
> player or console, locally or cross-server) — typically the commands attached to menu items,
> rewards, and config-driven actions.

## Initialization

`CommandManager.initialize` is called **lazily by `MenuAPI.initialize`**, or you can call it
directly. It throws `IllegalStateException` if initialized twice. It builds the executor, a
Caffeine cache (`maximumSize(1000)`, `expireAfterWrite(5, MINUTES)`), and registers the outgoing
plugin channels `exylia:commands` and `BungeeCord`. `execute` before `initialize` throws
`IllegalStateException("CommandManager not initialized")`.

## Usage

### Execute a single command

```java
CommandAPI.execute(player, "player: spawn");   // run as the player
CommandAPI.execute(player, "console: give %player_name% diamond 1"); // run as console
```

Commands support `player:` / `console:` prefixes to choose the sender, and placeholders are
processed via [Placeholders](Placeholders.md).

### Execute a list from config

```java
CommandAPI.fromConfig(player, section); // runs a YAML-defined list of commands
```

```yaml
commands:
  - "console: broadcast %player_name% joined"
  - "player: warp hub"
```

### Cross-server

`ProxyCommandSender` dispatches over BungeeCord / the `exylia:commands` plugin channel so commands
can target other servers on the network.

## Threading Considerations

- Command dispatch to Bukkit runs on the main/region thread as required.
- Placeholder resolution happens as part of execution.

## Best Practices

- Use `fromConfig(player, section)` for YAML-driven command lists (menus, rewards, actions).
- Prefix commands with `player:` / `console:` explicitly for clarity.
- Use **Lamp** for actual slash-command registration in your plugin — `CommandAPI` is for
  execution.
- Rely on the cross-server proxy for network-wide commands instead of custom channel code.

## Common Mistakes

- Treating `CommandAPI` as a command-registration framework — it is an executor.
- Calling `execute` before `initialize` → `IllegalStateException` (open a menu or call
  `CommandAPI.initialize` first).

## Relationship With Other Systems

- Called by [Rewards](Rewards.md) (`CommandRewardProvider`) and [Menus](Menus.md) item clicks.
- Processes [Placeholders](Placeholders.md).
- Lazily initialized by [Menus](Menus.md) (`MenuAPI.initialize`).
- Distinct from the [Sequence](Sequence.md) `[COMMAND]` token, which uses the console directly.
- Reloaded via the [Reload](Reload.md) system (`CommandAdapter`).
