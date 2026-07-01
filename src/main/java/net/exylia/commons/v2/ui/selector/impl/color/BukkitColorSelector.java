package net.exylia.commons.v2.ui.selector.impl.color;

import net.exylia.commons.v2.chat.api.ChatInputAPI;
import net.exylia.commons.v2.ui.selector.core.AbstractSelector;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.BiConsumer;

public final class BukkitColorSelector extends AbstractSelector<BukkitColorResult> {

    private static final List<ColorEntry> COLORS = List.of(
            new ColorEntry("WHITE",   "White",   "#FFFFFF", Color.WHITE),
            new ColorEntry("SILVER",  "Silver",  "#C0C0C0", Color.SILVER),
            new ColorEntry("GRAY",    "Gray",    "#808080", Color.GRAY),
            new ColorEntry("BLACK",   "Black",   "#3A3A3A", Color.BLACK),
            new ColorEntry("RED",     "Red",     "#FF0000", Color.RED),
            new ColorEntry("MAROON",  "Maroon",  "#800000", Color.MAROON),
            new ColorEntry("YELLOW",  "Yellow",  "#FFFF00", Color.YELLOW),
            new ColorEntry("OLIVE",   "Olive",   "#808000", Color.OLIVE),
            new ColorEntry("LIME",    "Lime",    "#00FF00", Color.LIME),
            new ColorEntry("GREEN",   "Green",   "#008000", Color.GREEN),
            new ColorEntry("AQUA",    "Aqua",    "#00FFFF", Color.AQUA),
            new ColorEntry("TEAL",    "Teal",    "#008080", Color.TEAL),
            new ColorEntry("BLUE",    "Blue",    "#0000FF", Color.BLUE),
            new ColorEntry("NAVY",    "Navy",    "#000080", Color.NAVY),
            new ColorEntry("FUCHSIA", "Fuchsia", "#FF00FF", Color.FUCHSIA),
            new ColorEntry("PURPLE",  "Purple",  "#800080", Color.PURPLE),
            new ColorEntry("ORANGE",  "Orange",  "#FFA500", Color.ORANGE)
    );

    private BukkitColorSelector(Player player) {
        super(player);
        this.title = "Select Color";
    }

    public static BukkitColorSelector of(Player player) {
        return new BukkitColorSelector(player);
    }

    public BukkitColorSelector title(String title) {
        this.title = title;
        return this;
    }

    public BukkitColorSelector onSelect(BiConsumer<Player, BukkitColorResult> callback) {
        this.onSelect = callback;
        return this;
    }

    public BukkitColorSelector onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    @Override
    public void open() {
        ChatInputAPI.OptionBuilder builder = ChatInputAPI.option(player, title);
        for (ColorEntry entry : COLORS) {
            builder.option(entry.name(), "<color:" + entry.hex() + ">█ " + entry.label());
        }
        builder
                .columns(4)
                .onCancel(onCancel)
                .onResponse(this::handleSelected)
                .ask();
    }

    private void handleSelected(String name) {
        ColorEntry found = null;
        for (ColorEntry entry : COLORS) {
            if (entry.name().equals(name)) {
                found = entry;
                break;
            }
        }
        if (found == null) {
            if (onCancel != null) onCancel.run();
            return;
        }
        if (onSelect != null) {
            onSelect.accept(player, new BukkitColorResult(found.name(), found.color()));
        }
    }

    public static Color fromName(String name) {
        if (name == null) return Color.WHITE;
        for (ColorEntry entry : COLORS) {
            if (entry.name().equalsIgnoreCase(name)) return entry.color();
        }
        return Color.WHITE;
    }

    private record ColorEntry(String name, String label, String hex, Color color) {}
}
