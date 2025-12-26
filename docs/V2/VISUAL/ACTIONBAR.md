# VISUAL SYSTEM - ACTIONBAR

## Descripción
Sistema para mostrar mensajes en la action bar (barra encima del hotbar). Soporta tres modos: simple, permanent, y countdown. Incluye soporte para global countdowns (broadcast a todos los jugadores) con gestión avanzada (pause, resume, restart). Ideal para información persistente que no obstruye la visión.

## API Principal

### Envío Simple
- `send(Player player, String text)` → `CompletableFuture<String>` - Muestra mensaje una vez
- `send(Player player, String text, PlaceholderContext context)` → `CompletableFuture<String>` - Con contexto
- `send(Player player, ActionBarConfig config)` → `CompletableFuture<String>` - Con configuración
- `send(Player player, ActionBarConfig config, PlaceholderContext context)` → `CompletableFuture<String>`

### Envío Permanent
- `sendPermanent(Player player, String text)` → `CompletableFuture<String>` - Mensaje permanente
- `sendPermanent(Player player, String text, PlaceholderContext context)` → `CompletableFuture<String>` - Con contexto
- `sendPermanent(Player player, ActionBarConfig config, PlaceholderContext context)` → `CompletableFuture<String>`

### Countdown
- `countdown(Player player, int durationSeconds)` → `CompletableFuture<String>` - Countdown simple
- `countdown(Player player, int durationSeconds, String text)` → `CompletableFuture<String>` - Con texto custom
- `countdown(Player player, int durationSeconds, String text, PlaceholderContext context)` → `CompletableFuture<String>`
- `countdown(Player player, int durationSeconds, ActionBarConfig config, PlaceholderContext context)` → `CompletableFuture<String>`

### Countdown en Milisegundos
- `countdownMillis(Player player, long durationMillis)` → `CompletableFuture<String>`
- `countdownMillis(Player player, long durationMillis, String text)` → `CompletableFuture<String>`
- `countdownMillis(Player player, long durationMillis, String text, PlaceholderContext context)` → `CompletableFuture<String>`
- `countdownMillis(Player player, long durationMillis, ActionBarConfig config, PlaceholderContext context)` → `CompletableFuture<String>`

### Builders
- `builder()` → `ActionBarBuilder` - Builder de configuración
- `countdownBuilder(Player player, int durationSeconds)` → `CountdownActionBarBuilder` - Builder countdown
- `countdownMillisBuilder(Player player, long durationMillis)` → `CountdownActionBarBuilder` - Countdown en millis

### Cancelación
- `cancel(Player player, String actionBarId)` → `boolean` - Cancela actionbar específico
- `cancelAll(Player player)` - Cancela todos los actionbars del jugador

## Global Countdowns (Broadcast)

### Creación y Gestión
- `broadcastCountdown(String id, int durationSeconds)` → `GlobalCountdownActionBarBuilder` - Crea countdown global
- `getGlobalCountdown(String id)` → `Optional<GlobalCountdownInstance>` - Obtiene instancia
- `cancelGlobalCountdown(String id)` → `boolean` - Cancela countdown global
- `pauseGlobalCountdown(String id)` → `boolean` - Pausa countdown
- `resumeGlobalCountdown(String id)` → `boolean` - Resume countdown pausado
- `restartGlobalCountdown(String id)` → `boolean` - Reinicia countdown desde el inicio

## ActionBarBuilder

```java
ActionBarBuilder.create()
    .text(String text)
    .permanent()
    .updateInterval(long ticks)
    .build() → ActionBarConfig
```

**Métodos:**
- `text(String)` - Texto del action bar
- `permanent()` - Marca como permanente
- `updateInterval(long)` - Intervalo de actualización (default: 10 ticks)

## CountdownActionBarBuilder

```java
ActionBarAPI.countdownBuilder(player, 30)
    .text(String text)
    .updateInterval(long ticks)
    .context(PlaceholderContext context)
    .onComplete(Runnable callback)
    .onCancel(Runnable callback)
    .start() → CompletableFuture<String>
```

**Métodos:**
- `text(String)` - Texto (usa placeholders de tiempo)
- `updateInterval(long)` - Ticks entre actualizaciones (default: 10)
- `context(PlaceholderContext)` - Contexto personalizado
- `onComplete(Runnable)` - Ejecutado al completar
- `onCancel(Runnable)` - Ejecutado al cancelar
- `start()` - Inicia el countdown

## GlobalCountdownActionBarBuilder

```java
ActionBarAPI.broadcastCountdown("event-countdown", 300)
    .text(String text)
    .updateInterval(long ticks)
    .filter(Predicate<Player> filter)
    .onComplete(Runnable callback)
    .onTick(BiConsumer<Long, GlobalCountdownInstance> tickCallback)
    .start() → CompletableFuture<String>
```

**Métodos:**
- `text(String)` - Texto del countdown (con placeholders)
- `updateInterval(long)` - Ticks entre actualizaciones
- `filter(Predicate<Player>)` - Filtro de jugadores que lo verán
- `onComplete(Runnable)` - Callback al terminar
- `onTick(BiConsumer)` - Callback cada tick (recibe tiempo restante e instancia)
- `start()` - Inicia el countdown global

## Placeholders de Countdown

- `{time}` - Segundos restantes
- `{time_formatted}` - Tiempo formateado (ej: "5m 30s")
- `{time_minutes}` - Minutos restantes
- `{time_seconds}` - Segundos (sin minutos)

## Características Principales
- **Tres Modos**: Simple, Permanent, Countdown
- **Global Countdowns**: Broadcast a todos los jugadores con gestión avanzada
- **Pause/Resume**: Control fino sobre countdowns globales
- **Milisegundos**: Soporte para countdowns de alta precisión
- **Async Operations**: CompletableFuture en todo
- **Filtrado**: Global countdowns pueden filtrar jugadores
- **Callbacks**: onComplete, onCancel, onTick
- **Update Interval**: Configurable (default 10 ticks)

## Notas Importantes
- Action bar desaparece después de ~3 segundos si no se refresca
- Permanent mode refresca automáticamente según `updateInterval`
- Los global countdowns afectan a todos los jugadores online (o filtrados)
- Pause/Resume solo funciona con global countdowns
- Los callbacks onTick se ejecutan en main thread
- `countdownMillis` es útil para duraciones < 1 segundo
- El filtro se aplica dinámicamente - jugadores que se conecten después también lo verán
- Global countdown IDs deben ser únicos globalmente
- Los action bars se cancelan automáticamente al desconectar

## Ver También
- [VISUAL_OVERVIEW](VISUAL_OVERVIEW.md) - Arquitectura común
- [TITLE](TITLE.md) - Alternativa más visible
- [BOSSBAR](BOSSBAR.md) - También soporta global countdowns
- PLACEHOLDERS_SYSTEM - Sistema de placeholders
