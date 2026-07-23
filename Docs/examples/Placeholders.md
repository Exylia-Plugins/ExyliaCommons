# Example: Placeholders

Goal: register your own placeholders (annotated + functional), expose them to PlaceholderAPI, and
resolve placeholder-bearing text.

Related: [Placeholders](../Placeholders.md).

`Placeholders` is initialized during bootstrap. It works with or without PlaceholderAPI installed;
PAPI-provided placeholders resolve only when PAPI is present.

---

## Annotated placeholders (recommended)

Group related placeholders in a class and register the instance. Annotate methods with
`@Placeholder`. Attributes: `name` (required), `description`, `scope` (`PlaceholderScope`),
`cacheable` (default true), `cacheTtlMs` (default 1000), `async` (default false), `hasArgument`.

```java
public final class StatsPlaceholders {

    private final StatsService stats;

    public StatsPlaceholders(StatsService stats) {
        this.stats = stats;
    }

    @Placeholder(name = "myplugin_kills")
    public String kills(Player player) {
        return String.valueOf(stats.getKills(player.getUniqueId()));
    }

    // Cache an expensive computation for 5 seconds.
    @Placeholder(name = "myplugin_kdr", cacheTtlMs = 5000)
    public String kdr(Player player) {
        return FormatterAPI.formatPercent(stats.getKdr(player.getUniqueId()));
    }
}
```

```java
@Override
protected void onExyliaEnable() {
    Placeholders.registerAnnotatedClass(new StatsPlaceholders(statsService));
    // Expose your placeholders to PlaceholderAPI (opt-in; safe if PAPI absent):
    Placeholders.registerPapiExpander("myplugin");
}
```

## Functional resolvers

For dynamic cases, register resolvers directly. There are global (no player), player, context, and
relational variants.

```java
// Global — no player needed
Placeholders.registerGlobal("server_online", () -> String.valueOf(Bukkit.getOnlinePlayers().size()));

// Player-scoped
Placeholders.registerPlayer("myplugin_rank", player -> rankService.getRank(player).getName());

// Relational (two players)
Placeholders.registerRelational("myplugin_relation",
    (requester, target) -> clanService.relationBetween(requester, target).name());
```

## Resolving text

`process` takes **text first**, then an optional player/context.

```java
String line = Placeholders.process("{primary}Kills: %myplugin_kills%", player);

// With extra keyed context values (%amount% below):
PlaceholderContext ctx = PlaceholderContext.create()
    .withPlayer(player)
    .put("amount", 250);
String msg = Placeholders.process("You earned %amount% coins", player, ctx);

// Relational
String rel = Placeholders.processRelational("%myplugin_relation%", requester, target);
```

## Checking availability

```java
if (ExyliaPlugin.isPlaceholderAPIEnabled()) {
    // %vault_*% and other PAPI placeholders will resolve
}
```

---

## Why this way

- **Annotated classes** keep related placeholders together and are the recommended default.
- **Functional resolvers** cover dynamic or global cases without a class.
- **PAPI is optional** — your placeholders always resolve locally; `registerPapiExpander` only adds
  cross-plugin visibility when PAPI is present.
- **Keyed context (`put`)** lets you inject per-call values (`%amount%`) without registering a
  global placeholder.

## Common mistakes

- Calling `process(player, text)` — the signature is `process(text, player)` (text first).
- Doing blocking work in a synchronous resolver — it stalls every render. Keep resolvers fast (or
  use the async support / `processAsync`).
- Assuming `%vault_*%`/other PAPI placeholders resolve without PlaceholderAPI installed.
