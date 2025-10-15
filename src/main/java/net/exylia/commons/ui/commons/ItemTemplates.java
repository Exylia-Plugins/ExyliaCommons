package net.exylia.commons.ui.commons;

import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.builders.SimpleItemBuilder;
import org.bukkit.Material;

import java.util.function.Consumer;

import static net.exylia.commons.utils.PredefinedHeads.TEXTURE_BACK;

public class ItemTemplates {

    public static MenuItem closeButton(String material) {
        return new SimpleItemBuilder(material)
                .click(MenuClickEvent::closeMenu)
                .build();
    }

    public static MenuItem backButton() {
        return new SimpleItemBuilder(TEXTURE_BACK)
                .click(MenuClickEvent::openParentMenu)
                .build();
    }

    public static MenuItem infoItem(String material) {
        return new SimpleItemBuilder(material)
                .hideAttributes()
                .build();
    }

    public static MenuItem fillerItem(String material) {
        return new SimpleItemBuilder(material)
                .name(" ")
                .hideAttributes()
                .build();
    }

    public static MenuItem navigationArrow(String material, Consumer<net.exylia.commons.ui.events.MenuClickEvent> action) {
        return new SimpleItemBuilder(material)
                .click(action)
                .hideAttributes()
                .build();
    }
}
