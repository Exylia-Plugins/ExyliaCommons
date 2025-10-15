package net.exylia.commons.ui.builders;

import net.exylia.commons.ui.events.MenuClickEvent;
import org.bukkit.Material;

import java.util.function.Consumer;

public class SimpleItemBuilder extends ItemBuilder<SimpleItemBuilder> {

    public SimpleItemBuilder(Material material) {
        super(material);
    }

    public SimpleItemBuilder(String materialString) {
        super(materialString);
    }

    public SimpleItemBuilder click(Consumer<MenuClickEvent> handler) {
        item.setClickHandler(handler);
        return this;
    }
}
