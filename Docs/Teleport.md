# Teleport

## Overview

The teleport subsystem provides **local** (Folia-aware async) and **cross-server** teleportation.
Cross-server teleports persist a pending destination in Redis (via the database Redis pool) and
dispatch a BungeeCord `Connect` message; the target server restores the pending location on join.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `TeleportAPI` | `v2/teleport/api/TeleportAPI.java` | Static facade |
| `TeleportManager` | `v2/teleport/core/TeleportManager.java` | Routes local vs cross-server |
| `LocalTeleporter` | `v2/teleport/core/LocalTeleporter.java` | Folia-aware `teleportAsync` + hooks |
| `CrossServerTeleporter` | `v2/teleport/core/CrossServerTeleporter.java` | Redis pending + BungeeCord Connect |
| `TeleportJoinListener` | `v2/teleport/listener/TeleportJoinListener.java` | Consumes pending on join |
| `ExyliaLocation` | `v2/teleport/model/ExyliaLocation.java` | Serializable location (`server,world,x,y,z,yaw,pitch`) |

## Purpose

Teleporting reliably on Folia (entity-scheduled async) and across a BungeeCord/Velocity network
(persisting the destination through a server switch) is intricate. This subsystem encapsulates both
so plugins can teleport with a single call and a `CompletableFuture`.

## Initialization (depends on Database/Redis)

`TeleportAPI.initialize(plugin)` pulls its configuration from `DatabaseManager.getInstance()` — so
**the [Database](Database.md) layer must be initialized first**. It reads the server id, the
database Redis pool, and the Redis key prefix. If the Redis pool is `null`, cross-server teleports
are **skipped with a warning** (local teleports still work). It registers `TeleportJoinListener`
and throws `IllegalStateException` if initialized twice.

> Teleport uses the **database** Redis pool ([Database.md](Database.md)), **not** `SimpleRedis`.

## Public API

```java
void initialize(Plugin plugin);
boolean isInitialized();
void addPostTeleportHook(Consumer<Player> hook);
void shutdown();

CompletableFuture<Boolean> teleport(Player player, Location location);
CompletableFuture<Boolean> teleport(Player player, ExyliaLocation destination);
CompletableFuture<Boolean> teleport(Player player, String serialized);
CompletableFuture<Void> teleport(Collection<? extends Player> players, Location | ExyliaLocation | String);
```

## Cross-Server Flow

`TeleportManager.teleport(player, ExyliaLocation)`:

- **Same server** (`destination.isSameServer(currentServer)`) → local teleport (`toBukkitLocation`,
  warns if the world is missing).
- **Different server** → `CrossServerTeleporter`: an async task writes
  `keyPrefix:teleport:pending:<uuid>` with `SETEX` (TTL 300s), then (Folia-aware) sends the
  BungeeCord `Connect` message to the target server.
- **On join** → `handlePendingTeleport`: an async task consumes the pending key (Redis `GETDEL`),
  then teleports locally after ~150ms (`TaskAPI.atLater(player, ..., 150ms)`).

## Post-Teleport Hooks

`addPostTeleportHook(Consumer<Player>)` registers logic that runs after arrival for both local and
pending (cross-server) teleports — the right place for consistent "on arrival" behavior. Hook
exceptions are swallowed per-hook.

## Threading Considerations

- Redis IO runs on `TaskAPI.async`.
- Teleports use `player.teleportAsync` (on Folia, scheduled on the entity's scheduler).
- BungeeCord sends run on the main thread (`TaskAPI.sync`) or the entity scheduler on Folia.
- The API never throws through — malformed input yields a warning + completed-`false`/`null`
  future.

## Best Practices

- Initialize the [Database](Database.md) layer **before** `TeleportAPI.initialize`.
- Use `ExyliaLocation` / serialized strings for network teleports so destinations survive a server
  switch.
- Use `addPostTeleportHook` for consistent post-arrival logic across local and cross-server.
- Provide Redis if you need cross-server teleport; otherwise expect graceful local-only behavior.

## Common Mistakes

- Initializing before the `DatabaseManager` → `IllegalStateException`.
- Expecting cross-server teleports without Redis — silently degraded.
- Passing a malformed serialized `ExyliaLocation` — returns a completed-`false` future (no throw).

## Relationship With Other Systems

- Depends on [Database](Database.md) (Redis pool, server id, key prefix) and [TaskAPI](TaskAPI.md).
- Independent of the [Channel](Channel.md) Redis (`SimpleRedis`) — it uses the database pool.
