# Example: Database

Goal: define an entity, register it, and use the repository with async CRUD and ordering — using
`@PlayerSession` for per-player data (auto-flush on quit; **you** load on join).

Related: [Database](../Database.md), [TaskAPI](../TaskAPI.md), [Listeners](Listeners.md).

`Database` is opt-in — configure it via `database.yml` and call `Database.initialize(this)` in
`onExyliaEnable`.

---

## 1. Define an entity

Entities `extend Entity`, are annotated with `@Table`, map fields with `@Column`, and override
`getId()`. Complex types use `@Column(autoSerialize = true)`. Mark per-player entities with
`@PlayerSession`.

```java
@Getter
@Setter
@Table(name = "player_profiles", version = "1.0")
@PlayerSession                         // auto-flush on quit (no auto-load — see the join listener)
public class Profile extends Entity {

    // Use name = "id" when the YAML database adapter must be supported.
    @Column(name = "id", primaryKey = true, length = 36)
    private String uuid;

    @Column(length = 16)
    private String name;

    @Column
    private double balance;

    @Column
    private int kills;

    @Column(autoSerialize = true)       // complex type → serialized via the registry
    private Location lastLocation;

    public Profile() {}                 // required no-args constructor

    public static Profile createDefault(UUID uuid, String name) {
        Profile p = new Profile();
        p.uuid = uuid.toString();
        p.name = name;
        p.balance = Settings.Economy.STARTING_BALANCE;
        return p;
    }

    @Override
    public Object getId() {
        return uuid;                    // primary key
    }
}
```

## 2. Initialize and register

```java
@Override
protected void onExyliaEnable() {
    Database.initialize(this);          // reads database.yml (type: h2/mysql/mongodb/yaml)
    Database.registerEntity(Profile.class);
}
```

## 3. Use the repository (async CRUD)

```java
Repository<Profile> profiles = Database.getRepository(Profile.class);
// With write-behind enabled (default), save() buffers and flushes periodically.

// Load off-thread, apply on the main thread:
TaskAPI.databaseThenSync(
    () -> profiles.findById(uuid.toString()),
    opt -> opt.ifPresent(p -> cache.put(uuid, p))
);

// Save (async):
profiles.saveAsync(profile);

// Ordered leaderboard (top 10 balances, descending):
profiles.findAllOrderedByAsync("balance", false, 10)
    .thenAccept(top -> TaskAPI.runSync(() -> renderLeaderboard(top)));

// Query by a field (pass a String for UUID-valued fields):
profiles.findByAsync("name", "Notch")
    .thenAccept(opt -> { /* ... */ });
```

## 4. Load on join, flush on quit

`@PlayerSession` auto-flushes on quit but does **not** auto-load. Load on join in a listener (see
[Listeners.md](Listeners.md)):

```java
@EventHandler
public void onJoin(PlayerJoinEvent e) {
    UUID uuid = e.getPlayer().getUniqueId();
    TaskAPI.databaseThenSync(
    () -> {
        Optional<Profile> existing = profiles.findById(uuid.toString());
        if (existing.isPresent()) return existing.get();
        Profile created = Profile.createDefault(uuid, e.getPlayer().getName());
        profiles.save(created); // runs on the DATABASE pool; write-behind may buffer it
        return created;
    },
        profile -> cache.put(uuid, profile)
    );
}

@EventHandler
public void onQuit(PlayerQuitEvent e) {
    cache.remove(e.getPlayer().getUniqueId());   // framework flushes the DB row automatically
}
```

---

## Why this way

- **Annotation-driven entities** with `autoSerialize` for complex Bukkit types (registered
  serializers cover `Location`, `ItemStack`, `Component`, etc.).
- **All CRUD async** on the `DATABASE` pool, applied back on the main thread — never blocks the
  server.
- **Write-behind** (default on) makes saves instant; **`@PlayerSession`** ties the flush to quit.
- **Load-on-join / clear-on-quit** session model (no TTL for session data).

## Common mistakes

- Passing a config object to `Database.initialize` — it takes only `Plugin` and reads
  `database.yml`.
- Expecting `@PlayerSession` to load on join — it only flushes on quit; load it yourself.
- Blocking the main thread with sync repository methods — use the `...Async` variants.
- Passing a `UUID` (not a `String`) when querying a UUID-valued field on SQL backends.
- Expecting transactions/rollback — there are none; batch writes are not atomic.
