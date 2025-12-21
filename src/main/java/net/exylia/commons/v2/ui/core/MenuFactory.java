package net.exylia.commons.v2.ui.core;

import lombok.Getter;
import net.exylia.commons.v2.ui.cache.MenuCacheManager;
import net.exylia.commons.v2.ui.config.MenuConfig;
import net.exylia.commons.v2.ui.exception.MenuException;
import net.exylia.commons.v2.ui.model.MenuContext;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.MenuV2;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class MenuFactory {
    private final MenuCacheManager cacheManager;
    private final Map<MenuType, Class<? extends MenuV2>> menuTypeRegistry = new HashMap<>();

    public MenuFactory(MenuCacheManager cacheManager) {
        this.cacheManager = cacheManager;
        registerDefaultMenuTypes();
    }

    private void registerDefaultMenuTypes() {
    }

    public void registerMenuType(MenuType type, Class<? extends MenuV2> menuClass) {
        menuTypeRegistry.put(type, menuClass);
    }

    public MenuV2 createByType(MenuType type, String title, int rows) {
        Class<? extends MenuV2> menuClass = menuTypeRegistry.get(type);
        if (menuClass == null) {
            throw new MenuException("No menu class registered for type: " + type);
        }

        try {
            Constructor<? extends MenuV2> constructor = menuClass.getDeclaredConstructor(
                String.class, int.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(title, rows);
        } catch (Exception e) {
            throw new MenuException("Failed to create menu of type: " + type, e);
        }
    }

    public MenuV2 createFromConfig(MenuConfig config, MenuContext context) {
        if (config == null) {
            throw new MenuException("MenuConfig cannot be null");
        }

        String cachedKey = config.getId() + "_" + (context != null ? context.getId() : "default");

        return cacheManager.getTemplate(cachedKey).orElseGet(() -> {
            MenuV2 menu = buildFromConfig(config, context);
            cacheManager.cacheTemplate(cachedKey, menu);
            return menu;
        });
    }

    private MenuV2 buildFromConfig(MenuConfig config, MenuContext context) {
        MenuType type = config.getType() != null ? config.getType() : MenuType.SIMPLE;

        MenuV2 menu = createByType(type, config.getTitle(), config.getRows());

        if (context != null) {
            menu.setContext(context);
        }

        return menu;
    }

    public MenuV2 createInstance(MenuV2 template, MenuContext context) {
        if (template == null) {
            throw new MenuException("Template menu cannot be null");
        }

        String instanceId = UUID.randomUUID().toString();

        MenuV2 instance = template.clone();
        instance.setContext(context != null ? context : MenuContext.create());

        cacheManager.cacheInstance(instanceId, instance);

        return instance;
    }

    public MenuV2 cloneMenu(MenuV2 source) {
        if (source == null) {
            return null;
        }

        return source.clone();
    }
}
