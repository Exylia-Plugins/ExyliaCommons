# Example: Rewards

Goal: define rewards in YAML (with chance/conditions/priority) and grant them, plus build a reward
programmatically.

Related: [Rewards](../Rewards.md), [Items](../Items.md), [Commands](../Commands.md).

`RewardAPI.initialize(this)` runs automatically in bootstrap. Reward types are exactly
`COMMAND`, `ITEM`, `MESSAGE`.

---

## 1. Define rewards in YAML

Use the grouped form (`commands:` / `items:` / `messages:`). Each entry may carry `chance`
(default 100.0), `condition`, and `priority` (default 0).

```yaml
# crates.yml
common-crate:
  commands:
    - "give %player_name% iron_ingot 8"
    - command: "broadcast &e%player_name% &7opened a crate!"
      chance: 10.0                          # 10% chance to also broadcast
  items:
    - material: DIAMOND
      amount: 2
      name: "&bDiamond Reward"
      lore:
        - "&7From the common crate"
      chance: 40.0
    - material: NETHERITE_INGOT
      amount: 1
      name: "&5Rare!"
      chance: 2.0
      condition: "%myplugin_vip% == true"   # only if the condition passes
  messages:
    - "&aThanks for opening a crate!"
```

## 2. Grant from config

`give` reads the `rewards` key by default; `giveFromKey` reads a custom key. Both are **async** and
return the per-reward results.

```java
ConfigurationSection crates = Configs.get("crates").raw();

RewardAPI.giveFromKey(player, crates, "common-crate")
    .thenAccept(results -> {
        long granted = results.stream().filter(RewardResult::isSuccess).count();
        DebugAPI.logPluginDebug(player.getName() + " received " + granted + " rewards");
    });
```

With a shared placeholder context (used for conditions/text):

```java
PlaceholderContext ctx = PlaceholderContext.create()
    .withPlayer(player)
    .put("myplugin_vip", vipService.isVip(player));

RewardAPI.giveFromKey(player, crates, "common-crate", ctx);
```

## 3. Build a reward programmatically

```java
RewardAPI.builder()
    .command("give %player_name% diamond 1")
    .chance(50.0)
    .condition("%vault_eco_balance% >= 100")
    .successMessage("&aYou won a diamond!")
    .priority(1)
    .give(player)
    .thenAccept(result -> { /* RewardResult */ });
```

---

## Why this way

- **Rewards are data.** Designers tune drop tables (chance/condition/priority) in YAML.
- **Grants are async** and report a `RewardResult` per reward, so you can log/track outcomes.
- **Conditions + placeholder context** gate rewards without code branches.
- Item rewards reuse [item conventions](../Items.md); command rewards route through
  [CommandAPI](../Commands.md); message rewards through [MessageAPI](../Visuals.md).

## Common mistakes

- Expecting a `grant(...)` method — it is `give(...)` / `giveFromKey(...)`.
- Expecting a fourth reward type — only `COMMAND`, `ITEM`, `MESSAGE`.
- Treating `give*` as synchronous — it returns a `CompletableFuture<List<RewardResult>>`.
- Assuming `RewardContext` has silent/skip flags — it only carries player + placeholder context.
