package net.exylia.commons.v2.ui.model;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Getter
public class MenuContext {
    private final String id;
    private final PlaceholderContext placeholderContext;
    private final Map<String, Object> metadata;
    private final Map<String, Supplier<Object>> dynamicData;

    private Player player;
    private MenuV2 parentMenu;
    private boolean closeOnClickOutside;
    private boolean preventClose;

    private MenuContext(String id) {
        this.id = id;
        this.placeholderContext = PlaceholderContext.create();
        this.metadata = new ConcurrentHashMap<>();
        this.dynamicData = new ConcurrentHashMap<>();
        this.closeOnClickOutside = true;
        this.preventClose = false;
    }

    public static MenuContext create() {
        return new MenuContext(UUID.randomUUID().toString());
    }

    public static MenuContext create(Player player) {
        MenuContext context = new MenuContext(UUID.randomUUID().toString());
        context.withPlayer(player);
        return context;
    }

    public static MenuContext createWithParent(MenuV2 parent) {
        MenuContext context = new MenuContext(UUID.randomUUID().toString());
        context.withParent(parent);
        return context;
    }

    public static MenuContextBuilder builder() {
        return new MenuContextBuilder();
    }

    public static class MenuContextBuilder {
        private Player player;
        private MenuV2 parentMenu;
        private boolean closeOnClickOutside = true;
        private boolean preventClose = false;

        public MenuContextBuilder player(Player player) {
            this.player = player;
            return this;
        }

        public MenuContextBuilder parent(MenuV2 parent) {
            this.parentMenu = parent;
            return this;
        }

        public MenuContextBuilder closeOnClickOutside(boolean closeOnClickOutside) {
            this.closeOnClickOutside = closeOnClickOutside;
            return this;
        }

        public MenuContextBuilder preventClose(boolean preventClose) {
            this.preventClose = preventClose;
            return this;
        }

        public MenuContext build() {
            MenuContext context = new MenuContext(UUID.randomUUID().toString());
            if (player != null) {
                context.withPlayer(player);
            }
            if (parentMenu != null) {
                context.withParent(parentMenu);
            }
            context.closeOnClickOutside = this.closeOnClickOutside;
            context.preventClose = this.preventClose;
            return context;
        }
    }

    public MenuContext withPlayer(Player player) {
        this.player = player;
        this.placeholderContext.withPlayer(player);
        return this;
    }

    public MenuContext withParent(MenuV2 parent) {
        this.parentMenu = parent;
        return this;
    }

    public MenuContext put(String key, Object value) {
        metadata.put(key, value);
        placeholderContext.put(key, value);
        return this;
    }

    public MenuContext putDynamic(String key, Supplier<Object> supplier) {
        dynamicData.put(key, supplier);
        return this;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value == null) {
            Supplier<Object> supplier = dynamicData.get(key);
            if (supplier != null) {
                value = supplier.get();
            }
        }

        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }

        return Optional.empty();
    }

    public MenuContext setCloseOnClickOutside(boolean closeOnClickOutside) {
        this.closeOnClickOutside = closeOnClickOutside;
        return this;
    }

    public MenuContext setPreventClose(boolean preventClose) {
        this.preventClose = preventClose;
        return this;
    }

    public MenuContext merge(MenuContext other) {
        if (other != null) {
            this.metadata.putAll(other.metadata);
            this.dynamicData.putAll(other.dynamicData);
        }
        return this;
    }

    public MenuContext copy() {
        MenuContext copy = new MenuContext(UUID.randomUUID().toString());
        copy.metadata.putAll(this.metadata);
        copy.dynamicData.putAll(this.dynamicData);
        copy.player = this.player;
        copy.parentMenu = this.parentMenu;
        copy.closeOnClickOutside = this.closeOnClickOutside;
        copy.preventClose = this.preventClose;

        return copy;
    }
}
