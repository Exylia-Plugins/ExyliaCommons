# ⚡ Referencia Rápida - Sistema UI v2

## 📑 Tipos de Menú

```yaml
type: SIMPLE                  # Menú básico
type: PAGINATION              # Con paginación
type: MULTI_PAGINATION        # Múltiples secciones paginadas
type: FULL_INVENTORY          # Con acceso a inventario del jugador
type: PAGINATION_FULL         # Paginación + inventario
type: MULTI_PAGINATION_FULL   # Todo combinado
```

## 🎨 Filler (Solo 2 opciones)

```yaml
filler:
  global:    # Rellena TODO
    material: GRAY_STAINED_GLASS_PANE
  border:    # Solo bordes (sobrescribe global)
    material: BLACK_STAINED_GLASS_PANE
```

## 📍 Slots

```yaml
slot: 13                    # Único
slots: [10, 11, 12]         # Lista
slots: '10-16'              # Rango
slots: '10-16,19-25'        # Mixto
```

## 🎁 Item Básico

```yaml
items:
  mi_item:
    material: DIAMOND
    amount: '1'
    name: '&bNombre'
    lore:
      - '&7Línea 1'
    slot: 13
    actions:
      - 'message: &aHola'
```

## 🖱️ Click Types

```
left, right, middle
shift_left, shift_right
drop, swap, double, number_key
any
```

## 🖱️ Click Actions

```yaml
# Sin prefijo = ANY
actions:
  - 'message: &aHola'
  - 'sound: ENTITY_PLAYER_LEVELUP'
  - 'close'
  - 'back'

# Con prefijo = Click específico
actions:
  - 'left: message: &aClick izquierdo'
  - 'right: sound: BLOCK_NOTE_BLOCK_PLING'
  - 'shift_left: close'
```

## 🔄 Refresh

```yaml
refresh:
  mode: DISABLED   # No refresca
  mode: FULL       # Refresca todo
  mode: SMART      # Solo items dinámicos (recomendado)
  mode: SLOT_ONLY  # Como SMART pero sin actualizar título
  interval: 20     # Ticks (20 = 1 seg)
```

## 📄 Pagination

```yaml
type: PAGINATION
pagination:
  slots: '10-16,19-25'
  items:
    - material: DIAMOND
      name: 'Item #%index%'
  navigation:
    previous:
      slot: 48
      material: ARROW
      name: '← Anterior'
    next:
      slot: 50
      material: ARROW
      name: 'Siguiente →'
```

## 🗂️ Multi-Pagination

```yaml
type: MULTI_PAGINATION
sections:
  weapons:
    slots: '10-12'
    items: [...]
    selected_template:
      glowing: true
    navigation:
      previous: { slot: 9 }
      next: { slot: 13 }
```

## 💾 Snapshot

```yaml
snapshot:
  enabled: true
  restore_on_close: true
  snapshot_id: 'my_menu'
```

## 👤 Player Inventory

```yaml
player_inventory:
  enabled: true
  allowed_slots: '0-35'  # 0-8=hotbar, 9-35=inventory
```

## 🔤 Placeholders

### General
```
%menu_id%, %menu_type%, %menu_title%, %menu_size%
%player_name%, %player_uuid%
```

### Pagination
```
%current_page%, %total_pages%, %total_items%
%index%, %page_index%
```

### Multi-Pagination
```
%section_name%, %section_page%, %section_total_pages%
%selected_index%
```

### Custom
```java
PlaceholderContext.create().put("var", "valor")
```
```yaml
lore:
  - '&7Variable: {var}'
```

## 🎯 Item Completo

```yaml
my_item:
  material: DIAMOND_SWORD
  amount: '1'
  name: '&cNombre'
  lore: ['&7Línea']
  slot: 13
  enchantments:
    DAMAGE_ALL: 5
  glowing: true
  hide_attributes: true
  unbreakable: true
  max_stack_size: 1
  item_model: 1001
  custom_nbt:
    'key': 'value'
  skull: '%player_name%'
  potion:
    type: SPEED
    upgraded: true
  leather_armor:
    color: '#FF0000'
  actions:
    - 'left: message: &aHola'
  commands:
    - 'give %player_name% diamond'
  click_sounds:
    - 'ENTITY_EXPERIENCE_ORB_PICKUP'
  dynamic_update: true
  update_interval: 20
```

## 🚀 Código Java

### Inicializar
```java
MenuAPI.initialize(plugin);
```

### Abrir
```java
// Desde config
ConfigurationSection config = ...;
MenuAPI.open(player, config);

// Async
MenuAPI.openAsync(player, config);

// Programático
MenuData menuData = MenuData.builder()
    .title("&6Título")
    .type(MenuType.SIMPLE)
    .size(54)
    .build();
MenuAPI.open(player, menuData);
```

### Gestionar
```java
MenuAPI.getActiveMenu(player);
MenuAPI.close(player);
MenuAPI.navigateBack(player);
MenuAPI.clearHistory(player);
```

## 📊 Cuando Usar Cada Tipo

| Tipo | Uso |
|------|-----|
| SIMPLE | Menús de opciones, configuración |
| PAGINATION | Tiendas, catálogos |
| MULTI_PAGINATION | Tiendas con categorías |
| FULL_INVENTORY | Gestores de inventario |
| PAGINATION_FULL | Bancos, almacenes |
| MULTI_PAGINATION_FULL | Sistemas complejos |

## ⚠️ Limitaciones

1. **FILLER**: Solo `global` y `border`
2. **CLICK ACTIONS**: Prefijo `tipo:` para clicks específicos
3. **SLOTS**: Rangos con `-`, listas con `,`
4. **SNAPSHOT**: Solo en FULL_INVENTORY tipos
5. **NAVIGATION**: Solo en PAGINATION tipos

## 🔗 Links Útiles

- Materials: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html
- Sounds: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html
- Enchantments: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/enchantments/Enchantment.html

## 📁 Archivos

- `01_simple_complete.yml` - Menú simple completo
- `02_pagination_complete.yml` - Paginación
- `03_multi_pagination_complete.yml` - Multi-paginación
- `04_full_inventory_complete.yml` - Con inventario
- `05_pagination_full_complete.yml` - Paginación + inventario
- `06_multi_pagination_full_complete.yml` - Todo combinado
- `ADVANCED_EXAMPLES.yml` - Patrones avanzados
- `README.md` - Documentación completa
