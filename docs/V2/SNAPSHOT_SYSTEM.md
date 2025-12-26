# SNAPSHOT SYSTEM

## Descripción
Sistema de snapshots (instantáneas) de jugadores que captura y restaura estados completos incluyendo inventario, armor, health, food, exp, potion effects, flight state, y gamemode. Con soporte async, builder para snapshots selectivos, registro de snapshots nombrados, y cache para acceso rápido. Ideal para arenas, minijuegos, o cualquier mecánica que requiera guardar/restaurar estados.

## Inicialización
```java
SnapshotAPI.initialize(JavaPlugin plugin)
```

## API Principal

### Creación de Snapshots
- `create(Player player)` → `SnapshotData` - Crea snapshot sync
- `createAsync(Player player)` → `CompletableFuture<SnapshotData>` - Crea snapshot async
- `createAndRegister(Player player, String snapshotId)` → `SnapshotData` - Crea y registra
- `createAndRegisterAsync(Player player, String snapshotId)` → `CompletableFuture<SnapshotData>` - Async

### Builder
- `builder(Player player)` → `SnapshotBuilder` - Crea builder para snapshot selectivo

### Restauración
- `restore(Player player, SnapshotData snapshot)` - Restaura snapshot sync
- `restoreAsync(Player player, SnapshotData snapshot)` → `CompletableFuture<Boolean>` - Restaura async
- `restoreRegistered(Player player, String snapshotId)` → `boolean` - Restaura por ID
- `restoreRegisteredAsync(Player player, String snapshotId)` → `CompletableFuture<Boolean>` - Async

### Registro de Snapshots
- `register(UUID playerUuid, String snapshotId, SnapshotData snapshot)` - Registra snapshot
- `unregister(UUID playerUuid, String snapshotId)` - Desregistra snapshot
- `getRegistered(UUID playerUuid, String snapshotId)` → `Optional<SnapshotData>` - Obtiene snapshot

### Gestión
- `isInitialized()` → `boolean` - Verifica inicialización
- `shutdown()` - Cierra el sistema
- `getManager()` → `SnapshotManager` - Manager para acceso avanzado

## SnapshotBuilder

Builder para snapshots selectivos:

```java
SnapshotAPI.builder(player)
    .excludeInventory()
    .excludeHealth()
    .excludeFood()
    .excludeExp()
    .excludePotionEffects()
    .excludeFlightState()
    .build() → SnapshotData
    .buildAsync() → CompletableFuture<SnapshotData>
```

**Métodos de Exclusión:**
- `excludeInventory()` - No incluye inventario, armor, ni offhand
- `excludeHealth()` - No incluye health ni maxHealth
- `excludeFood()` - No incluye food level ni saturation
- `excludeExp()` - No incluye level ni exp
- `excludePotionEffects()` - No incluye potion effects
- `excludeFlightState()` - No incluye allowFlight, flying, ni flySpeed

## SnapshotData

Modelo que representa el estado de un jugador:

### Datos Incluidos
- **GameMode**: Modo de juego actual
- **Inventory**: Inventario completo (36 slots)
- **Armor**: Armadura (4 slots)
- **OffHand**: Item en mano secundaria
- **Health**: Vida actual y máxima
- **Food**: Nivel de comida y saturación
- **Experience**: Nivel y barra de experiencia
- **Potion Effects**: Efectos de poción activos
- **Flight**: Estado de vuelo y velocidad

### Métodos
- `fromPlayer(Player player)` → `SnapshotData` - Crea desde jugador
- `applyToPlayer(Player player)` - Aplica snapshot a jugador

## Uso de Registro

El registro permite guardar snapshots nombrados:

```java
// Crear y registrar
SnapshotAPI.createAndRegister(player, "arena-entry");

// Restaurar después
SnapshotAPI.restoreRegistered(player, "arena-entry");

// Limpiar
SnapshotAPI.unregister(player.getUniqueId(), "arena-entry");
```

## Características Principales
- **Complete State**: Captura estado completo del jugador
- **Selective Snapshots**: Builder para excluir componentes
- **Async Operations**: Operaciones async para no bloquear
- **Named Registry**: Sistema de registro con IDs
- **Cache System**: Cache para snapshots registrados
- **Automatic Cleanup**: Limpieza automática al shutdown
- **Restoration Safety**: Validación antes de restaurar
- **Thread-Safe**: Seguro para uso concurrente

## PotionEffectData

Los efectos de poción se serializan con:
- **type**: Tipo del efecto
- **duration**: Duración en ticks
- **amplifier**: Nivel del efecto (0 = nivel 1)
- **ambient**: Si es efecto ambiental
- **particles**: Si muestra partículas
- **icon**: Si muestra icono

## Notas Importantes
- Los snapshots se crean desde el estado actual del jugador
- La restauración limpia el inventario y efectos antes de aplicar
- Los snapshots registrados persisten en memoria (no en disco)
- Use async para evitar lag al crear snapshots grandes
- El builder por defecto incluye todo - use excludeX() para selectivos
- La restauración es atómica (todo o nada)
- Los snapshots son inmutables después de crearse
- El sistema limpia snapshots registrados al shutdown
- El inventario incluye los 36 slots principales (no enderchest)
- La maxHealth se restaura antes que health para evitar errores
- Los flight states solo se aplican si allowFlight es true
- Los snapshots no incluyen ubicación del jugador
- El sistema es ideal para arenas PvP, minijuegos, parkour, etc.

## Ver También
- REGION_SYSTEM - Snapshots en entrada/salida de regiones
- DATABASE_SYSTEM - Persistencia de snapshots en BD
- ITEMS_SYSTEM - Serialización de items en snapshots
