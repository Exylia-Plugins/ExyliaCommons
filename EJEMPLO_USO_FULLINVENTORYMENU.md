# Ejemplos Prácticos de FullInventoryMenu

## Ejemplo Completo 1: Sistema de Tienda con Categorías

```java
import net.exylia.commons.ui.menus.FullInventoryMenu;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo jugadores pueden usar este comando!");
            return true;
        }

        openShopMenu(player);
        return true;
    }

    private void openShopMenu(Player player) {
        FullInventoryMenu shopMenu = new FullInventoryMenu("§6§lTIENDA PREMIUM", 6);

        // ==================== MENÚ SUPERIOR ====================
        // Items de la tienda (slots 0-53)

        // Categoría: Bloques
        shopMenu.setItem(10, new MenuItem(Material.STONE)
            .setName("§fPiedra")
            .setLore("§7Precio: §a$10", "§7Click para comprar")
            .setClickHandler(event -> buyItem(player, "stone", 10)));

        shopMenu.setItem(11, new MenuItem(Material.OAK_LOG)
            .setName("§fMadera de Roble")
            .setLore("§7Precio: §a$15", "§7Click para comprar")
            .setClickHandler(event -> buyItem(player, "oak_log", 15)));

        // Categoría: Herramientas
        shopMenu.setItem(19, new MenuItem(Material.DIAMOND_PICKAXE)
            .setName("§bPico de Diamante")
            .setLore("§7Precio: §a$500", "§7Click para comprar")
            .setClickHandler(event -> buyItem(player, "diamond_pickaxe", 500)));

        // Categoría: Alimentos
        shopMenu.setItem(28, new MenuItem(Material.COOKED_BEEF)
            .setName("§6Carne Cocinada")
            .setLore("§7Precio: §a$5", "§7Click para comprar x64")
            .setClickHandler(event -> buyItem(player, "cooked_beef", 5)));

        // ==================== INVENTARIO INFERIOR ====================
        // Bloquear completamente el inventario del jugador

        // Decoración: Paneles negros en la hotbar (slots 0-8)
        for (int i = 0; i < 9; i++) {
            shopMenu.setPlayerSlotItem(i, new MenuItem(Material.BLACK_STAINED_GLASS_PANE)
                .setName("§7Hotbar Bloqueada")
                .hideAllAttributes());
        }

        // Información del jugador en el inventario (slots 9-35)
        shopMenu.setPlayerSlotItem(13, new MenuItem(Material.PLAYER_HEAD)
            .setName("§e" + player.getName())
            .setLore(
                "§7Balance: §a$" + getPlayerMoney(player),
                "§7",
                "§7Compras: §e" + getPlayerPurchases(player)
            ));

        // Botón de salir
        shopMenu.setPlayerSlotItem(31, new MenuItem(Material.BARRIER)
            .setName("§cCerrar Tienda")
            .setLore("§7Click para cerrar")
            .setClickHandler(event -> {
                player.closeInventory();
                player.sendMessage("§a¡Gracias por visitar la tienda!");
            }));

        // ==================== CONFIGURACIÓN DE SEGURIDAD ====================
        shopMenu.setAllowPlayerInventoryInteraction(false);  // NO permitir clicks en inventario
        shopMenu.setAllowHotbarSwap(false);                  // NO permitir swap con números
        shopMenu.setAllowDropItems(false);                   // NO permitir tirar items
        shopMenu.setRestoreInventoryOnClose(true);           // Restaurar inventario al cerrar
        shopMenu.setClearPlayerInventoryOnOpen(true);        // Limpiar inventario al abrir

        // Filler decorativo
        shopMenu.setGlobalFiller(new MenuItem(Material.GRAY_STAINED_GLASS_PANE)
            .setName(" ")
            .hideAllAttributes());

        shopMenu.open(player);
    }

    private void buyItem(Player player, String itemType, int price) {
        if (getPlayerMoney(player) >= price) {
            // Lógica de compra
            removePlayerMoney(player, price);
            givePlayerItem(player, itemType);
            player.sendMessage("§a¡Compraste " + itemType + " por $" + price + "!");
            player.closeInventory();
        } else {
            player.sendMessage("§c¡No tienes suficiente dinero!");
            player.playSound(player.getLocation(), "entity.villager.no", 1, 1);
        }
    }

    private double getPlayerMoney(Player player) { return 1000; }
    private int getPlayerPurchases(Player player) { return 0; }
    private void removePlayerMoney(Player player, int amount) { }
    private void givePlayerItem(Player player, String itemType) { }
}
```

