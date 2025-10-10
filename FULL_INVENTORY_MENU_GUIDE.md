# Full Inventory Menu System - Guía Completa

## Descripción General

El **FullInventoryMenu** es un sistema de menús avanzado que permite controlar completamente el inventario del jugador, reemplazando los 36 slots inferiores (hotbar + inventario principal) con items personalizados del menú, todo esto con protección anti-dupes extremadamente robusta.

### Características Principales

✅ **Control Total del Inventario**: Reemplaza los 36 slots del inventario del jugador
✅ **Sistema de Snapshot**: Backup automático del inventario antes de abrir el menú
✅ **Restauración Automática**: Restaura el inventario original al cerrar
✅ **Protección Anti-Dupes**: Sistema completo de prevención de duplicación de items
✅ **Slots Editables**: Permite definir slots específicos que el jugador puede modificar
✅ **Compatible con Paginación**: Funciona con menús paginados y multi-paginados
✅ **Sin PacketEvents**: Todo usando API nativa de Bukkit/Paper

---

## Clases del Sistema

### 1. `InventorySnapshot`
Sistema de backup/restore del inventario del jugador.

**Métodos principales:**
- `capture(Player)` - Captura el estado actual del inventario
- `restore(Player)` - Restaura el inventario capturado
- `hasExpired(long)` - Verifica si el snapshot ha expirado

### 2. `FullInventoryMenu`
Menú base con control del inventario inferior.

**Métodos de configuración:**
```java
menu.setAllowPlayerInventoryInteraction(boolean)  // Permitir interacción
menu.setAllowHotbarSwap(boolean)                  // Permitir swap con hotbar
menu.setAllowDropItems(boolean)                   // Permitir tirar items
menu.setRestoreInventoryOnClose(boolean)          // Restaurar al cerrar
menu.setClearPlayerInventoryOnOpen(boolean)       // Limpiar al abrir
```

**Métodos para gestionar items del inventario:**
```java
menu.setPlayerSlotItem(int slot, MenuItem item)   // Establecer item en slot
menu.addEditablePlayerSlot(int slot)              // Agregar slot editable
menu.addEditablePlayerSlots(int... slots)         // Agregar múltiples slots
menu.clearPlayerSlotItems()                       // Limpiar todos los items
```

### 3. `PaginatedFullInventoryMenu`
Versión paginada del FullInventoryMenu.

### 4. `MultiPaginatedFullInventoryMenu`
Versión multi-paginada del FullInventoryMenu.

### 5. `FullInventoryMenuBuilder`
Builder para crear menús fácilmente.

---

## Ejemplos de Uso

### Ejemplo 1: Menú Básico Sin Interacción

```java
FullInventoryMenu menu = new FullInventoryMenu("§6Mi Menú", 3);

// Items en el menú superior (slots 0-26)
menu.setItem(13, new MenuItem(Material.DIAMOND)
    .setName("§bDiamante")
    .setLore("§7Click para recibir"));

// Items en el inventario del jugador (slots 0-35)
menu.setPlayerSlotItem(0, new MenuItem(Material.IRON_SWORD)
    .setName("§cEspada de Hierro")
    .setLore("§7Este item no se puede mover"));

// Configuración: NO permitir interacción
menu.setAllowPlayerInventoryInteraction(false);
menu.setAllowDropItems(false);
menu.setRestoreInventoryOnClose(true);

menu.open(player);
```

### Ejemplo 2: Menú con Slots Editables (Tipo Mochila)

```java
FullInventoryMenu backpackMenu = new FullInventoryMenu("§6Mochila", 3);

// Items decorativos en el menú superior
backpackMenu.setGlobalFiller(new MenuItem(Material.BLACK_STAINED_GLASS_PANE)
    .setName(" "));

// Slots editables en el inventario inferior (simular una mochila de 27 slots)
backpackMenu.addEditablePlayerSlotRange(0, 26);

// Permitir interacción SOLO en slots editables
backpackMenu.setAllowPlayerInventoryInteraction(true);
backpackMenu.setAllowDropItems(false);  // NO permitir tirar items

// Obtener items guardados al cerrar
backpackMenu.setCloseHandler(p -> {
    Map<Integer, ItemStack> items = backpackMenu.getEditablePlayerItems();
    // Guardar items en base de datos, archivo, etc.
    saveBackpackItems(p, items);
});

backpackMenu.open(player);
```

### Ejemplo 3: Usando el Builder

```java
FullInventoryMenu menu = FullInventoryMenuBuilder.create("§aTienda", 4)
    // Items del menú superior
    .setItem(10, new MenuItem(Material.DIAMOND).setName("§bDiamante - 100$"))
    .setItem(11, new MenuItem(Material.EMERALD).setName("§aEsmeralda - 50$"))

    // Items en el inventario del jugador
    .setPlayerItem(0, new MenuItem(Material.BARRIER).setName("§cSlot Bloqueado"))
    .setPlayerItem(8, new MenuItem(Material.BARRIER).setName("§cSlot Bloqueado"))

    // Configuración de seguridad
    .disableAllPlayerInteraction()
    .restoreInventoryOnClose(true)
    .clearPlayerInventoryOnOpen(true)

    // Fillers decorativos
    .setGlobalFiller(new MenuItem(Material.GRAY_STAINED_GLASS_PANE).setName(" "))

    .build();

menu.open(player);
```

