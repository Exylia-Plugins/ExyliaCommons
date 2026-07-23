# Menus

## Overview

A YAML-driven and programmatic inventory-menu framework. It supports multiple menu types, async
item building, placeholder/action/command integration, single and multi-section pagination,
navigation history, packet-based title updates, refresh strategies, animations, item-input
capture, fillers, sounds, and automatic snapshot/restore of player inventory.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `MenuAPI` | `v2/ui/api/MenuAPI.java` | Static facade — your entry point |
| `MenuManager` | `v2/ui/core/MenuManager.java` | Singleton lifecycle, registry, open flow |
| `MenuFactory` | `v2/ui/core/MenuFactory.java` | Creates the right `MenuBase` per `MenuType` |
| `MenuParser` | `v2/ui/config/MenuParser.java` | Parses YAML menu definitions |
| `MenuClickHandler` / `MenuCloseHandler` | `v2/ui/event/...` | Event callbacks |
| `NavigationManager` / `NavigationStack` | `v2/ui/navigation/...` | Back-history |
| `PaginationContext` / `PageCalculator` | `v2/ui/pagination/...` | Paging |
| `InventoryTitleUpdater` / `PacketEventsSupport` | `v2/ui/packet/...` | Live title updates |
| `AutoSnapshotHandler` | `v2/ui/snapshot/AutoSnapshotHandler.java` | Inventory save/restore |

## Purpose

Inventory GUIs are ubiquitous and tedious to build correctly (async data, placeholders, pagination,
Folia threading, safe close handling). This framework makes menus **declarative** (define them in
YAML) while still allowing programmatic construction, and handles the hard parts (async item
population, packet title updates, snapshot/restore) for you.

## Initialization

```java
MenuAPI.initialize(plugin); // also initializes CommandAPI + SkullAPI, registers listeners & actions
```

`MenuManager.getInstance()` throws `IllegalStateException` if not initialized.

## Defining a Menu in YAML

Menus live as `.yml` files (typically under a `menus/` folder). Top-level keys:

- `title` (default `"Menu"`)
- `type` (default `SIMPLE`) — one of the supported menu types
- `size` (default `54`; must be a multiple of 9, between 9 and 54, else
  `InvalidMenuConfigException`)
- an items section, where each item is a slot-bound definition

### Item definition keys

Each item supports a rich key set (aliases in parentheses). All are optional unless noted:

`material`; `name` / `display-name`; `lore` (string or list; `<nl>` splits lines);
`amount` (int or string); `glow` / `glowing`; `hide-attributes` / `hide_attributes`
(**default true**); `hide-tooltip` / `hide_tooltip`; `enchantments:` (name→level map);
`potion:` section OR flat `potion_effects` / `base_potion_type` / `potion_color`;
`armor_trim:` (`pattern`, `material`); `leather_color` (string or section);
`banner_design` (base64) or `banner_patterns:`; `click_sounds`; `item_model` / `item-model`;
`tooltip_style` / `tooltip-style`; `dynamic_update` + `update_interval` (default 20);
`attributes` (e.g. `"GENERIC_ATTACK_DAMAGE:100:ADD"`); `nbt:` (section or `key:value` list);
`force-consumable` / `force_consumable` + `consumable-time/nutrition/saturation/sound`;
`unbreakable`; `max_stack_size` / `maxStackSize`; `slot` XOR `slots` (specifying **both** →
`IllegalArgumentException`); `actions` (list); `commands` (list);
`requires-target` / `requiresTarget`; `condition`.

See [Items.md](Items.md) for the full item-building semantics.

### Example menu YAML

```yaml
title: "&8Main Menu"
type: SIMPLE
size: 27
items:
  info:
    slot: 13
    material: PAPER
    name: "{primary}Welcome %player_name%"
    lore:
      - "{muted}Click a button below"
    glow: true
  shop:
    slot: 11
    material: EMERALD
    name: "{success}Shop"
    actions:
      - "[open_menu] shop"
    click_sounds:
      - "UI_BUTTON_CLICK"
  close:
    slot: 15
    material: BARRIER
    name: "{error}Close"
    commands:
      - "player: menu close"
```

## Opening a Menu

Use the async open path so item population (placeholders, skulls) runs off-thread and the display
write happens on the correct thread automatically. Real signatures (verbatim):

