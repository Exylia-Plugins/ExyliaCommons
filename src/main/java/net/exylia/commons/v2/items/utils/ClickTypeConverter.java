package net.exylia.commons.v2.items.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;

@UtilityClass
public class ClickTypeConverter {

    public ClickType fromAction(Action action, boolean sneaking) {
        if (action == null) {
            return ClickType.LEFT;
        }

        return switch (action) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK ->
                sneaking ? ClickType.SHIFT_LEFT : ClickType.LEFT;
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK ->
                sneaking ? ClickType.SHIFT_RIGHT : ClickType.RIGHT;
            case PHYSICAL ->
                ClickType.LEFT;
        };
    }

    public ClickType fromAction(Action action) {
        return fromAction(action, false);
    }
}
