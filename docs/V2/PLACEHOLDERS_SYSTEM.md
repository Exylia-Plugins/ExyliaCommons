# PLACEHOLDERS SYSTEM

## Descripción
Sistema avanzado de placeholders con soporte para cache, procesamiento async, integración con PlaceholderAPI, y registro mediante anotaciones. Permite crear placeholders personalizados con tres scopes: globales, de jugador, y contextuales. Incluye sistema de contexts tipados para pasar datos adicionales durante el procesamiento.

## Inicialización
```java
Placeholders.initialize(JavaPlugin plugin)
```

Para integración con PlaceholderAPI:
```java
Placeholders.registerPapiExpander(String identifier)
```

## API Principal

### Registro de Resolvers

#### Registro Manual
- `registerGlobal(String name, GlobalPlaceholderResolver resolver)` - Placeholder global sin contexto
- `registerPlayer(String name, PlayerPlaceholderResolver resolver)` - Placeholder que requiere Player
- `registerContext(String name, ContextPlaceholderResolver resolver)` - Placeholder que usa PlaceholderContext

#### Registro por Anotaciones
- `registerAnnotatedClass(Object instance)` - Escanea y registra métodos con @Placeholder
- `registerAnnotatedClasses(Object... instances)` - Registra múltiples clases

### Procesamiento (Sync)
- `process(String text)` → `String` - Procesa solo placeholders globales
- `process(String text, Player player)` → `String` - Procesa con contexto de jugador
- `process(String text, PlaceholderContext context)` → `String` - Procesa con contexto custom
- `process(String text, Player player, PlaceholderContext context)` → `String` - Procesa con ambos

### Procesamiento (Async)
- `processAsync(String text)` → `CompletableFuture<String>` - Async global
- `processAsync(String text, Player player)` → `CompletableFuture<String>` - Async con player
- `processAsync(String text, PlaceholderContext context)` → `CompletableFuture<String>` - Async con context
- `processAsync(String text, Player player, PlaceholderContext context)` → `CompletableFuture<String>` - Async completo

### Utilidades
- `extractPlaceholders(String text)` → `List<String>` - Extrae todos los placeholders del texto
- `containsPlaceholders(String text)` → `boolean` - Verifica si contiene placeholders
- `hasResolver(String name)` → `boolean` - Verifica si un resolver está registrado
- `getRegisteredPlaceholders()` → `Set<String>` - Obtiene todos los placeholders registrados

### Gestión de Cache y Stats
- `clearCache()` - Limpia cache de placeholders
- `getStats()` → `PlaceholderRegistryStats` - Obtiene estadísticas
- `shutdown()` - Cierra el sistema y desregistra PAPI

### Creación de Contexts
- `createContext()` → `PlaceholderContext` - Crea contexto vacío
- `createContext(Player player)` → `PlaceholderContext` - Crea contexto con player

## Anotación @Placeholder

Permite registrar placeholders mediante anotaciones en métodos:

```java
@Placeholder(
    name = "placeholder_name",
    description = "Descripción del placeholder",
    scope = PlaceholderScope.GLOBAL,  // GLOBAL, PLAYER, CONTEXT
    cacheable = true,
    cacheTtlMs = 1000,
    async = false
)
public String myPlaceholder() {
    return "valor";
}
```

**Parámetros de la Anotación:**
- `name` - Nombre del placeholder (sin % %)
- `description` - Descripción para documentación
- `scope` - GLOBAL (sin contexto), PLAYER (requiere Player), CONTEXT (requiere PlaceholderContext)
- `cacheable` - Si el resultado debe cachearse
- `cacheTtlMs` - Tiempo de vida del cache en milisegundos
- `async` - Si debe ejecutarse asíncronamente

## PlaceholderContext

Sistema de contexto tipado para pasar datos adicionales:

```java
PlaceholderContext context = PlaceholderContext.create()
    .with(myObject)
    .with(MyClass.class, instance)
    .put("key", value)
    .withPlayer(player)
    .withCurrentTime();

Object obj = context.get("key");
MyClass instance = context.get(MyClass.class);
boolean has = context.has("key");
PlaceholderContext copy = context.copy();
```

**Métodos:**
- `with(Object object)` - Añade objeto por su clase
- `with(Class<T> type, T object)` - Añade objeto con tipo específico
- `put(String key, Object value)` - Añade valor con key string
- `withPlayer(Player player)` - Añade jugador al contexto
- `withCurrentTime()` - Añade timestamp actual
- `get(Class<T> type)` → `T` - Obtiene objeto por tipo
- `get(String key)` → `Object` - Obtiene por key
- `has(String key)` → `boolean` - Verifica existencia
- `copy()` → `PlaceholderContext` - Crea copia

## Tipos de Resolvers

### GlobalPlaceholderResolver
```java
() -> String
```
No recibe parámetros, retorna String.

### PlayerPlaceholderResolver
```java
(Player player) -> String
```
Recibe Player, retorna String.

### ContextPlaceholderResolver
```java
(PlaceholderContext context) -> String
```
Recibe PlaceholderContext, retorna String.

## Características Principales
- **Cache Inteligente**: Cache configurable por placeholder con TTL
- **Async Support**: Procesamiento asíncrono con CompletableFuture
- **Integración PAPI**: Soporte completo para PlaceholderAPI
- **Registro por Anotaciones**: Registro declarativo con @Placeholder
- **Contexts Tipados**: Sistema de contexto type-safe
- **Múltiples Scopes**: Global, Player, y Context
- **Extracción de Placeholders**: Análisis de texto para detectar placeholders
- **Thread-Safe**: Seguro para uso concurrente

## Notas Importantes
- Los placeholders se procesan con el formato `%placeholder_name%`
- El cache se aplica solo si `cacheable = true` en la anotación
- Los resolvers async deben manejar sus propios errores
- PlaceholderAPI debe estar instalado para usar la integración PAPI
- El contexto es inmutable - `with()` y `put()` retornan nuevas instancias
- Los métodos anotados deben ser públicos y accesibles
- El registro por anotaciones escanea toda la clase al llamar `registerAnnotatedClass()`

## Ver También
- CONFIG_SYSTEM - Usa placeholders en configuración
- VISUAL/MESSAGE - Sistema de mensajes usa placeholders
- SCOREBOARD_SYSTEM - Scoreboards usan placeholders
- HOLOGRAM_SYSTEM - Hologramas usan placeholders
