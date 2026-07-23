# Anti-Pattern: Deprecated & Unimplemented APIs

Do not call these. They are either explicitly `@Deprecated`, stubbed, or throw at runtime. Each
entry names the preferred replacement.

---

## Explicitly `@Deprecated`

### `Configs.debug()`

`v2/config/Configs.java` — `@Deprecated`.

```java
@Deprecated
public static boolean debug() {
    if (mainConfig == null) return false;
    return DebugDefaults.Debug.LEVEL > 0;
}
```

**Why:** it only reports whether the debug level is `> 0`, conflating the multi-level debug model
into a boolean.

**Preferred:** use the [Debug](../Debug.md) system directly — read `DebugDefaults.Debug.LEVEL`, or
just log via `DebugAPI.logPluginDebug(...)` / `logLibDebug(...)`, which are already level-gated.

### `NBTManager.getNBTValue(ItemStack, String)`

`v2/items/utils/NBTManager.java` — `@Deprecated`, delegates to `getString(...)`.

**Why:** superseded by the typed accessor.

**Preferred:** use `NBTManager.getString(itemStack, key)` (and the other typed getters). Better,
declare NBT declaratively in the [item YAML](../Items.md) (`nbt:` section) and build via
`ItemsAPI` rather than reading/writing NBT by hand.

### `SystemDetector.detectAll()` (no-arg)

`v2/reload/core/SystemDetector.java` — `@Deprecated`.

**Why:** the no-arg overload can't see the registered systems. The reload engine itself calls the
current `detectAll(Map<String, ReloadableSystem> systems)` overload.

**Preferred:** you should not call `SystemDetector` directly at all — use
`ReloadAPI.getInstance().getAvailability()` / `getAvailableSystems()`. See [../Reload.md](../Reload.md).

---

## Stubbed — always fail (do NOT rely on them)

### `EconomyManager.transfer(...)` / `EconomyAPI.transfer(...)`

`v2/economy/core/EconomyManager.java`:

```java
// TODO(human): Implement transfer logic with validation, withdraw, deposit, and rollback on failure
private TransferResult transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal amount, EconomyProvider provider) {
    return TransferResult.failure("Not implemented");
}
```

**Every** public `transfer(...)` overload funnels into this stub and returns
`TransferResult.failure("Not implemented")`.

**Preferred:** perform the transfer explicitly and roll back on failure:

```java
EconomyResponse w = EconomyAPI.withdraw(from, amount);
if (!w.isSuccess()) return; // insufficient funds / not available
EconomyResponse d = EconomyAPI.deposit(to, amount);
if (!d.isSuccess()) {
    EconomyAPI.deposit(from, amount); // rollback
}
```

### `DeluxeCombatProvider.canAttack(...)`

`v2/combat/provider/DeluxeCombatProvider.java` — the body is a `TODO` that unconditionally returns
`true`:

```java
@Override
public boolean canAttack(Player attacker, Player defender) {
    if (!enabled) return true;
    // TODO(human): Implement canAttack logic combining DeluxeCombat checks
    return true;
}
```

**Why:** `CombatAPI.canAttack(...)` under the DeluxeCombat provider currently always returns `true`
— it does **not** actually consult DeluxeCombat's PvP rules.

**Preferred:** don't treat `CombatAPI.canAttack(...)` as authoritative PvP gating under
DeluxeCombat yet. If you need reliable PvP checks, verify against the underlying plugin directly or
guard your own rules until this is implemented.

---

## Throws at runtime — unsupported operations

### `YAMLFallbackAdapter.executeQuery(...)` / `MongoDBAdapter.executeQuery(...)`

Both throw `UnsupportedOperationException` (`"...Query execution not supported..."`).

**Why:** raw query execution is not supported on the YAML fallback or MongoDB adapters.

**Preferred:** use the [`Repository`](../Database.md) methods (`findBy`, `findAllBy`,
`findAllOrderedBy`, `findAllPagedOrderedBy`, ...). Never write raw queries against these backends.

### `YAMLFallbackAdapter` sorting

`findAllSorted` contains `// TODO: Implement sorting` and returns **unsorted** results;
`findAllSortedPaged` can throw `IndexOutOfBoundsException` on an out-of-range page skip.

**Preferred:** don't use the YAML adapter for ordered/paged data — it's a dev/fallback store. Use
H2 or MySQL for anything that needs sorting/paging in production.

---

## General rule

If a method is `@Deprecated`, returns `"Not implemented"`, contains a `TODO(human)`, or throws
`UnsupportedOperationException`, treat it as **not part of the supported API**. Prefer the named
replacement, and never build a feature on a stub.
