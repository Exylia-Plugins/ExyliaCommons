# Items

## Overview

The items subsystem builds `ItemStack`s from YAML sections or fluent builders, with rich metadata
support (enchantments, potions, trims, banners, NBT, attributes, custom models, consumable
components, and more). It is the layer the [Menus](Menus.md) framework uses to turn item YAML into
real stacks.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `ItemsAPI` | `v2/items/api/ItemsAPI.java` | Static facade |
| item builders/parsers | `v2/items/...` | Build stacks from YAML/builders |

## Purpose

Item construction from config is error-prone and verbose with the raw Bukkit API. This subsystem
centralizes it so items are defined once, declaratively, and reused across menus, rewards, and
kits — with placeholder and color support baked in.

## Item YAML Format

An item is a section with any of these keys (aliases in parentheses). This is the **same schema
used by menu items**:

| Key(s) | Meaning |
|--------|---------|
| `material` | Bukkit material name |
| `name`, `display-name` | Display name (supports color/placeholders) |
| `lore` | String or list; `<nl>` splits a string into lines |
| `amount` | Int or string |
| `glow`, `glowing` | Enchant glint without visible enchant |
| `hide-attributes`, `hide_attributes` | **Default true** |
| `hide-tooltip`, `hide_tooltip` | Hide tooltip |
| `enchantments:` | Map of enchantment name → level |
| `potion:` / `potion_effects` / `base_potion_type` / `potion_color` | Potion data |
| `armor_trim:` | `pattern`, `material` |
| `leather_color` | String or section |
| `banner_design` (base64) or `banner_patterns:` | Banner data |
| `item_model`, `item-model` | Custom model reference |
| `tooltip_style`, `tooltip-style` | Tooltip style |
| `attributes` | e.g. `"GENERIC_ATTACK_DAMAGE:100:ADD"` |
| `nbt:` | Section or `key:value` list |
| `force-consumable` (+ `consumable-time/nutrition/saturation/sound`) | Force consumable component |
| `unbreakable` | Unbreakable flag |
| `max_stack_size`, `maxStackSize` | Max stack |
| `click_sounds` | Sounds on click (menu context) |
| `slot` XOR `slots` | Placement (menu context); **both → `IllegalArgumentException`** |
| `actions`, `commands` | Behavior on click (menu context) |
| `requires-target`, `requiresTarget`, `condition` | Conditional display |
| `dynamic_update` + `update_interval` (default 20) | Live refresh (menu context) |

### Example

```yaml
sword:
  material: DIAMOND_SWORD
  name: "{primary}Champion Blade"
  lore:
    - "{muted}A legendary weapon<nl>{success}+50 Attack"
  enchantments:
    SHARPNESS: 5
  attributes:
    - "GENERIC_ATTACK_DAMAGE:100:ADD"
  unbreakable: true
  glow: true
```

## Usage

`ItemsAPI` parses a YAML section into an `ItemData`, then *processes* it (resolving placeholders,
colors, and skulls) into a `ProcessedItem`. Real methods (verbatim):

```java
ItemData      parseFromConfig(ConfigurationSection config);
ProcessedItem process(ItemData itemData, Player player [, boolean validate]);
ProcessedItem processFromConfig(ConfigurationSection config, Player player);

// Async variants (recommended in menu population — offloads placeholder/skull work)
CompletableFuture<ProcessedItem> processAsync(ItemData itemData, Player player [, boolean validate]);
CompletableFuture<ProcessedItem> processFromConfigAsync(ConfigurationSection config, Player player);

List<Component> processLore(List<String> rawLore, Player player [, PlaceholderContext context]);
```

Typical one-shot build from a config section:

```java
ProcessedItem item = ItemsAPI.processFromConfig(section, player);
ItemStack stack = item.getItemStack(); // apply to inventory on the main/region thread
```

Item text can contain [placeholders](Placeholders.md) and [color presets](Formatting.md); pass a
player so they resolve. Use the **async** variants inside menu population.

> `ItemsAPI.initialize(plugin)` exists; it is brought up as part of the UI stack. Call it yourself
> only if you use items outside menus.

## Threading Considerations

- Item **construction** can run off the main thread (this is what menus do during async
  population).
- Placing an item into a live inventory must happen on the main/region thread.

## Best Practices

- Define reusable items in YAML and reference them from menus/rewards rather than building in code.
- Remember `hide_attributes` defaults to **true**; set it false if you want vanilla attribute
  lines shown.
- Use color presets in `name`/`lore` for consistent theming.
- Use `<nl>` to keep multi-line lore readable in a single YAML string when convenient.

## Common Mistakes

- Specifying both `slot` and `slots` → `IllegalArgumentException`.
- Expecting attribute lines to show without setting `hide_attributes: false`.
- Assuming placeholders resolve without passing a player/context.

## Relationship With Other Systems

- Consumed by [Menus](Menus.md) and [Rewards](Rewards.md).
- Uses [Skulls](Skulls.md) for player-head items.
- Formats text via [Placeholders](Placeholders.md) + [ColorAPI](Formatting.md).
