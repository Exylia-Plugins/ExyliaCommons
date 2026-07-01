# ExyliaCommons — Clan Provider Integration Guide

Integrate any clan/team/faction plugin with ExyliaCommons so all Exylia plugins can query
clan data through a unified API regardless of which clan plugin the server uses.

---

## Overview

ExyliaCommons ships with built-in support for the following clan plugins:

| Plugin | Plugin name (server) |
|--------|----------------------|
| FactionsUUID | `Factions` |
| HuskTowns | `HuskTowns` |
| ZelTeams | `ZelTeams` |
| RunithClans | `RunithClans` |
| UltimateClans | `UltimateClans` |
| KingdomsX | `Kingdoms` |
| SimpleClans | `SimpleClans` |

If your plugin is not on that list, use the integration API described below.

---

## Setup

### 1. Add the dependency

Download `exylia-clans-api-1.0.0.jar` and place it in your `libs/` folder.

**Gradle:**
```groovy
dependencies {
    compileOnly files('libs/exylia-clans-api-1.0.0.jar')
}
```

**Maven:**
```xml
<dependency>
    <groupId>net.exylia</groupId>
    <artifactId>exylia-clans-api</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
    <systemPath>${project.basedir}/libs/exylia-clans-api-1.0.0.jar</systemPath>
</dependency>
```

> **Do not shade** this jar into your plugin. ExyliaCommons already provides these classes
> at runtime. Shading them would cause classloading conflicts.

### 2. Declare the soft dependency

```yaml
# plugin.yml
softdepend: [ExyliaCommons]
```

---

## Integration paths

| Path | What you implement | Complexity |
|------|--------------------|------------|
| **Bridge** | `ClanProviderBridge` — pure Java, no Bukkit types | Low |
| **Full API** | `AbstractClanProvider` — direct access to the `Clan` model | Medium |

Both paths use `ClanIntegration` as the single entry point. It calls ExyliaCommons via
reflection, so your plugin will not crash even if ExyliaCommons is absent.

---

## Path 1 — Bridge API (recommended)

Implement `ClanProviderBridge`. It uses only `java.util.*` types — no Bukkit or Exylia
imports required in your implementation. Return `null` from any lookup method to indicate
"not found".

### Implement

```java
import net.exylia.commons.v2.clan.provider.ClanProviderBridge;
import net.exylia.commons.v2.clan.provider.ClanProviderBridge.ClanSnapshot;

public class MyClanBridge implements ClanProviderBridge {

    private final MyClanPlugin plugin;

    public MyClanBridge(MyClanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getProviderName() {
        return "MyClanPlugin";
    }

    @Override
    public ClanSnapshot getPlayerClan(UUID playerId) {
        MyClan clan = plugin.getClanManager().getClanByPlayer(playerId);
        if (clan == null) return null;

        // Minimal factory method — leaders + members only
        return ClanSnapshot.of(
                clan.getId(),
                clan.getName(),
                clan.getTag(),
                clan.getLeaders(),   // Set<UUID>
                clan.getMembers()    // Set<UUID>
        );
    }

    @Override
    public ClanSnapshot getClanByTag(String tag) {
        MyClan clan = plugin.getClanManager().getByTag(tag);
        if (clan == null) return null;

        // Full constructor — use fields your plugin supports; leave others as 0/""/false
        return new ClanSnapshot(
                clan.getId(),
                clan.getName(),
                clan.getTag(),
                clan.getDisplayName(),
                clan.getLeaders(),
                clan.getModerators(),
                clan.getMembers(),
                clan.getOnlineMembers(),
                clan.getLevel(),
                clan.getBalance(),
                clan.getCreatedAt(),
                clan.isVerified(),
                clan.getDescription(),
                clan.getMaxMembers(),
                clan.getKillDeathRatio()
        );
    }

    @Override
    public ClanSnapshot getClanById(String id) {
        MyClan clan = plugin.getClanManager().getById(id);
        if (clan == null) return null;
        return ClanSnapshot.of(clan.getId(), clan.getName(), clan.getTag(),
                clan.getLeaders(), clan.getMembers());
    }

    @Override
    public Collection<ClanSnapshot> getAllClans() {
        return plugin.getClanManager().getAllClans().stream()
                .map(clan -> ClanSnapshot.of(
                        clan.getId(), clan.getName(), clan.getTag(),
                        clan.getLeaders(), clan.getMembers()))
                .toList();
    }
}
```

### Register

```java
import net.exylia.commons.v2.clan.integration.ClanIntegration;

@Override
public void onEnable() {
    if (ClanIntegration.isAvailable()) {
        ClanIntegration.register(new MyClanBridge(this));
        getLogger().info("Registered clan bridge with ExyliaCommons.");
    }
}

@Override
public void onDisable() {
    ClanIntegration.unregister("MyClanPlugin");
}
```

---

## Path 2 — Full API (extend AbstractClanProvider)

Use this when you need full control: custom async behaviour, manual cache invalidation,
or direct access to the `Clan` builder.

### Implement

