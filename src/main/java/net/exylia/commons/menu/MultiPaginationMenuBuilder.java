package net.exylia.commons.menu;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

import static net.exylia.commons.menu.MenuBuilder.buildMenuItem;

/**
 * Constructor para menús multi-paginados desde configuración
 */
@Deprecated
public class MultiPaginationMenuBuilder {

    private final JavaPlugin plugin;

    public MultiPaginationMenuBuilder(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Construye un menú multi-paginado desde una configuración
     * @param config Configuración del menú
     * @param player Jugador para el que se construye
     * @return Menú multi-paginado construido
     */
    public MultiPaginationMenu buildMenu(FileConfiguration config, Player player) {
        return buildMenuFromSection(config, player);
    }

    /**
     * Construye un menú multi-paginado desde una sección de configuración
     * @param section Sección de configuración
     * @param player Jugador para el que se construye
     * @return Menú multi-paginado construido
     */
    public MultiPaginationMenu buildMenu(ConfigurationSection section, Player player) {
        return buildMenuFromSection(section, player);
    }

    private MultiPaginationMenu buildMenuFromSection(ConfigurationSection config, Player player) {
        String title = config.getString("title", "Multi-Pagination Menu");
        int rows = config.getInt("rows", 6);

        MultiPaginationMenu menu = new MultiPaginationMenu(title, rows);

        // Configurar actualizaciones dinámicas si están habilitadas
        if (config.getBoolean("dynamic_updates", false)) {
            long updateInterval = config.getLong("update_interval", 20L);
            menu.enableDynamicUpdates(plugin, updateInterval);
        }

        // Configurar placeholders en el título si está especificado
        if (config.getBoolean("use_placeholders_in_title", false)) {
            menu.usePlaceholdersInTitle(true);
        }

        ConfigurationSection fillerSection = config.getConfigurationSection("filler");
        if (fillerSection != null) {
            MenuItem fillerItem = buildMenuItem(fillerSection, player);
            menu.setGlobalFiller(fillerItem);
        }

        for (String key : config.getKeys(false)) {
            ConfigurationSection sectionConfig = config.getConfigurationSection(key);
            if (sectionConfig == null) continue;

            if (isPaginationSection(sectionConfig)) {
                buildPaginationSection(menu, key, sectionConfig, player);
            }
        }

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            loadStaticItems(menu, itemsSection, player, rows);
        }

        return menu;
    }

    /**
     * Verifica si una sección de configuración es una sección de paginación
     */
    private boolean isPaginationSection(ConfigurationSection section) {
        // Una sección es de paginación si tiene prev_button o next_button o un subsection con slots
        return section.contains("prev_button") ||
                section.contains("next_button") ||
                hasPaginationItems(section);
    }

