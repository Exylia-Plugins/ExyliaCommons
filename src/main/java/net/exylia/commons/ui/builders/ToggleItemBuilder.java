package net.exylia.commons.ui.builders;

import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builder for toggle items
 * Provides a clean API without hardcoded messages or lore
 */
public class ToggleItemBuilder extends ItemBuilder<ToggleItemBuilder> {

    private Consumer<Boolean> setter;
    private Supplier<Boolean> getter;
    private Supplier<Boolean> condition;
    private Consumer<MenuClickEvent> onToggle;
    private Consumer<MenuClickEvent> onDeny;

    public ToggleItemBuilder(Material material) {
        super(material);
    }

    public ToggleItemBuilder(String materialString) {
        super(materialString);
    }

    /**
     * Sets the value setter
     * @param setter The setter function
     * @return This builder
     */
    public ToggleItemBuilder setter(Consumer<Boolean> setter) {
        this.setter = setter;
        return this;
    }

    /**
     * Sets the value getter
     * @param getter The getter function
     * @return This builder
     */
    public ToggleItemBuilder getter(Supplier<Boolean> getter) {
        this.getter = getter;
        return this;
    }

    /**
     * Sets the condition for allowing toggles
     * @param condition The condition supplier
     * @return This builder
     */
    public ToggleItemBuilder condition(Supplier<Boolean> condition) {
        this.condition = condition;
        return this;
    }

    /**
     * Sets the callback for when toggle is successful
     * @param callback The callback
     * @return This builder
     */
    public ToggleItemBuilder onToggle(Consumer<MenuClickEvent> callback) {
        this.onToggle = callback;
        return this;
    }

    /**
     * Sets the callback for when toggle is denied
     * @param callback The callback
     * @return This builder
     */
    public ToggleItemBuilder onDeny(Consumer<MenuClickEvent> callback) {
        this.onDeny = callback;
        return this;
    }

    @Override
    public MenuItem build() {
        if (setter == null || getter == null) {
            throw new IllegalStateException("Both setter and getter must be provided");
        }

        item.setClickHandler(event -> {
            // Check condition if provided
            if (condition != null && !condition.get()) {
                if (onDeny != null) {
                    onDeny.accept(event);
                }
                return;
            }

            // Toggle the value
            boolean currentValue = getter.get();
            setter.accept(!currentValue);

            // Call toggle callback
            if (onToggle != null) {
                onToggle.accept(event);
            }
        });

        return super.build();
    }
}