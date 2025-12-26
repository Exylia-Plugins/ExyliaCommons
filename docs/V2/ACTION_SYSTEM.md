# ACTION SYSTEM

## Descripción
Sistema de acciones ejecutables personalizado que permite crear, registrar y ejecutar acciones con handlers custom. Incluye pipeline de middlewares (validación, permisos, cooldowns, rate limiting), sistema de cache con Caffeine, auditoría de ejecuciones, namespaces para organización, y parsing avanzado de argumentos con soporte para quoted strings.

## Inicialización
```java
ActionAPI.initialize(JavaPlugin plugin)
```

## API Principal

### Creación de Acciones
- `create(String id)` → `ActionBuilder` - Crea builder para acción
- `create(String id, JavaPlugin owner)` → `ActionBuilder` - Con plugin owner
- `createChain(String id)` → `ActionChainBuilder` - Builder para cadena de acciones
- `createChain(String id, JavaPlugin owner)` → `ActionChainBuilder`

### Registro
- `register(Action action)` - Registra acción (sync)
- `registerAsync(Action action)` → `CompletableFuture<Void>` - Registra async

### Ejecución
- `execute(String actionString, ActionContext context)` → `ActionResult` - Ejecuta sync
- `executeAsync(String actionString, ActionContext context)` → `CompletableFuture<ActionResult>` - Ejecuta async

### Consulta
- `get(String id)` → `Optional<Action>` - Obtiene acción por ID
- `getAll()` → `Collection<Action>` - Todas las acciones
- `getAllByNamespace(String namespace)` → `Collection<Action>` - Por namespace

### Desregistro
- `unregister(String actionId, JavaPlugin owner)` - Desregistra acción específica
- `unregisterAll(JavaPlugin owner)` - Desregistra todas de un plugin

### Gestión
- `reload()` - Recarga el sistema
- `shutdown()` - Cierra y libera recursos
- `getStats()` → `ActionStats` - Estadísticas detalladas
- `getManager()` → `ActionManager` - Manager para acceso avanzado
- `isInitialized()` → `boolean` - Verifica inicialización

## ActionBuilder

```java
ActionAPI.create("my-action")
    .plugin(JavaPlugin plugin)
    .namespace(String namespace)
    .description(String description)
    .async()  // o .sync()
    .permission(String permission)
    .cooldown(long duration, TimeUnit unit)
    .rateLimit(int requestsPerMinute)
    .auditable()
    .handler(BiConsumer<ActionContext, ParsedArguments> handler)
    .build() → Action
    .buildAsync() → CompletableFuture<Action>
```

**Métodos:**
- `plugin(JavaPlugin)` - Plugin owner
- `namespace(String)` - Namespace para organización (ej: "combat", "economy")
- `description(String)` - Descripción de la acción
- `async()` - Ejecuta en thread pool async
- `sync()` - Ejecuta en main thread (default)
- `permission(String)` - Permiso requerido
- `cooldown(long, TimeUnit)` - Cooldown entre ejecuciones
- `rateLimit(int)` - Límite de requests por minuto
- `auditable()` - Habilita logging de auditoría
- `handler(BiConsumer)` - Handler que ejecuta la acción
- `build()` / `buildAsync()` - Construye la acción

## ActionChainBuilder

Construye cadenas de acciones que se ejecutan secuencialmente:

```java
ActionAPI.createChain("chain-id")
    .add(action1)
    .add(action2)
    .add(action3)
    .onFailure((ctx, error) -> { /* handle error */ })
    .build()
```

## ActionContext

Contexto pasado al handler de la acción:

```java
// En el handler
(ActionContext context, ParsedArguments args) -> {
    Player player = context.getPlayer();
    Location location = context.getLocation();
    Map<String, Object> data = context.getData();

    // Usar parsed arguments
    String arg1 = args.getString(0);
    int arg2 = args.getInt(1);
}
```

## ParsedArguments

Sistema de parsing de argumentos avanzado:

- `getString(int index)` → `String` - Obtiene string
- `getInt(int index)` → `int` - Obtiene int
- `getDouble(int index)` → `double` - Obtiene double
- `getBoolean(int index)` → `boolean` - Obtiene boolean
- `size()` → `int` - Cantidad de argumentos
- Soporte para quoted strings: `"argumento con espacios"`

## Pipeline de Middlewares

Las acciones pasan por un pipeline de middlewares:

1. **ValidationMiddleware** - Valida que la acción exista y esté disponible
2. **PermissionMiddleware** - Verifica permisos
3. **CooldownMiddleware** - Verifica cooldowns
4. **RateLimitMiddleware** - Verifica rate limiting
5. **LoggingMiddleware** - Logging de debug

## Características Principales
- **Namespace Organization**: Organiza acciones por categorías
- **Async/Sync Execution**: Control del thread de ejecución
- **Permission System**: Sistema de permisos integrado
- **Cooldown System**: Cooldowns configurables con cache
- **Rate Limiting**: Token bucket rate limiter
- **Audit Logging**: Logging de ejecuciones
- **Cache System**: Cache con Caffeine para cooldowns, rate limits, y resultados
- **Argument Parsing**: Parsing avanzado con quoted strings
- **Action Chains**: Encadenamiento de acciones
- **Middleware Pipeline**: Sistema extensible de middlewares
- **Stats & Monitoring**: Estadísticas detalladas

## ActionStats

Estadísticas disponibles:
- Total de acciones registradas
- Total de namespaces
- Acciones por namespace
- Estadísticas de cache
- Tamaño de cache de cooldowns
- Tamaño de cache de rate limits
- Tamaño de audit log
- Estado de auditoría

## Notas Importantes
- Debe inicializarse antes de usar con `initialize(plugin)`
- Las acciones async usan un pool de threads dedicado
- El cooldown se aplica por jugador
- El rate limiting usa algoritmo token bucket
- Los middlewares se ejecutan en orden
- El audit log se almacena en memoria (se pierde al reiniciar)
- Los namespaces ayudan a organizar acciones de diferentes features
- Las action chains se ejecutan secuencialmente y paran al primer error
- El parsing de argumentos soporta escaped quotes: `\"texto\"`
- El cache se puede invalidar con `reload()`

## Ver También
- COMMAND_SYSTEM - Sistema de comandos complementario
- REWARD_SYSTEM - Puede usar acciones como recompensas
- PLACEHOLDERS_SYSTEM - Útil para argumentos dinámicos