### Ejemplo 4: Menú Paginado con Inventario

```java
PaginatedFullInventoryMenu paginatedMenu = new PaginatedFullInventoryMenu(
    "§6Tienda - Página {page}/{pages}",
    4,
    "10-16,19-25"  // Slots para items paginados
);

// Agregar items a paginar
for (int i = 0; i < 50; i++) {
    paginatedMenu.addItem(new MenuItem(Material.values()[i])
        .setName("§eItem #" + i)
        .setClickHandler(event -> {
            event.getPlayer().sendMessage("§aCompraste el item!");
        }));
}

// Items fijos en el inventario del jugador
for (int i = 0; i < 9; i++) {
    paginatedMenu.setPlayerSlotItem(i, new MenuItem(Material.BARRIER)
        .setName("§cHotbar Bloqueada"));
}

// Configuración
paginatedMenu.setAllowPlayerInventoryInteraction(false);
paginatedMenu.setRestoreInventoryOnClose(true);

paginatedMenu.open(player);
```

### Ejemplo 5: Sistema de Crafting Personalizado

```java
FullInventoryMenu craftingMenu = new FullInventoryMenu("§6Crafting Avanzado", 3);

// Grid de crafting 3x3 en el menú superior
int[] craftingSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
for (int slot : craftingSlots) {
    craftingMenu.addEditableSlot(slot);  // Slots editables en menú superior
}

// Slot de resultado
craftingMenu.setItem(24, new MenuItem(Material.LIME_STAINED_GLASS_PANE)
    .setName("§aResultado")
    .setLore("§7Coloca items en el grid"));

// Bloquear inventario del jugador completamente
craftingMenu.setAllowPlayerInventoryInteraction(false);
craftingMenu.setAllowDropItems(false);

// Lógica de crafting
craftingMenu.setGlobalClickHandler(event -> {
    // Verificar pattern de crafting
    checkCraftingPattern(craftingMenu);
});

craftingMenu.open(player);
```

### Ejemplo 6: Multi-Paginado con Inventario

```java
MultiPaginatedFullInventoryMenu multiMenu = new MultiPaginatedFullInventoryMenu(
    "§6Categorías",
    6
);

// Sección 1: Armas
var weaponsSection = multiMenu.addSection("weapons", 10, 11, 12, 13, 14, 15, 16);
weaponsSection.addItem(new MenuItem(Material.DIAMOND_SWORD).setName("§cEspada"));
weaponsSection.addItem(new MenuItem(Material.BOW).setName("§cArco"));

// Sección 2: Armaduras
var armorSection = multiMenu.addSection("armor", 19, 20, 21, 22, 23, 24, 25);
armorSection.addItem(new MenuItem(Material.DIAMOND_HELMET).setName("§9Casco"));

// Items en el inventario del jugador
for (int i = 0; i < 36; i++) {
    multiMenu.setPlayerSlotItem(i, new MenuItem(Material.BLACK_STAINED_GLASS_PANE)
        .setName(" "));
}

multiMenu.setAllowPlayerInventoryInteraction(false);
multiMenu.open(player);
```

---

## Protecciones Anti-Dupes Implementadas

### 1. Snapshot del Inventario
Al abrir el menú, se crea una copia exacta del inventario:
- Storage contents (36 slots)
- Armor contents (4 slots)
- Extra contents (offhand)
- Held item slot

### 2. Validaciones en Clicks
- **Clicks en slots no editables**: Cancelados completamente
- **Hotbar swap (tecla número)**: Cancelado si `allowHotbarSwap = false`
- **Shift-click**: Bloqueado entre inventarios
- **Drag events**: Validados y cancelados según configuración

### 3. Validaciones en Drops
- **PlayerDropItemEvent**: Cancelado si `allowDropItems = false`
- Previene que items del menú sean tirados fuera

### 4. Restauración al Cerrar
- **Restauración automática**: Inventario restaurado al cerrar
- **Limpieza de snapshots**: Eliminados al salir del servidor
- **Limpieza de snapshots expirados**: Automatic cleanup de snapshots antiguos

### 5. Sincronización de Items Editables
- **Sync delayed**: Sincronización con 1 tick de delay
- **Clonación de items**: Todos los items son clonados
- **Estado consistente**: El estado interno siempre refleja el inventario real

---

## Configuraciones Recomendadas por Caso de Uso

### Tienda (Shop)
```java
menu.setAllowPlayerInventoryInteraction(false);
menu.setAllowDropItems(false);
menu.setRestoreInventoryOnClose(true);
menu.setClearPlayerInventoryOnOpen(true);
```

