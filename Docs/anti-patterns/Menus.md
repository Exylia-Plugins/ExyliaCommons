# Anti-Pattern: Incorrect Menu Implementations

The menu framework is data-driven and thread-aware. These are the concrete ways plugins misuse it.

Reference: [../Menus.md](../Menus.md), [../Items.md](../Items.md), [../Skulls.md](../Skulls.md),
[../best-practices/UI.md](../best-practices/UI.md).

---

## 1. Specifying both `slot` and `slots`

**Anti-pattern:**

```yaml
# BAD — throws IllegalArgumentException
item:
  slot: 10
  slots: [11, 12]
```

`v2/items/processor/ConfigurationParser.java`:

```java
if (config.contains("slot") && config.contains("slots")) {
    throw new IllegalArgumentException("Cannot specify both 'slot' and 'slots' in configuration");
}
```

**Preferred:** use `slot` for one placement or `slots` for many — never both on one item.

---

## 2. Invalid menu size

**Anti-pattern:** `size` that isn't a multiple of 9 or is outside 9–54 → `InvalidMenuConfigException`.

**Preferred:** `size` ∈ {9,18,27,36,45,54}. Default is 54.

---

## 3. Building menus/inventories in Java

**Anti-pattern:** custom `InventoryHolder`, hardcoded slots, and `ItemStack` assembly in code.

**Why:** duplicates the entire menu framework and drops async population, actions, pagination,
snapshots, refresh diffing, and packet title updates.

**Preferred:** define the layout in a `menus/*.yml` and open it:

```java
MenuAPI.openAsync(player, Configs.get("menus/main").raw());
```

---

## 4. Global click handlers instead of per-item actions

**Anti-pattern:** one big `onClick` switch on slot numbers.

**Why:** brittle (breaks when slots move) and un-composable. The framework runs per-item `actions`
/ `commands` through the [action pipeline](../Actions.md) with `ActionSource.MENU`.

**Preferred:** attach behavior to items and register the actions once.

```yaml
shop:
  slot: 11
  material: EMERALD
  actions: ["myplugin:open_shop"]
```

```java
ActionAPI.register(ActionAPI.create("open_shop", this).namespace("myplugin").sync()
    .handler((ctx, args) -> openMenu(ctx.getPlayer(), "menus/shop")).build());
```

Use the built-in navigation actions (`exyliacommons:close/back/next_page/previous_page`) instead of
re-implementing them.

---

## 5. Reopening the menu to refresh it

**Anti-pattern:** a timer that calls `openAsync` every tick to update values.

**Why:** the framework diffs items and rewrites only changed slots; reopening flickers and wastes
work.

**Preferred:** `dynamic_update` + `update_interval` on items, `RefreshMode.SMART`, and a pagination
supplier / `PlaceholderContext` for live data. See [Performance](Performance.md).

---

## 6. Expecting sync skulls to show real skins

**Anti-pattern:** relying on `SkullAPI.fromPlayer(...)` in menu items to render actual player skins
without preloading.

**Why:** the sync path returns **cache/default (Steve)**; it never blocks to fetch.

**Preferred:** `SkullAPI.preloadPlayers(...)` / `batchPlayers(...)` before opening, so heads render
from cache. See [../Skulls.md](../Skulls.md).

---

## 7. Blocking data loads while opening

**Anti-pattern:**

```java
// BAD — DB call on the main thread while building the menu
List<Profile> top = repo.findAllOrderedBy("balance", false, 100);
MenuAPI.openAsync(player, section);
```

**Preferred:** load off-thread, then open on the main thread.

```java
Tasks.db(() -> repo.findAllOrderedBy("balance", false, 100))
    .thenAccept(r -> Tasks.sync(() -> openLeaderboard(player, r.getValue())));
```

---

## 8. Mutating inventories / items off the main thread

**Anti-pattern:** editing the open inventory or an `ItemStack` from an async pool.

**Why:** only item *processing* (placeholders, skull fetch) is async; **inventory writes must be
synchronous** (main/region thread).

**Preferred:** let `openAsync` / the refresh system perform writes; if you must change an item, do it
via `Tasks.sync` / the menu's refresh, not from async code.

---

## 9. Hand-managing inventory backup/restore

**Anti-pattern:** manually saving and restoring a player's inventory around a full-inventory menu.

**Preferred:** enable snapshotting — the framework's `AutoSnapshotHandler` handles it.

```yaml
snapshot:
  enabled: true
  restore_on_close: true
```

---

## 10. Forgetting to initialize / clean up

**Anti-pattern:** using `MenuAPI` without `MenuAPI.initialize(this)` (→ `IllegalStateException`), or
never `ActionAPI.unregisterAll(this)` on disable so menu actions leak.

**Preferred:** initialize in `onExyliaEnable` (this also brings up `CommandAPI` + `SkullAPI`), and
`ActionAPI.unregisterAll(this)` in `onExyliaDisable`.

---

## 11. Assuming `hide_attributes` defaults to false

**Anti-pattern:** expecting vanilla attribute lines to show without setting the flag.

**Why:** `hide_attributes` defaults to **true** in the item schema.

**Preferred:** set `hide_attributes: false` explicitly when you want attributes visible.

---

## Checklist

- [ ] Never both `slot` and `slots`; `size` a multiple of 9 (9–54).
- [ ] Layout in YAML; open with `MenuAPI.openAsync`.
- [ ] Behavior via per-item actions/commands + built-in navigation actions.
- [ ] Refresh by diff (`SMART`/`dynamic_update`), never reopen loops.
- [ ] Skulls preloaded; no reliance on sync fetch for real skins.
- [ ] Data loaded off-thread, then opened on main thread.
- [ ] No inventory/item mutation off the main thread.
- [ ] Full-inventory menus use `snapshot.enabled`.
- [ ] `MenuAPI.initialize(this)` on enable; `ActionAPI.unregisterAll(this)` on disable.
- [ ] `hide_attributes: false` only when attributes should show.
