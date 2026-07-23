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

## Public API (`CommandAPI`, static)

All execution methods are **async** and return a `CompletableFuture`:

```java
void initialize(JavaPlugin plugin);   boolean isInitialized();

CompletableFuture<CommandResult>       execute(Player player, String commandString [, PlaceholderContext context]);
CompletableFuture<List<CommandResult>> executeAll(Player player, List<String> commands [, PlaceholderContext context]);
CompletableFuture<List<CommandResult>> fromConfig(Player player, ConfigurationSection section [, PlaceholderContext context]);
CompletableFuture<List<CommandResult>> fromConfigKey(Player player, ConfigurationSection section, String key [, ...]);
CompletableFuture<CommandResult>       executeAsync(Player player, String commandString [, PlaceholderContext context]);
CommandBuilder builder();   CommandStats getStats();   void shutdown();
```

### Command prefixes (sender selection)

The prefix chooses the sender (`v2/command/model/CommandType.java`). **A command with no prefix
runs as CONSOLE.**

| Prefix | Runs as | Cross-server |
|--------|---------|--------------|
| `player:` | the player | no |
| `console:` | the console | no |
| `player-proxy:` | the player, forwarded to another server | yes (proxy) |
| `console-proxy:` | the console, forwarded | yes (proxy) |
| *(none)* | console (default) | no |

### Execute a single command

```java
CommandAPI.execute(player, "player: spawn")
    .thenAccept(result -> { /* CommandResult */ });
CommandAPI.execute(player, "give %player_name% diamond 1"); // no prefix → console
```

Placeholders are processed via [Placeholders](Placeholders.md).

### Execute a list from config

```java
CommandAPI.fromConfig(player, section);          // uses the loader's default key
CommandAPI.fromConfigKey(player, section, "commands");
```

```yaml
commands:
  - "console: broadcast %player_name% joined"
  - "player: warp hub"
```

### Cross-server

The `player-proxy:` / `console-proxy:` prefixes forward the command over BungeeCord. Under the
hood `ProxyCommandSender` uses the BungeeCord `Forward`/`ExyliaCommand` sub-channel; it requires
proxy messaging to be enabled and the player to be online (otherwise the result reports
`"Proxy messaging not enabled"` / `"Player not online for proxy command"`).

## Threading Considerations

- `execute*` returns a `CompletableFuture` and runs asynchronously; command dispatch to Bukkit is
  scheduled on the main/region thread as required.
- Placeholder resolution happens as part of execution.

## Best Practices

- Use `fromConfig(player, section)` / `fromConfigKey(...)` for YAML-driven command lists (menus,
  rewards, actions).
- Prefix commands with `player:` / `console:` explicitly for clarity; remember **no prefix =
  console**.
- Use **Lamp** for actual slash-command registration in your plugin — `CommandAPI` is for
  execution.
- Use `player-proxy:` / `console-proxy:` for network-wide commands instead of custom channel code.
- Consume the returned `CommandResult`(s) to detect failures.

## Common Mistakes

- Treating `CommandAPI` as a command-registration framework — it is an executor.
- Assuming a no-prefix command runs as the player — it runs as **console** by default.
- Treating `execute*` as synchronous/void — it returns a `CompletableFuture<CommandResult>`.
- Calling `execute` before `initialize` → `IllegalStateException("CommandManager not initialized")`
  (open a menu or call `CommandAPI.initialize` first). Double-init throws
  `IllegalStateException("CommandManager already initialized")`.

## Relationship With Other Systems

- Called by [Rewards](Rewards.md) (command rewards) and [Menus](Menus.md) item clicks.
- Processes [Placeholders](Placeholders.md).
- Lazily initialized by [Menus](Menus.md) (`MenuAPI.initialize` → `MenuManager.initialize`).
- Distinct from the [Sequence](Sequence.md) `[COMMAND]` token, which uses the console directly.
- Reloaded via the [Reload](Reload.md) system (`CommandAdapter`, registered as `"CommandManager"`).
