# 📚 Guía Completa: Sistema UI v2

## 📋 Índice

1. [Tipos de Menús](#tipos-de-menús)
2. [Componentes Principales](#componentes-principales)
3. [Fillers (Rellenos)](#fillers-rellenos)
4. [Slots](#slots)
5. [Items](#items)
6. [Click Actions](#click-actions)
7. [Placeholders](#placeholders)
8. [Casos de Uso](#casos-de-uso)

---

## 🎯 Tipos de Menús

### 1. SIMPLE
**Archivo:** `01_simple_complete.yml`

Menú básico sin paginación. Items en posiciones fijas.

```yaml
type: SIMPLE
size: 54
items:
  item1:
    material: DIAMOND
    slot: 13
```

**Usos:** Menús principales, opciones de configuración, selección de categorías.

---

### 2. PAGINATION
**Archivo:** `02_pagination_complete.yml`

Menú con paginación automática. Los items se distribuyen en páginas.

```yaml
type: PAGINATION
pagination:
  slots: '10-16,19-25,28-34'
  items:
    - material: DIAMOND
      name: 'Item #%index%'
```

**Usos:** Tiendas, listados de items, catálogos.

---

### 3. MULTI_PAGINATION
**Archivo:** `03_multi_pagination_complete.yml`

Múltiples secciones paginadas independientes.

```yaml
type: MULTI_PAGINATION
sections:
  weapons:
    slots: '10-12,19-21'
    items: [...]
  armor:
    slots: '14-16,23-25'
    items: [...]
```

**Usos:** Tiendas con categorías, sistemas de selección múltiple.

---

### 4. FULL_INVENTORY
**Archivo:** `04_full_inventory_complete.yml`

Menú con acceso al inventario del jugador + snapshot.

```yaml
type: FULL_INVENTORY
snapshot:
  enabled: true
  restore_on_close: true
player_inventory:
  enabled: true
  allowed_slots: '0-35'
```

**Usos:** Gestores de inventario, editores, sistemas de mochila.

---

### 5. PAGINATION_FULL
**Archivo:** `05_pagination_full_complete.yml`

Combina paginación + acceso al inventario.

```yaml
type: PAGINATION_FULL
pagination:
  slots: '0-26'  # Parte superior
player_inventory:
  enabled: true
  allowed_slots: '0-35'  # Inventario del jugador
```

**Usos:** Bancos, almacenes con páginas, sistemas de vault.

---

### 6. MULTI_PAGINATION_FULL
**Archivo:** `06_multi_pagination_full_complete.yml`

**EL MÁS COMPLETO:** Múltiples secciones + inventario + snapshot.

```yaml
type: MULTI_PAGINATION_FULL
sections:
  storage: { slots: '0-8', items: [...] }
  crafting: { slots: '9-17', items: [...] }
player_inventory:
  enabled: true
```

**Usos:** Sistemas complejos de almacenamiento, crafting avanzado, RPG systems.

---

## 🧩 Componentes Principales

### Estructura Base

```yaml
title: '&6Mi Menú'
type: SIMPLE  # SIMPLE | PAGINATION | MULTI_PAGINATION | FULL_INVENTORY | PAGINATION_FULL | MULTI_PAGINATION_FULL
size: 54      # 9, 18, 27, 36, 45, 54

refresh:
  mode: SMART      # DISABLED | FULL | SMART | SLOT_ONLY
  interval: 20     # Ticks (20 = 1 segundo)

filler:
  global: { material: GRAY_STAINED_GLASS_PANE }
  border: { material: BLACK_STAINED_GLASS_PANE }

items:
  item1: { ... }
```

---

## 🎨 Fillers (Rellenos)

### ⚠️ IMPORTANTE: Solo hay 2 tipos de filler

```yaml
filler:
  # GLOBAL: Rellena TODOS los slots vacíos
  global:
    material: GRAY_STAINED_GLASS_PANE
    name: ' '
    hide_attributes: true

  # BORDER: SOLO los bordes (sobrescribe global)
  border:
    material: BLACK_STAINED_GLASS_PANE
    name: ' '
    glowing: true
```

**¿Bordes?**
- Primera fila
- Última fila
- Primera columna (slot % 9 == 0)
- Última columna (slot % 9 == 8)

### ❌ NO hay más opciones de filler

**Para patrones custom:**
```yaml
# Usa items normales en slots específicos
items:
  line_top:
    material: RED_STAINED_GLASS_PANE
    slots: [9, 18, 27, 36]  # Columna personalizada
```

---

## 📍 Slots

### Opciones de Slot

```yaml
# 1. Slot único
slot: 13

# 2. Lista de slots
slots: [10, 11, 12, 13]

# 3. Rango
slots: '10-16'

# 4. Mixto
slots: '10-16,19-25,28-34'

# 5. Con comas
slots: '10,12,14,16'
```

### Slots del Inventario del Jugador

```
0-8    = Hotbar
9-35   = Inventario principal
36-39  = Armadura
40     = Offhand
```

---

## 🎁 Items

### Todas las Propiedades

```yaml
items:
  complete_item:
    # BÁSICO
    material: DIAMOND_SWORD
    amount: '1'
    name: '&cNombre'
    display_name: '&cAlternativa a name'
    lore:
      - '&7Línea 1'
      - '&7Línea 2'

    # SLOT
    slot: 13
    # O: slots: [10, 11, 12]
    # O: slots: '10-16,19-25'

    # ENCHANTMENTS
    enchantments:
      DAMAGE_ALL: 5
      FIRE_ASPECT: 2
      MENDING: 1

    # FLAGS
    glowing: true
    hide_attributes: true
    unbreakable: true
    max_stack_size: 1

    # CUSTOM MODEL
    item_model: 1001

    # NBT CUSTOM
    custom_nbt:
      'custom_id': 'sword_1'
      'damage': '100'

    # ATRIBUTOS
    attributes:
      - 'GENERIC_ATTACK_DAMAGE:10:ADD_NUMBER'
      - 'GENERIC_ATTACK_SPEED:0.2:ADD_SCALAR'

    # SKULL (PLAYER_HEAD)
    skull: '%player_name%'
    # O: skull: 'Notch'
    # O: skull_texture: 'base64...'

    # POTION
    potion:
      type: SPEED
      extended: false
      upgraded: true

    # LEATHER ARMOR
    leather_armor:
      color: '#FF0000'

    # ARMOR TRIM (1.20+)
    armor_trim:
      material: 'EMERALD'
      pattern: 'VEX'

    # CLICK ACTIONS
    actions:
      - 'message: &aHola!'
      - 'sound: ENTITY_PLAYER_LEVELUP'
      - 'close'
      - 'back'

    # CLICK COMMANDS
    commands:
      - 'give %player_name% diamond 1'
      - 'eco give %player_name% 100'

    # CLICK SOUNDS
    click_sounds:
      - 'ENTITY_EXPERIENCE_ORB_PICKUP'

    # DYNAMIC UPDATE
    dynamic_update: true
    update_interval: 20  # Ticks
```

---

## 🖱️ Click Actions

### Click Types Disponibles

```yaml
# left       - Click izquierdo
# right      - Click derecho
# middle     - Click del medio (rueda)
# shift_left - Shift + Click izquierdo
# shift_right- Shift + Click derecho
# drop       - Tecla Q (drop)
# swap       - Tecla F (swap offhand)
# double     - Doble click
# number_key - Teclas 1-9
# any        - Cualquier tipo
```

### Sintaxis

```yaml
actions:
  # Sin prefijo = ANY (cualquier click)
  - 'message: &aCualquier click'

  # Con prefijo = Click específico
  - 'left: message: &aClick izquierdo'
  - 'right: sound: BLOCK_NOTE_BLOCK_PLING'
  - 'shift_left: close'
  - 'shift_right: back'
  - 'drop: message: &cSoltaste el item'

commands:
  - 'left: give %player_name% diamond 1'
  - 'right: eco give %player_name% 100'
```

### Acciones Built-in

```yaml
- 'close'          # Cierra el menú
- 'back'           # Vuelve al menú anterior
- 'next_page'      # Siguiente página (PAGINATION)
- 'previous_page'  # Página anterior (PAGINATION)
- 'select: %index%'# Selecciona item (MULTI_PAGINATION)
```

---

## 🔤 Placeholders

### Placeholders del Menú

```
%menu_id%      - UUID del menú
%menu_type%    - Tipo de menú
%menu_title%   - Título del menú
%menu_size%    - Tamaño del menú
```

### Placeholders del Jugador

```
%player_name%  - Nombre del jugador
%player_uuid%  - UUID del jugador
%player_*%     - PlaceholderAPI placeholders
```

### Placeholders de Paginación

```
%current_page%    - Página actual
%total_pages%     - Total de páginas
%total_items%     - Total de items
%items_per_page%  - Items por página
%index%           - Índice global (0-N)
%page_index%      - Índice en página
```

### Placeholders de Multi-Paginación

```
%section_name%         - Nombre de la sección
%section_page%         - Página de la sección
%section_total_pages%  - Total páginas de la sección
%section_total_items%  - Total items de la sección
%selected_index%       - Índice del item seleccionado
```

### Placeholders Personalizados

```java
// En tu código
PlaceholderContext context = PlaceholderContext.create()
    .put("custom_var", "valor")
    .put("balance", "1000");

MenuData menuData = MenuData.builder()
    .title("Balance: {balance}")
    .context(context)
    .build();
```

```yaml
# En el YML
title: '&6Balance: {balance}'
lore:
  - '&7Variable: {custom_var}'
```

---

## 💡 Casos de Uso

### 🏪 Tienda con Categorías

```yaml
type: MULTI_PAGINATION
sections:
  weapons: { ... }
  armor: { ... }
  potions: { ... }
```

### 🏦 Sistema de Banco

```yaml
type: PAGINATION_FULL
pagination:
  slots: '0-26'  # Slots del banco
player_inventory:
  allowed_slots: '0-35'  # Inventario del jugador
```

### ⚙️ Configuración de Plugin

```yaml
type: SIMPLE
items:
  toggle_pvp: { ... }
  set_difficulty: { ... }
  manage_worlds: { ... }
```

### 🎒 Sistema de Mochila

```yaml
type: FULL_INVENTORY
snapshot:
  enabled: true
  restore_on_close: false  # Guarda cambios
```

### 🔨 Sistema de Crafteo

```yaml
type: MULTI_PAGINATION
sections:
  recipes:
    items:
      - material: CRAFTING_TABLE
        actions:
          - 'craft: table'
```

---

## 📊 Comparación de Tipos

| Tipo | Paginación | Multi-Sección | Inventario | Complejidad |
|------|-----------|---------------|-----------|-------------|
| SIMPLE | ❌ | ❌ | ❌ | ⭐ |
| PAGINATION | ✅ | ❌ | ❌ | ⭐⭐ |
| MULTI_PAGINATION | ✅ | ✅ | ❌ | ⭐⭐⭐ |
| FULL_INVENTORY | ❌ | ❌ | ✅ | ⭐⭐ |
| PAGINATION_FULL | ✅ | ❌ | ✅ | ⭐⭐⭐ |
| MULTI_PAGINATION_FULL | ✅ | ✅ | ✅ | ⭐⭐⭐⭐⭐ |

---

## 🚀 Inicio Rápido

### 1. Inicializar en onEnable()

```java
@Override
public void onEnable() {
    MenuAPI.initialize(this);
}
```

### 2. Abrir un menú

```java
// Desde config
ConfigurationSection menuConfig = config.getConfigurationSection("menus.shop");
MenuAPI.open(player, menuConfig);

// Async
MenuAPI.openAsync(player, menuConfig).thenRun(() -> {
    player.sendMessage("¡Menú abierto!");
});

// Programático
MenuData menuData = MenuData.builder()
    .title("&6Mi Menú")
    .type(MenuType.SIMPLE)
    .size(54)
    .build();
MenuAPI.open(player, menuData);
```

### 3. Gestionar menús activos

```java
// Obtener menú activo
MenuAPI.getActiveMenu(player).ifPresent(menu -> {
    // Hacer algo con el menú
});

// Cerrar
MenuAPI.close(player);

// Navegar atrás
MenuAPI.navigateBack(player);
```

---

## ⚠️ Notas Importantes

1. **FILLER**: Solo `global` y `border`. Para patrones custom usa items normales.

2. **SLOTS**: Acepta rangos (`'10-16'`), listas (`[10,11,12]`) o mixto (`'10-16,19'`).

3. **SNAPSHOT**: Protege el inventario al abrir. Se restaura automáticamente al cerrar.

4. **REFRESH**: `SMART` solo actualiza items con placeholders dinámicos.

5. **CLICK ACTIONS**: Prefijo `tipo_click:` para clicks específicos, sin prefijo = `any`.

6. **NAVEGACIÓN**: El sistema trackea historial automáticamente para `back`.

---

## 📁 Archivos de Ejemplo

- `01_simple_complete.yml` - Menú simple con todas las opciones
- `02_pagination_complete.yml` - Paginación básica
- `03_multi_pagination_complete.yml` - Múltiples secciones
- `04_full_inventory_complete.yml` - Con inventario del jugador
- `05_pagination_full_complete.yml` - Paginación + inventario
- `06_multi_pagination_full_complete.yml` - Todo combinado

---

## 🔗 Recursos

- **Materiales Bukkit**: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html
- **Sounds**: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html
- **Enchantments**: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/enchantments/Enchantment.html
