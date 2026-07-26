# Additional Systems

This page covers public framework APIs that are useful but were previously easy to miss in the
subsystem index: custom crafting, the standalone YAML repository, expression evaluation, client
features, and Discord webhooks.

These systems are **opt-in**. They are not part of the `ExyliaPlugin` core bootstrap and should be
initialized only when the plugin uses them.

## Crafting

`CraftingAPI` owns custom recipe registration and crafting-inventory event handling. Initialize it,
then load recipes from a `Config` or register `CustomRecipe` objects.

```java
CraftingAPI.initialize(this);
CraftingAPI.loadRecipes(Configs.get("recipes"), "recipes");
// or: CraftingAPI.registerRecipe(customRecipe);
```

Use `CraftingAPI.getRecipe(id)`, `getAllRecipes()`, `unregisterRecipe(id)`, and
`unregisterAllRecipes()` for lifecycle management. Call `CraftingAPI.reload()` after changing the
recipe file and `CraftingAPI.shutdown()` from your plugin-owned cleanup if the manager is not being
managed elsewhere.

Keep recipe/item processing aligned with `ItemsAPI`; do not implement a second crafting listener.
World/inventory operations remain Bukkit-thread work.

## Standalone YAML persistence

`Yaml` is a separate YAML-backed entity/repository system. It is not the same as `Configs` (typed
configuration files) and is not the same as the database YAML fallback adapter.

```java
Yaml.initialize(getDataFolder().toPath().resolve("data"));
Yaml.registerEntity(MyEntity.class);
YamlRepository<MyEntity> repository = Yaml.getRepository(MyEntity.class);
```

Use `Yaml.initialize(YamlConfig)` when custom storage options are required. Use the async
repository methods for file operations and call `Yaml.shutdown()` when your plugin owns the
manager. Choose this system for simple file persistence; choose `Database` for backend selection,
SQL/Mongo, write-behind, L2 caching, and network invalidation.

## Expression evaluation

`ExpressionAPI` evaluates numeric formulas and optionally resolves placeholders first. Invalid or
blank formulas return the supplied default and log an error for parse failures.

```java
double amount = ExpressionAPI.evaluate(
    "%vault_eco_balance% * 0.10",
    player,
    PlaceholderContext.create().withPlayer(player),
    0.0
);
```

Use this for small numeric rules (reward amounts, scaling, conditions that are explicitly numeric).
Do not use it as a general scripting engine or for blocking work.

## Client APIs

`ClientAPI.initialize(plugin)` initializes the client integration group:

- client-side cooldown display (`CooldownAPI`)
- item cooldown storage/listeners (`ItemCooldownAPI`)
- waypoints (`WaypointAPI`)
- team tracking (`TeamTrackerAPI`)
- packet client teams when PacketEvents is enabled

It also registers its listeners and starts the waypoint poller. Initialize once and call
`ClientAPI.shutdown()` during plugin-owned cleanup. Optional Lunar/Feather/Apollo integrations are
detected safely; do not assume a client has them.

Use the individual facades after `ClientAPI.initialize`:

```java
WaypointAPI.show(player, definition);
ItemCooldownAPI.set(player, "grappling_hook", 30_000L, Material.FISHING_ROD);
```

Do not initialize `CooldownAPI`, `ItemCooldownAPI`, or their managers manually; `ClientAPI` wires
their shared dependencies and listeners.

## Discord webhooks

`DiscordWebhooks` sends configured webhook templates asynchronously. It is opt-in and reads
`webhooks.yml` by default (or a custom config path).

```java
DiscordWebhooks.initialize(this);
DiscordWebhooks.send("daily-report", player)
    .thenAccept(response -> {
        if (!response.success()) {
            DebugAPI.logPluginWarn("Discord webhook failed: " + response.body());
        }
    });
```

Use `WebhookBuilder`/`WebhookTemplate` for programmatic templates. Keep webhook URLs and tokens in
server-owned configuration, never in source control. Network calls are asynchronous; do not touch
Bukkit from the completion callback without `TaskAPI.at`/`sync`.

## When not to use these systems

- Do not use `Yaml` as a replacement for a production multi-server database.
- Do not use `ExpressionAPI` for arbitrary code execution.
- Do not initialize all client integrations just to use one visual API.
- Do not send Discord webhooks synchronously from event/tick threads.

See [Configuration.md](Configuration.md), [Database.md](Database.md),
[DependencyUsage](best-practices/DependencyUsage.md), and [TaskAPI.md](TaskAPI.md).
