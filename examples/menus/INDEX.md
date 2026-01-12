# 📂 Índice de Ejemplos - Sistema UI v2

Todos los archivos de ejemplo para el sistema de menús de ExyliaCommons v2.

---

## 📚 Documentación

### 📖 README.md
**Documentación completa del sistema**
- Explicación detallada de todos los tipos de menú
- Componentes principales
- Fillers, Slots, Items
- Click Actions y Placeholders
- Casos de uso

### ⚡ QUICK_REFERENCE.md
**Referencia rápida**
- Sintaxis básica
- Código Java
- Placeholders
- Cuando usar cada tipo
- Links útiles

### 📋 CHEATSHEET.txt
**Hoja de referencia visual ASCII**
- Layout de slots
- Comandos rápidos
- Sintaxis en formato tabla
- Tips rápidos

---

## 🎯 Ejemplos de Menús (YML)

### 01. 01_simple_complete.yml
**Menú SIMPLE - Completo**

Menú básico sin paginación con **TODAS** las opciones posibles:
- ✅ Todos los tipos de items (diamond, potion, skull, leather armor, etc.)
- ✅ Todas las propiedades de items
- ✅ Enchantments, NBT, custom model
- ✅ Click actions con todos los tipos de click
- ✅ Dynamic update
- ✅ Fillers (global + border)
- ✅ Refresh modes

**Usa esto para:** Menús principales, configuración, selección de opciones

---

### 02. 02_pagination_complete.yml
**Menú PAGINATION - Completo**

Menú con paginación automática de items:
- ✅ Configuración de slots de paginación
- ✅ Items que se paginan automáticamente
- ✅ Botones de navegación (previous/next/info)
- ✅ Placeholders de paginación (%current_page%, %index%, etc.)
- ✅ Items fijos que no se paginan
- ✅ Refresh dinámico

**Usa esto para:** Tiendas, catálogos, listados de items

---

### 03. 03_multi_pagination_complete.yml
**Menú MULTI_PAGINATION - Completo**

Múltiples secciones paginadas independientes:
- ✅ 3 secciones de ejemplo (weapons, armor, potions)
- ✅ Cada sección con su propia paginación
- ✅ Selected template (items seleccionados brillan)
- ✅ Filler específico por sección
- ✅ Navegación independiente por sección
- ✅ Placeholders de sección (%section_name%, %section_page%, etc.)

**Usa esto para:** Tiendas con categorías, sistemas de selección múltiple

---

### 04. 04_full_inventory_complete.yml
**Menú FULL_INVENTORY - Completo**

Menú con acceso al inventario del jugador + snapshot:
- ✅ Sistema de snapshot (guardar/restaurar inventario)
- ✅ Configuración de player_inventory
- ✅ Allowed_slots (0-35)
- ✅ Botones de gestión (sort, clear, save, backup, restore)
- ✅ Display de armadura equipada
- ✅ Estadísticas del inventario
- ✅ Compress/expand items

**Usa esto para:** Gestores de inventario, sistemas de mochila, editores

---

### 05. 05_pagination_full_complete.yml
**Menú PAGINATION_FULL - Completo**

Combina paginación + acceso al inventario:
- ✅ Slots del banco (paginados)
- ✅ Inventario del jugador (accesible)
- ✅ Snapshot system
- ✅ Botones deposit/withdraw all
- ✅ Quick actions
- ✅ Search, sort, upgrade
- ✅ Navegación de páginas del banco

**Usa esto para:** Bancos, sistemas de almacenamiento, vaults

---

### 06. 06_multi_pagination_full_complete.yml
**Menú MULTI_PAGINATION_FULL - El más completo**

Combina TODO: múltiples secciones + paginación + inventario:
- ✅ 3 secciones (storage, craftings, upgrades)
- ✅ Cada sección pagina independientemente
- ✅ Acceso al inventario del jugador
- ✅ Sistema de snapshot
- ✅ Selected templates por sección
- ✅ Navegación independiente
- ✅ Items fijos (balance, stats, ayuda, config)
- ✅ Quick actions

**Usa esto para:** Sistemas complejos de almacenamiento, crafting avanzado, sistemas RPG

---

## 🚀 Ejemplos Avanzados

### 07. ADVANCED_EXAMPLES.yml
**Patrones avanzados y casos especiales**

