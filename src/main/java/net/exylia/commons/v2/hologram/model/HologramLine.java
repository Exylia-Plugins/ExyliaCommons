package net.exylia.commons.v2.hologram.model;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

/**
 * A single row inside a {@link Hologram}'s vertical stack. Every line is
 * rendered as its own display entity (packet-based when PacketEvents is
 * available, a real Bukkit entity otherwise) so text, item and block rows
 * can be freely mixed within the same hologram.
 * <p>
 * Exactly one of the type-specific accessors ({@link #getText()},
 * {@link #getItem()}, {@link #getBlock()}) is meaningful, matching
 * {@link #getType()}.
 */
@Getter
public class HologramLine {
    private final HologramType type;
    private final String text;
    private final Component component;
    private final ItemStack item;
    private final ItemDisplay.ItemDisplayTransform itemTransform;
    private final BlockData block;
    private final HologramProperties properties;

    private HologramLine(HologramType type, String text, Component component, ItemStack item,
                          ItemDisplay.ItemDisplayTransform itemTransform, BlockData block,
                          HologramProperties properties) {
        this.type = type;
        this.text = text;
        this.component = component;
        this.item = item;
        this.itemTransform = itemTransform;
        this.block = block;
        this.properties = properties;
    }

    public HologramLine(String text) {
        this(HologramType.TEXT, text, null, null, null, null, null);
    }

    public HologramLine(String text, HologramProperties properties) {
        this(HologramType.TEXT, text, null, null, null, null, properties);
    }

    public HologramLine(String text, Component component, HologramProperties properties) {
        this(HologramType.TEXT, text, component, null, null, null, properties);
    }

    public static HologramLine text(String text) {
        return new HologramLine(text);
    }

    public static HologramLine text(String text, HologramProperties properties) {
        return new HologramLine(text, properties);
    }

    public static HologramLine item(ItemStack item) {
        return item(item, ItemDisplay.ItemDisplayTransform.FIXED, null);
    }

    public static HologramLine item(ItemStack item, ItemDisplay.ItemDisplayTransform transform) {
        return item(item, transform, null);
    }

    public static HologramLine item(ItemStack item, ItemDisplay.ItemDisplayTransform transform, HologramProperties properties) {
        return new HologramLine(HologramType.ITEM, null, null, item,
                transform != null ? transform : ItemDisplay.ItemDisplayTransform.FIXED, null, properties);
    }

    public static HologramLine block(BlockData block) {
        return block(block, null);
    }

    public static HologramLine block(BlockData block, HologramProperties properties) {
        return new HologramLine(HologramType.BLOCK, null, null, null, null, block, properties);
    }

    public HologramProperties getPropertiesOrDefault(HologramProperties defaultProperties) {
        return properties != null ? properties : defaultProperties;
    }

    public boolean hasCustomProperties() {
        return properties != null;
    }

    public boolean isText() {
        return type == HologramType.TEXT;
    }
}
