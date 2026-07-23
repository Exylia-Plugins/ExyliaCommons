# UI Framework (Overview)

This is the high-level map of the ExyliaCommons UI stack. It ties together three subsystems that
are almost always used together:

| Subsystem | Doc | Responsibility |
|-----------|-----|----------------|
| **Menus** | [Menus.md](Menus.md) | YAML-driven and programmatic inventory GUIs |
| **Items** | [Items.md](Items.md) | Building `ItemStack`s from YAML/builders |
| **Skulls** | [Skulls.md](Skulls.md) | Player-head textures with async fetch + caching |

## How They Fit Together

A menu is a grid of items. Menu items are built by the **Items** layer (from YAML sections or
builders). When an item is a player head, the **Skull** layer resolves its texture asynchronously
(from cache when possible). Clicking items triggers [Actions](Actions.md), [Commands](Commands.md),
sounds, and navigation. Item text is formatted through [ColorAPI](Formatting.md) and may contain
[placeholders](Placeholders.md).

```
Menu (ui)  ──uses──▶ Items (items)  ──uses──▶ Skulls (skull)
   │                                              ▲
   ├─ Actions / Commands on click                 │ async texture fetch
   ├─ Placeholders + ColorAPI for text            │
   ├─ Pagination + Navigation                     │
   ├─ Packet title updates (PacketEvents optional)│
   └─ Snapshot integration (save/restore inv)
```

## Initialization

`MenuAPI.initialize(plugin)` is **opt-in** (call it in `onExyliaEnable`). It **also lazily
initializes `CommandAPI` and `SkullAPI`**, so opening menus brings those up automatically. If you
use `SkullAPI`/`CommandAPI` independently, initialize them yourself.

```java
@Override
protected void onExyliaEnable() {
    MenuAPI.initialize(this); // brings up CommandAPI + SkullAPI too
}
```

## Threading At a Glance

- **Inventory reads/writes must be on the main/region thread.** The framework offloads *item
  population* (placeholder resolution, skull fetch) to async work, but the final inventory writes
  are synchronous.
- Prefer the **async open/process** paths (`openAsync`/`processAsync`) and let the framework
  handle the sync display step.
- Preload skulls at startup so menus render from cache without stalls.

Continue to the per-subsystem docs for full detail.
