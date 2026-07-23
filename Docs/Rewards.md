# Rewards

## Overview

The reward subsystem defines and grants rewards (items, commands, and more) from YAML, with
probability, conditions, priority, and provider-based execution. It is used for crates, quests,
vote rewards, and any "give the player X" flow.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `RewardAPI` | `v2/reward/api/RewardAPI.java` | Static facade |
| `RewardManager` | `v2/reward/core/RewardManager.java` | Singleton lifecycle |
| `RewardExecutor` | `v2/reward/core/...` | Dispatches to providers |
| `RewardProvider` | `v2/reward/provider/...` | Backend per reward type |
| `RewardContext` | `v2/reward/...` | Grant flags (silent, skipConditions, ...) |
| `RewardType` | `v2/reward/model/...` | Enum of reward types |

## Purpose

Rewards are content and should be data-driven. This subsystem lets designers declare rewards in
YAML with chance/conditions/priority and lets code grant them uniformly, delegating each reward
type to a provider.

## Initialization

`RewardManager.initialize(plugin)` is called **automatically** during bootstrap. The reward-editor
UI actions are registered by `RewardEditorActionRegistrar.register(plugin)`.

## Defining Rewards in YAML

Rewards are declared under a `rewards:` section. Each reward has a type, its payload, and optional
`chance`, `condition`, and `priority`.

```yaml
rewards:
  diamond_prize:
    type: ITEM
    chance: 25.0
    priority: 1
    item:
      material: DIAMOND
      amount: 3
      name: "{success}Diamond Prize"
  broadcast:
    type: COMMAND
    command: "console: broadcast %player_name% won a prize!"
  condition_gated:
    type: COMMAND
    condition: "%vault_eco_balance% >= 1000"
    command: "player: buy premium"
```

The three built-in reward types are wired in `RewardExecutor` (item, command, and the third
enum type). Reward text/commands support [placeholders](Placeholders.md).

## Granting Rewards

Grant through `RewardAPI`, optionally supplying a `RewardContext` to control behavior:

```java
RewardAPI.grant(player, reward);
RewardAPI.grant(player, reward, RewardContext.builder()
    .silent(true)          // suppress messages
    .skipConditions(true)  // ignore condition checks (admin grant/testing)
    .skipProbability(true) // always grant regardless of chance
    .build());
```

## Chance, Conditions, Priority

- **`chance`** — probability the reward is granted (unless `skipProbability`).
- **`condition`** — a boolean expression (placeholders allowed) gating the grant (unless
  `skipConditions`).
- **`priority`** — ordering among competing rewards.

## Threading Considerations

- Reward granting that touches inventories/Bukkit runs on the correct thread; command rewards go
  through [CommandAPI](Commands.md).
- Condition evaluation resolves placeholders.

## Best Practices

- Define rewards in YAML under `rewards:` using the grouped form for readability.
- Use `chance`/`condition`/`priority` for weighted, gated rewards.
- Use `RewardContext` flags (`silent`, `skipConditions`, `skipProbability`) for admin grants and
  testing.
- Reuse [item YAML](Items.md) definitions for item rewards.

## Common Mistakes

- Expecting a new reward type to work without extending `RewardType` **and** adding a provider —
  the executor is wired to the built-in enum types.
- Forgetting that `command` rewards run through the sender prefixes (`player:` / `console:`).

## Extension Points

- Implement a `RewardProvider` and register it in `RewardExecutor` for a new reward type (requires
  extending `RewardType` and the provider map).

## Relationship With Other Systems

- Item rewards use [Items](Items.md); command rewards use [CommandAPI](Commands.md).
- Conditions/text use [Placeholders](Placeholders.md).
- The reward-editor UI ties [Menus](Menus.md) ↔ [Actions](Actions.md) ↔ Rewards.
- Reloaded via the [Reload](Reload.md) system (`RewardAdapter`).
