# Plan: Complete in-game effect editor

1. Build the effect editor state layer: `EffectEditorSession`, registry, selector, single-entry clipboard, and list clipboard. Keep mutable editor data portable and copy entries with fresh IDs.
2. Add the UI layer mirroring the existing loot/potion editors: paginated effect list, type selector, and type-aware edit menu. Expose add/edit/delete/copy/paste/list copy/list paste/save/cancel actions with the existing menu item and placeholder conventions.
3. Add action handlers and chat/numeric input flows for all effect fields, including particle/sound/potion/firework/title/actionbar/message editing, validation/fallback feedback, and reopening the correct menu after each input.
4. Register the actions during core bootstrap, preserve existing action names, and avoid changing unrelated editor behavior.
5. Compile and run tests, inspect only the feature diff, then commit and push the completed feature on the current branch.
