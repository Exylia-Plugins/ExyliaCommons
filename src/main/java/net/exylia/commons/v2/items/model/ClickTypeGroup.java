package net.exylia.commons.v2.items.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.inventory.ClickType;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public enum ClickTypeGroup {
    LEFT("left", ClickType.LEFT),
    RIGHT("right", ClickType.RIGHT),
    MIDDLE("middle", ClickType.MIDDLE),
    SHIFT_LEFT("shift_left", ClickType.SHIFT_LEFT),
    SHIFT_RIGHT("shift_right", ClickType.SHIFT_RIGHT),
    DROP("drop", ClickType.DROP),
    SWAP("swap", ClickType.SWAP_OFFHAND),
    DOUBLE("double", ClickType.DOUBLE_CLICK),
    NUMBER_KEY("number_key", ClickType.NUMBER_KEY),
    ANY("any", null);

    private final String prefix;
    private final ClickType bukkitClickType;

    private static final Map<String, ClickTypeGroup> BY_PREFIX = new ConcurrentHashMap<>();
    private static final Map<ClickType, ClickTypeGroup> BY_BUKKIT = new EnumMap<>(ClickType.class);

    static {
        for (ClickTypeGroup type : values()) {
            BY_PREFIX.put(type.prefix.toLowerCase(), type);
            if (type.bukkitClickType != null) {
                BY_BUKKIT.put(type.bukkitClickType, type);
            }
        }
    }

    public static ClickTypeGroup fromPrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        return BY_PREFIX.get(prefix.toLowerCase());
    }

    public static ClickTypeGroup fromBukkit(ClickType clickType) {
        if (clickType == null) {
            return ANY;
        }
        return BY_BUKKIT.getOrDefault(clickType, ANY);
    }

    public static boolean isValid(String prefix) {
        if (prefix == null) {
            return false;
        }
        return BY_PREFIX.containsKey(prefix.toLowerCase());
    }
}
