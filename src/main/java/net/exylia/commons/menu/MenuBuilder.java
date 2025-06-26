package net.exylia.commons.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class MenuBuilder {

    private final JavaPlugin plugin;

    /**
     * Constructor del MenuBuilder
     * @param plugin Plugin principal
     */
    public MenuBuilder(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Crea un menú a partir de la configuración
     * @param menuConfig Configuración del menú (FileConfiguration)
     * @param player Jugador para el que se crea el menú
     * @return Menú creado o null si no existe la configuración
     */
    public Menu buildMenu(FileConfiguration menuConfig, Player player) {
        return buildMenuFromSection(menuConfig, player);
    }

    /**
     * Crea un menú a partir de una sección de configuración
     * @param menuSection Sección de configuración del menú
     * @param player Jugador para el que se crea el menú
     * @return Menú creado o null si no existe la configuración
     */
    public Menu buildMenu(ConfigurationSection menuSection, Player player) {
        return buildMenuFromSection(menuSection, player);
    }

    /**
     * Crea un menú a partir de la configuración con soporte para placeholders personalizados
     * @param menuConfig Configuración del menú (FileConfiguration)
     * @param player Jugador para el que se crea el menú
     * @param placeholderContext Objeto de contexto para placeholders personalizados (puede ser un array)
     * @return Menú creado o null si no existe la configuración
     */
    public Menu buildMenu(FileConfiguration menuConfig, Player player, Object... placeholderContext) {
        return buildMenuFromSection(menuConfig, player, placeholderContext);
    }

    /**
     * Crea un menú a partir de una sección de configuración con soporte para placeholders personalizados
     * @param menuSection Sección de configuración del menú
     * @param player Jugador para el que se crea el menú
     * @param placeholderContext Objeto de contexto para placeholders personalizados (puede ser un array)
     * @return Menú creado o null si no existe la configuración
     */
    public Menu buildMenu(ConfigurationSection menuSection, Player player, Object... placeholderContext) {
        return buildMenuFromSection(menuSection, player, placeholderContext);
    }

    /**
     * Crea un menú paginado a partir de una sección de configuración
     * @param menuSection Sección de configuración del menú
     * @param player Jugador para el que se crea el menú
     * @param itemSlots Posiciones donde colocar los ítems paginados
     * @return Menú paginado creado o null si no existe la configuración
     */
    public PaginationMenu buildPaginationMenu(ConfigurationSection menuSection, Player player, String itemSlots) {
        return buildPaginationMenuFromSection(menuSection, player, itemSlots);
    }

    /**
     * Crea un menú paginado a partir de una sección de configuración
     * @param menuSection Sección de configuración del menú
     * @param player Jugador para el que se crea el menú
     * @param itemSlots Posiciones donde colocar los ítems paginados
     * @return Menú paginado creado o null si no existe la configuración
     */
    public PaginationMenu buildPaginationMenu(ConfigurationSection menuSection, Player player, String itemSlots, Object... placeholderContext) {
        return buildPaginationMenuFromSection(menuSection, player, itemSlots, placeholderContext);
    }

    /**
     * Implementación interna para crear un menú desde cualquier tipo de configuración
     */
    private Menu buildMenuFromSection(ConfigurationSection menuSection, Player player) {
        return buildMenuFromSection(menuSection, player, null);
    }

    /**
     * Implementación interna para crear un menú desde cualquier tipo de configuración con placeholders
     */
    private Menu buildMenuFromSection(ConfigurationSection menuSection, Player player, Object placeholderContext) {
        // Propiedades básicas del menú
        String title = menuSection.getString("title", "Menu");
        int rows = menuSection.getInt("rows", 3);
        Menu menu = new Menu(title, rows);

        // Configuración de actualizaciones dinámicas del menú
        if (menuSection.getBoolean("dynamic_updates", false)) {
            long updateInterval = menuSection.getLong("update_interval", 20L);
            menu.enableDynamicUpdates(plugin, updateInterval);
        }

        // Configurar placeholders en el título si está especificado en la configuración
        if (placeholderContext != null && menuSection.getBoolean("use_placeholders_in_title", false)) {
            menu.usePlaceholdersInTitle(true);
            menu.setTitlePlaceholderContext(placeholderContext);
        }

        // Cargar ítems
        ConfigurationSection itemsSection = menuSection.getConfigurationSection("items");
        if (itemsSection != null) {
            System.out.println("=== PROCESANDO SECCIÓN ITEMS (MENU NORMAL) ===");
            System.out.println("Contexto disponible: " + (placeholderContext != null ? placeholderContext.getClass().getSimpleName() : "null"));

            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    System.out.println("Procesando item: " + itemKey);

                    MenuItem menuItem;
                    if (placeholderContext != null) {
                        if (placeholderContext instanceof Object[] contexts) {
                            Object[] allContexts = new Object[contexts.length + 1];
                            allContexts[0] = player;
                            System.arraycopy(contexts, 0, allContexts, 1, contexts.length);
                            System.out.println("Creando item con " + allContexts.length + " contextos expandidos");
                            menuItem = buildMenuItem(itemSection, allContexts);
                        } else {
                            System.out.println("Creando item con contexto único");
                            menuItem = buildMenuItem(itemSection, player, placeholderContext);
                        }
                    } else {
                        System.out.println("Creando item solo con player");
                        menuItem = buildMenuItem(itemSection, player);
                    }

                    List<Integer> slots = getItemSlots(itemSection, rows);

                    for (int slot : slots) {
                        if (slot >= 0 && slot < rows * 9) {
                            menu.setItem(slot, menuItem);
                        }
                    }
                }
            }
            System.out.println("=== FIN SECCIÓN ITEMS (MENU NORMAL) ===");
        }

        return menu;
    }

    /**
     * Implementación interna para crear un menú paginado desde cualquier tipo de configuración
     */
    private PaginationMenu buildPaginationMenuFromSection(ConfigurationSection menuSection, Player player, Object itemSlots, Object... placeholderContext) {
        String title = menuSection.getString("title", "Menu");
        int rows = menuSection.getInt("rows", 6);

        PaginationMenu paginationMenu;
        if (itemSlots instanceof String) {
            paginationMenu = new PaginationMenu(title, rows, (String) itemSlots);
        } else {
            paginationMenu = new PaginationMenu(title, rows, (int[]) itemSlots);
        }

        // Configurar botón de página anterior
        ConfigurationSection prevSection = menuSection.getConfigurationSection("prev_button");
        if (prevSection != null) {
            MenuItem prevButton = buildMenuItem(prevSection, player);

            List<Integer> prevSlots = getItemSlots(prevSection, rows);
            int prevSlot = prevSlots.isEmpty() ? (rows * 9 - 9) : prevSlots.get(0);

            paginationMenu.setPreviousPageButton(prevButton, prevSlot);
        }

        // Configurar botón de página siguiente
        ConfigurationSection nextSection = menuSection.getConfigurationSection("next_button");
        if (nextSection != null) {
            MenuItem nextButton = buildMenuItem(nextSection, player);

            List<Integer> nextSlots = getItemSlots(nextSection, rows);
            int nextSlot = nextSlots.isEmpty() ? (rows * 9 - 1) : nextSlots.get(0);

            paginationMenu.setNextPageButton(nextButton, nextSlot);
        }

        // Configurar filler global
        ConfigurationSection fillerSection = menuSection.getConfigurationSection("global_filler");
        if (fillerSection != null) {
            MenuItem fillerItem = buildMenuItem(fillerSection, player);
            paginationMenu.setGlobalFiller(fillerItem);
        }

        // Configurar filler de sección
        ConfigurationSection sectionFillerSection = menuSection.getConfigurationSection("section_filler");
        if (sectionFillerSection != null) {
            MenuItem sectionFillerItem = buildMenuItem(sectionFillerSection, player);
            paginationMenu.setItemSlotsFillerItem(sectionFillerItem);
        }

        ConfigurationSection itemsSection = menuSection.getConfigurationSection("items");
        if (itemsSection != null) {
            System.out.println("=== PROCESANDO SECCIÓN ITEMS ===");
            System.out.println("Contextos disponibles: " + (placeholderContext != null ? placeholderContext.length : 0));
            if (placeholderContext != null) {
                for (int i = 0; i < placeholderContext.length; i++) {
                    System.out.println("Contexto [" + i + "]: " + (placeholderContext[i] != null ? placeholderContext[i].getClass().getSimpleName() : "null"));
                }
            }

            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    System.out.println("Procesando item: " + itemKey);

                    MenuItem menuItem;

                    if (placeholderContext != null && placeholderContext.length > 0) {
                        Object[] allContexts = new Object[placeholderContext.length + 1];
                        allContexts[0] = player;
                        System.arraycopy(placeholderContext, 0, allContexts, 1, placeholderContext.length);

                        System.out.println("Creando item con " + allContexts.length + " contextos");
                        menuItem = buildMenuItem(itemSection, allContexts);
                    } else {
                        System.out.println("Creando item solo con player");
                        menuItem = buildMenuItem(itemSection, player);
                    }

                    List<Integer> slots = getItemSlots(itemSection, rows);

                    for (int slot : slots) {
                        if (slot >= 0 && slot < rows * 9) {
                            paginationMenu.setItem(slot, menuItem);
                        }
                    }
                }
            }
            System.out.println("=== FIN SECCIÓN ITEMS ===");
        }

        return paginationMenu;
    }

    /**
     * Obtiene todos los slots para un ítem según su configuración
     * @param itemSection Sección de configuración del ítem
     * @param rows Número de filas del menú para validación
     * @return Lista de slots donde debe colocarse el ítem
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

        // "slots: [0,1,2,3]"
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

    public static MenuItem buildMenuItem(ConfigurationSection itemSection, Player player) {
        MenuItem menuItem = new MenuItem(itemSection.getString("material", "STONE"), player);
        applyItemConfig(menuItem, itemSection, player);
        return menuItem;
    }

    public static MenuItem buildMenuItem(ConfigurationSection itemSection, Player player, Object placeholderContext) {
        MenuItem menuItem = new MenuItem(itemSection.getString("material", "STONE"), player, placeholderContext);
        applyItemConfig(menuItem, itemSection, player);
        return menuItem;
    }

    public static MenuItem buildMenuItem(ConfigurationSection itemSection, Object... contexts) {
        Player player = null;
        for (Object context : contexts) {
            if (context instanceof Player) {
                player = (Player) context;
                break;
            }
        }

        if (player == null) {
            throw new IllegalArgumentException("Se requiere al menos un Player en los contextos");
        }

        MenuItem menuItem = new MenuItem(itemSection.getString("material", "STONE"), player, contexts);
        applyItemConfig(menuItem, itemSection, player);
        return menuItem;
    }

    private static void applyItemConfig(MenuItem menuItem, ConfigurationSection itemSection, Player player) {
        if (itemSection.contains("name")) {
            menuItem.setName(itemSection.getString("name"));
        }

        // Soporte mejorado para amount con placeholders
        if (itemSection.contains("amount")) {
            Object amountValue = itemSection.get("amount");

            if (amountValue instanceof String amountString) {
                // Si es un string, podría contener placeholders
                menuItem.setAmount(amountString);
            } else if (amountValue instanceof Integer) {
                // Si es un entero directo
                menuItem.setAmount((Integer) amountValue);
            } else {
                // Fallback: convertir a string y tratar como tal
                menuItem.setAmount(String.valueOf(amountValue));
            }
        }

        if (itemSection.contains("glow")) {
            menuItem.setGlowing(itemSection.getBoolean("glow", false));
        }

        if (itemSection.contains("lore")) {
            menuItem.setLore(itemSection.getStringList("lore").toArray(new String[0]));
        }

        if (itemSection.getBoolean("hide_attributes", false)) {
            menuItem.hideAllAttributes();
        }

        if (itemSection.getBoolean("use_placeholders", false)) {
            menuItem.setPlaceholderPlayer(player);
            menuItem.usePlaceholders(true);
            menuItem.updatePlaceholders(player);
        }

        if (itemSection.getBoolean("dynamic_update", false)) {
            menuItem.setDynamicUpdate(true);
            if (itemSection.contains("update_interval")) {
                menuItem.setUpdateInterval(itemSection.getLong("update_interval", 20L));
            }
        }

        if (itemSection.contains("action")) {
            menuItem.setAction(itemSection.getString("action"));
        }

        if (itemSection.contains("commands")) {
            List<String> commands = itemSection.getStringList("commands");
            if (!commands.isEmpty()) {
                menuItem.setCommands(commands);
            }
        }
    }
}