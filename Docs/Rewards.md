# Rewards

## Overview

The reward subsystem defines and grants rewards (commands, items, messages) from YAML, with
probability, conditions, and priority. It is used for crates, quests, vote rewards, and any
"give the player X" flow.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `RewardAPI` | `v2/reward/api/RewardAPI.java` | Static facade |
| `RewardManager` | `v2/reward/core/RewardManager.java` | Singleton lifecycle |
| `RewardConfigLoader` | `v2/reward/config/RewardConfigLoader.java` | Parses reward YAML |
| `RewardConfig` / `RewardEntry` | `v2/reward/model/...` | Parsed reward payloads |
| `RewardContext` | `v2/reward/model/RewardContext.java` | Carries player + `PlaceholderContext` |
| `RewardResult` | `v2/reward/model/RewardResult.java` | Per-reward outcome |
| `RewardType` | `v2/reward/model/RewardType.java` | Enum: `COMMAND`, `ITEM`, `MESSAGE` |

## Purpose

Rewards are content and should be data-driven. This subsystem lets designers declare rewards in
YAML with chance/conditions/priority and lets code grant them uniformly.

## Initialization

`RewardAPI.initialize(plugin)` (→ `RewardManager.getInstance().initialize(plugin)`) is called
**automatically** during bootstrap. In the bootstrap order, `RewardManager` initializes **before**
`ActionAPI`, and the reward-editor UI actions are registered afterward by
`RewardEditorActionRegistrar.register(plugin)`.

## Reward Types

`RewardType` has exactly three values (`v2/reward/model/RewardType.java`): **`COMMAND`**,
**`ITEM`**, **`MESSAGE`**. There is no fourth type.

## Defining Rewards in YAML

Rewards are read from a config section. By default `RewardAPI.give(player, section)` reads the key
`rewards`; `giveFromKey(player, section, key)` reads a custom key. The loader
(`RewardConfigLoader`) accepts **three shapes**:

### 1. Grouped form (a section with `commands:` / `items:` / `messages:` lists) — recommended

```yaml
rewards:
  commands:
    - "give %player_name% diamond 3"                 # plain string → COMMAND
    - command: "broadcast %player_name% won!"        # section form (chance/condition/priority)
      chance: 25.0
      condition: "%vault_eco_balance% >= 1000"
      message: "&aYou won the rare prize!"
      priority: 1
  items:
    - material: DIAMOND
      amount: 3
      name: "&bDiamond Prize"
      lore:
        - "&7A shiny reward"
      chance: 100.0
      condition: ""
      priority: 0
  messages:
    - "&aThanks for voting!"
```

### 2. List of typed entries

```yaml
rewards:
  - type: COMMAND
    command: "give %player_name% diamond 1"
    chance: 100.0
    priority: 0
  - type: ITEM
    material: EMERALD
    amount: 5
    name: "&aEmeralds"
  - type: MESSAGE
    message: "&aReward granted!"
    chance: 50.0
```

For list entries, `type` defaults to `COMMAND` if omitted.

### 3. Inline string list

```yaml
rewards:
  - "command: give %player_name% diamond 1"
  - "message: &aThanks for playing!"
  - "eco add %player_name% 100"    # no prefix → treated as COMMAND
```

### Field reference

| Field | Applies to | Default | Notes |
|-------|-----------|---------|-------|
| `command` | COMMAND | — | The command line to run |
| `material`, `amount`, `name`, `lore` | ITEM | `STONE`, `1`, none, none | Item payload |
| `message` | MESSAGE (payload); COMMAND/ITEM (optional feedback) | — | On COMMAND/ITEM it is a message sent alongside |
| `chance` | all (section/entry forms) | `100.0` | Probability percent |
| `condition` | COMMAND/ITEM/MESSAGE (section/entry) | none | Boolean expression; placeholders allowed |
| `priority` | COMMAND/ITEM/MESSAGE (section/entry) | `0` | Ordering |

Command/message/item text supports [placeholders](Placeholders.md).

## Granting Rewards

`RewardAPI` returns `CompletableFuture<List<RewardResult>>` — grants are **async** and report a
result per reward.

```java
// From a config section (reads the "rewards" key)
RewardAPI.give(player, section)
    .thenAccept(results -> results.forEach(r -> log(r)));

// From a custom key
RewardAPI.giveFromKey(player, questSection, "completion-rewards");

// From programmatically built entries
List<RewardEntry> entries = /* build via RewardConfig/RewardBuilder */;
RewardAPI.give(player, entries);

// With a shared PlaceholderContext (wrapped into a RewardContext internally)
RewardAPI.give(player, section, placeholderContext);
```

`RewardContext` (built internally by the context overloads) carries the `player` and a
`PlaceholderContext` — it does **not** carry `silent`/`skipConditions`/`skipProbability` flags.

## Threading Considerations

- `give*` returns a `CompletableFuture` and runs the reward pipeline off-thread; work that touches
  Bukkit (item give, command dispatch) is scheduled on the correct thread.
- Command rewards run through [CommandAPI](Commands.md); condition evaluation resolves placeholders.

## Best Practices

- Prefer the **grouped form** (`commands:`/`items:`/`messages:`) for readability.
- Use `chance`/`condition`/`priority` for weighted, gated rewards.
- Reuse [item YAML](Items.md) conventions (`material`/`amount`/`name`/`lore`) for item rewards.
- Consume the returned `List<RewardResult>` to detect which rewards actually fired.

## Common Mistakes

- Assuming a fourth reward type exists — only `COMMAND`, `ITEM`, `MESSAGE`.
- Expecting `RewardAPI.grant(...)` — the method is `give(...)` / `giveFromKey(...)`.
- Expecting `RewardContext` grant flags (silent/skip*) — it only holds player + placeholder context.
- Treating `give*` as synchronous — it returns a `CompletableFuture`.

## Extension Points

- Build rewards programmatically via `RewardAPI.builder()` (`RewardBuilder`) and pass
  `List<RewardEntry>` to `give`.
- Reward execution is wired to the three built-in `RewardType`s; adding a new type would require
  extending `RewardType` and the executor's dispatch.

## Relationship With Other Systems

- Item rewards use [Items](Items.md); command rewards use [CommandAPI](Commands.md); message
  rewards use [MessageAPI](Visuals.md).
- Conditions/text use [Placeholders](Placeholders.md).
- The reward-editor UI ties [Menus](Menus.md) ↔ [Actions](Actions.md) ↔ Rewards.
- Reloaded via the [Reload](Reload.md) system (`RewardAdapter`, registered as `"RewardManager"`).
