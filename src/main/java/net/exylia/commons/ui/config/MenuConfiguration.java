package net.exylia.commons.ui.config;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.events.MenuClickEvent;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.menus.PaginationMenu;
import net.exylia.commons.ui.menus.EditableMenu;
import net.exylia.commons.actions.ActionContext;
import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.actions.GlobalActionManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration-based menu builder
 * Replaces the old MenuBuilder with a cleaner API
 */
public class MenuConfiguration {

    private final JavaPlugin plugin;

    public MenuConfiguration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Builds a menu from configuration
     * @param config The configuration
     * @param player The player
     * @return The built menu
     */
    public Menu buildMenu(FileConfiguration config, Player player) {
        return buildMenu(config, player, null);
    }

    /**
     * Builds a menu from configuration with context
     * @param config The configuration
     * @param player The player
     * @param context The menu context
     * @return The built menu
     */
    public Menu buildMenu(FileConfiguration config, Player player, ExyliaContext context) {
        return buildFromSection(config, player, context);
    }

    /**
     * Builds a menu from a configuration section
     * @param section The configuration section
     * @param player The player
     * @param context The menu context
     * @return The built menu
     */
    public Menu buildMenu(ConfigurationSection section, Player player, ExyliaContext context) {
        return buildFromSection(section, player, context);
    }

