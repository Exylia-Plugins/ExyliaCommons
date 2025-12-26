# COMMAND SYSTEM

## Descripción
Sistema de ejecución asíncrona de comandos con soporte completo para placeholders, carga desde configuración YAML, ejecución batch de múltiples comandos, cache de resultados con Caffeine, y proxy command sender para ejecución desde otros contextos. Ideal para ejecutar comandos desde eventos, recompensas, o acciones.

## Inicialización
```java
CommandAPI.initialize(JavaPlugin plugin)
```

## API Principal

### Ejecución Simple
- `execute(Player player, String commandString)` → `CompletableFuture<CommandResult>` - Ejecuta comando async
- `execute(Player player, String commandString, PlaceholderContext context)` → `CompletableFuture<CommandResult>` - Con contexto de placeholders

### Ejecución Batch
- `executeAll(Player player, List<String> commands)` → `CompletableFuture<List<CommandResult>>` - Múltiples comandos
- `executeAll(Player player, List<String> commands, PlaceholderContext context)` → `CompletableFuture<List<CommandResult>>` - Con contexto

### Desde Configuración
- `fromConfig(Player player, ConfigurationSection section)` → `CompletableFuture<List<CommandResult>>` - Carga desde section
- `fromConfig(Player player, ConfigurationSection section, PlaceholderContext context)` → `CompletableFuture<List<CommandResult>>` - Con contexto
- `fromConfigKey(Player player, ConfigurationSection section, String key, PlaceholderContext context)` → `CompletableFuture<List<CommandResult>>` - Desde key específica

### Builder y Gestión
- `builder()` → `CommandBuilder` - Crea builder para comandos programáticos
- `getStats()` → `CommandStats` - Estadísticas del sistema
- `shutdown()` - Cierra el sistema

## CommandBuilder

Builder para construcción programática de comandos:

```java
CommandAPI.builder()
    .command(String command)
    .player(Player player)
    .context(PlaceholderContext context)
    .async(boolean async)
    .execute() → CompletableFuture<CommandResult>
```

## CommandContext

Contexto pasado en ejecución de comandos:
- Contiene el jugador
- Contiene PlaceholderContext para procesamiento
- Metadata adicional si es necesario

## CommandResult

Resultado de ejecución de comando:
- `isSuccess()` → `boolean` - Si se ejecutó exitosamente
- `getCommand()` → `String` - El comando ejecutado
- `getExecutionTime()` → `long` - Tiempo de ejecución en nanos
- `getError()` → `Optional<Throwable>` - Error si falló

## Configuración YAML

```yaml
commands:
  - "give %player% diamond 1"
  - "say %player_name% recibió un diamante"
  - "title %player% title {\"text\":\"Recompensa\",\"color\":\"gold\"}"

# O como sección
rewards:
  commands:
    - "eco give %player% 100"
    - "give %player% emerald 5"
```

Uso:
```java
CommandAPI.fromConfig(player, section.getConfigurationSection("rewards"));
```

## Características Principales
- **Async Execution**: Todos los comandos se ejecutan asíncronamente
- **Placeholder Support**: Procesamiento completo de placeholders
- **Batch Processing**: Ejecuta múltiples comandos en paralelo
- **YAML Integration**: Carga fácil desde configuración
- **Cache System**: Cache de resultados con Caffeine (TTL 5 min, 1000 entradas)
- **Proxy Sender**: Ejecuta comandos como si fueran del jugador
- **Stats Monitoring**: Estadísticas de ejecución
- **Error Handling**: Manejo robusto de errores

## Cache System

El sistema cachea resultados de ejecución:
- **TTL**: 5 minutos
- **Max Entries**: 1000
- **Key**: Combinación de comando + jugador
- Mejora performance para comandos repetitivos

## CommandStats

Estadísticas disponibles:
- Total de comandos ejecutados
- Total de comandos exitosos
- Total de comandos fallados
- Tasa de éxito
- Tiempo promedio de ejecución
- Hit rate del cache
- Comandos en cache

## Proxy Command Sender

El sistema usa un proxy sender que:
- Ejecuta comandos como si fueran del jugador
- Mantiene permisos del jugador
- Redirige output si es necesario
- Funciona con plugins que verifican sender

## Notas Importantes
- Los comandos se ejecutan **desde consola** con permisos completos
- El procesamiento de placeholders es async
- El batch execution ejecuta comandos en paralelo (no secuencial)
- Los comandos se cachean por 5 minutos para mejor performance
- El proxy sender simula que el jugador ejecuta el comando
- Los errores se capturan y retornan en CommandResult
- Los placeholders se procesan antes de ejecutar
- El sistema es thread-safe para uso concurrente
- Los comandos desde config se procesan en lote
- El cache se limpia automáticamente después del TTL

## Ejemplos de Uso

### Simple
```java
CommandAPI.execute(player, "give %player% diamond 1");
```

### Con Contexto
```java
PlaceholderContext context = PlaceholderContext.create()
    .with(customData);

CommandAPI.execute(player, "give %player% %custom_item%", context);
```

### Batch
```java
List<String> commands = Arrays.asList(
    "give %player% diamond 1",
    "eco give %player% 100",
    "title %player% title {\"text\":\"Recompensa!\"}"
);

CommandAPI.executeAll(player, commands);
```

### Desde Config
```yaml
# rewards.yml
rewards:
  victory:
    commands:
      - "give %player% diamond 5"
      - "eco give %player% 1000"
      - "broadcast %player_name% ganó!"
```

```java
ConfigurationSection section = config.getConfigurationSection("rewards.victory");
CommandAPI.fromConfig(player, section);
```

## Ver También
- ACTION_SYSTEM - Sistema de acciones (similar pero más extenso)
- REWARD_SYSTEM - Usa CommandAPI para recompensas
- PLACEHOLDERS_SYSTEM - Placeholders en comandos
- CONFIG_SYSTEM - Carga de comandos desde YAML