10 ejemplos de patrones avanzados:
1. **Patrón decorativo custom** - Crear diseños con items normales
2. **Menú de confirmación** - Sistema Yes/No
3. **Selector de color** - Selección visual de colores
4. **Barra de progreso** - Indicador visual de progreso
5. **Ranking/Leaderboard** - Top 10 jugadores
6. **Wheel of Fortune** - Ruleta animada
7. **Sistema de votación** - Votos con barra de progreso
8. **Calendario de eventos** - Listado de eventos con registro
9. **Minimapa interactivo** - Mapa del servidor con teleports
10. **Quest tracker** - Seguimiento de misiones

**Usa esto para:** Inspiración, patrones complejos, animaciones

---

## 📊 Comparación Rápida

| Archivo | Tipo | Paginación | Multi-Sección | Inventario | Dificultad |
|---------|------|-----------|---------------|-----------|-----------|
| 01_simple_complete.yml | SIMPLE | ❌ | ❌ | ❌ | ⭐ Fácil |
| 02_pagination_complete.yml | PAGINATION | ✅ | ❌ | ❌ | ⭐⭐ Media |
| 03_multi_pagination_complete.yml | MULTI_PAGINATION | ✅ | ✅ | ❌ | ⭐⭐⭐ Media-Alta |
| 04_full_inventory_complete.yml | FULL_INVENTORY | ❌ | ❌ | ✅ | ⭐⭐ Media |
| 05_pagination_full_complete.yml | PAGINATION_FULL | ✅ | ❌ | ✅ | ⭐⭐⭐ Media-Alta |
| 06_multi_pagination_full_complete.yml | MULTI_PAGINATION_FULL | ✅ | ✅ | ✅ | ⭐⭐⭐⭐⭐ Muy Alta |
| ADVANCED_EXAMPLES.yml | Varios | - | - | - | ⭐⭐⭐⭐ Alta |

---

## 🎯 ¿Por dónde empezar?

### Si eres nuevo:
1. Lee **README.md** para entender los conceptos
2. Mira **01_simple_complete.yml** para un menú básico
3. Usa **QUICK_REFERENCE.md** como referencia rápida
4. Ten **CHEATSHEET.txt** abierto mientras programas

### Si necesitas algo específico:
- **Tienda simple:** → `02_pagination_complete.yml`
- **Tienda con categorías:** → `03_multi_pagination_complete.yml`
- **Banco/Almacén:** → `05_pagination_full_complete.yml`
- **Sistema complejo:** → `06_multi_pagination_full_complete.yml`
- **Patrones especiales:** → `ADVANCED_EXAMPLES.yml`

### Para referencia rápida:
- **Sintaxis completa:** → `QUICK_REFERENCE.md`
- **Comandos rápidos:** → `CHEATSHEET.txt`
- **Documentación detallada:** → `README.md`

---

## ⚠️ Respuestas a Preguntas Frecuentes

### ❓ ¿Hay más tipos de filler además de global y border?
**❌ NO.** Solo hay 2 tipos de filler:
- `global` - Rellena todos los slots vacíos
- `border` - Solo rellena los bordes (sobrescribe global)

Para patrones custom, usa items normales con `slots` específicos.
Ver: **ADVANCED_EXAMPLES.yml** → `pattern_custom`

### ❓ ¿Cómo uso slots?
```yaml
slot: 13              # Un solo slot
slots: [10, 11, 12]   # Lista
slots: '10-16'        # Rango
slots: '10-16,19-25'  # Mixto
```

### ❓ ¿Qué placeholders puedo usar?
Ver **QUICK_REFERENCE.md** → Sección "Placeholders"

Básicos: `%player_name%`, `%menu_title%`
Paginación: `%current_page%`, `%index%`
Custom: `{tu_variable}` (con PlaceholderContext)

### ❓ ¿Cómo hago click actions específicos?
```yaml
actions:
  - 'message: &aAny click'          # Sin prefijo = ANY
  - 'left: message: &aClick izq'    # Con prefijo
  - 'right: close'
  - 'shift_left: back'
```

### ❓ ¿Cuál tipo de menú debo usar?
Ver tabla de comparación arriba o **README.md** → "Cuando Usar Cada Tipo"

---

## 🔗 Recursos Externos

- **Materials:** https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html
- **Sounds:** https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html
- **Enchantments:** https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/enchantments/Enchantment.html

---

## 📝 Notas Finales

- Todos los ejemplos están completamente comentados
- Cada archivo es independiente y puede usarse como template
- Los ejemplos avanzados requieren implementación custom en tu plugin
- Placeholders custom (`{var}`) requieren PlaceholderContext en código

---

**¡Feliz desarrollo con ExyliaCommons UI v2!** 🚀
