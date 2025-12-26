# RELOAD SYSTEM

## Descripción
Sistema centralizado de recarga async para todos los sistemas de ExyliaCommons V2 y plugin, con detección automática de sistemas disponibles, estadísticas detalladas, timeouts configurables, exclusión selectiva de sistemas, y notificaciones visuales. Permite recargar configuraciones sin reiniciar el servidor.

## Inicialización
```java
ReloadAPI.initialize(JavaPlugin plugin)
```

## API Principal

### Recarga Completa
- `reloadAll()` → `CompletableFuture<ReloadStats>` - Recarga todos los sistemas async
- `reloadAll(Player player)` → `CompletableFuture<ReloadStats>` - Con notificaciones visuales al jugador
- `reloadAll(CommandSender sender)` → `CompletableFuture<ReloadStats>` - Con stats detalladas a sender

### Recarga con Timeout
- `reloadAll(long timeoutSeconds)` → `CompletableFuture<ReloadStats>` - Con timeout
- `reloadAll(Player player, long timeoutSeconds)` → `CompletableFuture<ReloadStats>` - Player + timeout
- `reloadAll(CommandSender sender, long timeoutSeconds)` → `CompletableFuture<ReloadStats>` - Sender + timeout

### Recarga Selectiva
- `reloadAllExcept(String... excludedSystems)` → `CompletableFuture<ReloadStats>` - Excluye sistemas específicos
- `reloadAllExcept(Player player, String... excludedSystems)` → `CompletableFuture<ReloadStats>` - Con jugador
- `reloadAllExcept(CommandSender sender, String... excludedSystems)` → `CompletableFuture<ReloadStats>` - Con sender

### Recarga Individual
- `reloadSystem(String systemName)` → `CompletableFuture<ReloadStats>` - Recarga solo un sistema

### Registro de Sistemas
- `registerReloadable(String name, ReloadableSystem system)` - Registra sistema recargable
- `unregisterReloadable(String name)` - Desregistra sistema

### Consultas
- `getAvailability()` → `SystemAvailability` - Sistemas disponibles y sus estados
- `getRegisteredSystems()` → `List<String>` - Lista de sistemas registrados
- `getAvailableSystems()` → `List<String>` - Lista de sistemas disponibles

### Gestión
- `getInstance()` → `ReloadAPI` - Obtiene instancia
- `isInitialized()` → `boolean` - Verifica inicialización

## ReloadableSystem (Interface)

Para que un sistema sea recargable debe implementar:
```java
public interface ReloadableSystem {
    CompletableFuture<Boolean> reload();
}
```

## ReloadStats

Estadísticas del reload:
- `isSuccess()` → `boolean` - Si el reload fue exitoso
- `getSuccessCount()` → `int` - Sistemas recargados exitosamente
- `getFailureCount()` → `int` - Sistemas que fallaron
- `getSkippedCount()` → `int` - Sistemas saltados
- `getFormattedDuration()` → `String` - Duración formateada
- `getErrors()` → `Map<String, Throwable>` - Errores por sistema

## SystemAvailability

Información de disponibilidad:
- `getAvailableSystems()` → `Set<String>` - Sistemas disponibles
- `isAvailable(String systemName)` → `boolean` - Si un sistema está disponible

## Características Principales
- **Async Reload**: Recarga completamente asíncrona
- **Auto-detection**: Detecta sistemas disponibles automáticamente
- **Selective Reload**: Excluye sistemas específicos
- **Timeout Support**: Cancela reload después de timeout
- **Visual Feedback**: Notificaciones y bossbars para jugadores
- **Detailed Stats**: Estadísticas completas del proceso
- **Error Handling**: Captura y reporta errores por sistema
- **Individual Reload**: Recarga sistemas uno por uno
- **Registration System**: Registro dinámico de sistemas recargables

## Notificaciones Visuales

Cuando se usa con Player/CommandSender:
- BossBar mostrando progreso del reload
- Título con resultado final
- Mensaje de chat con estadísticas detalladas
- Sonidos de éxito/fallo

## Sistemas Auto-Detectados

El sistema detecta automáticamente:
- Config (Configs)
- Database
- Hologram
- Region
- Scoreboard
- Visual systems (Title, ActionBar, BossBar, etc.)
- Y cualquier sistema registrado manualmente

## Notas Importantes
- Todos los reloads se ejecutan async para no bloquear el servidor
- El timeout default es configurable por sistema
- Los errores de un sistema no detienen el reload de otros
- Las notificaciones visuales se envían solo a jugadores (no console)
- Los sistemas se recargan en paralelo para mejor performance
- El ReloadStats incluye errores específicos de cada sistema
- Los sistemas no disponibles son automáticamente saltados
- El registro de sistemas es thread-safe
- Se recomienda timeout de 30-60 segundos para reloads completos
- Los sistemas con dependencias deben manejar el orden internamente

## Ver También
- LIFECYCLE_SYSTEM - Ciclo de vida de sistemas
- CONFIG_SYSTEM - Recarga de configuraciones
- DATABASE_SYSTEM - Recarga de repositorios
- HOLOGRAM_SYSTEM - Recarga de hologramas
