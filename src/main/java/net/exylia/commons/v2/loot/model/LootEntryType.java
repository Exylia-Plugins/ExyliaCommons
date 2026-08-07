package net.exylia.commons.v2.loot.model;

/**
 * What a {@link LootEntry} actually gives when rolled. ITEM is the default and the only type
 * historically supported (item-spawners, loot-chests, event loot tables). COMMAND is opt-in for
 * callers that want it (e.g. mines granting currency or triggering a custom effect on block
 * break) — callers that only want items (chests, spawners) simply never expose the command
 * option in their editor UI and can otherwise ignore this entirely.
 */
public enum LootEntryType {
    ITEM,
    COMMAND
}
