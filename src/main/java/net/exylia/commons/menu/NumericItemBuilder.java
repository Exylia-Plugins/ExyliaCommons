package net.exylia.commons.menu;

import net.exylia.commons.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.exylia.commons.utils.PredefinedColors.*;

@Deprecated
public class NumericItemBuilder {
    private String name;
    private String material;
    private String unit;
    private int minValue = -1;
    private int maxValue = Integer.MAX_VALUE;
    private int[] increments = {1, 10};
    private Consumer<Integer> setter;
    private Supplier<Integer> getter;
    private Player player;
    private Function<Integer, String> displayFormatter;
    private Runnable onUpdate;

    public NumericItemBuilder(String name, String material, String unit) {
        this.name = name;
        this.material = material;
        this.unit = unit;
        this.displayFormatter = value -> value == -1 ? "Disabled" : value + " " + unit;
    }

    public NumericItemBuilder range(int min, int max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }

    public NumericItemBuilder increments(int normal, int shift) {
        this.increments = new int[]{normal, shift};
        return this;
    }

    public NumericItemBuilder setter(Consumer<Integer> setter) {
        this.setter = setter;
        return this;
    }

    public NumericItemBuilder getter(Supplier<Integer> getter) {
        this.getter = getter;
        return this;
    }

    public NumericItemBuilder player(Player player) {
        this.player = player;
        return this;
    }

    public NumericItemBuilder displayFormatter(Function<Integer, String> formatter) {
        this.displayFormatter = formatter;
        return this;
    }

    public NumericItemBuilder onUpdate(Runnable callback) {
        this.onUpdate = callback;
        return this;
    }

    public MenuItem build() {
        MenuItem item = new MenuItem(material);
        updateItemState(item);

        item.setClickHandler(clickInfo -> {
            int currentValue = getter.get();
            int newValue = calculateNewValue(currentValue, clickInfo.clickType());

            setter.accept(newValue);

            MenuItem updatedItem = this.build();
            clickInfo.updateThisItem(updatedItem);

            if (player != null) {
                MessageUtils.sendMessageAsync(player, COLOR_SUCCESS + name + " set to " +
                        displayFormatter.apply(newValue) + "!");
            }

            if (onUpdate != null) {
                onUpdate.run();
            }
        });

        return item;
    }

    private void updateItemState(MenuItem item) {
        int currentValue = getter.get();
        item.setName(COLOR_PRIMARY + name);
        item.setLore(
                COLOR_LETTERS + "Current: " + COLOR_INFO + displayFormatter.apply(currentValue),
                "",
                COLOR_SECONDARY + " | " + COLOR_LETTERS + "Left-click:" + COLOR_INFO + " +" + increments[0] + " " + unit,
                COLOR_SECONDARY + " | " + COLOR_LETTERS + "Right-click:" + COLOR_INFO + " -" + COLOR_INFO + increments[0] + " " + unit,
                COLOR_SECONDARY + " | " + COLOR_LETTERS + "Shift+Left-click:" + COLOR_INFO + " +" + COLOR_INFO + increments[1] + " " + unit,
                COLOR_SECONDARY + " | " + COLOR_LETTERS + "Shift+Right-click:" + COLOR_INFO + " -" + COLOR_INFO + increments[1] + " " + unit,
                COLOR_SECONDARY + " | " + COLOR_LETTERS + "Middle-click: Reset to " + COLOR_INFO + (minValue == -1 ? "disabled" : "default")
        );
    }

    private int calculateNewValue(int currentValue, ClickType clickType) {
        int newValue = currentValue;

        switch (clickType) {
            case MIDDLE:
                newValue = minValue;
                break;
            case LEFT:
                newValue = (currentValue == -1) ? increments[0] : currentValue + increments[0];
                break;
            case RIGHT:
                newValue = Math.max(minValue, (currentValue == -1) ? minValue : currentValue - increments[0]);
                break;
            case SHIFT_LEFT:
                newValue = (currentValue == -1) ? increments[1] : currentValue + increments[1];
                break;
            case SHIFT_RIGHT:
                newValue = Math.max(minValue, (currentValue == -1) ? minValue : currentValue - increments[1]);
                break;
        }

        return Math.min(maxValue, Math.max(minValue, newValue));
    }
}