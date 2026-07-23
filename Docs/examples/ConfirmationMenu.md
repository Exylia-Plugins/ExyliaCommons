# Example: Confirmation Menu

Goal: a **reusable** confirm/deny dialog. Because menu buttons trigger stateless actions, the
production pattern is: a YAML dialog whose confirm/deny buttons call two registered actions, and a
small per-player callback registry that the actions dispatch to. This lets any part of your plugin
request a confirmation without duplicating menu code.

Related: [Menus](../Menus.md), [Actions](../Actions.md).

---

## 1. The dialog YAML

`menus/confirm.yml`:

```yaml
title: "&8Are you sure?"
type: SIMPLE
size: 27
items:
  prompt:
    slot: 13
    material: PAPER
    name: "&e%confirm_title%"
    lore:
      - "&7%confirm_description%"
  confirm:
    slot: 11
    material: LIME_WOOL
    name: "&aConfirm"
    click_sounds:
      - "UI_BUTTON_CLICK"
    actions:
      - "myplugin:confirm_yes"
  deny:
    slot: 15
    material: RED_WOOL
    name: "&cCancel"
    click_sounds:
      - "UI_BUTTON_CLICK"
    actions:
      - "myplugin:confirm_no"
```

## 2. A reusable confirmation helper

```java
public final class ConfirmationService {

    /** Per-player pending confirmation callbacks. */
    private final Map<UUID, Runnable> onConfirm = new ConcurrentHashMap<>();
    private final Map<UUID, Runnable> onDeny    = new ConcurrentHashMap<>();

    private final JavaPlugin plugin;

    public ConfirmationService(JavaPlugin plugin) {
        this.plugin = plugin;
        registerActions();
    }

    /** Ask the player to confirm; run the matching callback on their choice. */
    public void ask(Player player, String title, String description,
                    Runnable confirm, Runnable deny) {
        onConfirm.put(player.getUniqueId(), confirm);
        onDeny.put(player.getUniqueId(), deny);

        ConfigurationSection section = Configs.get("menus/confirm").raw();
        MenuData data = MenuAPI.parse(section);

        // Inject the prompt text via keyed placeholders on the menu context.
        data.setContext(PlaceholderContext.create()
            .withPlayer(player)
            .put("confirm_title", title)
            .put("confirm_description", description));

        MenuAPI.openAsync(player, data);
    }

    private void registerActions() {
        ActionAPI.register(ActionAPI.create("confirm_yes", plugin)
            .namespace("myplugin")
            .sync()
            .handler((ctx, args) -> finish(ctx.getPlayer(), true))
            .build());

        ActionAPI.register(ActionAPI.create("confirm_no", plugin)
            .namespace("myplugin")
            .sync()
            .handler((ctx, args) -> finish(ctx.getPlayer(), false))
            .build());
    }

    private void finish(Player player, boolean confirmed) {
        UUID id = player.getUniqueId();
        Runnable confirm = onConfirm.remove(id);
        Runnable deny = onDeny.remove(id);

        MenuAPI.close(player); // closes the dialog on the main thread

        Runnable chosen = confirmed ? confirm : deny;
        if (chosen != null) chosen.run();
    }

    /** Call from plugin disable. */
    public void shutdown() {
        onConfirm.clear();
        onDeny.clear();
        ActionAPI.unregisterAll(plugin);
    }
}
```

## 3. Use it anywhere

```java
confirmationService.ask(player,
    "Delete your home?",
    "This cannot be undone.",
    () -> {                                  // on confirm
        homeManager.delete(player);
        MessageAPI.send(player, "&aHome deleted.");
    },
    () -> MessageAPI.send(player, "&7Cancelled.")   // on deny
);
```

---

## Why this way

- **One dialog, reused everywhere.** Callers pass callbacks; they never build the menu.
- **Callbacks are keyed per player** in a `ConcurrentHashMap`, cleared when the choice is made
  (avoids leaks and stale callbacks).
- **Actions are `.sync()`** because they close a menu and typically touch the Bukkit API.

## Common mistakes

- Storing callbacks in fields instead of per-player — two players confirming at once would collide.
- Forgetting to `remove(...)` the callback after dispatch — leaks and double-execution risk.
- Not clearing the maps / unregistering actions on disable.
- Running heavy work directly in the `.sync()` action — offload with `Tasks.io/db(...)` and apply
  results back on the main thread.
