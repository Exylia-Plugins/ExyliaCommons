# VISUAL SYSTEM - BOSSBAR

## Descripción
Sistema para mostrar boss bars personalizadas (barras superiores estilo boss de Minecraft). Soporta tres modos: simple, permanent, y countdown. Incluye configuración de color, estilo, progreso, y soporte para global countdowns con gestión avanzada. Ideal para mostrar progreso, timers, o información persistente.

## API Principal

### Envío Simple
- `send(Player player, String text)` → `CompletableFuture<String>` - Boss bar simple
- `send(Player player, String text, PlaceholderContext context)` → `CompletableFuture<String>` - Con contexto
- `send(Player player, BossBarConfig config)` → `CompletableFuture<String>` - Con configuración
- `send(Player player, BossBarConfig config, PlaceholderContext context)` → `CompletableFuture<String>`

### Envío Permanent
- `sendPermanent(Player player, String text)` → `CompletableFuture<String>` - Boss bar permanente
- `sendPermanent(Player player, String text, PlaceholderContext context)` → `CompletableFuture<String>` - Con contexto
- `sendPermanent(Player player, BossBarConfig config, PlaceholderContext context)` → `CompletableFuture<String>`

### Countdown
- `countdown(Player player, int durationSeconds)` → `CompletableFuture<String>` - Countdown simple
- `countdown(Player player, int durationSeconds, String text)` → `CompletableFuture<String>` - Con texto
- `countdown(Player player, int durationSeconds, String text, PlaceholderContext context)` → `CompletableFuture<String>`
- `countdown(Player player, int durationSeconds, BossBarConfig config, PlaceholderContext context)` → `CompletableFuture<String>`

### Builders
- `builder()` → `BossBarBuilder` - Builder de configuración
- `countdownBuilder(Player player, int durationSeconds)` → `CountdownBossBarBuilder` - Builder countdown

### Cancelación
- `cancel(Player player, String bossBarId)` → `boolean` - Cancela boss bar específica
- `cancelAll(Player player)` - Cancela todas las boss bars del jugador

## Global Countdowns (Broadcast)

### Creación y Gestión
- `broadcastCountdown(String id, int durationSeconds)` → `GlobalCountdownBossBarBuilder` - Countdown global
- `getGlobalCountdown(String id)` → `Optional<GlobalCountdownInstance>` - Obtiene instancia
- `cancelGlobalCountdown(String id)` → `boolean` - Cancela
- `pauseGlobalCountdown(String id)` → `boolean` - Pausa
- `resumeGlobalCountdown(String id)` → `boolean` - Resume
- `restartGlobalCountdown(String id)` → `boolean` - Reinicia

## BossBarBuilder

```java
BossBarBuilder.create()
    .text(String text)
    .color(BossBar.Color color)
    .style(BossBar.Overlay style)
    .progress(double progress)
    .permanent()
    .updateInterval(long ticks)
    .build() → BossBarConfig
```

**Métodos:**
- `text(String)` - Texto de la boss bar
- `color(BossBar.Color)` - Color (BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW)
- `style(BossBar.Overlay)` - Estilo (PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20)
- `progress(double)` - Progreso 0.0 - 1.0 (default: 1.0)
- `permanent()` - Marca como permanente
- `updateInterval(long)` - Intervalo de actualización en ticks (default: 20)

## CountdownBossBarBuilder

```java
BossBarAPI.countdownBuilder(player, 120)
    .text(String text)
    .color(BossBar.Color color)
    .style(BossBar.Overlay style)
    .context(PlaceholderContext context)
    .onComplete(Runnable callback)
    .onCancel(Runnable callback)
    .start() → CompletableFuture<String>
```

**Métodos:**
- `text(String)` - Texto (con placeholders de tiempo)
- `color(BossBar.Color)` - Color de la barra
- `style(BossBar.Overlay)` - Estilo de división
- `context(PlaceholderContext)` - Contexto personalizado
- `onComplete(Runnable)` - Callback al completar
- `onCancel(Runnable)` - Callback al cancelar
- `start()` - Inicia el countdown

## GlobalCountdownBossBarBuilder

```java
BossBarAPI.broadcastCountdown("global-event", 300)
    .text(String text)
    .color(BossBar.Color color)
    .style(BossBar.Overlay style)
    .updateInterval(long ticks)
    .filter(Predicate<Player> filter)
    .onComplete(Runnable callback)
    .onTick(BiConsumer<Long, GlobalCountdownInstance> tickCallback)
    .start() → CompletableFuture<String>
```

**Métodos:**
- `text(String)` - Texto del countdown
- `color(BossBar.Color)` - Color
- `style(BossBar.Overlay)` - Estilo
- `updateInterval(long)` - Ticks entre updates
- `filter(Predicate<Player>)` - Filtro de jugadores
- `onComplete(Runnable)` - Al terminar
- `onTick(BiConsumer)` - Cada tick
- `start()` - Inicia

## Colores de BossBar

- `BossBar.Color.BLUE` - Azul
- `BossBar.Color.GREEN` - Verde
- `BossBar.Color.PINK` - Rosa
- `BossBar.Color.PURPLE` - Morado
- `BossBar.Color.RED` - Rojo
- `BossBar.Color.WHITE` - Blanco
- `BossBar.Color.YELLOW` - Amarillo

## Estilos de BossBar

- `BossBar.Overlay.PROGRESS` - Barra continua sin divisiones
- `BossBar.Overlay.NOTCHED_6` - 6 segmentos
- `BossBar.Overlay.NOTCHED_10` - 10 segmentos
- `BossBar.Overlay.NOTCHED_12` - 12 segmentos
- `BossBar.Overlay.NOTCHED_20` - 20 segmentos

## Placeholders de Countdown

- `{time}` - Segundos restantes
- `{time_formatted}` - Tiempo formateado
- `{time_minutes}` - Minutos
- `{time_seconds}` - Segundos

## Características Principales
- **Tres Modos**: Simple, Permanent, Countdown
- **Global Countdowns**: Broadcast con gestión avanzada
- **Colores y Estilos**: 7 colores y 5 estilos diferentes
- **Progreso**: Control del progreso de la barra
- **Pause/Resume**: Control sobre global countdowns
- **Filtrado**: Global countdowns pueden filtrar jugadores
- **Callbacks**: onComplete, onCancel, onTick
- **Auto-progress**: En countdowns, el progreso baja automáticamente

## Notas Importantes
- En countdown mode, el progreso se calcula automáticamente (tiempo restante / duración total)
- Puedes tener múltiples boss bars por jugador
- Las boss bars se apilan verticalmente en la pantalla
- El color y estilo se pueden cambiar dinámicamente (requiere recrear)
- Global countdown IDs deben ser únicos
- Las boss bars se cancelan automáticamente al desconectar
- El filtro en global countdowns se aplica dinámicamente
- Los callbacks se ejecutan en main thread
- El progreso debe estar entre 0.0 y 1.0

## Ver También
- [VISUAL_OVERVIEW](VISUAL_OVERVIEW.md) - Arquitectura común
- [ACTIONBAR](ACTIONBAR.md) - Alternativa más discreta
- [TITLE](TITLE.md) - Alternativa más prominente
- PLACEHOLDERS_SYSTEM - Sistema de placeholders
