# Code Organization Standard

This standard is about **how responsibilities are split within a subsystem** — the "why" behind the
[folder structure](FolderStructure.md). Each class should have one clear job.

## Separation of concerns

| Concern | Where it lives | Reference |
|---------|----------------|-----------|
| Public contract | `api/XxxAPI` | `ClanAPI`, `MenuAPI`, `Database` |
| State + lifecycle | `core/XxxManager` | `VisualManager`, `DatabaseManager` |
| Object construction | `builder/` or `api/XxxBuilder` | `RegionBuilder`, `TitleConfig.Builder` |
| Detection / dispatch | `core/XxxDetector`, `core/XxxFactory` | `ClanDetector`, `MenuFactory` |
| Backends / integrations | `provider/` or `adapter/` | `SQLAdapter`, `FactionsUUIDProvider` |
| Rendering / output | `renderer/` | scoreboard/hologram renderers |
| Persistence | `repository/`, `entity/` | `Repository`, `Entity` |
| Data shapes | `model/` | `Clan`, `CombatData`, `MenuData` |
| Config | `config/` | `DatabaseDefaults`, `ClanConfig` |
| Events | `listener/` | `PlayerCacheListener` |
| Errors | `exception/` | `DatabaseException` |

If a class touches more than one of these, split it.

**Reference:** the reward subsystem — `RewardAPI` (facade) → `RewardManager` (lifecycle) →
`RewardConfigLoader` (parsing) → `RewardExecutor` (dispatch) → providers (per-type execution) →
`model/` (`RewardConfig`, `RewardResult`, `RewardType`). No single class parses YAML *and* executes
*and* holds state.

---

## Class responsibilities

### Facade (`XxxAPI`)
- `final`, private constructor.
- Only delegation + init validation. **No business logic, no state.**

### Manager (`XxxManager`)
- Singleton. Owns registries, caches, and lifecycle.
- Coordinates the other classes; it does not itself parse config or render.

### Factory / Detector
- Pure "given input, produce the right object/implementation" logic.
- Example: `MenuFactory.create(player, menuData)` switches on `MenuType`; `ClanDetector` picks a
  provider.

### Builder
- Accumulates parameters, validates, and produces an immutable/config object in `build()`.
- No side effects until `build()` (or an explicit terminal like `.ask()` / `.give(player)`).

### Provider / Adapter (SPI)
- Implements one interface. Knows about exactly one backend/integration.
- Guards construction against missing classes (`NoClassDefFoundError`) so an absent dependency
  never crashes the subsystem — as `ClanDetector.tryCreate(...)` and `EconomyManager.tryRegister`
  do.

### Model
- Data + trivial derived accessors. Prefer immutability (final fields, builders). Use Lombok
  (`@Getter`, `@Builder`, `@Data`) to cut boilerplate, matching the framework.

---

## Method-level standards

- **Keep methods focused and cohesive.** A method that loads, transforms, schedules, and renders
  should be four methods.
- **Return futures for async work** (`CompletableFuture<...>`); return values for sync work. Don't
  hide async behind a blocking call.
- **Push threading to the edges.** Business logic should be thread-agnostic; scheduling
  (`Tasks.db`, `Tasks.sync`, `Tasks.at`) happens in the manager/facade layer. See
  [Threading.md](Threading.md).
- **No comments unless the logic is non-obvious** (per project style). Names carry the intent.

## Consuming-plugin services

Model your own features the same way. A feature service is a mini-subsystem:

```java
// profile/api/ProfileService.java — the facade for your feature
public final class ProfileService {
    private static final ProfileService INSTANCE = new ProfileService();
    public static ProfileService get() { return INSTANCE; }

    private Repository<Profile> repository;
    private final Map<UUID, Profile> cache = new ConcurrentHashMap<>();

    public void initialize(JavaPlugin plugin) {
        Database.registerEntity(Profile.class);
        this.repository = Database.getRepository(Profile.class);
    }

    public CompletableFuture<Profile> load(UUID uuid) {        // async boundary
        return Tasks.db(() -> repository.findById(uuid).orElseGet(() -> Profile.defaultFor(uuid)))
            .thenApply(TaskResult::getValue);
    }

    public Profile cached(UUID uuid) { return cache.get(uuid); } // sync accessor

    public void shutdown() { cache.clear(); }
}
```

- The service is the **facade**; `Profile` is the **model/entity**; loading is the **async
  boundary**; the cache is **session state loaded on join, cleared on quit**.

## Anti-patterns

- A `Manager` that also parses YAML, renders items, and runs commands.
- Blocking DB/IO calls buried inside otherwise-sync business methods.
- Passing internal mutable models across the facade boundary.
- Copy-pasting integration checks instead of a provider/adapter.
