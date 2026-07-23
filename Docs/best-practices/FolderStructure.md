# Folder Structure Standard

## The canonical subsystem layout

Every subsystem is a package under `net.exylia.commons.v2` (in the library) or under your plugin's
root package (in a consuming plugin). Inside it, classes are grouped into **sub-packages by
responsibility**, not dumped in one folder.

This is the exact layout the framework uses. **Reference:** `v2/clan`, `v2/visual`, `v2/database`.

```
<subsystem>/
├── api/          Public facade(s) — XxxAPI, plus public builders exposed to consumers.
├── core/         Manager singleton + orchestration (XxxManager, XxxFactory, XxxRegistry).
├── model/        Data types, enums, immutable value objects.
├── config/       Config schema classes, config POJOs, serializers.
├── builder/      Fluent builders (if not exposed directly under api/).
├── provider/     SPI interface + integration/backend implementations.
├── adapter/      Backend implementations behind an SPI (DB adapters, reload adapters).
├── cache/        Caffeine caches / in-memory stores.
├── listener/     Bukkit listeners owned by the subsystem.
├── renderer/     Rendering/output logic (visuals, scoreboards, holograms).
├── exception/    Domain-specific exceptions.
└── util/         Subsystem-local helpers (only if genuinely subsystem-specific).
```

Not every subsystem needs every folder — include only what applies. A small subsystem may be just
`api/` + `core/` + `model/`.

---

## Real examples from the framework

`v2/clan` (integration facade with provider detection):

```
clan/
├── api/          ClanAPI, ClanStats
├── core/         ClanManager, ClanDetector
├── provider/     ClanProviderRegistry, FactionsUUIDProvider, SimpleClansProvider, NoClanProvider, ...
├── cache/        ClanCacheManager, PlayerClanCache, ClanDataCache
├── config/       ClanConfig
├── listener/     PlayerCacheListener
└── exception/    ...
```

`v2/database` (pluggable-backend persistence):

```
database/
├── api/          Database, transfer/DatabaseTransferAPI
├── core/         DatabaseManager
├── adapter/      DatabaseAdapter (SPI), SQLAdapter, MongoDBAdapter, YAMLFallbackAdapter
├── repository/   Repository, RepositoryImpl, WriteBehindRepository
├── entity/       Entity (base class)
├── annotation/   Table, Column, PlayerSession, Index
├── serialization/ SerializationRegistry, SerializerFactory
├── cache/        cache strategy
├── redis/        RedisConnectionPool, InvalidationBus
├── config/       DatabaseDefaults
└── exception/    DatabaseException, ConnectionException, ...
```

---

## Consuming-plugin structure

Mirror the same idea at the plugin level. Group by **feature**, and inside each feature use the
subsystem layout.

```
com.exylia.myplugin/
├── MyPlugin.java                 (extends ExyliaPlugin — the only thing at the root)
├── profile/                      (a feature)
│   ├── api/       ProfileService (facade) 
│   ├── core/      ProfileManager
│   ├── model/     Profile (entity), ProfileSnapshot
│   ├── config/    ProfileDefaults (@ConfigSchema)
│   └── listener/  ProfileSessionListener
├── shop/
│   ├── api/       ShopService
│   ├── core/      ShopManager
│   ├── model/     Shop, ShopItem
│   └── menu/      shop menu wiring (actions registered here)
└── command/                      (Lamp command classes)
    ├── ProfileCommand.java
    └── ShopCommand.java
```

### Resources

```
src/main/resources/
├── plugin.yml
├── config.yml                    (schema-backed; keep custom keys in separate files if strict)
├── messages.yml
├── menus/                        (menu.*.yml definitions)
│   ├── main.yml
│   └── shop.yml
├── database.yml                  (only if you use the Database subsystem)
└── data/                         (structured, non-schema config)
    └── shops.yml
```

> Menus, scoreboards, holograms, rewards, and sequences are **data**. Keep them in dedicated
> resource files/folders, not embedded in code.

---

## Rules

- **One class per file**, named after the class.
- **The plugin's main class is the only thing at the root package.** Everything else is under a
  feature package.
- **Group by feature, then by responsibility.** Avoid flat `utils`/`managers`/`models` packages
  that span unrelated features.
- **Keep `util/` small and local.** A helper used by two features belongs in a shared feature or a
  well-named `common/` package — not a dumping ground.
- **Match the framework's names** for sub-packages (`api`, `core`, `model`, `config`, `provider`,
  `adapter`, `cache`, `listener`, `renderer`, `exception`) so navigation is uniform.

## Anti-patterns

- A single package with 40 unrelated classes.
- `Utils` / `Helpers` / `Managers` packages grouped by *type* instead of *feature*.
- Menu/reward/scoreboard definitions hardcoded in Java instead of YAML resources.
- Business logic in the plugin main class.
