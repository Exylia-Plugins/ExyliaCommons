package net.exylia.commons.menu;

import net.exylia.commons.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.exylia.commons.utils.PredefinedColors.*;
import static net.exylia.commons.utils.PredefinedHeads.*;

/**
 * Utilidades estáticas para crear menús y items comunes del sistema
 */
@Deprecated
public class MenuCommons {

    // ==================== MENÚS DE CONFIRMACIÓN ====================

    /**
     * Crea un menú de confirmación simple con botones Aceptar/Cancelar
     * @param title Título del menú
     * @param confirmAction Acción a ejecutar si confirma
     * @param cancelAction Acción a ejecutar si cancela (opcional)
     * @param returnAction Acción para volver al menú anterior (opcional)
     * @return Menú de confirmación configurado
     */
    public static Menu createConfirmationMenu(String title, Consumer<Player> confirmAction,
                                              Consumer<Player> cancelAction, Consumer<Player> returnAction) {
        Menu menu = new Menu(title, 3);

        // Botón de confirmar (verde)
        MenuItem confirmButton = new MenuItem(TEXTURE_CONFIRM)
                .setName(COLOR_SUCCESS + "✓ Confirmar")
                .setLore(
                        COLOR_LETTERS + "Haz clic para confirmar",
                        COLOR_LETTERS + "esta acción"
                )
                .hideAllAttributes()
                .setClickHandler(clickInfo -> {
                    Player player = clickInfo.player();
                    player.closeInventory();

                    if (confirmAction != null) {
                        confirmAction.accept(player);
                    }

                    if (returnAction != null) {
                        // Volver al menú después de un pequeño delay
                        MenuManager.getPlugin().getServer().getScheduler()
                                .runTaskLater(MenuManager.getPlugin(), () -> returnAction.accept(player), 3L);
                    }
                });

        // Botón de cancelar (rojo)
        MenuItem cancelButton = new MenuItem(TEXTURE_CANCEL)
                .setName(COLOR_ERROR + "✗ Cancelar")
                .setLore(
                        COLOR_LETTERS + "Haz clic para cancelar",
                        COLOR_LETTERS + "esta acción"
                )
                .hideAllAttributes()
                .setClickHandler(clickInfo -> {
                    Player player = clickInfo.player();
                    player.closeInventory();

                    if (cancelAction != null) {
                        cancelAction.accept(player);
                    }

                    if (returnAction != null) {
                        // Volver al menú inmediatamente
                        MenuManager.getPlugin().getServer().getScheduler()
                                .runTaskLater(MenuManager.getPlugin(), () -> returnAction.accept(player), 1L);
                    }
                });

        // Filler decorativo
        MenuItem filler = new MenuItem("BLACK_STAINED_GLASS_PANE")
                .setName(" ")
                .hideAllAttributes();

        // Colocar items
        menu.setGlobalFiller(filler);
        menu.setItem(11, confirmButton);  // Slot izquierdo
        menu.setItem(15, cancelButton);   // Slot derecho

        return menu;
    }

    /**
     * Crea un menú de confirmación con mensaje personalizado
     * @param title Título del menú
     * @param message Mensaje a mostrar en el centro
     * @param confirmAction Acción a ejecutar si confirma
     * @param cancelAction Acción a ejecutar si cancela (opcional)
     * @param returnAction Acción para volver al menú anterior (opcional)
     * @return Menú de confirmación configurado
     */
    public static Menu createConfirmationMenuWithMessage(String title, String message,
                                                         Consumer<Player> confirmAction,
                                                         Consumer<Player> cancelAction,
                                                         Consumer<Player> returnAction) {
        Menu menu = createConfirmationMenu(title, confirmAction, cancelAction, returnAction);

        // Item informativo en el centro
        MenuItem messageItem = new MenuItem("PAPER")
                .setName(COLOR_INFO + "⚠ Confirmación")
                .setLore(COLOR_LETTERS + message)
                .hideAllAttributes()
                .setClickHandler(clickInfo -> {
                    // No hacer nada, solo informativo
                });

        menu.setItem(13, messageItem);  // Centro del menú

        return menu;
    }

