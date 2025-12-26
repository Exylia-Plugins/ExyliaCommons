# CLAN SYSTEM

## Descripción
Sistema de integración con plugins de clanes que proporciona una API unificada para consultar información de clanes independientemente del plugin usado. Soporta múltiples providers (SimpleClans, ClansPro, etc.), cache de alta performance con Caffeine, operaciones async, y detección automática de plugins. Ideal para sistemas que requieren información de clanes sin acoplarse a un plugin específico.

## Inicialización
```java
ClanAPI.initialize(JavaPlugin plugin)
```

## API Principal

### Consultas de Clan por Jugador
- `getPlayerClan(UUID playerId)` → `Optional<Clan>` - Clan del jugador sync
- `getPlayerClan(Player player)` → `Optional<Clan>` - Por Player object
- `getPlayerClanAsync(UUID playerId)` → `CompletableFuture<Optional<Clan>>` - Async
- `getPlayerClanAsync(Player player)` → `CompletableFuture<Optional<Clan>>` - Async

### Consultas de Nombre/Tag
- `getPlayerClanName(UUID playerId)` → `Optional<String>` - Nombre del clan
- `getPlayerClanName(Player player)` → `Optional<String>` - Por Player
- `getPlayerClanNameAsync(UUID playerId)` → `CompletableFuture<Optional<String>>` - Async
- `getPlayerClanTag(UUID playerId)` → `Optional<String>` - Tag del clan
- `getPlayerClanTag(Player player)` → `Optional<String>` - Por Player
- `getPlayerClanTagAsync(UUID playerId)` → `CompletableFuture<Optional<String>>` - Async

### Búsqueda de Clanes
- `getClanByTag(String tag)` → `Optional<Clan>` - Clan por tag
- `getClanByTagAsync(String tag)` → `CompletableFuture<Optional<Clan>>` - Async
- `getClanById(String id)` → `Optional<Clan>` - Clan por ID
- `getClanByIdAsync(String id)` → `CompletableFuture<Optional<Clan>>` - Async
- `getAllClans()` → `Collection<Clan>` - Todos los clanes
- `getAllClansAsync()` → `CompletableFuture<Collection<Clan>>` - Async

### Verificaciones
- `hasPlayerClan(UUID playerId)` → `boolean` - Si jugador tiene clan
- `hasPlayerClan(Player player)` → `boolean` - Por Player
- `isSameClan(UUID player1, UUID player2)` → `boolean` - Si están en el mismo clan
- `isSameClan(Player player1, Player player2)` → `boolean` - Por Player objects

### Roles
- `isLeader(UUID playerId)` → `boolean` - Si es líder del clan
- `isLeader(Player player)` → `boolean` - Por Player
- `isModerator(UUID playerId)` → `boolean` - Si es moderador del clan
- `isModerator(Player player)` → `boolean` - Por Player

### Provider
- `getActiveProviderName()` → `String` - Nombre del provider activo
- `getActiveProvider()` → `ClanProvider` - Provider activo

### Gestión
- `isInitialized()` → `boolean` - Verifica inicialización
- `reload()` - Recarga el sistema
- `shutdown()` - Cierra el sistema
- `getStats()` → `ClanStats` - Estadísticas del sistema
- `clearCache()` - Limpia cache
- `getManager()` → `ClanManager` - Manager para acceso avanzado

## Clan (Modelo)

Modelo unificado de clan:
- `getId()` → `String` - ID único del clan
- `getName()` → `String` - Nombre del clan
- `getTag()` → `String` - Tag/prefijo del clan
- `getLeader()` → `UUID` - UUID del líder
- `getMembers()` → `Set<UUID>` - Miembros del clan
- `getModerators()` → `Set<UUID>` - Moderadores del clan
- `isLeader(UUID playerId)` → `boolean` - Si es líder
- `isModerator(UUID playerId)` → `boolean` - Si es moderador
- `isMember(UUID playerId)` → `boolean` - Si es miembro

## ClanProvider (Interface)

Interface que implementan los adapters de plugins:
- `getProviderName()` → `String` - Nombre del provider
- `isAvailable()` → `boolean` - Si el plugin está disponible
- `getPlayerClan(UUID)` → `Optional<Clan>` - Implementación específica
- Y otros métodos de consulta

## ClanStats

Estadísticas del sistema:
- `getProviderName()` → `String` - Provider activo
- `getTotalClans()` → `int` - Total de clanes
- `getPlayerClanCacheSize()` → `long` - Tamaño cache de jugadores
- `getClanDataCacheSize()` → `long` - Tamaño cache de clanes
- `getPlayerClanCacheHitRate()` → `double` - Hit rate cache jugadores
- `getClanDataCacheHitRate()` → `double` - Hit rate cache clanes

## Providers Soportados

El sistema detecta automáticamente:
1. **SimpleClans** - Plugin SimpleClans
2. **ClansPro** - Plugin ClansPro
3. **NoProvider** - Fallback cuando no hay plugin (retorna vacío)

## Características Principales
- **Multi-Provider**: Soporta múltiples plugins de clanes
- **Unified API**: API consistente sin importar el plugin
- **Auto-Detection**: Detecta y usa el plugin disponible
- **Caffeine Cache**: Cache de alta performance
- **Async Operations**: Operaciones async para consultas pesadas
- **Fallback Provider**: NoProvider cuando no hay plugin de clanes
- **Stats & Monitoring**: Estadísticas detalladas de uso
- **Thread-Safe**: Seguro para uso concurrente

## Cache System

El sistema mantiene 2 caches:
1. **Player → Clan Cache**: Mapeo de jugador a clan
2. **Clan Data Cache**: Datos completos de clanes

Configuración del cache:
- TTL: 5 minutos
- Max Size: 1000 entradas
- Eviction automática
- Hit rate tracking

## Notas Importantes
- El sistema requiere un plugin de clanes instalado (o usa NoProvider)
- El provider se selecciona automáticamente en la inicialización
- El cache mejora significativamente el rendimiento
- Las operaciones async son recomendadas para consultas en hot paths
- El NoProvider retorna Optional.empty() para todas las consultas
- Los métodos sync bloquean el thread hasta obtener resultado
- El cache se invalida automáticamente después del TTL
- `isSameClan` retorna false si alguno no tiene clan
- Los roles (leader/moderator) dependen del plugin de clanes
- El sistema es read-only (no crea/modifica clanes)
- El reload recarga la configuración del cache
- Las estadísticas incluyen hit rates para optimización
- El sistema es ideal para PvP, protecciones, o features sociales

## Ver También
- PLACEHOLDERS_SYSTEM - Placeholders de clanes
- DATABASE_SYSTEM - Almacenamiento relacionado a clanes
- REGION_SYSTEM - Protecciones por clan
