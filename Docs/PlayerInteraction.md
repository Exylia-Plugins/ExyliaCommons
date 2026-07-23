# Player Interaction (Chat Input, Conversations, Wizards, Snapshots)

This document groups four related subsystems that capture player input or player state. They are
all **opt-in** and often used together (e.g. an admin wizard that captures locations and snapshots
the player's inventory).

| Subsystem | Facade | Purpose |
|-----------|--------|---------|
| Chat Input | `ChatInputAPI` | Ask a player for typed input (with multi-transport delivery) |
| Conversations | `ConversationAPI` | Private multi-party relayed chat |
| Wizards | `WizardAPI` | Interactive multi-step, interaction-driven flows |
| Snapshots | `SnapshotAPI`, `SnapshotStoreAPI` | Capture/restore full player state |

---

## Chat Input (`ChatInputAPI`)

Unified interactive input across four delivery mechanisms with automatic capability detection and
fallback: **Dialog** (1.21.6+ client dialogs), **Chat**, **Inventory** (GUI), and **Bedrock**
(Floodgate forms). Supports text, integer, decimal, boolean, single-option, confirmation,
multi-number, and sanitized "id" inputs.

### Initialization

`ChatInputAPI.init(plugin)` is called during bootstrap (`ChatInputManager.init`), and
`ChatInputManager.shutdown()` runs in the shutdown coordinator. It is effectively always available.

### Fluent API

```java
ChatInputAPI.text(player, "Enter a name")
    .maxLength(16)
    .validator(s -> s.matches("[A-Za-z]+"))
    .onResponse(name -> create(name))
    .onCancel(() -> player.sendMessage("Cancelled"))
    .ask();

ChatInputAPI.integer(player, "Enter amount").range(1, 64).onResponse(this::give).ask();
ChatInputAPI.confirm(player, "Delete shop?").onConfirm(this::delete).onDeny(() -> {}).ask();
ChatInputAPI.id(player, "Shop id").onResponse(this::create).ask(); // auto-sanitizes to [a-z0-9_-], max 32
```

Builders: `text`, `integer`, `decimal`, `bool`, `option`, `confirm`, `numbers`, `id`. Common chain
methods include `.maxLength/.range/.validator/.onResponse/.onCancel/.forceChat/.forceTitle/
.option(k,l)/.columns/.onConfirm/.onDeny/.field(...)/.ask()`.

### Delivery Resolution & Fallback

`resolveHandlerType`: (1) `forceChat` → CHAT; (2) Bedrock player → BEDROCK if Floodgate confirmed,
else fallback; (3) client supports dialogs → DIALOG; (4) fallback: text/number → CHAT, otherwise
INVENTORY. If the dialog handler can't show, it re-routes to chat/inventory. Multi-number input
under non-dialog handlers runs **sequentially field-by-field**.

### Threading & Lifecycle

- One active session per player (submitting a new request cancels the previous). `PlayerQuitEvent`
  cancels sessions. Callbacks run on the completing handler's thread (chat/inventory are
  main-thread).

### Best Practices / Mistakes

- Chain follow-up prompts via `onResponse` instead of firing two requests (the second stomps the
  first).
- Use `id(...)` for identifiers rather than manual regex.
- Use `.forceChat()` when chat is required regardless of client.

---

## Conversations (`ConversationAPI`)

Private multi-party chat: participants' normal chat is intercepted and relayed to the other
participants using recipient/sender formats.

### Initialization

```java
ConversationAPI.initialize(plugin); // registers chat intercept + cleanup listeners
```

### API

```java
void start(String id, String recipientFormat, String senderFormat, Collection<Player> participants);
void end(String conversationId);
boolean isInConversation(Player);
Set<Conversation> getPlayerConversations(Player);  Set<String> getPlayerConversationIds(Player);
void addParticipant(String conversationId, Player);  void removeParticipant(String conversationId, Player);
boolean conversationExists(String conversationId);   int getActiveCount();  void clearAll();
```

### Validation

`start` throws `IllegalArgumentException` if id/recipientFormat/senderFormat is blank, participants
is null/empty, or there are **fewer than 2 online participants**. Only online participants are
added. A player can be in multiple conversations at once.

### Notes