    /**
     * Crea un menú de confirmación para eliminación con advertencias
     * @param title Título del menú
     * @param itemToDelete Nombre del item a eliminar
     * @param deleteAction Acción a ejecutar si confirma la eliminación
     * @param returnAction Acción para volver al menú anterior
     * @return Menú de confirmación de eliminación
     */
    public static Menu createDeleteConfirmationMenu(String title, String itemToDelete,
                                                    Consumer<Player> deleteAction, Consumer<Player> returnAction) {
        return createConfirmationMenuWithMessage(
                title,
                "¿Estás seguro de que quieres eliminar " + itemToDelete + "?|Esta acción no se puede deshacer.",
                player -> {
                    if (deleteAction != null) {
                        deleteAction.accept(player);
                    }
                    MessageUtils.sendMessageAsync(player, COLOR_SUCCESS + itemToDelete + " eliminado correctamente.");
                },
                player -> {
                    MessageUtils.sendMessageAsync(player, COLOR_INFO + "Eliminación cancelada.");
                },
                returnAction
        );
    }

    // ==================== ITEMS COMUNES ====================

    /**
     * Crea un item de eliminación estándar
     * @param itemName Nombre del item a eliminar
     * @param deleteAction Acción a ejecutar al confirmar eliminación
     * @param returnAction Acción para volver al menú después de la confirmación
     * @return Item de eliminación configurado
     */
    public static MenuItem createDeleteItem(String itemName, Consumer<Player> deleteAction, Consumer<Player> returnAction) {
        return new MenuItem(TEXTURE_DELETE)
                .setName(COLOR_ERROR + "🗑 Eliminar " + itemName)
                .setLore(
                        COLOR_LETTERS + "Haz clic para eliminar",
                        COLOR_LETTERS + "este elemento",
                        "",
                        COLOR_WARNING + "⚠ Esta acción es irreversible"
                )
                .hideAllAttributes()
                .setClickHandler(clickInfo -> {
                    Menu confirmMenu = createDeleteConfirmationMenu(
                            "Confirmar Eliminación",
                            itemName,
                            deleteAction,
                            returnAction
                    );
                    confirmMenu.open(clickInfo.player());
                });
    }

    /**
     * Crea un item de eliminación con condición
     * @param itemName Nombre del item a eliminar
     * @param deleteAction Acción a ejecutar al confirmar eliminación
     * @param returnAction Acción para volver al menú después de la confirmación
     * @param canDeleteCheck Función que determina si se puede eliminar
     * @param denyMessage Mensaje si no se puede eliminar
     * @return Item de eliminación configurado
     */
    public static MenuItem createConditionalDeleteItem(String itemName, Consumer<Player> deleteAction,
                                                       Consumer<Player> returnAction, Supplier<Boolean> canDeleteCheck,
                                                       String denyMessage) {
        return new MenuItem(TEXTURE_DELETE)
                .setName(COLOR_ERROR + "🗑 Eliminar " + itemName)
                .setLore(
                        COLOR_LETTERS + "Haz clic para eliminar",
                        COLOR_LETTERS + "este elemento",
                        "",
                        COLOR_WARNING + "⚠ Esta acción es irreversible"
                )
                .hideAllAttributes()
                .setClickHandler(clickInfo -> {
                    if (canDeleteCheck != null && !canDeleteCheck.get()) {
                        MessageUtils.sendMessageAsync(clickInfo.player(),
                                COLOR_ERROR + (denyMessage != null ? denyMessage : "No puedes eliminar este elemento."));
                        return;
                    }

                    Menu confirmMenu = createDeleteConfirmationMenu(
                            "Confirmar Eliminación",
                            itemName,
                            deleteAction,
                            returnAction
                    );
                    confirmMenu.open(clickInfo.player());
                });
    }

    /**
     * Crea un item de edición estándar
     * @param itemName Nombre del item a editar
     * @param editAction Acción a ejecutar al hacer clic
     * @return Item de edición configurado
     */
    public static MenuItem createEditItem(String itemName, Consumer<MenuClickInfo> editAction) {
        return new MenuItem(TEXTURE_EDIT)
                .setName(COLOR_PRIMARY + "✏ Editar " + itemName)
                .setLore(
                        COLOR_LETTERS + "Haz clic para editar",
                        COLOR_LETTERS + "este elemento"
                )
                .hideAllAttributes()
                .setClickHandler(editAction);
    }

    /**
     * Crea un item de información/detalles
     * @param itemName Nombre del item
     * @param infoLines Líneas de información a mostrar
     * @return Item de información configurado
     */
    public static MenuItem createInfoItem(String itemName, String... infoLines) {
        return new MenuItem(TEXTURE_INFO)
                .setName(COLOR_INFO + "ℹ " + itemName)
                .setLore(infoLines)
                .hideAllAttributes()
                .setClickHandler(clickInfo -> {
                    // Item informativo, no hace nada al hacer clic
                });
    }