    /**
     * Verifica si la sección tiene items paginables
     */
    private boolean hasPaginationItems(ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null && subSection.contains("slots")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Construye una sección paginable del menú
     */
    private void buildPaginationSection(MultiPaginationMenu menu, String sectionName,
                                        ConfigurationSection config, Player player) {

        // Encontrar la configuración de items para esta sección
        ConfigurationSection itemsConfig = null;
        List<Integer> slots = new ArrayList<>();

        for (String key : config.getKeys(false)) {
            ConfigurationSection subSection = config.getConfigurationSection(key);
            if (subSection != null && subSection.contains("slots")) {
                itemsConfig = subSection;
                slots = getItemSlots(subSection, menu.size / 9);
                break;
            }
        }

        if (itemsConfig == null || slots.isEmpty()) {
            return;
        }

        // Crear la sección
        MultiPaginationMenu.PaginationSection section = menu.addSection(sectionName,
                slots.stream().mapToInt(Integer::intValue).toArray());

        // Configurar botones de navegación
        ConfigurationSection prevSection = config.getConfigurationSection("prev_button");
        if (prevSection != null) {
            MenuItem prevButton = buildMenuItem(prevSection, player);
            List<Integer> prevSlots = getItemSlots(prevSection, menu.size / 9);
            int prevSlot = prevSlots.isEmpty() ? 0 : prevSlots.get(0);
            section.setPreviousButton(prevButton, prevSlot);
        }

        ConfigurationSection nextSection = config.getConfigurationSection("next_button");
        if (nextSection != null) {
            MenuItem nextButton = buildMenuItem(nextSection, player);
            List<Integer> nextSlots = getItemSlots(nextSection, menu.size / 9);
            int nextSlot = nextSlots.isEmpty() ? 8 : nextSlots.get(0);
            section.setNextButton(nextButton, nextSlot);
        }

        // Configurar filler de la sección
        ConfigurationSection fillerSection = config.getConfigurationSection("filler");
        if (fillerSection != null) {
            MenuItem fillerItem = buildMenuItem(fillerSection, player);
            section.setFillerItem(fillerItem);
        }

        // Configurar template para item seleccionado si existe
        ConfigurationSection selectedConfig = config.getConfigurationSection("selected_" +
                itemsConfig.getName());
        if (selectedConfig == null) {
            // Buscar con otros nombres comunes
            for (String possibleName : new String[]{"selected", "selected_item", "selected_template"}) {
                selectedConfig = config.getConfigurationSection(possibleName);
                if (selectedConfig != null) break;
            }
        }

        if (selectedConfig != null) {
            MenuItem selectedTemplate = buildMenuItem(selectedConfig, player);
            section.setSelectedItemTemplate(selectedTemplate);
        }

        // TODO: Los items reales se añadirían dinámicamente desde el código del plugin
        // Por ahora, añadir algunos items de ejemplo si hay una lista en la configuración
        if (itemsConfig.contains("example_items")) {
            List<?> exampleItems = itemsConfig.getList("example_items");
            if (exampleItems != null) {
                for (Object itemObj : exampleItems) {
                    if (itemObj instanceof ConfigurationSection) {
                        MenuItem item = buildMenuItem((ConfigurationSection) itemObj, player);
                        section.addItem(item);
                    }
                }
            }
        }
    }

    /**
     * Carga items estáticos normales (no paginados)
     */
    private void loadStaticItems(MultiPaginationMenu menu, ConfigurationSection itemsSection,
                                 Player player, int rows) {
        for (String itemKey : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
            if (itemSection != null) {
                MenuItem menuItem = buildMenuItem(itemSection, player);

                List<Integer> slots = getItemSlots(itemSection, rows);
                for (int slot : slots) {
                    if (slot >= 0 && slot < rows * 9) {
                        menu.setItem(slot, menuItem);
                    }
                }
            }
        }
    }

    /**
     * Obtiene todos los slots para un ítem según su configuración
     * (Copiado de MenuBuilder para reutilizar la lógica)
     */
    private List<Integer> getItemSlots(ConfigurationSection itemSection, int rows) {
        List<Integer> slots = new ArrayList<>();
        int maxSlot = rows * 9 - 1;

        if (itemSection.contains("slot")) {
            int slot = itemSection.getInt("slot", -1);
            if (slot >= 0 && slot <= maxSlot) {
                slots.add(slot);
            }
        }

        if (itemSection.contains("slots") && itemSection.isString("slots")) {
            String slotsString = itemSection.getString("slots");
            if (slotsString != null) {
                String[] parts = slotsString.split(",");

                for (String part : parts) {
                    part = part.trim();

                    if (part.contains("-")) {
                        String[] range = part.split("-");
                        if (range.length == 2) {
                            try {
                                int start = Integer.parseInt(range[0].trim());
                                int end = Integer.parseInt(range[1].trim());

                                start = Math.max(0, start);
                                end = Math.min(maxSlot, end);

                                for (int i = start; i <= end; i++) {
                                    slots.add(i);
                                }
                            } catch (NumberFormatException e) {
                                // Ignorar
                            }
                        }
                    }
                    else {
                        try {
                            int slot = Integer.parseInt(part);
                            if (slot >= 0 && slot <= maxSlot) {
                                slots.add(slot);
                            }
                        } catch (NumberFormatException e) {
                            // Ignorar
                        }
                    }
                }
            }
        }

        if (itemSection.contains("slots") && itemSection.isList("slots")) {
            List<Integer> slotsList = itemSection.getIntegerList("slots");
            for (int slot : slotsList) {
                if (slot >= 0 && slot <= maxSlot) {
                    slots.add(slot);
                }
            }
        }

        return slots;
    }
}