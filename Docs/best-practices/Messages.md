# Messages Standard

All player-facing text in Exylia plugins flows through the framework's messaging and formatting
layers, using **shared color presets** so the whole network shares one theme. Raw, hardcoded
strings are an anti-pattern.

Reference: `v2/visual` (`MessageAPI`, `ColorAPI`), `v2/config` (`Messages`),
`v2/formatter` (`FormatterAPI`), [../Visuals.md](../Visuals.md),
[../Formatting.md](../Formatting.md), [../Configuration.md](../Configuration.md).

---

## 1. Send text with `MessageAPI`

Send/broadcast through `MessageAPI`, never `player.sendMessage(...)` with a manually colored string.
`MessageAPI` handles formatting, presets, placeholders, and multi-line/filtered/radius delivery.

```java
MessageAPI.send(player, "{primary}Welcome back, %player_name%!");
MessageAPI.broadcast("{info}The event starts in 5 minutes.");
MessageAPI.sendToFiltered(p -> p.hasPermission("staff"), "{warning}[Staff] restart soon");
MessageAPI.sendInRadius(location, 20.0, "{warning}A boss spawned nearby!");
```

## 2. Store user-facing text in `messages.yml`

Player-facing strings live in `messages.yml` (via `Messages`), not inline in code. This lets server
owners re-word and translate without a rebuild, and keeps behavioral config separate from copy.

- **Standard:** behavioral values → schema `config.yml` ([Configuration.md](Configuration.md));
  player-facing copy → `messages.yml`.
- Missing keys resolve to a visible error marker, so problems surface in-game immediately.

## 3. Theme with color presets, never hardcoded hex

Use named presets from `colors.yml` (`{primary}`, `{success}`, `{error}`, `{warning}`, `{muted}`,
`{highlight}`, gradients, …) so a network restyle is one file edit. `ColorAPI` resolves them and
caches parsed output.

```yaml
join-message: "{primary}Welcome {highlight}%player_name% {primary}to the server!"
```

- Don't hardcode `&#ff5555` or duplicate the same gradient across files — reference a preset.
- Presets are reloaded through the reload system (`ColorAPI.reloadPresets()`), so themed text
  updates network-wide on reload.

## 4. Format values with `FormatterAPI`

Numbers, currency, percentages, and durations are formatted consistently with `FormatterAPI`
(config-driven), not with ad-hoc `String.format`/`DecimalFormat`.

```java
String bal = FormatterAPI.formatPrice(balance);       // e.g. "$1,250"
String pct = FormatterAPI.formatPercent(ratio);
String dur = FormatterAPI.formatTime(remainingSeconds);
```

Use `AsyncFormatterAPI` inside async pipelines (e.g. building a leaderboard off-thread).

## 5. Prefer transient visuals for feedback

For non-chat feedback (round start, cooldowns, warnings), prefer titles/action bars/boss bars over
chat spam — they're less intrusive and rate-limited. Use keyed updatables/countdowns rather than
re-sending each tick. See [../Visuals.md](../Visuals.md) and
[../examples/VisualComponents.md](../examples/VisualComponents.md).

```java
ActionBarAPI.sendUpdatable(player, "cooldown", cooldownConfig, context);
```

## 6. Always pass a player/context so placeholders resolve

Text with `%placeholders%` or per-call values needs a player (and optionally a
`PlaceholderContext` with keyed values). See [../Placeholders.md](../Placeholders.md).

```java
PlaceholderContext ctx = PlaceholderContext.create().withPlayer(player).put("amount", 250);
MessageAPI.send(player, "You earned %amount% coins.", ctx);
```

## 7. Wording conventions

- Keep messages **concise and actionable**; lead with the outcome.
- Use presets to convey severity: `{success}`, `{warning}`, `{error}`, `{info}`, `{muted}`.
- English source copy; localize via `messages.yml` rather than branching in code.
- Prefix system/library logs (not player messages) via [Debug](../Debug.md) — don't log to players.

---

## Checklist

- [ ] Player text via `MessageAPI` (or title/bar APIs), never manual `sendMessage` with raw colors.
- [ ] User-facing copy lives in `messages.yml`; behavior in schema config.
- [ ] Colors reference `colors.yml` presets, never hardcoded hex/gradients.
- [ ] Numbers/durations via `FormatterAPI` / `AsyncFormatterAPI`.
- [ ] Non-chat feedback uses titles/bars with updatables, not chat spam.
- [ ] Player/`PlaceholderContext` passed so placeholders resolve.

## Anti-patterns

- `player.sendMessage(ChatColor.RED + "...")` / hardcoded legacy or hex colors.
- Player-facing strings hardcoded in Java instead of `messages.yml`.
- Duplicated theme colors/gradients across files instead of a preset.
- `String.format`/`DecimalFormat` for currency/percent/duration instead of `FormatterAPI`.
- Spamming chat every tick for feedback that belongs on an action bar/title.
