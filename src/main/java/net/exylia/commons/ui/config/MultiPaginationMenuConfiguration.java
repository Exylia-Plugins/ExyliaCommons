package net.exylia.commons.ui.config;

import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.ui.items.MenuItem;
import net.exylia.commons.ui.menus.MultiPaginationMenu;
import net.exylia.commons.ui.builders.MultiPaginationMenuBuilder;
import net.exylia.commons.ui.builders.MenuItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MultiPaginationMenuConfiguration {

    private final JavaPlugin plugin;

    public MultiPaginationMenuConfiguration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public MultiPaginationMenu buildMenu(FileConfiguration config, Player player, ExyliaContext context) {
        return buildFromSection(config, player, context);
    }

    public MultiPaginationMenu buildMenu(ConfigurationSection section, Player player, ExyliaContext context) {
        return buildFromSection(section, player, context);
    }

    private MultiPaginationMenu buildFromSection(ConfigurationSection config, Player player, ExyliaContext context) {
        String title = config.getString("title", "Multi-Pagination Menu");
        int rows = config.getInt("rows", 6);

        MultiPaginationMenuBuilder builder = new MultiPaginationMenuBuilder(title, rows, context);

        if (config.getBoolean("dynamic_updates", false)) {
            long interval = config.getLong("update_interval", 20L);
            builder.dynamicUpdates(plugin, interval);
        }

        ConfigurationSection globalFillerConfig = config.getConfigurationSection("global_filler");
        if (globalFillerConfig != null) {
            MenuItem globalFiller = buildMenuItem(globalFillerConfig, player, context);
            builder.globalFiller(globalFiller);
        }

        ConfigurationSection borderFillerConfig = config.getConfigurationSection("border_filler");
        if (borderFillerConfig != null) {
            MenuItem borderFiller = buildMenuItem(borderFillerConfig, player, context);
            builder.borderFiller(borderFiller);
        }

        ConfigurationSection sectionsConfig = config.getConfigurationSection("sections");
        if (sectionsConfig != null) {
            for (String sectionName : sectionsConfig.getKeys(false)) {
                ConfigurationSection sectionConfig = sectionsConfig.getConfigurationSection(sectionName);
                if (sectionConfig != null) {
                    configureSection(builder, sectionName, sectionConfig, player, context);
                }
            }
        }

        MultiPaginationMenu menu = builder.build();
        configureSounds(menu, config);

        ConfigurationSection itemsConfig = config.getConfigurationSection("items");
        if (itemsConfig != null) {
            loadStaticItems(menu, itemsConfig, player, context, rows);
        }

        return menu;
    }

    private void configureSection(MultiPaginationMenuBuilder builder, String sectionName,
                                  ConfigurationSection config, Player player, ExyliaContext context) {

        int[] slots = parseSlots(config.getString("slots", "10-16"), 6);

        MultiPaginationMenuBuilder.SectionBuilder sectionBuilder = builder.addSection(sectionName, slots);

        ConfigurationSection prevButtonConfig = config.getConfigurationSection("prev_button");
        if (prevButtonConfig != null) {
            MenuItem prevButton = buildMenuItem(prevButtonConfig, player, context);
            int prevSlot = prevButtonConfig.getInt("slot", 45);
            sectionBuilder.previousButton(prevButton, prevSlot);
        }

        ConfigurationSection nextButtonConfig = config.getConfigurationSection("next_button");
        if (nextButtonConfig != null) {
            MenuItem nextButton = buildMenuItem(nextButtonConfig, player, context);
            int nextSlot = nextButtonConfig.getInt("slot", 53);
            sectionBuilder.nextButton(nextButton, nextSlot);
        }

        ConfigurationSection fillerConfig = config.getConfigurationSection("filler");
        if (fillerConfig != null) {
            MenuItem filler = buildMenuItem(fillerConfig, player, context);
            sectionBuilder.filler(filler);
        }

        ConfigurationSection selectedConfig = config.getConfigurationSection("selected_template");
        if (selectedConfig != null) {
            MenuItem selectedTemplate = buildMenuItem(selectedConfig, player, context);
            sectionBuilder.selectedTemplate(selectedTemplate);
        }

    }

    private void loadStaticItems(MultiPaginationMenu menu, ConfigurationSection itemsConfig,
                                 Player player, ExyliaContext context, int rows) {
        for (String itemKey : itemsConfig.getKeys(false)) {
            ConfigurationSection itemConfig = itemsConfig.getConfigurationSection(itemKey);
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

    private void configureSounds(MultiPaginationMenu menu, ConfigurationSection config) {
        if (config.contains("open_sounds")) {
            if (config.isList("open_sounds")) {
                menu.setOpenSounds(config.getStringList("open_sounds"));
            } else {
                menu.addOpenSound(config.getString("open_sounds"));
            }
        }

        if (config.contains("close_sounds")) {
            if (config.isList("close_sounds")) {
                menu.setCloseSounds(config.getStringList("close_sounds"));
            } else {
                menu.addCloseSound(config.getString("close_sounds"));
            }
        }

        if (config.contains("click_sounds")) {
            if (config.isList("click_sounds")) {
                menu.setClickSounds(config.getStringList("click_sounds"));
            } else {
                menu.addClickSound(config.getString("click_sounds"));
            }
        }
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
}