---

## Ejemplo Completo 2: Sistema de Mochila Editable

```java
import net.exylia.commons.ui.menus.FullInventoryMenu;
import net.exylia.commons.ui.builders.FullInventoryMenuBuilder;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class BackpackSystem {

    public void openBackpack(Player player, int backpackSize) {
        FullInventoryMenu backpack = FullInventoryMenuBuilder.create("§6§lMOCHILA", 6)
            // ==================== MENÚ SUPERIOR ====================
            // Título decorativo
            .setItem(4, new MenuItem(Material.CHEST)
                .setName("§6§lTu Mochila")
                .setLore(
                    "§7Tamaño: §e" + backpackSize + " slots",
                    "§7Guardado automáticamente"
                ))

            // Filler decorativo en el menú superior
            .setGlobalFiller(new MenuItem(Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .hideAllAttributes())

            .build();

        // ==================== INVENTARIO INFERIOR ====================
        // Los primeros 'backpackSize' slots son editables (la mochila)
        for (int i = 0; i < Math.min(backpackSize, 36); i++) {
            backpack.addEditablePlayerSlot(i);
        }

        // Slots restantes bloqueados con paneles rojos
        for (int i = backpackSize; i < 36; i++) {
            backpack.setPlayerSlotItem(i, new MenuItem(Material.RED_STAINED_GLASS_PANE)
                .setName("§cSlot Bloqueado")
                .setLore("§7Mejora tu mochila", "§7para desbloquear")
                .hideAllAttributes());
        }

        // Cargar items guardados de la mochila
        Map<Integer, ItemStack> savedItems = loadBackpackItems(player);
        savedItems.forEach((slot, item) -> {
            if (slot < backpackSize) {
                backpack.setEditablePlayerItem(slot, item);
            }
        });

        // ==================== CONFIGURACIÓN ====================
        backpack.setAllowPlayerInventoryInteraction(true);   // Permitir interacción en slots editables
        backpack.setAllowDropItems(false);                   // NO permitir tirar items (seguridad)
        backpack.setRestoreInventoryOnClose(false);          // NO restaurar (queremos guardar cambios)

        // Guardar al cerrar
        backpack.setCloseHandler(p -> {
            Map<Integer, ItemStack> items = backpack.getEditablePlayerItems();
            saveBackpackItems(p, items);
            p.sendMessage("§a¡Mochila guardada!");
        });

        backpack.open(player);
    }

    private Map<Integer, ItemStack> loadBackpackItems(Player player) {
        // Cargar desde base de datos
        return Map.of();
    }

    private void saveBackpackItems(Player player, Map<Integer, ItemStack> items) {
        // Guardar en base de datos
        // DatabaseManager.getInstance().getRepository(BackpackEntity.class)...
    }
}
```

---

## Ejemplo Completo 3: Sistema de Crafting Personalizado

```java
import net.exylia.commons.ui.menus.FullInventoryMenu;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomCraftingSystem {

    public void openCraftingTable(Player player) {
        FullInventoryMenu craftingMenu = new FullInventoryMenu("§3§lMesa de Crafteo", 5);

        // ==================== MENÚ SUPERIOR ====================
        // Grid de crafting 3x3
        int[] craftingGrid = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int slot : craftingGrid) {
            craftingMenu.setItem(slot, new MenuItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName("§7Slot de Crafting")
                .hideAllAttributes());
        }

        // Flecha (decoración)
        craftingMenu.setItem(23, new MenuItem(Material.ARROW)
            .setName("§aResultado →")
            .hideAllAttributes());

        // Slot de resultado
        craftingMenu.setItem(24, new MenuItem(Material.LIME_STAINED_GLASS_PANE)
            .setName("§aResultado")
            .setLore("§7Coloca items en el grid")
            .hideAllAttributes());

        // Botón de craftear
        craftingMenu.setItem(34, new MenuItem(Material.CRAFTING_TABLE)
            .setName("§a§lCRAFTEAR")
            .setLore("§7Click para craftear")
            .setClickHandler(event -> {
                attemptCrafting(player, craftingMenu);
            }));

        // ==================== INVENTARIO INFERIOR ====================
        // Bloquear completamente
        for (int i = 0; i < 36; i++) {
            craftingMenu.setPlayerSlotItem(i, new MenuItem(Material.BLACK_STAINED_GLASS_PANE)
                .setName("§7Inventario Bloqueado")
                .setLore("§7Usa el grid superior")
                .hideAllAttributes());
        }

        // ==================== CONFIGURACIÓN ====================
        craftingMenu.setAllowPlayerInventoryInteraction(false);
        craftingMenu.setAllowDropItems(false);
        craftingMenu.setRestoreInventoryOnClose(true);

        // Filler
        craftingMenu.setGlobalFiller(new MenuItem(Material.GRAY_STAINED_GLASS_PANE)
            .setName(" ")
            .hideAllAttributes());

        craftingMenu.open(player);
    }

    private void attemptCrafting(Player player, FullInventoryMenu menu) {
        // Lógica de verificación de pattern de crafting
        // ...
        player.sendMessage("§a¡Crafteado exitosamente!");
    }
}
```

