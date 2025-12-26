# REGION SYSTEM

## Descripción
Sistema completo de regiones 3D con flags de protección, permisos (owners/members), eventos de entrada/salida, integración con WorldEdit para schematics, bloques temporales, visualización con partículas, sistema de selección con varita, y detección espacial optimizada. Ideal para arenas, zonas protegidas, minijuegos, etc.

## Inicialización
```java
RegionAPI.initialize(JavaPlugin plugin)
```

## API Principal

### Creación
- `getInstance()` → `RegionAPI` - Obtiene instancia
- `createRegion(String id)` → `RegionBuilder` - Crea builder

### Registro
- `registerRegion(Region region)` → `boolean` - Registra región
- `unregisterRegion(String regionId)` → `boolean` - Desregistra región

### Consulta
- `getRegion(String regionId)` → `Optional<Region>` - Por ID
- `getAllRegions()` → `Collection<Region>` - Todas las regiones
- `getRegionsAt(Location location)` → `List<Region>` - En ubicación
- `getHighestPriorityRegionAt(Location location)` → `Optional<Region>` - Mayor prioridad

### Consultas de Jugador
- `getPlayerRegions(Player player)` → `Set<Region>` - Regiones del jugador
- `isPlayerInRegion(Player player, Region region)` → `boolean` - En región específica
- `isPlayerInAnyRegion(Player player)` → `boolean` - En cualquier región

### Sistema de Selección Visual
- `showSelector(Player player, Region region)` → `SelectionSession` - Muestra selector
- `showSelector(Player player, Region region, Color color)` → `SelectionSession` - Con color custom
- `stopSelector(Player player)` - Detiene visualización
- `getSelectionSession(Player player)` → `Optional<SelectionSession>` - Sesión activa
- `hasActiveSelection(Player player)` → `boolean` - Tiene sesión activa

### Wand (Varita de Selección)
- `giveWand(Player player)` → `ItemStack` - Da varita al jugador
- `createWand()` → `ItemStack` - Crea varita
- `createWand(String selectionId)` → `ItemStack` - Con ID custom
- `isWand(ItemStack item)` → `boolean` - Verifica si es varita

### Sistema de Selección
- `getPlayerSelection(Player player)` → `Optional<Selection>` - Selección activa
- `setSelectionPos1(Player player, Location location)` - Define pos1
- `setSelectionPos2(Player player, Location location)` - Define pos2
- `clearPlayerSelection(Player player)` - Limpia selección
- `setSelectionCallback(Player player, Consumer<Selection> callback)` - Callback al completar
- `clearSelectionCallback(Player player)` - Limpia callback
- `createRegionFromSelection(String regionId, Player player)` → `Region` - Crea desde selección

### Schematics
- `getSchematicManager()` → `SchematicManager` - Manager de schematics

## RegionBuilder

```java
RegionAPI.getInstance().createRegion("region-id")
    .selection(Location pos1, Location pos2)
    .displayName(String displayName)
    .description(String description)
    .priority(RegionPriority priority)
    .flag(RegionFlag flag, boolean value)
    .owners(UUID... owners)
    .members(UUID... members)
    .metadata(String key, Object value)
    .allowedBlocks(Material... materials)
    .onEnter(BiConsumer<Region, Player> callback)
    .onExit(BiConsumer<Region, Player> callback)
    .temporaryBlocksSeconds(int seconds)
    .safeZone()  // Preset para zona segura
    .membersOnly()  // Solo miembros
    .playerBuildOnly()  // Solo bloques de jugadores
    .temporaryBlocks(int seconds, boolean reGive)
    .build() → Region
```

## Region (Modelo)

### Información Básica
- `getId()` → `String`
- `getWorld()` → `World`
- `getMinimumPoint()` → `Location`
- `getMaximumPoint()` → `Location`
- `getCenter()` → `Location`
- `getVolume()` → `long`

### Contención
- `contains(Location location)` → `boolean`
- `contains(Player player)` → `boolean`

### Flags
- `setFlag(RegionFlag flag, RegionFlagState state)`
- `setFlag(RegionFlag flag, boolean value)`
- `getFlagState(RegionFlag flag)` → `RegionFlagState`
- `getFlagValue(RegionFlag flag)` → `boolean`
- `isFlagSet(RegionFlag flag)` → `boolean`
- `removeFlag(RegionFlag flag)`
- `clearFlags()`

### Permisos
- `isOwner(UUID playerId)` → `boolean`
- `isMember(UUID playerId)` → `boolean`
- `addOwner(UUID playerId)`
- `addMember(UUID playerId)`
- `removeOwner(UUID playerId)`
- `removeMember(UUID playerId)`