```java
CompletableFuture<Void> MenuAPI.openAsync(Player player, ConfigurationSection config);
CompletableFuture<Void> MenuAPI.openAsync(Player player, MenuData menuData);
void                    MenuAPI.open(Player player, ConfigurationSection config);   // sync open
void                    MenuAPI.open(Player player, MenuData menuData);
boolean                 MenuAPI.refresh(Player player);
Optional<MenuBase>      MenuAPI.getActiveMenu(Player player);
// Item-input menus (capture items the player places):
ItemInputMenu MenuAPI.openItemInput(Player player, ConfigurationSection config, Consumer<Map<Integer, ItemStack>> onClose);
```

```java
// Open a YAML-defined menu:
MenuAPI.openAsync(player, Configs.get("menus/main").section("root"));
```

Programmatic construction is available via `MenuData`/builders when YAML is not appropriate.
Snapshot-enabled menus save the player's inventory on open and restore it on close (see below).

## Click & Close Handling

- Per-item `actions` and `commands` fire on click (grouped by click type). This is the preferred
  model — attach behavior to items, not global handlers.
- Global `MenuClickHandler` / `MenuCloseHandler` callbacks exist for menu-wide logic.
- Clicks are **debounced (~150ms)** to prevent double-processing.

## Pagination & Navigation

- Single- and multi-section pagination via `PaginationContext` / `PageCalculator`.
- Provide live data with a **pagination supplier** (`withPaginationSupplier`) combined with a
  non-`DISABLED` refresh mode so pages update automatically.
- **Navigation history** is capped at 10 entries (bounded memory), stored as copies. Use the
  navigation stack for "back" buttons.
- Page navigation is debounced to prevent spam.

## Live Title Updates (Packets)

`InventoryTitleUpdater` updates the open inventory's title without reopening it, using
**PacketEvents when available** (`PacketEventsSupport` gates on the optional dependency). Title
updates are suppressed when the title is unchanged (`lastRenderedTitle`). If PacketEvents is not
installed, title updates degrade gracefully.

## Refresh Strategies

Items can be marked `dynamic_update` with an `update_interval`. Refresh diffs the old vs new
`ItemStack` and **writes only changed slots**. `SMART`/`SLOT_ONLY` modes process only dynamic
items, minimizing work and packets.

## Snapshot Integration

Menus that expose the player's own inventory can enable snapshotting:

```yaml
snapshot:
  enabled: true
  restore_on_close: true
```

On open, the player's inventory is saved; on close it is restored. This uses the
[Snapshot](PlayerInteraction.md) subsystem via `AutoSnapshotHandler`.

## Threading Considerations

- **Inventory writes must be synchronous** (main/region thread). Only item *processing*
  (placeholders, skull fetch) is async — done internally via `ItemsAPI.processAsync`.
- Prefer `MenuAPI.openAsync`; the framework performs the sync display step for you. (`MenuAPI` has
  no `processAsync` method — item processing is an `ItemsAPI` concern.)
- Never mutate an inventory or `ItemStack` off the main thread.

## Best Practices

- Prefer `MenuAPI.openAsync` and let the framework schedule the sync display.
- Enable `snapshot.enabled: true` (with `restore_on_close: true`) for full-inventory menus.
- Use pagination suppliers with a non-`DISABLED` refresh mode for live data.
- **Preload/batch skulls at startup** (`SkullAPI.preloadPlayers(String...)`) so heads render from
  cache.
- Attach `actions`/`commands`/`click_sounds` per item with click-type groups instead of global
  handlers.
- Call `MenuAPI.shutdown()` on disable (handled by the coordinator for shared state; call it if
  you own extra resources).

## Common Mistakes

- Forgetting `MenuAPI.initialize(plugin)` → `IllegalStateException` from `getInstance`.
- Specifying **both** `slot` and `slots` on an item → `IllegalArgumentException`.
- `size` not a multiple of 9 / out of 9–54 range → `InvalidMenuConfigException`.
- Mutating inventories/items off the main thread.
- Relying on the `hide_attributes` default being false — it defaults to **true**.
- Expecting real player skins from synchronous skull building — sync returns cached/default only.

## Performance Considerations

- 150ms click/navigation debounces prevent double-processing and page spam.
- Refresh diffs items and writes only changed slots; smart modes process only dynamic items.
- Title updates are suppressed when unchanged.
- Item population runs off-thread; only inventory writes are synchronous.

## Relationship With Other Systems

- Builds items via [Items](Items.md), heads via [Skulls](Skulls.md).
- Fires [Actions](Actions.md) (`ActionSource.MENU`) and [Commands](Commands.md) on click.
- Formats text via [ColorAPI](Formatting.md) and [Placeholders](Placeholders.md).
- Uses [Snapshot](PlayerInteraction.md) for inventory save/restore.
- The reward editor UI ties Menus ↔ [Actions](Actions.md) ↔ [Rewards](Rewards.md) together.
