# ITEMS SYSTEM

## Descripción
Sistema completo de configuración de items desde YAML con soporte para enchantments, atributos, armor trims, pociones, leather armor colors, item models, NBT custom, placeholders dinámicos, skulls, y actualización automática. Procesa configuraciones complejas y convierte a ItemStacks de Bukkit con todas las propiedades aplicadas.

## Modelo Principal

### ItemData

Modelo builder para configuración de items:

```java
ItemData.builder()
    .rawMaterial(String material)
    .rawAmount(String amount)
    .rawName(String name)
    .rawDisplayName(String displayName)
    .rawLore(List<String> lore)
    .loreDynamicSupplier(Supplier<List<String>> supplier)
    .rawEnchantments(Map<String, Integer> enchants)
    .potionConfig(PotionConfig config)
    .armorTrimConfig(ArmorTrimConfig config)
    .leatherArmorConfig(LeatherArmorConfig config)
    .glowing(boolean glow)
    .hideAttributes(boolean hide)
    .rawItemModel(String model)
    .clickSounds(List<String> sounds)
    .context(PlaceholderContext context)
    .updateInterval(long ticks)
    .dynamicUpdate(boolean dynamic)
    .rawAttributes(List<String> attributes)
    .customNBT(Map<String, String> nbt)
    .unbreakable(boolean unbreakable)
    .maxStackSize(int size)
    .build() → ItemData
```

## Configuración YAML

### Item Básico
```yaml
material: DIAMOND_SWORD
amount: 1
name: "&6Espada Épica"
lore:
  - "&7Una espada legendaria"
  - "&7Daño: &c+10"
```

### Item con Enchantments
```yaml
material: DIAMOND_SWORD
enchantments:
  sharpness: 5
  unbreaking: 3
  fire_aspect: 2
```

### Item con Atributos
```yaml
material: DIAMOND_SWORD
attributes:
  - "GENERIC_ATTACK_DAMAGE:10:ADD_NUMBER"
  - "GENERIC_ATTACK_SPEED:0.5:ADD_SCALAR"
```

### Armor con Trim
```yaml
material: DIAMOND_CHESTPLATE
armor-trim:
  material: NETHERITE
  pattern: SENTRY
```

### Leather Armor Coloreada
```yaml
material: LEATHER_CHESTPLATE
leather-color:
  red: 255
  green: 0
  blue: 0
```

### Poción
```yaml
material: POTION
potion:
  type: SPEED
  level: 2
  extended: false
  splash: false
```

### Skull Custom
```yaml
material: PLAYER_HEAD
skull-texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA..."
```

### Item con NBT Custom
```yaml
material: DIAMOND
custom-nbt:
  CustomKey: "CustomValue"
  AnotherKey: "123"
```

### Item Model (1.20.5+)
```yaml
material: STICK
item-model: "custom:my_model"
```

### Item con Placeholders
```yaml
material: DIAMOND
name: "&6Item de %player_name%"
lore:
  - "&7Nivel: %player_level%"
  - "&7Dinero: %player_money%"
```

## Propiedades Soportadas

### Básicas
- `material` - Material del item (required)
- `amount` - Cantidad (soporta placeholders)
- `name` / `display-name` - Nombre del item
- `lore` - Lista de líneas de lore

### Enchantments
- `enchantments` - Map de encantamientos (nombre: nivel)
- `glowing` - Efecto glow sin enchants

### Atributos
- `attributes` - Lista de atributos ("TIPO:VALOR:OPERACION")
- `hide-attributes` - Oculta atributos del item

### Armor
- `armor-trim.material` - Material del trim
- `armor-trim.pattern` - Patrón del trim
- `leather-color.red` - Componente rojo (0-255)
- `leather-color.green` - Componente verde (0-255)
- `leather-color.blue` - Componente azul (0-255)

### Pociones
- `potion.type` - Tipo de poción
- `potion.level` - Nivel de la poción
- `potion.extended` - Duración extendida
- `potion.splash` - Poción arrojadiza

### Customización
- `skull-texture` - Textura base64 para skulls
- `skull-owner` - Nombre del dueño del skull
- `item-model` - Custom model (1.20.5+)
- `custom-nbt` - NBT tags custom
- `unbreakable` - Item irrompible
- `max-stack-size` - Tamaño máximo del stack

### Dinámico
- `update-interval` - Ticks entre actualizaciones de placeholders
- `dynamic-update` - Habilita actualización automática
- `click-sounds` - Sonidos al hacer click

## Procesadores

El sistema usa procesadores especializados:
- **EnchantmentProcessor** - Procesa enchantments
- **AttributeProcessor** - Procesa atributos
- **ArmorTrimProcessor** - Procesa armor trims
- **LeatherArmorProcessor** - Procesa colores de leather
- **PotionProcessor** - Procesa pociones
- **ItemModelProcessor** - Procesa custom models
- **ConfigurationParser** - Parsea desde YAML

## Características Principales
- **YAML Configuration**: Carga items completos desde YAML
- **Placeholder Support**: Placeholders en nombre, lore, y cantidad
- **Dynamic Updates**: Actualización automática de placeholders
- **Complete Customization**: Enchants, atributos, NBT, models, etc.
- **Armor Trims**: Soporte completo para armor trims 1.20+
- **Potion Support**: Configuración completa de pociones
- **Skull Textures**: Skulls con texturas base64
- **NBT Support**: NBT tags custom
- **Attribute System**: Atributos de Minecraft completos
- **Builder Pattern**: Construcción programática fluida

## Formato de Atributos

Los atributos usan el formato:
```
TIPO:VALOR:OPERACION
```

**Tipos comunes:**
- GENERIC_ATTACK_DAMAGE
- GENERIC_ATTACK_SPEED
- GENERIC_MAX_HEALTH
- GENERIC_MOVEMENT_SPEED
- GENERIC_ARMOR
- GENERIC_ARMOR_TOUGHNESS
- GENERIC_KNOCKBACK_RESISTANCE

**Operaciones:**
- ADD_NUMBER - Suma valor
- ADD_SCALAR - Suma porcentaje
- MULTIPLY_SCALAR_1 - Multiplica

## Notas Importantes
- El material es la única propiedad requerida
- Los placeholders se procesan en tiempo de creación
- Dynamic update requiere que el item esté en un inventario activo
- Los skulls requieren el SkullAPI inicializado
- Los armor trims solo funcionan en 1.20+
- Los item models solo funcionan en 1.20.5+
- Los atributos se aplican con UUID aleatorio
- El NBT custom usa strings (se parsean automáticamente)
- Los enchantments se validan antes de aplicar
- La cantidad soporta placeholders que retornan números
- El glowing effect requiere flag específico de NBT
- Los click sounds requieren integración con UI system
- El sistema es ideal para rewards, shops, y custom items
- Los items son inmutables después de procesarse
- La actualización dinámica consume recursos - usar con moderación

## Ver También
- CONFIG_SYSTEM - Carga de items desde config
- SKULL_SYSTEM - Texturas para skulls
- PLACEHOLDERS_SYSTEM - Placeholders en items
- REWARD_SYSTEM - Items como recompensas