    /**
     * Crea un item de guardar/aplicar cambios
     * @param saveAction Acción a ejecutar al guardar
     * @return Item de guardar configurado
     */
    public static MenuItem createSaveItem(Consumer<MenuClickInfo> saveAction) {
        return new MenuItem(TEXTURE_SAVE)
                .setName(COLOR_SUCCESS + "💾 Guardar Cambios")
                .setLore(
                        COLOR_LETTERS + "Haz clic para guardar",
                        COLOR_LETTERS + "todos los cambios"
                )
                .hideAllAttributes()
                .setClickHandler(clickInfo -> {
                    if (saveAction != null) {
                        saveAction.accept(clickInfo);
                    }
                    MessageUtils.sendMessageAsync(clickInfo.player(), COLOR_SUCCESS + "Cambios guardados correctamente.");
                });
    }

    /**
     * Crea un item de cancelar/descartar cambios
     * @param returnAction Acción para volver al menú anterior
     * @return Item de cancelar configurado
     */
    public static MenuItem createCancelItem(Consumer<Player> returnAction) {
        return new MenuItem(TEXTURE_CANCEL)
                .setName(COLOR_ERROR + "✗ Cancelar")
                .setLore(
                        COLOR_LETTERS + "Haz clic para cancelar",
                        COLOR_LETTERS + "y descartar cambios"
                )
                .hideAllAttributes()
                .setClickHandler(clickInfo -> {
                    MessageUtils.sendMessageAsync(clickInfo.player(), COLOR_INFO + "Cambios descartados.");
                    if (returnAction != null) {
                        clickInfo.player().closeInventory();
                        MenuManager.getPlugin().getServer().getScheduler()
                                .runTaskLater(MenuManager.getPlugin(), () -> returnAction.accept(clickInfo.player()), 1L);
                    } else {
                        clickInfo.player().closeInventory();
                    }
                });
    }

    /**
     * Crea un item de volver/regresar
     * @param returnAction Acción para volver al menú anterior
     * @return Item de volver configurado
     */
    public static MenuItem createBackItem(Consumer<Player> returnAction) {
        return new MenuItem(TEXTURE_BACK)
                .setName(COLOR_ERROR + "↓ Volver")
                .setLore(COLOR_LETTERS + "Haz clic para volver")
                .hideAllAttributes()
                .setClickHandler(clickInfo -> {
                    if (returnAction != null) {
                        returnAction.accept(clickInfo.player());
                    } else {
                        clickInfo.player().closeInventory();
                    }
                });
    }

    /**
     * Crea un item de cerrar menú
     * @return Item de cerrar configurado
     */
    public static MenuItem createCloseItem() {
        return new MenuItem(TEXTURE_CLOSE)
                .setName(COLOR_ERROR + "✕ Cerrar")
                .setLore(COLOR_LETTERS + "Haz clic para cerrar este menú")
                .hideAllAttributes()
                .setClickHandler(clickInfo -> clickInfo.player().closeInventory());
    }

    // ==================== MENÚS DE LISTA CON PAGINACIÓN ====================

    /**
     * Crea un menú paginado básico para mostrar una lista de items
     * @param title Título base del menú
     * @param items Lista de items a mostrar
     * @param itemSlots Slots donde colocar los items
     * @return Menú paginado configurado
     */
    public static PaginationMenu createListMenu(String title, List<MenuItem> items, int... itemSlots) {
        PaginationMenu menu = new PaginationMenu(title + " - Página %page%/%pages%", 6, itemSlots);
        menu.addItems(items);

        // Configurar filler
        MenuItem filler = new MenuItem("GRAY_STAINED_GLASS_PANE")
                .setName(" ")
                .hideAllAttributes();
        menu.setGlobalFiller(filler);

        return menu;
    }

