# SCOREBOARD SYSTEM

## Descripción
Sistema completo de scoreboards usando FastBoard con async rendering, cache de líneas procesadas, soporte de placeholders, teams configurables, actualización automática, contexts personalizados por jugador, y carga desde YAML. Optimizado para alto rendimiento con cache inteligente.

## Inicialización
```java
ScoreboardAPI.initialize(Plugin plugin)
```

## API Principal

### Builder y Carga
- `builder()` → `ScoreboardBuilder` - Crea builder
- `load(ConfigurationSection section)` → `Scoreboard` - Carga desde YAML

### Mostrar/Ocultar
- `show(Player player, Scoreboard scoreboard)` → `CompletableFuture<String>` - Muestra scoreboard
- `show(Player player, Scoreboard scoreboard, PlaceholderContext context)` → `CompletableFuture<String>` - Con contexto
- `hide(Player player)` → `boolean` - Oculta scoreboard del jugador
- `hideAll()` - Oculta todos los scoreboards

### Consultas
- `has(Player player)` → `boolean` - Tiene scoreboard activo
- `get(Player player)` → `Optional<ScoreboardInstance>` - Obtiene instancia
- `getActiveCount()` → `int` - Cantidad de scoreboards activos

### Actualización
- `updateContext(Player player, PlaceholderContext context)` - Actualiza contexto
- `forceUpdate(Player player)` - Fuerza actualización inmediata

### Teams
- `addToTeam(Player owner, Player target)` → `boolean` - Añade a team
- `removeFromTeam(Player owner, Player target)` → `boolean` - Remueve de team
- `isInTeam(Player owner, Player target)` → `boolean` - Verifica membresía
- `getTeamMembers(Player owner)` → `Set<String>` - Obtiene miembros
- `clearTeam(Player owner)` - Limpia team
- `updateTeamPrefix(Player owner, String prefix)` - Actualiza prefix
- `updateTeamSuffix(Player owner, String suffix)` - Actualiza suffix

### Gestión
- `getStats()` → `ScoreboardStats` - Estadísticas del sistema
- `clearCache()` - Limpia cache de líneas procesadas

## ScoreboardBuilder

```java
ScoreboardAPI.builder()
    .title(String title)
    .line(String line)
    .lines(String... lines)
    .updateInterval(long ticks)
    .enabled(boolean enabled)
    .withTeam()
    .teamName(String name)
    .teamPrefix(String prefix)
    .teamSuffix(String suffix)
    .teamColor(ChatColor color)
    .build() → Scoreboard
```

**Métodos:**
- `title(String)` - Título del scoreboard
- `line(String)` - Añade una línea
- `lines(String...)` - Añade múltiples líneas
- `updateInterval(long)` - Ticks entre actualizaciones (default: 20)
- `enabled(boolean)` - Estado inicial (default: true)
- `withTeam()` - Habilita team
- `teamName(String)` - Nombre del team
- `teamPrefix(String)` - Prefix del team
- `teamSuffix(String)` - Suffix del team
- `teamColor(ChatColor)` - Color del team

## Configuración YAML

```yaml
scoreboard:
  enabled: true
  title: "&6Mi Scoreboard"
  lines:
    - "&7Jugador: &f%player_name%"
    - "&7Nivel: &a%player_level%"
    - ""
    - "&7Kills: &c%player_kills%"
    - "&7Deaths: &7%player_deaths%"
  update-interval: 20
  team:
    enabled: true
    name: "main"
    prefix: "&a[VIP] "
    suffix: ""
    color: GREEN
```

## ScoreboardInstance

Instancia activa de un scoreboard para un jugador:
- Maneja el FastBoard subyacente
- Procesa placeholders con PlaceholderContext
- Actualiza automáticamente según updateInterval
- Gestiona el team si está configurado
- Cache de líneas procesadas para performance

## Team System

Los teams permiten prefijos, sufijos y colores para jugadores:

```java
// Configurar team en scoreboard
ScoreboardAPI.builder()
    .withTeam()
    .teamPrefix("&a[VIP] ")
    .teamColor(ChatColor.GREEN)
    .build();

// Añadir jugadores al team
ScoreboardAPI.addToTeam(owner, target1);
ScoreboardAPI.addToTeam(owner, target2);

// Actualizar prefix/suffix dinámicamente
ScoreboardAPI.updateTeamPrefix(owner, "&b[ADMIN] ");
```

## PlaceholderContext

Los scoreboards soportan contexts personalizados:

```java
PlaceholderContext context = PlaceholderContext.create()
    .with(customData)
    .withPlayer(player);

ScoreboardAPI.show(player, scoreboard, context);

// Actualizar contexto sin recrear scoreboard
ScoreboardAPI.updateContext(player, newContext);
```

## Características Principales
- **FastBoard Integration**: Usa FastBoard para performance óptimo
- **Async Rendering**: Procesamiento de placeholders async
- **Line Caching**: Cache de líneas procesadas
- **Auto-update**: Actualización automática según interval
- **Placeholder Support**: Soporte completo con contexts
- **Team System**: Prefijos, sufijos y colores configurables
- **Per-Player Contexts**: Contexto único por jugador
- **Dynamic Updates**: Actualización de contexto sin recrear
- **Stats Monitoring**: Estadísticas de uso y performance
- **YAML Configuration**: Carga fácil desde config

## ScoreboardStats

Estadísticas disponibles:
- Total de scoreboards activos
- Total de actualizaciones procesadas
- Hit rate del cache de líneas
- Tiempo promedio de actualización
- Scoreboards con teams
- Tamaño del cache

## Notas Importantes
- El sistema usa FastBoard internamente para mejor performance
- Las líneas se procesan async para no bloquear main thread
- El cache de líneas procesadas mejora significativamente el rendimiento
- El updateInterval determina frecuencia de actualización de placeholders
- Los teams afectan cómo se ven los nametags de jugadores
- Múltiples jugadores pueden estar en el mismo team
- El team se gestiona por el "owner" del scoreboard
- Los scoreboards se ocultan automáticamente al desconectar
- El contexto de placeholders persiste entre actualizaciones
- `forceUpdate()` útil para actualizar inmediatamente después de cambio de datos

## Ver También
- PLACEHOLDERS_SYSTEM - Placeholders en líneas
- VISUAL/COLOR - Colores en título y líneas
- FORMATTER_SYSTEM - Formateo de valores en líneas
- DATABASE_SYSTEM - Persistencia de configuración
