package net.exylia.commons.ui.config;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.core.Menu;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.menus.PaginationMenu;
import net.exylia.commons.ui.menus.EditableMenu;
import net.exylia.commons.ui.builders.EditableMenuBuilder;
import net.exylia.commons.ui.builders.MenuItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MenuConfiguration {

    private final JavaPlugin plugin;

    public MenuConfiguration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Menu buildMenu(FileConfiguration config, Player player) {
        return buildMenu(config, player, null);
    }

    public Menu buildMenu(FileConfiguration config, Player player, ExyliaContext context) {
        return buildFromSection(config, player, context);
    }

    public Menu buildMenu(ConfigurationSection section, Player player, ExyliaContext context) {
        return buildFromSection(section, player, context);
    }

    private Menu buildFromSection(ConfigurationSection config, Player player, ExyliaContext context) {
        String title = config.getString("title", "Menu");
        int rows = config.getInt("rows", 3);
        String type = config.getString("type", "normal").toLowerCase();

        Menu menu = createMenuByType(type, title, rows, config, context, player);

        if (config.getBoolean("dynamic_updates", false)) {
            long interval = config.getLong("update_interval", 20L);
            menu.enableDynamicUpdates(plugin, interval);
        }

        configureFiller(menu, config.getConfigurationSection("global_filler"), "global", player, context);
        configureFiller(menu, config.getConfigurationSection("border_filler"), "border", player, context);

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            loadItems(menu, itemsSection, player, rows, context);
        }

        menu.enableSmartRefresh();
        return menu;
    }

    private Menu createMenuByType(String type, String title, int rows, ConfigurationSection config,
                                  ExyliaContext context, Player player) {
        return switch (type) {
            case "pagination" -> createPaginationMenu(title, rows, config, context);
            case "editable" -> createEditableMenu(title, rows, config, context, player);
            default -> new Menu(title, rows, context);
        };
    }

    private PaginationMenu createPaginationMenu(String title, int rows, ConfigurationSection config, ExyliaContext context) {
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

        return menu;
    }

    private EditableMenu createEditableMenu(String title, int rows, ConfigurationSection config,
                                            ExyliaContext context, Player player) {
        EditableMenuBuilder builder = new EditableMenuBuilder(title, rows, context);

        String editableSlotsString = config.getString("editable_slots");
        if (editableSlotsString != null && !editableSlotsString.isEmpty()) {
            int[] editableSlots = parseSlots(editableSlotsString, rows);
            builder.addEditableSlots(editableSlots);
        }

        ConfigurationSection sectionFillerSection = config.getConfigurationSection("section_filler");
        if (sectionFillerSection != null) {
            MenuItem sectionFiller = buildMenuItem(sectionFillerSection, player, context);
            builder.editableSlotFiller(sectionFiller);
        }

        EditableMenu menu = builder.build();

        ConfigurationSection editableSectionConfig = config.getConfigurationSection("editable_section");
        if (editableSectionConfig != null) {
            loadEditableItems(menu, editableSectionConfig, player, context);
        }

        return menu;
    }

    private void loadEditableItems(EditableMenu menu, ConfigurationSection editableSection,
                                   Player player, ExyliaContext context) {
        for (String itemKey : editableSection.getKeys(false)) {
            ConfigurationSection itemConfig = editableSection.getConfigurationSection(itemKey);
            if (itemConfig == null) continue;

            MenuItem menuItem = buildMenuItem(itemConfig, player, context);
            ItemStack itemStack = menuItem.buildProcessed(player);

            if (itemConfig.contains("slot")) {
                int slot = itemConfig.getInt("slot");
                if (menu.isSlotEditable(slot)) {
                    menu.setEditableItem(slot, itemStack);
                }
            } else if (itemConfig.contains("slots")) {
                List<Integer> slots = getItemSlots(itemConfig, menu.getRows());
                for (int slot : slots) {
                    if (menu.isSlotEditable(slot)) {
                        menu.setEditableItem(slot, itemStack.clone());
                        break;
                    }
                }
            }
        }
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
        return MenuItemBuilder.fromConfig(config, player, context);
    }


    private List<Integer> getItemSlots(ConfigurationSection config, int rows) {
        List<Integer> slots = new ArrayList<>();
        int maxSlot = rows * 9 - 1;

        if (config.contains("slot")) {
            int slot = config.getInt("slot", -1);
            if (slot >= 0 && slot <= maxSlot) {
                slots.add(slot);
            }
        }

        if (config.contains("slots") && config.isString("slots")) {
            String slotsString = config.getString("slots");
            int[] parsedSlots = parseSlots(slotsString, rows);
            for (int slot : parsedSlots) {
                slots.add(slot);
            }
        }

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