    private Menu buildFromSection(ConfigurationSection config, Player player, ExyliaContext context) {
        String title = config.getString("title", "Menu");
        int rows = config.getInt("rows", 3);
        String type = config.getString("type", "normal").toLowerCase();

        Menu menu = createMenuByType(type, title, rows, config, context);

        if (config.getBoolean("dynamic_updates", false)) {
            long interval = config.getLong("update_interval", 20L);
            menu.enableDynamicUpdates(plugin, interval);
        }

        // Configure fillers
        configureFiller(menu, config.getConfigurationSection("global_filler"), "global", player, context);
        configureFiller(menu, config.getConfigurationSection("border_filler"), "border", player, context);

        // Load items
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            loadItems(menu, itemsSection, player, rows, context);
        }
        menu.enableSmartRefresh();
        return menu;
    }

    private Menu createMenuByType(String type, String title, int rows, ConfigurationSection config, ExyliaContext context) {
        return switch (type) {
            case "pagination" -> {
                String slotsString = config.getString("item_slots", "10-16,19-25,28-34");
                int[] slots = parseSlots(slotsString, rows);

                PaginationMenu menu = new PaginationMenu(title, rows, slots, context);

                ConfigurationSection globalFillerSection = config.getConfigurationSection("global_filler");
                if (globalFillerSection != null) {
                    menu.setGlobalFiller(buildMenuItem(globalFillerSection, null, context));
                }

                ConfigurationSection sectionFillerSection = config.getConfigurationSection("section_filler");
                if (sectionFillerSection != null) {
                    menu.setItemSlotFiller(buildMenuItem(sectionFillerSection, null, context));
                }

                ConfigurationSection prevButtonSection = config.getConfigurationSection("prev_button");
                if (prevButtonSection != null) {
                    menu.setPreviousButton(buildMenuItem(prevButtonSection, null, context), prevButtonSection.getInt("slot", 1));
                }

                ConfigurationSection nextButtonSection = config.getConfigurationSection("next_button");
                if (nextButtonSection != null) {
                    menu.setNextButton(buildMenuItem(nextButtonSection, null, context), nextButtonSection.getInt("slot", 1));
                }

                yield menu;
            }
            case "editable" -> new EditableMenu(title, rows, context);
            default -> new Menu(title, rows, context);
        };
    }

    private void configureFiller(Menu menu, ConfigurationSection fillerConfig, String type, Player player, ExyliaContext context) {
        if (fillerConfig == null) return;

        MenuItem filler = buildMenuItem(fillerConfig, player, context);

        switch (type) {
            case "global" -> menu.setGlobalFiller(filler);
            case "border" -> menu.setBorderFiller(filler);
        }
    }

    private void loadItems(Menu menu, ConfigurationSection itemsSection, Player player, int rows, ExyliaContext context) {
        for (String itemKey : itemsSection.getKeys(false)) {
            ConfigurationSection itemConfig = itemsSection.getConfigurationSection(itemKey);
            if (itemConfig == null) continue;

            MenuItem item = buildMenuItem(itemConfig, player, context);
            List<Integer> slots = getItemSlots(itemConfig, rows);

            for (int slot : slots) {
                if (slot >= 0 && slot < rows * 9) {
                    menu.setItem(slot, item);
                }
            }
        }
    }

    private MenuItem buildMenuItem(ConfigurationSection config, Player player, ExyliaContext context) {
        String material = config.getString("material", "STONE");
        MenuItem item = new MenuItem(material);

        // Basic properties
        if (config.contains("name")) {
            item.setName(config.getString("name"));
        }

        if (config.contains("lore")) {
            item.setLoreList(config.getStringList("lore"));
        }

        if (config.contains("amount")) {
            Object amount = config.get("amount");
            if (amount instanceof String) {
                item.setAmount((String) amount);
            } else if (amount instanceof Integer) {
                item.setAmount((Integer) amount);
            }
        }

        // Visual properties
        if (config.getBoolean("glow", false)) {
            item.setGlowing(true);
        }

        if (config.getBoolean("hide_attributes", true)) {
            item.hideAllAttributes();
        }

        // Dynamic updates
        if (config.getBoolean("dynamic_update", false)) {
            item.setDynamicUpdate(true);
            if (config.contains("update_interval")) {
                item.setUpdateInterval(config.getLong("update_interval", 20L));
            }
        }

        // Actions - usando el sistema global
        if (config.contains("action")) {
            String actionString = config.getString("action");
            item.setClickHandler(event -> {
                ActionContext actionContext = createActionContextFromMenuClick(event);
                GlobalActionManager.executeAction(actionString, actionContext);
            });
        }

        if (config.contains("commands")) {
            List<String> commands = config.getStringList("commands");
            item.setClickHandler(event -> {
                executeCommands(commands, event.getPlayer(), context);
            });
        }

        item.withContext(context);

        return item;
    }

    private ActionContext createActionContextFromMenuClick(MenuClickEvent event) {
        ActionContext context = new ActionContext(event.getPlayer(), ActionSource.MENU);

        // Añadir datos del menú al contexto
        context.withData("menu", event.getMenu());
        context.withData("item", event.getItem());
        context.withData("slot", event.getSlot());
        context.withData("clickType", event.getClickType());

        return context;
    }

    private void executeCommands(List<String> commands, Player player, ExyliaContext context) {
        for (String command : commands) {
            String processed = command;

            // Process placeholders if context is available
            if (context != null) {
                processed = context.processPlaceholders(processed, player);
            }

            // Execute command based on prefix
            if (processed.startsWith("player:")) {
                String cmd = processed.substring(7).trim();
                player.performCommand(cmd);
            } else if (processed.startsWith("console:")) {
                String cmd = processed.substring(8).trim();
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            } else {
                // Default to player command
                player.performCommand(processed);
            }
        }
    }

    private List<Integer> getItemSlots(ConfigurationSection config, int rows) {
        List<Integer> slots = new ArrayList<>();
        int maxSlot = rows * 9 - 1;

        // Single slot
        if (config.contains("slot")) {
            int slot = config.getInt("slot", -1);
            if (slot >= 0 && slot <= maxSlot) {
                slots.add(slot);
            }
        }

        // Multiple slots as string
        if (config.contains("slots") && config.isString("slots")) {
            String slotsString = config.getString("slots");
            int[] parsedSlots = parseSlots(slotsString, rows);
            for (int slot : parsedSlots) {
                slots.add(slot);
            }
        }

        // Multiple slots as list
        if (config.contains("slots") && config.isList("slots")) {
            List<Integer> slotsList = config.getIntegerList("slots");
            for (int slot : slotsList) {
                if (slot >= 0 && slot <= maxSlot) {
                    slots.add(slot);
                }
            }
        }

        return slots;
    }

    private int[] parseSlots(String slotsString, int rows) {
        List<Integer> slots = new ArrayList<>();
        int maxSlot = rows * 9 - 1;

        if (slotsString == null || slotsString.isEmpty()) {
            return new int[0];
        }

        String[] parts = slotsString.split(",");
        for (String part : parts) {
            part = part.trim();

            if (part.contains("-")) {
                // Range: "10-16"
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());

                        for (int i = Math.max(0, start); i <= Math.min(maxSlot, end); i++) {
                            slots.add(i);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                // Single slot
                try {
                    int slot = Integer.parseInt(part);
                    if (slot >= 0 && slot <= maxSlot) {
                        slots.add(slot);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        return slots.stream().mapToInt(Integer::intValue).toArray();
    }
}