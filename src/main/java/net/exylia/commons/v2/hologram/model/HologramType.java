package net.exylia.commons.v2.hologram.model;

/**
 * The kind of Display entity a {@link Hologram} renders as.
 * <p>
 * Each hologram instance renders as exactly one underlying display
 * (TextDisplay, ItemDisplay or BlockDisplay), mirroring the relationship
 * Minecraft itself uses between a Display entity and its content.
 */
public enum HologramType {
    TEXT,
    ITEM,
    BLOCK
}
