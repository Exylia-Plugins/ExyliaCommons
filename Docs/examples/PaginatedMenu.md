# Example: Paginated Menu

Goal: show a paginated list of **live data** (e.g. a top-balance leaderboard) loaded from the
database off the main thread, rendered into a YAML-defined paginated layout with next/previous
navigation and periodic refresh.

Related: [Menus](../Menus.md), [Items](../Items.md), [TaskAPI](../TaskAPI.md),
[Database](../Database.md).

---

## 1. The paginated menu YAML

`menus/leaderboard.yml`:

```yaml
title: "&8Top Balances"
type: PAGINATION
size: 54
pagination:
  # Slots that hold the paginated entries (row 1-4 here).
  slots: [10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43]
  # Template applied to every entry; placeholders come from each entry's PlaceholderContext.
  item_template:
    material: PLAYER_HEAD
    name: "&e#%rank% &f%name%"
    lore:
      - "&7Balance: &a$%balance%"
  navigation:
    previous:
      slot: 45
      material: ARROW
      name: "&aPrevious Page"
    next:
      slot: 53
      material: ARROW
      name: "&aNext Page"
    info:
      slot: 49
      material: PAPER
      name: "&fPage %current_page%/%total_pages%"
items:
  close:
    slot: 48
    material: BARRIER
    name: "&cClose"
    actions:
      - "exyliacommons:close"
```

## 2. Open with a live data supplier

Parse the YAML into a `MenuData`, then attach a **pagination supplier** that produces one
`PlaceholderContext` per entry. The supplier is re-evaluated when the menu refreshes, so the list
stays current. Load the data **off the main thread**, then open on the main thread.

```java
public void openLeaderboard(Player player) {
    // 1. Load data off-thread (DATABASE pool), then open on the main/region thread.
    Tasks.db(() -> profileRepository.findAllOrderedBy("balance", false, 100))
        .thenAccept(result -> {
            if (!result.isSuccess()) {
                MessageAPI.send(player, "&cFailed to load the leaderboard.");
                return;
            }
            List<Profile> top = result.getValue();

            Tasks.sync(() -> openLeaderboardMenu(player, top));
        });
}

private void openLeaderboardMenu(Player player, List<Profile> top) {
    ConfigurationSection section = Configs.get("menus/leaderboard").raw();
    MenuData data = MenuAPI.parse(section); // parse YAML → MenuData

    // Each entry becomes a PlaceholderContext used by the item_template.
    // Keyed values (put) drive %rank%, %name%, %balance% in the template.
    AtomicInteger rank = new AtomicInteger(1);
    MenuData paged = data.withPaginationData(top, profile -> PlaceholderContext.create()
        .put("rank", rank.getAndIncrement())
        .put("name", profile.getName())
        .put("balance", FormatterAPI.formatPrice(profile.getBalance())));

    // Refresh the page every 5s so live data updates while the menu is open.
    paged.setRefreshMode(RefreshMode.SMART);
    paged.setRefreshInterval(100L); // ticks

    MenuAPI.openAsync(player, paged);
}
```

> If you prefer a self-refreshing source, use `MenuData.withPaginationSupplier(Supplier<List<T>>,
> Function<T, PlaceholderContext>)` instead of `withPaginationData` — the supplier is called each
> refresh, so the data reloads without reopening the menu. Keep the supplier cheap (or feed it from
> an already-loaded cache) since it runs on the refresh cadence.

## 3. Navigation

The `previous` / `next` / `info` buttons defined under `pagination.navigation` are wired
automatically by the framework — you do **not** register actions for them. Page changes are
debounced to prevent spam.

---

## Why this way

- **Data is loaded off-thread** (`Tasks.db`) and the menu is opened on the main thread
  (`Tasks.sync`) — never block the server thread loading a leaderboard.
- **Layout stays declarative.** The `item_template` + per-entry `PlaceholderContext` render each
  row; you never build `ItemStack`s by hand.
- **Live updates** come from `RefreshMode.SMART` + a supplier, and only changed slots are
  rewritten.

## Common mistakes

- Loading DB data on the main thread — always `Tasks.db(...)` then `Tasks.sync(...)`.
- Using an expensive supplier with a short refresh interval — cache the data or widen the interval.
- Forgetting `type: PAGINATION` — a `SIMPLE` menu ignores the `pagination` section.
- Manually registering next/previous actions — the navigation block handles them.