Cleanup on quit is automatic. Chat interception can conflict with [Channel](Channel.md) write-mode
and [Chat Input](#chat-input-chatinputapi) — decide precedence carefully if combining.

---

## Wizards (`WizardAPI`)

Interactive multi-step flows driven by player world interaction: generic interaction wizards,
location-picking wizards (N points), and region-selection wizards (N cuboids, optional wand), with
title/action-bar HUD feedback. Each returns a `CompletableFuture<T>`.

### Initialization

```java
WizardAPI.init(plugin);      // registers the manager as a Listener
WizardAPI.shutdown();        // cancels sessions, clears displays
```

### API

```java
<T> CompletableFuture<T> interaction(Player, [WizardConfig,] InteractionHandler<T>);
<T> CompletableFuture<T> location(Player, int count, [WizardConfig,] LocationHandler<T>);
<T> CompletableFuture<T> selection(Player, int count, [boolean giveWand | WizardConfig, ...], SelectionHandler<T>);
boolean cancel(Player);  boolean hasActive(Player);
Optional<WizardSession<?>> getSession(Player);  void updateDisplay(Player, WizardConfig);
```

Handlers are functional SPIs returning a `WizardResult<T>` (a terminal result completes the
future); each has a default `onStart` hook.

```java
WizardAPI.location(player, 2, (p, loc, all, remaining) -> {
    if (remaining == 0) return WizardResult.complete(new Cuboid(all.get(0), all.get(1)));
    return WizardResult.continueWizard();
}).thenAccept(cuboid -> saveRegion(cuboid));
```

### Flow & Threading

- Starting a wizard **cancels any existing one** for that player (one active per player).
- Interaction events route to the session at `EventPriority.HIGHEST` with a per-session cooldown
  (default 50ms). Selection wizards integrate with the [region](Regions.md) `SelectionManager`
  (wand + SHIFT+LEFT-CLICK to confirm multi-area).
- Runs on the **main thread** (Bukkit interaction events). Keep handlers fast; offload heavy work.
  Quit cancels the wizard. HUD via `TitleAPI`/`ActionBarAPI` updatables.

### Best Practices / Mistakes

- Don't nest wizards — starting one cancels the other.
- Always return a terminal `WizardResult` eventually, or the future stays pending until
  cancel/quit.
- Use the `giveWand=true` selection variant for region tools; the manager handles wand lifecycle.

---

## Snapshots (`SnapshotAPI` & `SnapshotStoreAPI`)

Capture and restore **complete player state**: inventory, armor, offhand, health, food, exp, potion
effects, and flight state. Two APIs:

- **`SnapshotAPI`** — transient, in-memory snapshots kept in a registry + cache.
- **`SnapshotStoreAPI`** — database-persisted player-state store (survives restarts; restore on
  join across contexts).

### Initialization

```java
SnapshotAPI.initialize(plugin);   // synchronized
SnapshotStoreAPI.initialize();    // lazily constructs the store, registers the PlayerStateRecord entity (needs Database)
```

### `SnapshotAPI` (transient)

```java
SnapshotBuilder builder(Player);                       // excludeInventory/Health/Food/Exp/PotionEffects/FlightState
CompletableFuture<SnapshotData> createAsync(Player);   SnapshotData create(Player);
CompletableFuture<SnapshotData> createAndRegisterAsync(Player, String id);  SnapshotData createAndRegister(Player, String id);
CompletableFuture<Boolean> restoreAsync(Player, SnapshotData);  void restore(Player, SnapshotData);
CompletableFuture<Boolean> restoreRegisteredAsync(Player, String id);  boolean restoreRegistered(Player, String id);
Optional<SnapshotData> getRegistered(UUID, String id);
void register(UUID, String id, SnapshotData);  void unregister(UUID, String id);
```

### `SnapshotStoreAPI` (persistent, needs [Database](Database.md))

```java
CompletableFuture<Void> save(Player, String contextId);
CompletableFuture<Void> saveAndClear(Player, String contextId); // saves then clears inv/armor/offhand
CompletableFuture<Void> restore(Player, Consumer<Location> teleportCallback);
void restoreSync(Player, Consumer<Location> teleportCallback);
void checkAndRestore(Player, Consumer<Location> teleportCallback);
```

`PlayerStateRecord` is a DB entity (`snapshot_player_states`, PK `uuid`) holding the serialized
`SnapshotData`, a `contextId`, and the last `Location`.

### Threading & Correctness

- Snapshot **capture** runs off-thread; **application to the player is forced onto the main/entity
  thread** by the API (`TaskAPI.runSync` / `Tasks.runOnEntity`). Do **not** apply snapshots
  off-thread yourself.
- The store uses a per-UUID `restoreGeneration` counter: `save`/`saveAndClear` bump it;
  `restore`/`checkAndRestore` abort if the generation changed (guards save/restore races). The
  record is deleted after a successful restore.
- `restoreRegistered(...)` blocks via `.join()` — avoid on the main thread; use the async variant.

### Best Practices / Mistakes

- Use `SnapshotAPI` for transient in-memory needs; use `SnapshotStoreAPI` for persistence across
  restarts.
- Use `contextId` to separate store contexts (minigame vs event).
- Let the API schedule application to the entity thread; never mutate player state off-thread.
- The [Menus](Menus.md) framework uses snapshots via `AutoSnapshotHandler` for
  `snapshot.enabled: true` menus.

---

## Relationship With Other Systems

- **Chat Input**, **Conversations**, **Channel** all intercept chat — combine with care.
- **Wizards** depend on [Regions](Regions.md) selection and [Visuals](Visuals.md) HUD.
- **Snapshots** depend on [Database](Database.md) (store) and [TaskAPI](TaskAPI.md), and integrate
  with [Menus](Menus.md).
