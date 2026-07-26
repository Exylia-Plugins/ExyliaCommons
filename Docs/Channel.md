# Channel

## Overview

The channel subsystem provides permission-gated chat channels with per-channel
cooldowns and a "write mode" (players type into a channel). It uses `SimpleRedis` pub/sub for
cross-server delivery for `sendMessage`, or a local-only messenger when Redis is unavailable.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `ChannelAPI` | `v2/channel/api/ChannelAPI.java` | Static facade |
| `ChannelManager` | `v2/channel/core/ChannelManager.java` | Singleton; registry, write-mode, messenger |
| `RedisChannelMessenger` / `LocalChannelMessenger` | `v2/channel/adapter/...` | Delivery |
| `ChannelRegistry` / `CooldownManager` / `WriteModeTracker` | `v2/channel/core/...` | State |
| `Channel` / `ChannelMessage` | `v2/channel/model/...` | Models |

## Purpose

Staff channels, trade channels, and network-wide chat rooms need permission gating, cooldowns, and
cross-server delivery. This subsystem provides all of it, transparently using Redis when present
and degrading to a local messenger otherwise.

## Initialization (Redis-vs-local decided once)

```java
ChannelAPI.initialize(plugin); // synchronized, idempotent
```

At init, the messenger is chosen based on whether `SimpleRedis` is connected:

- `SimpleRedis` connected → `RedisChannelMessenger` (cross-server).
- Otherwise → `LocalChannelMessenger` (local-only).

> **The messenger is fixed at init time.** Connecting Redis *after* channel init will **not**
> upgrade the messenger without re-initializing. Ensure [SimpleRedis](Redis.md) is initialized and
> connected before `ChannelAPI.initialize` if you want cross-server delivery.

Init also subscribes existing channels and registers the chat/cleanup listeners.

## Public API

```java
void initialize(Plugin plugin);
void create(String id, String permission, String format [, double cooldownSeconds]);
void delete(String channelId);
void updateFormat(String channelId, String newFormat);
void setCooldownMessage(String channelId, String cooldownMessage);
Optional<Channel> get(String channelId);   boolean exists(String channelId);

void setWriteMode(Player player, String channelId);
void clearWriteMode(Player player);
Optional<String> getWriteMode(Player player);  boolean isInWriteMode(Player player);

void sendMessage(String channelId, Player sender, String message);
void broadcast(String channelId, String message); // local server only

int getActiveChannelCount();  Set<String> getAllChannelIds();  void clearAll();
```

**Channel IDs must match `[a-z0-9-]+`** (else `IllegalArgumentException`).

## Usage

```java
ChannelAPI.initialize(this);
ChannelAPI.create("staff", "myserver.staff", "{muted}[Staff] {primary}%player%: {letters}%message%", 3.0);

// Player types into the channel until they clear write mode:
ChannelAPI.setWriteMode(player, "staff");

// Send explicitly:
ChannelAPI.sendMessage("staff", player, "hello team");
```

- A player with `<permission>` sees/uses the channel; `<permission>.bypass` skips the cooldown.
- `sendMessage` supports `%player%`, `%message%`, and `%server%` on Redis delivery. `broadcast`
  is local-only and supplies only the message value; do not rely on `%player%`/`%server%` there.

## Redis Messaging

`RedisChannelMessenger` (prefix `exyliacommons:channel:<id>`): `sendMessage` checks permission +
cooldown, builds a `ChannelMessage`, and `publishObjectAsync`es it. Subscribers re-broadcast to
online players holding the channel permission via [`MessageAPI.send`](Visuals.md). `shutdown()`
unsubscribes all.

## Threading Considerations

- Redis publish is **async**. Pub/sub callbacks run on the Redis listener thread and dispatch to
  players via `MessageAPI`.
- The current local fallback implementation performs Bukkit player iteration from
  `CompletableFuture.runAsync`; treat local fallback as unsafe for Folia-sensitive work and prefer
  Redis delivery or bridge local delivery through `TaskAPI.sync`/entity schedulers.
- Cooldown/write-mode/permission state is in-memory.

## Best Practices

- Initialize and connect [SimpleRedis](Redis.md) **before** `ChannelAPI.initialize` for
  cross-server delivery.
- Set up a `<permission>.bypass` permission for staff to skip cooldowns.
- Use `write mode` for chat-room UX; use `sendMessage`/`broadcast` for programmatic messages.
- Validate channel IDs against `[a-z0-9-]+`.

## Common Mistakes

- Expecting cross-server delivery when Redis wasn't connected at init — messenger is local and
  fixed.
- Channel IDs with invalid characters → `IllegalArgumentException`.
- `broadcast` on a missing channel throws; Redis `sendMessage` silently returns if missing.
- `broadcast` is not cross-server; use `sendMessage` with a sender for Redis delivery.

## Relationship With Other Systems

- Depends on [SimpleRedis](Redis.md), [MessageAPI](Visuals.md), and [Placeholders](Placeholders.md).
- Conceptually adjacent to [Conversations](PlayerInteraction.md) (both intercept chat) but
  independent — mind interception precedence if you use both.
- The Channel system has **no dedicated reload adapter** and is **not** in the default reload set.
  Reloading `SimpleRedis` via the `Redis` adapter rebuilds the connection but does **not**
  re-initialize the channel messenger, which is fixed at `ChannelAPI.initialize` (see the
  Redis-vs-local note above).
- There is no public `ChannelAPI.shutdown()`. `clearAll()` clears registry/write-mode state but does
  not unsubscribe Redis channels or clear every cooldown resource; plan channel cleanup around the
  manager lifecycle and avoid reloading Redis underneath an active messenger.
