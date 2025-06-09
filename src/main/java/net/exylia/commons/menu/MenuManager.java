package net.exylia.commons.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Sistema de gestión de menús interactivos para plugins de Exylia
 */
public class MenuManager implements Listener {
    private static JavaPlugin plugin;
    private static final Map<UUID, Menu> openMenus = new HashMap<>();
    private static final Map<UUID, PaginationMenu> openPaginationMenus = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Inicializa el sistema de menús
     * @param javaPlugin El plugin principal
     */
    public static void initialize(JavaPlugin javaPlugin) {
        if (initialized) return;
        plugin = javaPlugin;
        Bukkit.getPluginManager().registerEvents(new MenuManager(), plugin);
        initialized = true;
    }

    /**
     * Maneja los clics en inventarios
     * @param event Evento de clic en inventario
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        MenuItem item = menu.getItem(event.getSlot());
        if (item != null) {
            MenuClickInfo clickInfo = new MenuClickInfo(player, event.getClick(), event.getSlot(), menu, item);

            // 1. Ejecutar acción personalizada si existe usando el sistema global (prioridad alta)
            boolean actionExecuted = false;
            if (item.hasAction()) {
                actionExecuted = item.executeAction(clickInfo);
            }

            // 2. Ejecutar comandos si hay definidos (solo si no se ejecutó una acción o si la acción falló)
            if (!actionExecuted && !item.getCommands().isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    item.executeCommands(player);
                });
            }

            // 3. Ejecutar el handler de clic si está definido (siempre se ejecuta)
            if (item.getClickHandler() != null) {
                try {
                    item.getClickHandler().accept(clickInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Evita arrastrar items en menús
     * @param event Evento de arrastrar en inventario
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (openMenus.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Maneja el cierre de inventarios
     * @param event Evento de cierre de inventario
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Menu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;

        menu.onClose(player);

        PaginationMenu paginationMenu = openPaginationMenus.get(player.getUniqueId());
        if (paginationMenu != null) {
            openPaginationMenus.remove(player.getUniqueId());
        }

        openMenus.remove(player.getUniqueId());

        if (menu.getCloseHandler() != null) {
            menu.getCloseHandler().accept(player);
        }

        if (menu.getReturnMenu() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> menu.getReturnMenu().open(player), 1L);
        }
    }

    /**
     * Registra un menú abierto
     * @param player Jugador que abrió el menú
     * @param menu Menú abierto
     */
    static void registerOpenMenu(Player player, Menu menu) {
        openMenus.put(player.getUniqueId(), menu);
        if (menu instanceof PaginationMenu) {
            openPaginationMenus.put(player.getUniqueId(), (PaginationMenu) menu);
        }
    }

    /**
     * Registra un menú paginado abierto
     * @param player Jugador que abrió el menú
     * @param paginationMenu Menú paginado abierto
     */
    static void registerOpenPaginationMenu(Player player, PaginationMenu paginationMenu) {
        openPaginationMenus.put(player.getUniqueId(), paginationMenu);
    }

    /**
     * Obtiene el plugin asociado
     * @return Plugin principal
     */
    public static JavaPlugin getPlugin() {
        return plugin;
    }
}