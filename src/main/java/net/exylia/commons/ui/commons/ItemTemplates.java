package net.exylia.commons.ui.commons;

import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.builders.SimpleItemBuilder;
import org.bukkit.Material;

import java.util.function.Consumer;

import static net.exylia.commons.utils.PredefinedHeads.TEXTURE_BACK;

/**
 * Common item templates
 * Provides base item templates without styling
 */
public class ItemTemplates {

    /**
     * Creates a close button template
     * @param material The material to use
     * @return The close button item
     */
    public static MenuItem closeButton(String material) {
        return new SimpleItemBuilder(material)
                .click(MenuClickEvent::closeMenu)
                .build();
    }

    /**
     * Creates a back button template
     * @return The back button item
     */
    public static MenuItem backButton() {
        return new SimpleItemBuilder(TEXTURE_BACK)
                .click(MenuClickEvent::openParentMenu)
                .build();
    }

    /**
     * Creates an info item template
     * @param material The material to use
     * @return The info item
     */
    public static MenuItem infoItem(String material) {
        return new SimpleItemBuilder(material)
                .hideAttributes()
                .build();
    }

    /**
     * Creates a filler item template
     * @param material The material to use
     * @return The filler item
     */
    public static MenuItem fillerItem(String material) {
        return new SimpleItemBuilder(material)
                .name(" ")
                .hideAttributes()
                .build();
    }

    /**
     * Creates a navigation arrow template
     * @param material The material to use
     * @param action The navigation action
     * @return The navigation item
     */
    public static MenuItem navigationArrow(String material, Consumer<net.exylia.commons.ui.events.MenuClickEvent> action) {
        return new SimpleItemBuilder(material)
                .click(action)
                .hideAttributes()
                .build();
    }
}