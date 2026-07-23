# Integrations (Clans, Combat, Economy)

## Overview

ExyliaCommons abstracts several categories of third-party plugin behind uniform, cached facades so
your plugin never depends on a specific clan/combat/economy plugin. Each subsystem **auto-detects**
the installed backend and falls back to a no-op provider when none is present.

| Subsystem | Facade | Backends detected | Fallback |
|-----------|--------|-------------------|----------|
| Clans | `ClanAPI` | Factions, HuskTowns, ZelTeams, RunithClans, UClans, Kingdoms, SimpleClans, ExyliaClans | `NoClanProvider` |
| Combat | `CombatAPI` | DeluxeCombat, PvPManager | `NoCombatProvider` |
| Economy | `EconomyAPI` | Vault, PlayerPoints | `DummyProvider` |

All three are **opt-in** (call `initialize(plugin)` in `onExyliaEnable`) and throw
`IllegalStateException` if used before init.

---

## Clans (`ClanAPI`)

A provider-abstracted, cached **read** facade over clan/faction/town plugins, exposing a uniform
`Clan` model. The public contract (`ClanProvider`, `ClanProviderBridge`, `Clan`) lives in a
separate `clans-api` Gradle module so external plugins can integrate without depending on Bukkit or
the full library.

### Initialization

```java
ClanAPI.initialize(plugin); // ClanManager.initialize (synchronized, idempotent)
```

### Provider detection precedence (two-stage)

1. **Manual registry first** — `registerProvider(...)`/`registerBridge(...)`, sorted by priority
   **descending** (default 100). First `isEnabled()` wins.
2. **Auto-detect fallback** — fixed order:
   `Factions → HuskTowns → ZelTeams → RunithClans → UClans → Kingdoms → SimpleClans → ExyliaClans
   → NoClanProvider`. Each attempt swallows `NoClassDefFoundError`/`Exception` and continues.

> Clan registry priority is **descending** (highest wins) — different from economy's enum-order
> resolution.

### Key API (all static on `ClanAPI`)

```java
void initialize(JavaPlugin plugin);
void registerProvider(ClanProvider provider [, int priority]);
void registerBridge(ClanProviderBridge bridge [, int priority]);

Optional<Clan> getPlayerClan(UUID | Player);
CompletableFuture<Optional<Clan>> getPlayerClanAsync(UUID);
Optional<String> getPlayerClanName(UUID);  Optional<String> getPlayerClanTag(UUID);
Optional<Clan> getClanByTag(String);        Optional<Clan> getClanById(String);
Collection<Clan> getAllClans();
boolean hasPlayerClan(UUID);  boolean isSameClan(UUID, UUID);
boolean isLeader(UUID);       boolean isModerator(UUID);
String getActiveProviderName();  ClanProvider getActiveProvider();
void reload(); void shutdown(); void clearCache(); ClanStats getStats();
```

Async variants exist for name/tag/byTag/byId/allClans.

### Bridge integration (dependency-free)

`ClanProviderBridge` uses only JDK types + a `ClanSnapshot` record, so a third-party plugin can
expose its clans to ExyliaCommons without depending on Bukkit/Exylia classes. Register it with
`registerBridge`.

### Caching & threading

Results are cached in Caffeine (`PlayerClanCache` default TTL 5m/5000; `ClanDataCache` 10m/2000).
Sync getters query the provider then cache; async getters short-circuit on cache hit. The player
cache entry is invalidated on quit. Prefer `...Async` off the main thread for IO-backed backends.

### Reload

Reloaded via `ClanAdapter` (name `"Clan System"`, `NORMAL`), guarded by `ClanManager.isInitialized()`.

---

## Combat (`CombatAPI`)

Provider-abstracted combat-tag / PvP-state / KDR facade with a cached `CombatData` model.

### Initialization

```java
CombatAPI.initialize(plugin); // synchronized, idempotent
```

> **No default reload adapter** (unlike clans). To reload on `/reload`, register a custom
> `ReloadableSystem` that calls `CombatAPI.reload()` (see [Reload.md](Reload.md)).

### Provider detection

No manual registry. Fixed order: `DeluxeCombat → PvPManager → NoCombatProvider`. Each is created
only if the plugin is present and enabled; construction is guarded so a missing API class doesn't
crash detection.

### Key API (static on `CombatAPI`)

