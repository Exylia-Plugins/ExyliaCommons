# Formatting

## Overview

The formatting subsystem covers two related concerns:

1. **Value formatting** (`FormatterAPI`, `AsyncFormatterAPI`) — formatting numbers, currency,
   percentages, durations, and other values consistently across all Exylia plugins.
2. **Text/color formatting** (`ColorAPI`, color presets, gradients) — turning `&`/MiniMessage
   markup and named color presets into Adventure `Component`s.

**Key classes**

| Class | File | Role |
|-------|------|------|
| `FormatterAPI` | `v2/formatter/api/FormatterAPI.java` | Synchronous value formatting |
| `AsyncFormatterAPI` | `v2/formatter/api/AsyncFormatterAPI.java` | Async value formatting |
| `FormattersDefaults` | `v2/formatter/FormattersDefaults.java` | Schema on `config.yml` |
| `ColorAPI` | `v2/visual/api/ColorAPI.java` | Color/gradient/preset formatting |
| `ColorPresetManager` | `v2/visual/...` | Loads `colors.yml` presets |

## Purpose

Consistent presentation. Rather than each plugin re-implementing "format 1234567 as 1.2M" or
hand-writing gradient hex sequences, ExyliaCommons centralizes both, driven by configuration so
server owners can restyle output globally.

## Value Formatting (`FormatterAPI`)

`FormatterAPI` produces human-readable strings for numbers, currency, percentages, and durations
according to formats declared in `FormattersDefaults` (bound to `config.yml`, strict). Because it
reads from the schema, formats are configurable and reloadable.

`AsyncFormatterAPI` provides the same operations off the main thread for use inside async
pipelines (e.g. formatting large batches for a leaderboard build).

> `FormattersDefaults` is initialized during bootstrap
> (`ConfigSchemaRegistry.ensureDefaults(FormattersDefaults.class)`), and the formatter registry
> initializes lazily on first use.

### Usage pattern

`FormatterAPI` methods take a flexible `Object input` and return a formatted `String`. Real
methods (verbatim) include:

```java
// Prices / percentages
FormatterAPI.formatPrice(Object input);
FormatterAPI.formatPriceCompact(Object input);
FormatterAPI.formatPriceNoSymbol(Object input);
FormatterAPI.formatPriceWithSymbol(Object input, String symbol);
FormatterAPI.formatPercent(Object input);
FormatterAPI.formatPercentWithDecimals(Object input, int decimals);

// Durations / time
FormatterAPI.formatTime(Object input);
FormatterAPI.formatTimeClock(Object input [, ClockFormat format]);
FormatterAPI.formatTimeCompact(Object input);
FormatterAPI.formatTimeVerbal(Object input);
FormatterAPI.formatTimeWithPrecision(Object input, int precision);

// Dates
FormatterAPI.formatDate(Object input [, String pattern]);
FormatterAPI.formatDateISO(Object input);
FormatterAPI.formatDateRelative(Object input);
```

There are additional date/time helpers (relative-from, differences, `isDatePast/Future/Today`,
`getTimeComponents`, etc.). All price/percent formatting is driven by the `FormattersDefaults`
config so behavior is consistent and reconfigurable. `AsyncFormatterAPI` mirrors these for use in
async pipelines.

## Color & Gradient Formatting (`ColorAPI`)

`ColorAPI` (initialized in bootstrap via `ColorAPI.initialize(plugin)`) converts markup and named
presets into Adventure `Component`s. It supports:

- Legacy `&` color codes.
- MiniMessage markup.
- **Named color presets** from `colors.yml` referenced as `{presetName}`.
- Gradients, centering, and fonts.

### Color presets (`colors.yml`)

`colors.yml` is auto-created with a standard palette so all plugins share a theme. Default presets
include: `primary`, `secondary`, `secondary_light`, `letters`, `letters_black`, `error`,
`success`, `success_light`, `warning`, `warning_light`, `info`, `info_light`, `accent`, `neutral`,
`highlight`, `muted`, and gradient presets `gradient_primary`, `gradient_success`,
`gradient_warning`, `gradient_error`.

Reference a preset in any Exylia-formatted text:

```yaml
name: "{primary}Welcome {highlight}%player_name%"
```

### Reloading presets

`ColorAPI.reloadPresets()` reloads `colors.yml`; `ColorAPI.clearCache()` clears the formatted-text
cache. Both are wired into the [Reload](Reload.md) system, so a normal reload refreshes them.

## Threading Considerations

- `FormatterAPI` is synchronous and cheap; `AsyncFormatterAPI` is for use inside async pipelines.
- `ColorAPI` caches parsed components (Caffeine-backed) so repeated formatting of identical text
  is fast and thread-safe.

## Configuration

- `config.yml` — formatter formats (owned by the strict `FormattersDefaults` schema).
- `colors.yml` — named color/gradient presets.

## Best Practices

- **Use presets for theming.** Reference `{primary}`, `{success}`, etc. instead of hardcoding hex
  so the whole network restyles from one file.
- Use `FormatterAPI` for all numeric/duration display so formatting is consistent and
  reconfigurable.
- Use `AsyncFormatterAPI` only inside async work; prefer the sync API on the main thread.
- Reload presets/formats through the [Reload](Reload.md) system rather than manual calls.

## Common Mistakes

- Hardcoding gradients/hex instead of using presets — defeats central theming.
- Editing `colors.yml` or formatter config and not reloading — values are cached.
- Adding custom keys to the strict `config.yml` formatter section without a covering schema (they
  get pruned by strict finalization — see [Configuration.md](Configuration.md)).

## Relationship With Other Systems

- Backed by the [Configuration](Configuration.md) schema layer.
- `ColorAPI` is the formatting engine used by [Visuals](Visuals.md), [Menus](Menus.md),
  [Scoreboards](Scoreboards.md), and [Holograms](Holograms.md).
- Refreshed by the [Reload](Reload.md) system (`FormatterAdapter`, `ColorAdapter`,
  `ColorPresetAdapter`).
