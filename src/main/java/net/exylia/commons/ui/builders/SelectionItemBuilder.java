package net.exylia.commons.ui.builders;

import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builder for selection items
 * Cycles through a list of options
 */
public class SelectionItemBuilder<T> extends ItemBuilder<SelectionItemBuilder<T>> {

    private Consumer<T> setter;
    private Supplier<T> getter;
    private T[] options;
    private Consumer<MenuClickEvent> onSelectionChange;

    public SelectionItemBuilder(Material material) {
        super(material);
    }

    public SelectionItemBuilder(String materialString) {
        super(materialString);
    }

    /**
     * Sets the value setter
     * @param setter The setter function
     * @return This builder
     */
    public SelectionItemBuilder<T> setter(Consumer<T> setter) {
        this.setter = setter;
        return this;
    }

    /**
     * Sets the value getter
     * @param getter The getter function
     * @return This builder
     */
    public SelectionItemBuilder<T> getter(Supplier<T> getter) {
        this.getter = getter;
        return this;
    }

    /**
     * Sets the available options
     * @param options The options array
     * @return This builder
     */
    @SafeVarargs
    public final SelectionItemBuilder<T> options(T... options) {
        this.options = options;
        return this;
    }

    /**
     * Sets the callback for when selection changes
     * @param callback The callback
     * @return This builder
     */
    public SelectionItemBuilder<T> onSelectionChange(Consumer<MenuClickEvent> callback) {
        this.onSelectionChange = callback;
        return this;
    }

    @Override
    public MenuItem build() {
        if (setter == null || getter == null || options == null || options.length == 0) {
            throw new IllegalStateException("Setter, getter, and options must be provided");
        }

        item.setClickHandler(event -> {
            T currentValue = getter.get();
            int currentIndex = findIndex(currentValue);

            int newIndex = event.isRightClick() ?
                    (currentIndex - 1 + options.length) % options.length :
                    (currentIndex + 1) % options.length;

            setter.accept(options[newIndex]);

            if (onSelectionChange != null) {
                onSelectionChange.accept(event);
            }
        });

        return super.build();
    }

    private int findIndex(T value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                return i;
            }
        }
        return 0; // Default to first option if not found
    }
}