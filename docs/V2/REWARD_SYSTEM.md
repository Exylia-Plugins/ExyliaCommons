# REWARD SYSTEM

## Descripción
Sistema completo de recompensas que soporta comandos, items y mensajes con probabilidades, condiciones evaluables, procesamiento async, integración con placeholders, y carga desde configuración YAML. Ideal para sistemas de drop, logros, misiones, o cualquier mecánica que otorgue recompensas.

## Inicialización
```java
RewardAPI.initialize(JavaPlugin plugin)
```

## API Principal

### Dar Recompensas desde Config
- `give(Player player, ConfigurationSection section)` → `CompletableFuture<List<RewardResult>>` - Desde sección completa
- `give(Player player, ConfigurationSection section, PlaceholderContext context)` → `CompletableFuture<List<RewardResult>>` - Con contexto
- `giveFromKey(Player player, ConfigurationSection section, String key)` → `CompletableFuture<List<RewardResult>>` - Desde key específica
- `giveFromKey(Player player, ConfigurationSection section, String key, PlaceholderContext context)` → `CompletableFuture<List<RewardResult>>` - Key + contexto

### Builder
- `builder()` → `RewardBuilder` - Crea builder para recompensas programáticas

### Gestión
- `getStats()` → `RewardStats` - Estadísticas del sistema
- `shutdown()` - Cierra el sistema

## RewardBuilder

Construcción programática de recompensas:

```java
RewardAPI.builder()
    .command(String command)
    .message(String message)
    .item(String material, int amount)
    .item(ItemRewardConfig itemConfig)
    .chance(double chance)
    .condition(String condition)
    .successMessage(String message)
    .priority(int priority)
    .build() → Reward
    .give(Player player) → CompletableFuture<RewardResult>
    .give(Player player, PlaceholderContext context) → CompletableFuture<RewardResult>
```

**Métodos:**
- `command(String)` - Comando a ejecutar (procesa placeholders)
- `message(String)` - Mensaje a enviar al jugador
- `item(String, int)` - Item simple (material + cantidad)
- `item(ItemRewardConfig)` - Item complejo con enchants, meta, etc.
- `chance(double)` - Probabilidad 0.0-100.0 (ej: 50.0 = 50%)
- `condition(String)` - Condición para evaluar (ej: "%player_level% >= 10")
- `successMessage(String)` - Mensaje al otorgar la recompensa
- `priority(int)` - Prioridad de ejecución (mayor = primero)

## Configuración YAML

```yaml
rewards:
  - type: COMMAND
    data: "give %player% diamond 1"
    chance: 50.0
    condition: "%player_level% >= 10"
    message: "¡Recibiste un diamante!"
    priority: 1

  - type: MESSAGE
    data: "¡Felicidades!"
    chance: 100.0

  - type: ITEM
    chance: 25.0
    item:
      material: DIAMOND_SWORD
      amount: 1
      name: "&6Espada Épica"
      enchantments:
        sharpness: 5
        unbreaking: 3
```

## Tipos de Recompensa

### COMMAND
Ejecuta un comando desde consola:
```yaml
- type: COMMAND
  data: "give %player% emerald 64"
  chance: 75.0
```

### MESSAGE
Envía mensaje al jugador:
```yaml
- type: MESSAGE
  data: "&a¡Recompensa otorgada!"
  chance: 100.0
```

### ITEM
Da un item al jugador:
```yaml
- type: ITEM
  item:
    material: DIAMOND
    amount: 5
    glow: true
```

## ItemRewardConfig

Configuración completa de items:
- `material` - Material del item
- `amount` - Cantidad (soporta placeholders)
- `name` / `display-name` - Nombre del item
- `lore` - Lore (lista de strings)
- `enchantments` - Map de enchantments (nombre: nivel)
- `glow` - Boolean para glow effect
- `hide-attributes` - Ocultar atributos
- Y todos los parámetros de ITEMS_SYSTEM

## Condiciones

Las condiciones usan placeholders y operadores:

```yaml
# Ejemplos de condiciones
condition: "%player_level% >= 10"
condition: "%player_money% > 1000"
condition: "%player_health% <= 10.0"
condition: "%player_world% == world"
```

**Operadores soportados:**
- `>=`, `<=`, `>`, `<` - Comparación numérica
- `==`, `!=` - Igualdad/desigualdad
- `&&`, `||` - AND/OR lógicos

## Probabilidades

- **0.0** - Nunca se otorga
- **50.0** - 50% de probabilidad
- **100.0** - Siempre se otorga
- Soporta decimales: `33.33`, `12.5`, etc.

## RewardResult

Resultado de otorgar una recompensa:
- `isSuccess()` → `boolean` - Si se otorgó exitosamente
- `getReward()` → `Reward` - La recompensa
- `getFailureReason()` → `String` - Razón de fallo (si aplicable)
- `wasRolled()` → `boolean` - Si se evaluó la probabilidad
- `passedCondition()` → `boolean` - Si pasó la condición
- `wasGiven()` → `boolean` - Si se otorgó efectivamente

## RewardStats

Estadísticas disponibles:
- Total de recompensas procesadas
- Total de recompensas otorgadas
- Total de recompensas falladas
- Recompensas por tipo
- Tasa de éxito
- Tiempo promedio de procesamiento

## Características Principales
- **Múltiples Tipos**: Comandos, items, mensajes
- **Probabilidades**: Sistema de chance con RNG
- **Condiciones**: Evaluación de condiciones con placeholders
- **Async Processing**: Procesamiento asíncrono completo
- **Placeholder Support**: Integración completa con placeholders
- **Item Complexity**: Items complejos con enchants, meta, NBT, etc.
- **Priority System**: Orden de ejecución configurable
- **Batch Processing**: Procesa múltiples recompensas en paralelo
- **Stats & Monitoring**: Estadísticas detalladas
- **YAML Integration**: Carga fácil desde configuración

## Notas Importantes
- Las probabilidades usan `ThreadLocalRandom` para mejor performance
- Las condiciones se evalúan antes de verificar probabilidad
- Los comandos se ejecutan desde consola (tienen permisos completos)
- Los items se dan con `addItem()` - si inventario lleno, dropea
- Las recompensas se procesan en paralelo async
- El priority determina orden: mayor prioridad = primero
- Los placeholders se procesan en comandos y condiciones
- Si una condición falla, no se evalúa la probabilidad
- Los mensajes usan el sistema VISUAL/MESSAGE
- Las recompensas con chance 0.0 nunca se otorgan
- El sistema es thread-safe para uso concurrente

## Ver También
- ITEMS_SYSTEM - Configuración avanzada de items
- PLACEHOLDERS_SYSTEM - Placeholders en condiciones y comandos
- CONFIG_SYSTEM - Carga de recompensas desde YAML
- ACTION_SYSTEM - Alternativa para acciones custom