    /**
     * Crea un menú paginado con botones de navegación personalizados
     * @param title Título base del menú
     * @param items Lista de items a mostrar
     * @param itemSlots Slots donde colocar los items
     * @param prevSlot Slot del botón anterior
     * @param nextSlot Slot del botón siguiente
     * @return Menú paginado configurado
     */
    public static PaginationMenu createListMenuCustomNav(String title, List<MenuItem> items,
                                                         int[] itemSlots, int prevSlot, int nextSlot) {
        PaginationMenu menu = createListMenu(title, items, itemSlots);

        // Botones de navegación personalizados
        MenuItem prevButton = new MenuItem(TEXTURE_PREVIOUS_PAGE)
                .setName(COLOR_SECONDARY + "← Página Anterior")
                .setLore(COLOR_LETTERS + "Haz clic para ir a la página anterior")
                .hideAllAttributes();

        MenuItem nextButton = new MenuItem(TEXTURE_NEXT_PAGE)
                .setName(COLOR_SECONDARY + "Página Siguiente →")
                .setLore(COLOR_LETTERS + "Haz clic para ir a la página siguiente")
                .hideAllAttributes();

        menu.setPreviousPageButton(prevButton, prevSlot);
        menu.setNextPageButton(nextButton, nextSlot);

        return menu;
    }

    // ==================== MENÚS DE SELECCIÓN ====================

    /**
     * Crea un menú de selección simple
     * @param title Título del menú
     * @param options Lista de opciones a mostrar
     * @param onSelect Callback cuando se selecciona una opción (recibe el índice)
     * @param returnAction Acción para volver al menú anterior después de seleccionar
     * @return Menú de selección configurado
     */
    public static Menu createSelectionMenu(String title, List<String> options,
                                           Consumer<Integer> onSelect, Consumer<Player> returnAction) {
        int rows = Math.min(6, Math.max(3, (int) Math.ceil((double) options.size() / 7) + 2));
        Menu menu = new Menu(title, rows);

        // Filler
        MenuItem filler = new MenuItem("LIGHT_GRAY_STAINED_GLASS_PANE")
                .setName(" ")
                .hideAllAttributes();
        menu.setGlobalFiller(filler);

        // Agregar opciones
        int slot = 10; // Empezar en el slot 10
        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            MenuItem optionItem = new MenuItem("PAPER")
                    .setName(COLOR_PRIMARY + options.get(i))
                    .setLore(COLOR_LETTERS + "Haz clic para seleccionar")
                    .hideAllAttributes()
                    .setClickHandler(clickInfo -> {
                        if (onSelect != null) {
                            onSelect.accept(index);
                        }
                        clickInfo.player().closeInventory();
                        if (returnAction != null) {
                            MenuManager.getPlugin().getServer().getScheduler()
                                    .runTaskLater(MenuManager.getPlugin(), () -> returnAction.accept(clickInfo.player()), 1L);
                        }
                    });

            menu.setItem(slot, optionItem);

            // Avanzar al siguiente slot válido
            slot++;
            if (slot % 9 == 8) { // Si llegamos al borde derecho
                slot += 2; // Saltar al inicio de la siguiente fila
            }
        }

        // Botón de volver si hay acción de retorno
        if (returnAction != null) {
            menu.setItem(rows * 9 - 5, createBackItem(returnAction));
        }

        return menu;
    }

    // ==================== UTILIDADES DE SLOTS ====================

    /**
     * Genera un array de slots para el centro de un menú (evitando bordes)
     * @param rows Número de filas del menú
     * @return Array de slots centrales
     */
    public static int[] getCenterSlots(int rows) {
        if (rows < 3) return new int[]{4}; // Para menús muy pequeños

        // Slots centrales evitando la primera y última fila, y primera y última columna
        int[] slots = new int[(rows - 2) * 7];
        int index = 0;

        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots[index++] = row * 9 + col;
            }
        }

        return slots;
    }

    /**
     * Genera slots para los bordes de un menú
     * @param rows Número de filas del menú
     * @return Array de slots de borde
     */
    public static int[] getBorderSlots(int rows) {
        int totalSlots = rows * 9;
        int borderCount = (rows * 2) + ((rows - 2) * 2);
        int[] slots = new int[borderCount];
        int index = 0;

        // Primera fila
        for (int i = 0; i < 9; i++) {
            slots[index++] = i;
        }

        // Última fila
        for (int i = totalSlots - 9; i < totalSlots; i++) {
            slots[index++] = i;
        }

        // Columnas laterales (sin incluir esquinas ya agregadas)
        for (int row = 1; row < rows - 1; row++) {
            slots[index++] = row * 9;      // Columna izquierda
            slots[index++] = row * 9 + 8;  // Columna derecha
        }

        return slots;
    }
}