```java
boolean isInCombat(Player);
int getRemainingCombatTime(Player);  long getRemainingCombatTimeMillis(Player);
Optional<Player> getCurrentOpponent(Player);
void tag(Player target, Player attacker [, int seconds]);  void untag(Player);
boolean hasProtection(Player);  boolean hasPvPEnabled(Player);  void togglePvP(Player, boolean);
boolean canAttack(Player attacker, Player defender);
Optional<CombatData> getPlayerData(Player | UUID);
CompletableFuture<Optional<CombatData>> getPlayerDataAsync(Player | UUID);
Optional<Integer> getKills/getDeaths/getStreak/getHighestStreak/getPoints(Player);
Optional<Double> getKDR(Player);
void reload(); void shutdown(); void clearCache(); CombatStats getStats();
```

### Caching & threading

Only `getPlayerData*` is cached (Caffeine). **Live state** methods (`isInCombat`,
`getRemainingCombatTime`, `tag`, etc.) delegate directly to the provider and are **not cached** —
combat tag is volatile, so do not cache their results yourself. Under `NoCombatProvider`,
`tag`/`untag`/`togglePvP` are no-ops.

---

## Economy (`EconomyAPI`)

Multi-currency economy facade over Vault and PlayerPoints with a `BigDecimal` API and structured
`EconomyResponse` results.

### Initialization

```java
EconomyAPI.initialize(plugin); // registers providers, resolves default
```

### Provider detection

Providers are registered only if available (`isAvailable()`, guarded against
`NoClassDefFoundError`). The default is resolved in `CurrencyType` enum order: **Vault →
PlayerPoints → Dummy**. Per-currency lookup falls back to the default. `VaultProvider` pulls
`Economy` from Bukkit's `ServicesManager`; `PlayerPointsProvider` uses the PlayerPoints API.

### Key API (static on `EconomyAPI`)

```java
boolean isAvailable([CurrencyType]);
BigDecimal getBalance(OfflinePlayer | UUID [, CurrencyType]);
boolean has(OfflinePlayer, BigDecimal|double [, CurrencyType]);
EconomyResponse deposit(OfflinePlayer, BigDecimal|double [, CurrencyType]);
EconomyResponse withdraw(OfflinePlayer, BigDecimal|double [, CurrencyType]);
EconomyResponse set(OfflinePlayer, BigDecimal|double [, CurrencyType]);
boolean charge(OfflinePlayer, BigDecimal);  // withdraw().isSuccess()
boolean pay(OfflinePlayer, BigDecimal);     // deposit().isSuccess()
TransferResult transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal|double [, CurrencyType]);
String format(BigDecimal|double [, CurrencyType]);
EconomyProvider getProvider([CurrencyType]);
void reload(); void shutdown();
```

`EconomyResponse.ResponseType`: `SUCCESS, FAILURE, INSUFFICIENT_FUNDS, ACCOUNT_NOT_FOUND,
INVALID_AMOUNT, NOT_AVAILABLE`.

### ⚠️ `transfer` is a stub

`EconomyManager.transfer(...)` is **not implemented** — all public `transfer` overloads currently
return `TransferResult.failure("Not implemented")`. **Do not rely on transfers.** Instead do an
explicit `withdraw` from the sender, then `deposit` to the receiver, and roll back the withdraw if
the deposit fails.

### Threading

Operations are synchronous delegations to the underlying economy plugin (no dedicated executor).
If the backend does IO, offload to [TaskAPI](TaskAPI.md) yourself. Not in the default reload set.

---

## Best Practices (all three)

- Always `initialize` in `onExyliaEnable`; check `isInitialized()`/`isAvailable()` before relying
  on behavior.
- Prefer **async** variants (clans/combat data) off the main thread for IO-backed backends.
- Do not cache combat live-state; do not rely on economy `transfer`.
- Use clan **bridges** for third-party plugins that shouldn't depend on the full API.
- Register your own reload adapter for combat/economy if you need reload support.

## Common Mistakes

- Using any facade before `initialize` → `IllegalStateException`.
- Confusing clan registry priority (descending) with economy enum-order resolution.
- Relying on `EconomyAPI.transfer` — it always fails.
- Caching `CombatAPI.isInCombat` — it is volatile/live.
- Passing non-positive amounts to deposit/withdraw → `EconomyResponse.invalidAmount()`.

## Relationship With Other Systems

- All use [Debug](Debug.md) and Caffeine caching.
- Clans are reloaded by the [Reload](Reload.md) system; combat/economy are not by default.
- These are independent of each other.
