package net.exylia.commons.ui.builders;

import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class NumericItemBuilder extends ItemBuilder<NumericItemBuilder> {

    private Consumer<Integer> setter;
    private Supplier<Integer> getter;
    private int minValue = Integer.MIN_VALUE;
    private int maxValue = Integer.MAX_VALUE;
    private int normalIncrement = 1;
    private int shiftIncrement = 10;
    private Consumer<MenuClickEvent> onValueChange;

    public NumericItemBuilder(Material material) {
        super(material);
    }

    public NumericItemBuilder(String materialString) {
        super(materialString);
    }

    public NumericItemBuilder setter(Consumer<Integer> setter) {
        this.setter = setter;
        return this;
    }

    public NumericItemBuilder getter(Supplier<Integer> getter) {
        this.getter = getter;
        return this;
    }

    public NumericItemBuilder range(int min, int max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }

    public NumericItemBuilder increments(int normal, int shift) {
        this.normalIncrement = normal;
        this.shiftIncrement = shift;
        return this;
    }

    public NumericItemBuilder onValueChange(Consumer<MenuClickEvent> callback) {
        this.onValueChange = callback;
        return this;
    }

    @Override
    public MenuItem build() {
        if (setter == null || getter == null) {
            throw new IllegalStateException("Both setter and getter must be provided");
        }

        item.setClickHandler(event -> {
            int currentValue = getter.get();
            int newValue = calculateNewValue(currentValue, event);

            newValue = Math.max(minValue, Math.min(maxValue, newValue));

            setter.accept(newValue);

            if (onValueChange != null) {
                onValueChange.accept(event);
            }

            item.process(event.getPlayer());
        });

        return super.build();
    }

    private int calculateNewValue(int currentValue, MenuClickEvent event) {
        int increment = event.isShiftClick() ? shiftIncrement : normalIncrement;

        return switch (event.getClickType()) {
            case LEFT, SHIFT_LEFT -> currentValue + increment;
            case RIGHT, SHIFT_RIGHT -> currentValue - increment;
            case MIDDLE -> minValue;
            default -> currentValue;
        };
    }
}