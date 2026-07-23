# Example: Commands

Goal: execute commands as the player, the console, or forwarded to another server, driven from
config where possible.

> **Registering slash commands** in a consuming plugin is done with **Lamp** (per project
> convention), not `CommandAPI`. `CommandAPI` is an **executor** for the commands attached to menu
> items, rewards, and config-driven flows. This example covers execution.

Related: [Commands](../Commands.md).

`CommandAPI` is lazily initialized by `MenuAPI.initialize`; if you use it standalone, call
`CommandAPI.initialize(this)` in `onExyliaEnable`.

---

## Sender prefixes

| Prefix | Runs as |
|--------|---------|
| `player:` | the player |
| `console:` | the console |
| `player-proxy:` | the player, forwarded to another server (BungeeCord) |
| `console-proxy:` | the console, forwarded |
| *(none)* | **console** (default) |

## Execute a single command

`execute` is **async** and returns a `CompletableFuture<CommandResult>`.

```java
CommandAPI.execute(player, "player: spawn")
    .thenAccept(result -> {
        if (!result.isSuccess()) {
            MessageAPI.send(player, "{error}Could not run that command.");
        }
    });

CommandAPI.execute(player, "give %player_name% diamond 1"); // no prefix → console
```

## Run a list from config

Keep command lists in YAML and run them with `fromConfig` / `fromConfigKey`. Placeholders are
resolved during execution.

```yaml
# rewards.yml
join-commands:
  - "console: broadcast &e%player_name% &7joined for the first time!"
  - "player: warp tutorial"
  - "console: give %player_name% diamond 5"
```

```java
ConfigurationSection section = Configs.get("rewards").raw();
CommandAPI.fromConfigKey(player, section, "join-commands")
    .thenAccept(results -> {
        long failed = results.stream().filter(r -> !r.isSuccess()).count();
        if (failed > 0) DebugAPI.logPluginWarn(failed + " join commands failed for " + player.getName());
    });
```

## Cross-server (proxy)

```java
// Send the player to another server and run a command there on arrival.
CommandAPI.execute(player, "player-proxy: warp arena");
```

Proxy requires proxy messaging enabled and the player online; otherwise the `CommandResult` reports
`"Proxy messaging not enabled"` / `"Player not online for proxy command"`.

---

## Why this way

- **Config-driven command lists** let designers change join/reward commands without code.
- **Explicit sender prefixes** make intent obvious; remember **no prefix = console**.
- **Async results** let you detect and log failures instead of silently firing commands.
- **Proxy prefixes** handle cross-server dispatch without custom plugin-channel code.

## Common mistakes

- Expecting a no-prefix command to run as the player — it runs as **console**.
- Treating `execute*` as synchronous/void — it returns a `CompletableFuture<CommandResult>`.
- Using `CommandAPI` to register slash commands — use Lamp; `CommandAPI` executes.
- Calling `execute` before init — open a menu (lazy init) or call `CommandAPI.initialize(this)`.