```java
import net.exylia.commons.v2.clan.model.Clan;
import net.exylia.commons.v2.clan.provider.AbstractClanProvider;

public class MyClanProvider extends AbstractClanProvider {

    private final MyClanPlugin plugin;

    public MyClanProvider(MyClanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getProviderName() {
        return "MyClanPlugin";
    }

    @Override
    public Optional<Clan> getPlayerClan(UUID playerId) {
        MyClan clan = plugin.getClanManager().getClanByPlayer(playerId);
        if (clan == null) return Optional.empty();
        return Optional.of(buildClan(clan));
    }

    @Override
    public Optional<Clan> getClanByTag(String tag) {
        return Optional.ofNullable(plugin.getClanManager().getByTag(tag))
                .map(this::buildClan);
    }

    @Override
    public Optional<Clan> getClanById(String id) {
        return Optional.ofNullable(plugin.getClanManager().getById(id))
                .map(this::buildClan);
    }

    @Override
    public Collection<Clan> getAllClans() {
        return plugin.getClanManager().getAllClans().stream()
                .map(this::buildClan)
                .toList();
    }

    private Clan buildClan(MyClan clan) {
        Set<UUID> all = new HashSet<>();
        all.addAll(clan.getLeaders());
        all.addAll(clan.getMembers());

        Clan.ClanBuilder builder = Clan.builder()
                .id(clan.getId())
                .name(clan.getName())
                .tag(clan.getTag())
                .displayName(clan.getDisplayName())
                .level(clan.getLevel())
                .balance(clan.getBalance())
                .createdAt(clan.getCreatedAt())
                .verified(clan.isVerified())
                .description(clan.getDescription())
                .maxMembers(clan.getMaxSize())
                .providerName(getProviderName());

        clan.getLeaders().forEach(builder::leader);
        clan.getModerators().forEach(builder::moderator);
        clan.getMembers().forEach(builder::member);
        all.forEach(builder::allMember);

        return builder.build();
    }
}
```

### Register

```java
import net.exylia.commons.v2.clan.integration.ClanIntegration;

@Override
public void onEnable() {
    if (ClanIntegration.isAvailable()) {
        ClanIntegration.registerProvider(new MyClanProvider(this));
    }
}

@Override
public void onDisable() {
    ClanIntegration.unregister("MyClanPlugin");
}
```

---

## Priority

When multiple providers are registered, the one with the highest priority is used.

| Priority value | When to use |
|----------------|-------------|
| `100` | Default — overrides all built-in providers |
| `200+` | Force your provider above any other registered provider |
| `< 100` | Register as fallback only |

```java
// Override everything
ClanIntegration.register(new MyClanBridge(this), 200);

// Register as fallback (used only if no other external provider is registered)
ClanIntegration.register(new MyClanBridge(this), 50);
```

---

## ClanIntegration reference

```java
// Check if ExyliaCommons is present on the server
boolean available = ClanIntegration.isAvailable();

// Register a bridge (Path 1)
ClanIntegration.register(ClanProviderBridge bridge);
ClanIntegration.register(ClanProviderBridge bridge, int priority);

// Register a full provider (Path 2)
ClanIntegration.registerProvider(ClanProvider provider);
ClanIntegration.registerProvider(ClanProvider provider, int priority);

// Unregister — call in onDisable()
ClanIntegration.unregister(String providerName);
```

All methods return `boolean` — `true` if ExyliaCommons was found and the call succeeded.

---

## ClanSnapshot fields reference

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `String` | Yes | Unique clan identifier |
| `name` | `String` | Yes | Full display name |
| `tag` | `String` | Yes | Short tag / prefix |
| `displayName` | `String` | No | Formatted name — defaults to `name` if `null` |
| `leaders` | `Set<UUID>` | Yes | Leader UUIDs |
| `moderators` | `Set<UUID>` | No | Moderator UUIDs |
| `members` | `Set<UUID>` | Yes | Regular member UUIDs |
| `onlineMembers` | `Set<UUID>` | No | Currently online UUIDs |
| `level` | `int` | No | Clan level (`0` if not supported) |
| `balance` | `double` | No | Clan bank balance (`0` if not supported) |
| `createdAt` | `long` | No | Creation timestamp in millis (`0` if not supported) |
| `verified` | `boolean` | No | Whether the clan is verified |
| `description` | `String` | No | Clan description |
| `maxMembers` | `int` | No | Max allowed members (`0` if not supported) |
| `killDeathRatio` | `double` | No | K/D ratio (`0` if not supported) |

Use `ClanSnapshot.of(id, name, tag, leaders, members)` or
`ClanSnapshot.of(id, name, tag, leaders, moderators, members)` for the common cases.

---

## Quick checklist

- [ ] Add `exylia-clans-api-1.0.0.jar` as `compileOnly` — do **not** shade it
- [ ] Add `softdepend: [ExyliaCommons]` to `plugin.yml`
- [ ] Implement `ClanProviderBridge` (Bridge path) or `AbstractClanProvider` (Full API path)
- [ ] Call `ClanIntegration.register(...)` in `onEnable()` guarded by `isAvailable()`
- [ ] Call `ClanIntegration.unregister(providerName)` in `onDisable()`
