# Placeholders

## Overview

The placeholder subsystem lets plugins register named placeholders and resolve placeholder-bearing
text, with optional two-way bridging to **PlaceholderAPI** (PAPI). It is used by every text-facing
subsystem (menus, scoreboards, holograms, visuals, commands, rewards).

**Key classes**

| Class | File | Role |
|-------|------|------|
| `Placeholders` | `v2/placeholders/api/Placeholders.java` | Static facade |
| `PlaceholderRegistry` | `v2/placeholders/...` | Singleton registry |
| `PlaceholderContext` | `v2/placeholders/...` | Per-resolution context |
| `@Placeholder` | `v2/placeholders/...` | Annotation for declarative placeholders |

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

Annotate methods/classes with `@Placeholder` and register the class:

```java
Placeholders.registerAnnotatedClasses(new MyPlaceholders());
```

Annotated placeholders can declare a cache TTL (`cacheTtlMs`), an `async` flag, and support
argument placeholders (e.g. `%myplugin_top_1%`) via argument matching (`_*` / `hasArgument`)
instead of registering many discrete names.

### Programmatic

Register resolvers directly via the registry for dynamic cases.

## Resolving Text

```java
String out = Placeholders.process(player, "Hello {primary}%player_name%");
```

Resolution combines locally registered placeholders, PAPI (if present), and color presets. All
text-facing subsystems call this internally when you provide a player/context.

## Is PlaceholderAPI Required?

No. PAPI is a **`compileOnly`** dependency. Locally registered placeholders resolve without it.
PAPI-provided placeholders (`%vault_*%`, etc.) resolve only when PAPI is installed. Use
`ExyliaPlugin.isPlaceholderAPIEnabled()` to check.

## Threading Considerations

- Keep resolvers **fast**; if a resolver must do IO, mark it `async`.
- Set sensible `cacheTtlMs` to avoid recomputing expensive placeholders every render.
- `PlaceholderContext` is copied per-tick internally by consumers like [Visuals](Visuals.md).

## Best Practices

- Prefer **`@Placeholder`**-annotated classes registered via `registerAnnotatedClasses`.
- Use **argument placeholders** (`_*`) instead of registering many near-identical names.
- Cache expensive placeholders with `cacheTtlMs`.
- Do not assume PAPI is present; register your own placeholders locally.

## Common Mistakes

- Using `PlaceholderRegistry.getInstance()` before init → `IllegalStateException` (rare; bootstrap
  initializes it).
- Doing blocking work in a synchronous resolver → stalls renders. Mark it `async`.
- Assuming `%papi_*%` placeholders resolve without PlaceholderAPI installed.

## Relationship With Other Systems

- Consumed by [Menus](Menus.md), [Items](Items.md), [Scoreboards](Scoreboards.md),
  [Holograms](Holograms.md), [Visuals](Visuals.md), [Commands](Commands.md), [Rewards](Rewards.md),
  and [Actions](Actions.md).
- Works alongside [ColorAPI](Formatting.md) color presets.
- Reloaded via the [Reload](Reload.md) system (`PlaceholderAdapter`).
