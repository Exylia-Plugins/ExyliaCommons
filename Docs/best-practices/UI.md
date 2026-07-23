# UI Standard

Exylia UIs are **data-driven**. A menu is a YAML layout; behavior is attached to items via
**actions** and **commands**; item text uses **color presets and placeholders**. Java wires the
pieces together but never hardcodes layouts.

Reference: `v2/ui` (`MenuAPI`, `MenuParser`, `MenuActionRegistrar`), `v2/items` (`ItemsAPI`),
[../Menus.md](../Menus.md), [../Items.md](../Items.md),
[../examples/SimpleMenu.md](../examples/SimpleMenu.md).

---

## 1. Define layouts in YAML, not Java

Every menu is a `.yml` under `menus/`. Designers edit it without touching code.

```yaml
title: "&8Main Menu"
type: SIMPLE
size: 27
items:
  shop:
    slot: 11
    material: EMERALD
    name: "{success}Open Shop"
    actions:
      - "myplugin:open_shop"
  close:
    slot: 15
    material: BARRIER
    name: "{error}Close"
    actions:
      - "exyliacommons:close"     # built-in
```

**Standard:** the only Java for a menu is (a) registering any custom actions and (b) calling
`MenuAPI.openAsync(player, section)`.

## 2. Attach behavior with actions/commands, not click handlers

Buttons trigger registered [actions](../Actions.md) (via the pipeline, with `ActionSource.MENU`) and
`commands` (via [CommandAPI](../Commands.md)). Register custom actions once, owned by your plugin:

```java
ActionAPI.register(ActionAPI.create("open_shop", this)
    .namespace("myplugin")
    .sync()                                  // opening a menu touches the main thread
    .handler((ctx, args) -> openMenu(ctx.getPlayer(), "menus/shop"))
    .build());
// onDisable: ActionAPI.unregisterAll(this);
```

Built-in actions (`exyliacommons:close/back/next_page/previous_page`) cover navigation — don't
re-implement them.

## 3. Open asynchronously

Use `MenuAPI.openAsync`. Item population (placeholders, skulls) runs off-thread; the inventory write
happens on the correct thread automatically. **Never build/populate inventories on the main thread
in a hot path.**

```java
MenuAPI.openAsync(player, Configs.get("menus/main").raw());
```

For data-backed menus, **load the data off-thread first**, then open:

```java
Tasks.db(() -> repo.findAllOrderedBy("balance", false, 100))
    .thenAccept(r -> Tasks.sync(() -> openLeaderboard(player, r.getValue())));
```

## 4. Pagination via the framework, not manual paging

Use `type: PAGINATION` with a `pagination.item_template` + `pagination.navigation`, and feed live
data with `MenuData.withPaginationData(...)` / `withPaginationSupplier(...)` and a per-entry
`PlaceholderContext`. Navigation buttons are wired automatically. See
[../examples/PaginatedMenu.md](../examples/PaginatedMenu.md).

## 5. Build items with `ItemsAPI` and the shared item schema

Never assemble `ItemStack`s by hand. The item YAML schema (material, name, lore, `enchantments`,
`glow`, `nbt`, `attributes`, potions, trims, banners, `hide_attributes` [default **true**], …) is
shared by menus, rewards, and kits. Use player heads via [SkullAPI](../Skulls.md) and **preload**
them so menus render from cache.

## 6. Use snapshots for full-inventory menus

Menus that expose the player's own inventory enable snapshotting so state is saved on open and
restored on close:

```yaml
snapshot:
  enabled: true
  restore_on_close: true
```

Don't hand-manage inventory backup/restore — the framework's `AutoSnapshotHandler` does it.

## 7. Refresh by change, not by reopening

Mark dynamic items `dynamic_update` with an `update_interval`, and use `RefreshMode.SMART`. Reopening
a menu each tick to "refresh" is an anti-pattern — the framework diffs and rewrites only changed
slots. See [Performance.md](Performance.md).

## 8. Text = presets + placeholders

Item `name`/`lore` use `{preset}` color presets and `%placeholders%`
([Messages.md](Messages.md), [../Formatting.md](../Formatting.md)). Don't hardcode hex or duplicate
theme colors.

---

## Checklist

- [ ] Layout in a `menus/*.yml`; Java only registers actions and calls `openAsync`.
- [ ] Behavior via registered actions/commands (owned by the plugin, unregistered on disable).
- [ ] Data loaded off-thread, then opened on the main thread.
- [ ] Pagination via `type: PAGINATION` + supplier; navigation buttons not hand-built.
- [ ] Items via `ItemsAPI` + shared schema; skulls preloaded.
- [ ] Full-inventory menus use `snapshot.enabled`.
- [ ] Dynamic menus use `dynamic_update` + `SMART` refresh, not reopen loops.
- [ ] Text uses presets/placeholders.

## Anti-patterns

- Building menus/inventories in Java with hardcoded slots and items.
- Global click handlers instead of per-item actions.
- Populating items or opening menus synchronously with blocking data loads.
- Manual pagination math and hand-built next/previous buttons.
- Hand-rolled inventory backup/restore instead of snapshots.
- Reopening a menu every tick to update it.
