# Example: Simple Menu

Goal: open a small YAML-defined menu whose buttons run **registered actions** and **commands**.
This is the recommended production pattern — the menu layout is data, and behavior is attached via
actions/commands, not hardcoded click handlers.

Related: [Menus](../Menus.md), [Actions](../Actions.md), [Items](../Items.md).

---

## 1. The menu YAML

`src/main/resources/menus/main.yml` (copied to the data folder on first access):

```yaml
title: "&8Main Menu"
type: SIMPLE
size: 27
open_sounds:
  - "BLOCK_CHEST_OPEN"
items:
  info:
    slot: 13
    material: PAPER
    name: "&aWelcome, %player_name%"
    lore:
      - "&7Balance: &f%vault_eco_balance%"
      - ""
      - "&eClick a button below."
    glow: true
  shop:
    slot: 11
    material: EMERALD
    name: "&2Open Shop"
    click_sounds:
      - "UI_BUTTON_CLICK"
    actions:
      - "myplugin:open_shop"
  spawn:
    slot: 15
    material: COMPASS
    name: "&bTeleport to Spawn"
    commands:
      - "player: spawn"
  close:
    slot: 22
    material: BARRIER
    name: "&cClose"
    actions:
      - "myplugin:close"          # built-in menu action namespace is the initializing plugin name
```

> Built-in menu actions available out of the box: `close`, `back`, `next_page`, `previous_page`.
> They are registered under the initializing plugin's lowercase name, for example
> `myplugin:close`, not a fixed `exyliacommons` namespace.

## 2. Register your custom action

Menu buttons trigger **actions** through the [Action](../Actions.md) pipeline. Register your custom
action once in `onExyliaEnable`. The clicked player is available on the `ActionContext`.

```java
public final class MyPlugin extends ExyliaPlugin {

    @Override
    protected void onExyliaEnable() {
        // MenuAPI.initialize also brings up CommandAPI + SkullAPI.
        MenuAPI.initialize(this);

        registerMenuActions();
    }

    private void registerMenuActions() {
        Action openShop = ActionAPI.create("open_shop", this)
            .namespace("myplugin")               // → "myplugin:open_shop"
            .description("Open the shop menu")
            .sync()                              // opening a menu touches the main thread
            .handler((ctx, args) -> {
                Player player = ctx.getPlayer();
                openMenu(player, "menus/shop");  // open another YAML menu
            })
            .build();
        // build() already registered openShop. Do not register it a second time.
    }

    @Override
    protected void onExyliaDisable() {
        // Clean up every action this plugin registered.
        ActionAPI.unregisterAll(this);
    }

    /** Opens a YAML menu by its config path (async item population, sync display). */
    public void openMenu(Player player, String configPath) {
        ConfigurationSection section = Configs.get(configPath).raw();
        MenuAPI.openAsync(player, section);
    }
}
```

## 3. Open the menu

From a command, listener, or another action:

```java
openMenu(player, "menus/main");
```

`MenuAPI.openAsync` populates items off-thread (placeholders, skulls) and performs the inventory
write on the correct thread automatically.

---

## Why this way

- **Layout is data.** Designers edit `main.yml` without touching code.
- **Behavior is composable.** Buttons reference actions/commands; the same action can be reused
  across menus, and cross-server or console commands use the `player:` / `console:` prefixes.
- **Thread-correct by construction.** `openAsync` handles the async→sync handoff; your action is
  marked `.sync()` because it opens an inventory.

## Common mistakes

- Forgetting `MenuAPI.initialize(this)` → `IllegalStateException`.
- Registering an action without an owner (`ActionAPI.create(id, this)`) → no clean
  `unregisterAll(this)` on disable.
- Doing blocking work in a `.sync()` action — use `.async()` and re-open on the main thread if you
  need IO first (see [PaginatedMenu.md](PaginatedMenu.md)).
