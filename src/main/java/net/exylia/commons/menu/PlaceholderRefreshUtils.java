package net.exylia.commons.menu;

import org.bukkit.entity.Player;

/**
 * Utilidades para refrescar placeholders en menús desde acciones externas
 */
@Deprecated
public class PlaceholderRefreshUtils {

    /**
     * Refresca todos los items con placeholders del menú actual del jugador
     * Útil para actualizaciones masivas después de cambios importantes
     *
     * @param player Jugador cuyo menú se va a actualizar
     */
    public static void refreshMenuPlaceholders(Player player) {
        MenuManager.refreshMenuPlaceholders(player);
    }

    /**
     * Refresca un item específico con placeholders por su slot
     * Útil para actualizaciones específicas desde acciones externas
     *
     * @param player Jugador cuyo menú se va a actualizar
     * @param slot Slot del item a actualizar
     */
    public static void refreshItemPlaceholders(Player player, int slot) {
        MenuManager.refreshItemPlaceholders(player, slot);
    }

    /**
     * Refresca múltiples items específicos con placeholders
     *
     * @param player Jugador cuyo menú se va a actualizar
     * @param slots Slots de los items a actualizar
     */
    public static void refreshItemsPlaceholders(Player player, int... slots) {
        for (int slot : slots) {
            MenuManager.refreshItemPlaceholders(player, slot);
        }
    }

    /**
     * Verifica si un jugador tiene un menú abierto
     *
     * @param player Jugador a verificar
     * @return true si tiene un menú abierto
     */
    public static boolean hasMenuOpen(Player player) {
        return MenuManager.getOpenMenu(player) != null;
    }

    /**
     * Obtiene el menú actual del jugador
     *
     * @param player Jugador
     * @return Menú actual o null si no tiene uno abierto
     */
    public static Menu getCurrentMenu(Player player) {
        return MenuManager.getOpenMenu(player);
    }
}