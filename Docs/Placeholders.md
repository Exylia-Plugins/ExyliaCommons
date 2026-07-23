# Placeholders

## Overview

The placeholder subsystem lets plugins register named placeholders and resolve placeholder-bearing
text, with optional two-way bridging to **PlaceholderAPI** (PAPI). It is used by every text-facing
subsystem (menus, scoreboards, holograms, visuals, commands, rewards).

**Key classes**

| Class | File | Role |
|-------|------|------|
| `Placeholders` | `v2/placeholders/api/Placeholders.java` | Static facade |
| `PlaceholderRegistry` | `v2/placeholders/core/...` | Singleton registry |
| `PlaceholderContext` | `v2/placeholders/context/PlaceholderContext.java` | Per-resolution context |
| `@Placeholder` | `v2/placeholders/annotation/...` | Annotation for declarative placeholders |
| `GlobalPlaceholderResolver` / `PlayerPlaceholderResolver` / `ContextPlaceholderResolver` / `RelationalPlaceholderResolver` | `v2/placeholders/resolver/...` | Functional resolver SPIs |

## Purpose

Dynamic text is everywhere. Centralizing placeholder registration and resolution means one system
resolves `%player_name%`, `{primary}` color presets, and custom `%myplugin_stat%` placeholders
consistently, whether or not PlaceholderAPI is installed.

## Initialization

`Placeholders.initialize(plugin)` is called **automatically** during bootstrap. If PlaceholderAPI
is present, bridging is enabled; if absent, resolution still works for locally registered
placeholders (PAPI bridging is opt-in and safe when PAPI is missing).
`PlaceholderRegistry.getInstance()` throws `IllegalStateException` if used before init.

## Registering Placeholders

### Declarative (recommended)

Annotate methods/classes with `@Placeholder` and register the instance(s):

```java
Placeholders.registerAnnotatedClass(new MyPlaceholders());        // single instance
Placeholders.registerAnnotatedClasses(new A(), new B());          // varargs
```

Annotated placeholders can declare a cache TTL, an `async` flag, and support argument placeholders
(e.g. `%myplugin_top_1%`) instead of registering many discrete names.

### Programmatic (functional resolvers)

Register resolvers directly. There are four resolver categories:

```java
Placeholders.registerGlobal("server_online", () -> String.valueOf(onlineCount()));
Placeholders.registerPlayer("myplugin_kills", player -> String.valueOf(getKills(player)));
Placeholders.registerContext("myplugin_arg", (text, ctx) -> ctx.get("arg"));
Placeholders.registerRelational("myplugin_relation", (requester, target) -> relationBetween(requester, target));
```

### PlaceholderAPI bridging

```java
Placeholders.registerPapiExpander("myplugin");            // expose your placeholders to PAPI
Placeholders.registerRelationalPapiExpander("myplugin");  // relational PAPI expander
```

## Resolving Text

The `process` signatures are `(text)`, `(text, Player)`, `(text, PlaceholderContext)`,
`(text, Player, PlaceholderContext)` — **text is the first argument** (there is no
`process(player, text)` overload). Async and relational variants exist:

```java
String out = Placeholders.process("Hello {primary}%player_name%", player);
Placeholders.processAsync("...", player).thenAccept(this::send);

String rel = Placeholders.processRelational("%myplugin_relation%", requester, target);
String ctxOnly = Placeholders.processContextOnly(text, player, context);
String papiOnly = Placeholders.processPapiOnly(text, player);
```

Helpers: `extractPlaceholders(text)`, `containsPlaceholders(text)`, `hasResolver(name)`,
`getRegisteredPlaceholders()`, `clearCache()`, `getStats()`.

Resolution combines locally registered placeholders, PAPI (if present), and color presets. All
text-facing subsystems call this internally when you provide a player/context.

## Is PlaceholderAPI Required?

No. PAPI is a **`compileOnly`** dependency. Locally registered placeholders resolve without it.
PAPI-provided placeholders (`%vault_*%`, etc.) resolve only when PAPI is installed. Use
`ExyliaPlugin.isPlaceholderAPIEnabled()` to check. Bridging your placeholders **to** PAPI is opt-in
via `registerPapiExpander(...)`.

## Threading Considerations

- Keep resolvers **fast**; if a resolver must do IO, mark it `async`.
- Set sensible `cacheTtlMs` to avoid recomputing expensive placeholders every render.
- `PlaceholderContext` is copied per-tick internally by consumers like [Visuals](Visuals.md).

## Best Practices

- Prefer **`@Placeholder`**-annotated classes registered via `registerAnnotatedClass(es)`.
- Use `registerGlobal`/`registerPlayer`/`registerContext`/`registerRelational` for dynamic
  functional resolvers.
- Cache expensive placeholders with a cache TTL.
- Do not assume PAPI is present; register your own placeholders locally, and expose them to PAPI
  with `registerPapiExpander` only when you want cross-plugin visibility.

## Common Mistakes

- Calling `process(player, text)` — the signature is `process(text, player)` (text first).
- Using `PlaceholderRegistry.getInstance()` before init → `IllegalStateException` (rare; bootstrap
  initializes it).
- Doing blocking work in a synchronous resolver → stalls renders. Use the `async` flag / async
  resolvers and `processAsync`.
- Assuming `%papi_*%` placeholders resolve without PlaceholderAPI installed.

## Relationship With Other Systems

- Consumed by [Menus](Menus.md), [Items](Items.md), [Scoreboards](Scoreboards.md),
  [Holograms](Holograms.md), [Visuals](Visuals.md), [Commands](Commands.md), [Rewards](Rewards.md),
  and [Actions](Actions.md).
- Works alongside [ColorAPI](Formatting.md) color presets.
- Reloaded via the [Reload](Reload.md) system (`PlaceholderAdapter`, registered as
  `"PlaceholderSystem"`).
- Reloaded via the [Reload](Reload.md) system (`PlaceholderAdapter`).
