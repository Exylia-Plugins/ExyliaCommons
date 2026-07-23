# Example: Listeners

Goal: register Bukkit listeners the standard way, and inside them use ExyliaCommons systems
correctly — offloading IO with [TaskAPI](../TaskAPI.md) and loading `@PlayerSession` data on join
(which the framework does **not** auto-load).

Related: [TaskAPI](../TaskAPI.md), [Database](../Database.md), [Visuals](../Visuals.md).

ExyliaCommons does not replace Bukkit's event system — register listeners normally via
`getServer().getPluginManager().registerEvents(...)`. The value is in how you use the framework
from within them.

---

## Registering listeners

```java
public final class MyPlugin extends ExyliaPlugin {

    @Override
    protected void onExyliaEnable() {
        Database.initialize(this);
        Database.registerEntity(Profile.class);

        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new WelcomeListener(), this);
    }
}
```

## Load `@PlayerSession` data on join (flush is automatic on quit)

`@PlayerSession` entities auto-**flush on quit**, but there is **no auto-load on join** — do it
yourself. Load off-thread, apply on the main thread.

```java
public final class PlayerSessionListener implements Listener {

    private final MyPlugin plugin;
    private final Repository<Profile> profiles;

    public PlayerSessionListener(MyPlugin plugin) {
        this.plugin = plugin;
        this.profiles = Database.getRepository(Profile.class);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Load off the main thread (DATABASE pool), then apply on the main thread.
        TaskAPI.databaseThenSync(
            () -> profiles.findById(uuid).orElseGet(() -> Profile.createDefault(uuid, player.getName())),
            profile -> {
                plugin.getProfileCache().put(uuid, profile);   // load-on-join into your session cache
                MessageAPI.send(player, "{primary}Welcome back, %player_name%!");
            }
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // The framework's @PlayerSession quit listener flushes DB state automatically.
        // Clear your own in-memory session cache here.
        plugin.getProfileCache().remove(event.getPlayer().getUniqueId());
    }
}
```

## Doing IO safely inside an event

Never block the event thread with IO. Offload and bridge back.

```java
public final class WelcomeListener implements Listener {

    @EventHandler
    public void onFirstJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) return;
        Player player = event.getPlayer();

        TaskAPI.io(() -> WebhookClient.postJoin(player.getName()))  // network IO off-thread
            .thenRun(() -> TaskAPI.runSync(() ->
                Bukkit.broadcastMessage("Welcome " + player.getName() + "!")));
    }
}
```

---

## Why this way

- **Standard Bukkit registration** — ExyliaCommons doesn't hide the event system.
- **Load-on-join is explicit** for `@PlayerSession`; the framework only handles the quit flush.
- **IO is always off-thread** (`TaskAPI.io/database`) with results applied via `runSync`.
- **Session caches are cleared on quit** (load on join, clear on quit — avoid TTL for session
  data), matching the project's data conventions.

## Common mistakes

- Expecting `@PlayerSession` to auto-load on join — it does not; load it yourself.
- Blocking the event thread with DB/network calls — offload with `TaskAPI`.
- Applying Bukkit changes from an async callback — bridge back with `runSync` /
  `databaseThenSync`.
- Using TTL invalidation for per-session data — load on join, clear on quit instead.