### Jugadores Dentro
- `isPlayerInside(Player player)` → `boolean`
- `addPlayer(Player player)`
- `removePlayer(Player player)`
- `getPlayersInside()` → `Set<Player>`
- `cleanupOfflinePlayers()`

### Metadata
- `setMetadata(String key, Object value)`
- `getMetadata(String key, Class<T> type)` → `T`
- `removeMetadata(String key)`
- `hasMetadata(String key)` → `boolean`
- `clearMetadata()`

### Schematics (Async)
- `saveSchematic()` → `CompletableFuture<Boolean>`
- `saveSchematic(String name)` → `CompletableFuture<Boolean>`
- `regenerate()` → `CompletableFuture<Boolean>`
- `regenerate(String schematicName)` → `CompletableFuture<Boolean>`
- `cloneTo(Location targetCenter)` → `CompletableFuture<Region>`

## RegionFlag (Enum)

1. **PVP** - Permite combate PvP (default: true)
2. **BUILD** - Permite colocar bloques (default: true)
3. **BREAK** - Permite romper bloques (default: true)
4. **INTERACT** - Permite interactuar (default: true)
5. **PLAYER_BUILD_ONLY** - Solo romper bloques de jugadores (default: false)
6. **ALLOWED_BLOCKS_ONLY** - Solo materiales específicos (default: false)
7. **TEMPORARY_BLOCKS** - Bloques temporales (default: false)
8. **RE_GIVE_BLOCKS** - Devolver bloques temporales (default: false)
9. **REGION_MEMBERS_ONLY** - Solo miembros pueden entrar (default: false)
10. **ENTRY** - Permite entrar (default: true)
11. **EXIT** - Permite salir (default: true)
12. **ITEM_DROP** - Permite dropear items (default: true)
13. **ITEM_PICKUP** - Permite recoger items (default: true)

## RegionPriority (Enum)

- **LOW** - Prioridad baja
- **NORMAL** - Prioridad normal (default)
- **HIGH** - Prioridad alta
- **CRITICAL** - Prioridad crítica

## Eventos

- `RegionCreateEvent` - Al crear región
- `RegionDeleteEvent` - Al eliminar región
- `RegionEnterEvent` - Al entrar (cancelable con callbacks)
- `RegionExitEvent` - Al salir (cancelable con callbacks)
- `RegionPreEnterEvent` - Antes de entrar (cancelable)
- `RegionPreExitEvent` - Antes de salir (cancelable)

## SchematicManager

```java
SchematicManager sm = api.getSchematicManager();

// Guardar
sm.saveSchematic(region);
sm.saveSchematic(region, "nombre");

// Cargar
sm.loadSchematic("nombre");

// Pegar
sm.pasteSchematic("nombre", location);

// Regenerar
sm.regenerateRegion(region);
sm.regenerateRegion(region, "schematicName");

// Gestión
sm.deleteSchematic("nombre");
sm.schematicExists("nombre");
```

## Características Principales
- **3D Regions**: Regiones cúbicas 3D completas
- **Flag System**: 13 flags configurables
- **Permissions**: Sistema owners/members
- **Events**: Eventos de entrada/salida con callbacks
- **Spatial Index**: Índice espacial para búsqueda rápida
- **Temporary Blocks**: Bloques que desaparecen después de X segundos
- **Schematics**: Integración con WorldEdit
- **Visual Selection**: Partículas para visualizar selección
- **Wand Tool**: Varita para seleccionar fácilmente
- **Priority System**: Regiones con prioridades
- **Metadata**: Sistema de metadata extensible
- **Async Operations**: Schematics y operaciones pesadas async

## Notas Importantes
- Las regiones se verifican en orden de prioridad (CRITICAL > HIGH > NORMAL > LOW)
- Los callbacks onEnter/onExit se ejecutan async
- Los bloques temporales se rastrean por jugador
- PLAYER_BUILD_ONLY solo permite romper bloques colocados por jugadores
- ALLOWED_BLOCKS_ONLY requiere definir allowedBlocks
- Los schematics requieren WorldEdit
- La visualización con partículas se actualiza periódicamente
- El spatial index optimiza búsquedas de regiones en ubicación
- Los metadata son type-safe al obtener
- Las regiones persisten en base de datos si está configurado

## Ver También
- DATABASE_SYSTEM - Persistencia de regiones
- HOLOGRAM_SYSTEM - Hologramas en regiones
- VISUAL/PARTICLE - Visualización de regiones
- ACTION_SYSTEM - Acciones en callbacks