---

## Ejemplo Completo 4: Sistema de Inventario RPG

```java
import net.exylia.commons.ui.menus.FullInventoryMenu;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class RPGInventorySystem {

    public void openRPGInventory(Player player) {
        FullInventoryMenu rpgMenu = new FullInventoryMenu("§5§lINVENTARIO RPG", 6);

        // ==================== MENÚ SUPERIOR ====================
        // Stats del jugador
        rpgMenu.setItem(4, new MenuItem("playerhead-" + player.getName())
            .setName("§e" + player.getName())
            .setLore(
                "§7Nivel: §a" + getPlayerLevel(player),
                "§7HP: §c" + getPlayerHP(player),
                "§7Mana: §9" + getPlayerMana(player),
                "§7Fuerza: §e" + getPlayerStrength(player)
            ));

        // Slots de equipo especial (casco, pechera, etc.)
        rpgMenu.setItem(10, new MenuItem(Material.DIAMOND_HELMET)
            .setName("§bCasco Especial")
            .setLore("§7Drag & drop un casco aquí"));

        rpgMenu.setItem(19, new MenuItem(Material.DIAMOND_CHESTPLATE)
            .setName("§bPechera Especial")
            .setLore("§7Drag & drop una pechera aquí"));

        // Habilidades
        rpgMenu.setItem(15, new MenuItem(Material.ENCHANTED_BOOK)
            .setName("§dHabilidad: Fireball")
            .setLore("§7Mana: §915", "§7Click para usar")
            .setClickHandler(event -> {
                useAbility(player, "fireball");
            }));

        // ==================== INVENTARIO INFERIOR ====================
        // Hotbar con atajos
        rpgMenu.setPlayerSlotItem(0, new MenuItem(Material.DIAMOND_SWORD)
            .setName("§c[1] Espada Principal")
            .setClickHandler(event -> equipWeapon(player, "sword")));

        rpgMenu.setPlayerSlotItem(1, new MenuItem(Material.BOW)
            .setName("§c[2] Arco")
            .setClickHandler(event -> equipWeapon(player, "bow")));

        rpgMenu.setPlayerSlotItem(8, new MenuItem(Material.POTION)
            .setName("§c[9] Poción de Vida")
            .setClickHandler(event -> usePotion(player)));

        // Resto del inventario editable (mochila)
        for (int i = 9; i < 36; i++) {
            rpgMenu.addEditablePlayerSlot(i);
        }

        // ==================== CONFIGURACIÓN ====================
        rpgMenu.setAllowPlayerInventoryInteraction(true);  // Permitir gestionar items
        rpgMenu.setAllowHotbarSwap(true);                  // Permitir swap entre slots
        rpgMenu.setAllowDropItems(true);                   // Permitir tirar items
        rpgMenu.setRestoreInventoryOnClose(false);         // Guardar cambios

        // Guardar al cerrar
        rpgMenu.setCloseHandler(p -> {
            saveRPGInventory(p, rpgMenu.getEditablePlayerItems());
        });

        rpgMenu.open(player);
    }

    private int getPlayerLevel(Player player) { return 50; }
    private int getPlayerHP(Player player) { return 100; }
    private int getPlayerMana(Player player) { return 80; }
    private int getPlayerStrength(Player player) { return 25; }
    private void useAbility(Player player, String ability) { }
    private void equipWeapon(Player player, String weapon) { }
    private void usePotion(Player player) { }
    private void saveRPGInventory(Player player, Object items) { }
}
```

---

## Ejemplo Completo 5: Menu Paginado con Inventario (Catálogo de Items)