### Mochila/Almacenamiento
```java
menu.setAllowPlayerInventoryInteraction(true);
menu.setAllowDropItems(false);  // O true si permites tirar
menu.setRestoreInventoryOnClose(false);  // Los cambios se guardan
menu.addEditablePlayerSlotRange(0, 26);
```

### Sistema de Crafting
```java
menu.setAllowPlayerInventoryInteraction(false);
menu.setAllowDropItems(false);
menu.setRestoreInventoryOnClose(true);
// Slots editables solo en el grid de crafting
```

### Inventario Personalizado (RPG)
```java
menu.setAllowPlayerInventoryInteraction(true);
menu.setAllowHotbarSwap(true);
menu.setAllowDropItems(true);
menu.setRestoreInventoryOnClose(false);
```

---

## Métodos Estáticos Útiles

```java
// Forzar restauración de inventario (útil para comandos de admin)
FullInventoryMenu.forceRestoreInventory(player);

// Limpiar snapshot manualmente
FullInventoryMenu.clearSnapshot(player);

// Verificar si un jugador tiene snapshot
boolean hasSnapshot = FullInventoryMenu.hasSnapshot(player);

// Limpiar snapshots expirados (llamar periódicamente)
FullInventoryMenu.cleanupExpiredSnapshots();
```

---

## Consideraciones de Performance

1. **Snapshots en memoria**: Los snapshots son guardados en un `ConcurrentHashMap` estático
2. **Limpieza automática**: Snapshots se eliminan al cerrar el menú o al salir del servidor
3. **Limpieza periódica**: Llamar `cleanupExpiredSnapshots()` cada 5 minutos desde un scheduler
4. **Clonación de items**: Todos los items son clonados para evitar referencias mutables

---

## Troubleshooting

### Items se duplican al cerrar
**Causa**: `restoreInventoryOnClose = false` cuando debería ser `true`
**Solución**: Configurar correctamente la restauración

### Items desaparecen
**Causa**: Snapshot no creado correctamente
**Solución**: Verificar que el menú hereda de `FullInventoryMenu`

### Clicks no funcionan
**Causa**: MenuManager no actualizado
**Solución**: Verificar que MenuManager tiene el handler `handleFullInventoryMenuClick`

### Hotbar swap funciona cuando no debería
**Causa**: `allowHotbarSwap = true`
**Solución**: Configurar `menu.setAllowHotbarSwap(false)`

---

## Arquitectura Técnica

```
FullInventoryMenu
├── InventorySnapshot (backup del inventario)
├── PlayerInventoryItems (items del menú en slots 0-35)
├── EditablePlayerSlots (slots que el jugador puede modificar)
└── Configuración de seguridad
    ├── allowPlayerInventoryInteraction
    ├── allowHotbarSwap
    ├── allowDropItems
    ├── restoreInventoryOnClose
    └── clearPlayerInventoryOnOpen
```

**Flow del Sistema:**
1. Player abre menú → `InventorySnapshot.capture()`
2. Inventario limpiado (si `clearPlayerInventoryOnOpen = true`)
3. Items del menú colocados en inventario inferior
4. Clicks validados según configuración
5. Player cierra menú → `InventorySnapshot.restore()` (si `restoreInventoryOnClose = true`)
6. Snapshot eliminado de memoria

---

## Compatibilidad

- ✅ Menús simples (`Menu`)
- ✅ Menús paginados (`PaginationMenu`)
- ✅ Menús multi-paginados (`MultiPaginationMenu`)
- ✅ Menús editables (`EditableMenu`)
- ✅ Paper 1.20+
- ✅ Spigot 1.20+ (con limitaciones en Adventure components)

---

## Contribución al CLAUDE.md

Agregar esta sección al archivo `CLAUDE.md` del proyecto:

```markdown
#### 13. Full Inventory Menu System (`ui/menus/`)
**Menús con control completo del inventario del jugador**

- **FullInventoryMenu**: Menú que controla los 36 slots inferiores del inventario
- **PaginatedFullInventoryMenu**: Versión paginada con inventario personalizado
- **MultiPaginatedFullInventoryMenu**: Multi-paginación con inventario
- **InventorySnapshot**: Sistema de backup/restore anti-dupes
- **FullInventoryMenuBuilder**: Builder para creación simplificada

**Key Features:**
- Snapshot automático del inventario antes de abrir
- Restauración automática al cerrar
- Protección anti-dupes completa
- Slots editables configurables
- Sin dependencia de PacketEvents

**Usage:**
```java
FullInventoryMenu menu = FullInventoryMenuBuilder.create("Shop", 3)
    .setPlayerItem(0, new MenuItem(Material.DIAMOND))
    .disableAllPlayerInteraction()
    .restoreInventoryOnClose(true)
    .build();

menu.open(player);
```
```

---

**Sistema creado usando:**
- ✅ API nativa de Bukkit/Paper
- ✅ Sin PacketEvents
- ✅ Arquitectura basada en snapshots
- ✅ Compatible con todo el sistema de menús existente
