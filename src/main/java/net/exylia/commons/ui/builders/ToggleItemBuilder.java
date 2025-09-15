package net.exylia.commons.ui.builders;

import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;

import java.util.function.Consumer;
import java.util.function.Supplier;

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

    public ToggleItemBuilder setter(Consumer<Boolean> setter) {
        this.setter = setter;
        return this;
    }

    public ToggleItemBuilder getter(Supplier<Boolean> getter) {
        this.getter = getter;
        return this;
    }

    public ToggleItemBuilder condition(Supplier<Boolean> condition) {
        this.condition = condition;
        return this;
    }

    public ToggleItemBuilder onToggle(Consumer<MenuClickEvent> callback) {
        this.onToggle = callback;
        return this;
    }

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
            if (condition != null && !condition.get()) {
                if (onDeny != null) {
                    onDeny.accept(event);
                }
                return;
            }

            boolean currentValue = getter.get();
            setter.accept(!currentValue);

            if (onToggle != null) {
                onToggle.accept(event);
            }

            item.process(event.getPlayer());
        });

        return super.build();
    }
}