```java
import net.exylia.commons.ui.menus.PaginatedFullInventoryMenu;
import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ItemCatalogSystem {

    public void openCatalog(Player player) {
        PaginatedFullInventoryMenu catalog = new PaginatedFullInventoryMenu(
            "§6Catálogo - Página {page}/{pages}",
            6,
            "10-16,19-25,28-34"  // Slots para items paginados (21 slots)
        );

        // ==================== ITEMS PAGINADOS ====================
        // Agregar todos los materiales al catálogo
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir()) {
                catalog.addItem(new MenuItem(material)
                    .setName("§e" + material.name())
                    .setLore(
                        "§7Click para obtener 1x",
                        "§7Shift+Click para obtener 64x"
                    )
                    .setClickHandler(event -> {
                        int amount = event.getClick().isShiftClick() ? 64 : 1;
                        giveItem(player, material, amount);
                        player.sendMessage("§aRecibiste " + amount + "x " + material.name());
                    }));
            }
        }

        // ==================== INVENTARIO INFERIOR ====================
        // Barra de búsqueda (ficticia)
        catalog.setPlayerSlotItem(4, new MenuItem(Material.COMPASS)
            .setName("§eBúsqueda")
            .setLore("§7Próximamente...")
            .hideAllAttributes());

        // Categorías
        catalog.setPlayerSlotItem(0, new MenuItem(Material.GRASS_BLOCK)
            .setName("§aFiltrar: Bloques")
            .setClickHandler(event -> filterCategory(player, "blocks")));

        catalog.setPlayerSlotItem(1, new MenuItem(Material.DIAMOND_SWORD)
            .setName("§cFiltrar: Herramientas")
            .setClickHandler(event -> filterCategory(player, "tools")));

        catalog.setPlayerSlotItem(2, new MenuItem(Material.APPLE)
            .setName("§6Filtrar: Comida")
            .setClickHandler(event -> filterCategory(player, "food")));

        // Resto bloqueado
        for (int i = 5; i < 36; i++) {
            if (i != 4) {  // Excepto la búsqueda
                catalog.setPlayerSlotItem(i, new MenuItem(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(" ")
                    .hideAllAttributes());
            }
        }

        // ==================== CONFIGURACIÓN ====================
        catalog.setAllowPlayerInventoryInteraction(false);
        catalog.setAllowDropItems(false);
        catalog.setRestoreInventoryOnClose(true);

        // Botones de navegación personalizados
        catalog.setPreviousButton(
            new MenuItem(Material.RED_STAINED_GLASS_PANE)
                .setName("§c◀ Página Anterior"),
            45  // Esquina inferior izquierda
        );

        catalog.setNextButton(
            new MenuItem(Material.GREEN_STAINED_GLASS_PANE)
                .setName("§a▶ Siguiente Página"),
            53  // Esquina inferior derecha
        );

        catalog.open(player);
    }

    private void giveItem(Player player, Material material, int amount) {
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(material, amount));
    }

    private void filterCategory(Player player, String category) {
        player.sendMessage("§eFiltrando por categoría: " + category);
        // Reabrir menú con filtro aplicado
    }
}
```

---

## Resumen de Casos de Uso

| Caso de Uso | allowPlayerInventoryInteraction | allowDropItems | restoreInventoryOnClose | editableSlots |
|-------------|----------------------------------|----------------|-------------------------|---------------|
| **Tienda** | `false` | `false` | `true` | Ninguno |
| **Mochila** | `true` | `false` | `false` | Todos los de mochila |
| **Crafting** | `false` | `false` | `true` | Ninguno |
| **Inventario RPG** | `true` | `true` | `false` | Items normales |
| **Catálogo** | `false` | `false` | `true` | Ninguno |

---

## Tips de Desarrollo

1. **Siempre usa el Builder** cuando sea posible para código más limpio
2. **Clona los items** antes de pasarlos al menú para evitar modificaciones accidentales
3. **Configura closeHandler** si necesitas guardar datos al cerrar
4. **Usa ExyliaContext** para pasar datos dinámicos a los items
5. **Limpia snapshots** periódicamente con `FullInventoryMenu.cleanupExpiredSnapshots()`
6. **Prueba edge cases**: Drop items, hotbar swap, shift-click, drag, etc.
7. **No olvides** configurar las flags de seguridad según tu caso de uso

---

¡Sistema completo y listo para usar!
