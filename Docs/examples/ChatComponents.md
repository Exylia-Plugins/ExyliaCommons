# Example: Chat Components (Input & Messages)

Goal: prompt a player for typed input (with validation and cancellation) and send formatted chat
messages. `ChatInputAPI` automatically picks the best delivery (Dialog / Chat / Inventory /
Bedrock) per client and falls back gracefully.

Related: [PlayerInteraction](../PlayerInteraction.md#chat-input-chatinputapi), [Visuals](../Visuals.md).

`ChatInputAPI` is initialized during bootstrap — it is ready to use.

---

## Text input with validation

```java
ChatInputAPI.text(player, "&eEnter a name for your home:")
    .maxLength(16)
    .validator(s -> s.matches("[A-Za-z0-9_]+"))     // reject invalid chars
    .onResponse(name -> {
        // Runs on the completing handler's thread (chat/inventory = main thread).
        homeManager.create(player, name);
        MessageAPI.send(player, "{success}Home &f" + name + " {success}created!");
    })
    .onCancel(() -> MessageAPI.send(player, "{muted}Cancelled."))
    .ask();
```

## Numeric input with a range

```java
ChatInputAPI.integer(player, "&eHow many to buy? (1-64)")
    .range(1, 64)
    .onResponse(amount -> purchase(player, amount.intValue()))
    .onCancel(() -> MessageAPI.send(player, "{muted}Purchase cancelled."))
    .ask();
```

## Sanitized identifier

Use `id(...)` when the input becomes a key/id — it auto-sanitizes to `[a-z0-9_-]` (max 32 chars),
so you never hand-roll a regex.

```java
ChatInputAPI.id(player, "&eEnter a warp id:")
    .onResponse(id -> warpManager.create(player, id))
    .ask();
```

## Option selection

```java
ChatInputAPI.option(player, "&eChoose a class:")
    .option("warrior", "&cWarrior")
    .option("mage", "&9Mage")
    .option("archer", "&aArcher")
    .columns(3)
    .onResponse(key -> classManager.select(player, key))
    .onCancel(() -> {})
    .ask();
```

## Confirmation

```java
ChatInputAPI.confirm(player, "&cReset all your stats?")
    .onConfirm(() -> statsManager.reset(player))
    .onDeny(() -> MessageAPI.send(player, "{muted}Kept your stats."))
    .ask();
```

## Chaining prompts

Chain follow-up prompts inside `onResponse` — submitting a new request cancels the previous
session, so never fire two prompts at once.

```java
ChatInputAPI.text(player, "&eShop name?")
    .onResponse(name ->
        ChatInputAPI.integer(player, "&ePrice?")
            .range(1, 1_000_000)
            .onResponse(price -> createShop(player, name, price.intValue()))
            .ask())
    .ask();
```

## Sending messages

`MessageAPI` handles formatting, presets, and placeholders. All `send`/`broadcast` methods are
`void`.

```java
MessageAPI.send(player, "{primary}Welcome back, %player_name%!");
MessageAPI.send(player, List.of("{muted}Line one", "{muted}Line two")); // multi-line
MessageAPI.broadcast("{info}The event starts in 5 minutes!");
MessageAPI.broadcastExcluding("{muted}(You are AFK)", afkPlayers);
MessageAPI.sendToFiltered(p -> p.hasPermission("staff.chat"), "{warning}[Staff] restart soon");
MessageAPI.sendInRadius(location, 20.0, "{warning}A boss has spawned nearby!");
```

---

## Why this way

- **One API, every client.** `ChatInputAPI` detects Dialog/Chat/Inventory/Bedrock and falls back
  automatically — you write the prompt once.
- **Validation + cancellation are first-class**, so you never parse raw chat yourself.
- **`id(...)`** removes hand-written sanitization for identifiers.
- **`MessageAPI`** centralizes formatting so all output shares the network theme.

## Common mistakes

- Firing two prompts simultaneously — the second cancels the first. Chain in `onResponse`.
- Doing heavy work directly in `onResponse` on the main thread — offload with `Tasks.io/db(...)`.
- Parsing raw `AsyncPlayerChatEvent` yourself instead of using `ChatInputAPI` (which also handles
  Bedrock and dialog clients).
- Using `MessageAPI.broadcastRadius` — the method is `sendInRadius`.
