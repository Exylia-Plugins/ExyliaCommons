package net.exylia.commons.ui.builders;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.placeholders.PlaceholderSystemManager;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.actions.ActionContext;
import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.actions.GlobalActionManager;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Builder for creating MenuItems from ConfigurationSection
 */
public class MenuItemBuilder {

    /**
     * Creates a MenuItem from a ConfigurationSection
     * @param config The configuration section
     * @return The built MenuItem
     */
    public static MenuItem fromConfig(ConfigurationSection config) {
        return fromConfig(config, null, null);
    }

    /**
     * Creates a MenuItem from a ConfigurationSection with player context
     * @param config The configuration section
     * @param player The player (for placeholders)
     * @return The built MenuItem
     */
    public static MenuItem fromConfig(ConfigurationSection config, Player player) {
        return fromConfig(config, player, null);
    }

    /**
     * Creates a MenuItem from a ConfigurationSection with full context
     * @param config The configuration section
     * @param player The player (for placeholders)
     * @param context The menu context (for placeholders)
     * @return The built MenuItem
     */
    public static MenuItem fromConfig(ConfigurationSection config, Player player, ExyliaContext context) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration section cannot be null");
        }

        // Material (obligatorio)
        String material = config.getString("material", "STONE");
        MenuItem item = new MenuItem(material);

        // Configurar propiedades básicas, visuales, comportamiento, etc.
        configureBasicProperties(item, config);
        configureVisualProperties(item, config);
        configureBehavior(item, config);
        configureActions(item, config);
        configureCommands(item, config, player, context);

        if (context != null) {
            item.withContext(context);
        }

        item.process(player);

        return item;
    }

    private static void configureBasicProperties(MenuItem item, ConfigurationSection config) {
        // Nombre
        if (config.contains("name")) {
            item.setName(config.getString("name"));
        }

        // Lore
        if (config.contains("lore")) {
            if (config.isList("lore")) {
                item.setLoreList(config.getStringList("lore"));
            } else {
                item.setLore(config.getString("lore"));
            }
        }

        // Cantidad
        if (config.contains("amount")) {
            Object amount = config.get("amount");
            if (amount instanceof String) {
                item.setAmount((String) amount);
            } else if (amount instanceof Integer) {
                item.setAmount((Integer) amount);
            }
        }
    }

    private static void configureVisualProperties(MenuItem item, ConfigurationSection config) {
        // Brillo
        if (config.getBoolean("glow", false)) {
            item.setGlowing(true);
        }

        // Ocultar atributos
        if (config.getBoolean("hide_attributes", false)) {
            item.hideAllAttributes();
        }

        // Flags específicos
        if (config.contains("item_flags")) {
            // TODO: Implementar flags específicos si es necesario
        }
    }

    private static void configureBehavior(MenuItem item, ConfigurationSection config) {
        // Actualizaciones dinámicas
        if (config.getBoolean("dynamic_update", false)) {
            item.setDynamicUpdate(true);

            if (config.contains("update_interval")) {
                item.setUpdateInterval(config.getLong("update_interval", 20L));
            }
        }
    }

    private static void configureActions(MenuItem item, ConfigurationSection config) {
        // Acción simple - usando el sistema global
        if (config.contains("action")) {
            String actionString = config.getString("action");
            item.setClickHandler(event -> {
                ActionContext actionContext = createActionContextFromMenuClick(event);
                GlobalActionManager.executeAction(actionString, actionContext);
            });
        }

        // Click handler personalizado (para casos especiales)
        if (config.contains("click_type")) {
            String clickType = config.getString("click_type");
            switch (clickType.toLowerCase()) {
                case "close" -> item.setClickHandler(MenuClickEvent::closeMenu);
                case "back" -> item.setClickHandler(MenuClickEvent::openParentMenu);
                // Agregar más tipos según necesidad
            }
        }
    }

    private static ActionContext createActionContextFromMenuClick(MenuClickEvent event) {
        ActionContext context = new ActionContext(event.getPlayer(), ActionSource.MENU);

        // Añadir datos del menú al contexto
        context.withData("menu", event.getMenu());
        context.withData("item", event.getItem());
        context.withData("slot", event.getSlot());
        context.withData("clickType", event.getClickType());

        return context;
    }

    private static void configureCommands(MenuItem item, ConfigurationSection config, Player player, Object... context) {
        // Comandos
        if (config.contains("commands")) {
            java.util.List<String> commands = config.getStringList("commands");
            if (!commands.isEmpty()) {
                item.setClickHandler(event -> {
                    executeCommands(commands, event.getPlayer(), context);
                });
            }
        }
    }

    private static void executeCommands(java.util.List<String> commands, Player player, Object... context) {
        for (String command : commands) {
            String processed = command;

            // Procesar placeholders si hay contexto
            if (context != null) {
                processed = PlaceholderSystemManager.getInstance().process(processed, player);
            }

            // Ejecutar comando según prefijo
            if (processed.startsWith("player:")) {
                String cmd = processed.substring(7).trim();
                player.performCommand(cmd);
            } else if (processed.startsWith("console:")) {
                String cmd = processed.substring(8).trim();
                org.bukkit.Bukkit.getServer().dispatchCommand(
                        org.bukkit.Bukkit.getServer().getConsoleSender(), cmd);
            } else {
                // Default a comando de jugador
                player.performCommand(processed);
            }
        }
    }

    /**
     * Creates a fluent builder for MenuItem
     * @param material The material
     * @return A fluent builder
     */
    public static FluentMenuItemBuilder create(String material) {
        return new FluentMenuItemBuilder(new MenuItem(material));
    }

    /**
     * Creates a fluent builder from existing MenuItem
     * @param item The existing item
     * @return A fluent builder
     */
    public static FluentMenuItemBuilder from(MenuItem item) {
        return new FluentMenuItemBuilder(item);
    }

    // ==================== FLUENT BUILDER ====================

    public static class FluentMenuItemBuilder {
        private final MenuItem item;

        public FluentMenuItemBuilder(MenuItem item) {
            this.item = item;
        }

        public FluentMenuItemBuilder name(String name) {
            item.setName(name);
            return this;
        }

        public FluentMenuItemBuilder lore(String... lore) {
            item.setLore(lore);
            return this;
        }

        public FluentMenuItemBuilder lore(java.util.List<String> lore) {
            item.setLoreList(lore);
            return this;
        }

        public FluentMenuItemBuilder amount(int amount) {
            item.setAmount(amount);
            return this;
        }

        public FluentMenuItemBuilder amount(String amountString) {
            item.setAmount(amountString);
            return this;
        }

        public FluentMenuItemBuilder glow(boolean glowing) {
            item.setGlowing(glowing);
            return this;
        }

        public FluentMenuItemBuilder glow() {
            return glow(true);
        }

        public FluentMenuItemBuilder hideAttributes() {
            item.hideAllAttributes();
            return this;
        }

        public FluentMenuItemBuilder dynamicUpdate(boolean dynamic) {
            item.setDynamicUpdate(dynamic);
            return this;
        }

        public FluentMenuItemBuilder updateInterval(long interval) {
            item.setUpdateInterval(interval);
            return this;
        }

        public FluentMenuItemBuilder click(java.util.function.Consumer<MenuClickEvent> handler) {
            item.setClickHandler(handler);
            return this;
        }

        public FluentMenuItemBuilder action(String actionString) {
            item.setClickHandler(event -> {
                ActionContext actionContext = createActionContextFromMenuClick(event);
                GlobalActionManager.executeAction(actionString, actionContext);
            });
            return this;
        }

        public FluentMenuItemBuilder closeOnClick() {
            item.setClickHandler(event -> event.closeMenu());
            return this;
        }

        public FluentMenuItemBuilder backOnClick() {
            item.setClickHandler(event -> event.openParentMenu());
            return this;
        }

        public MenuItem build() {
            return item;
        }
    }
}