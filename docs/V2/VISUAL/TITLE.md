# VISUAL SYSTEM - TITLE

## Descripción
Sistema para mostrar títulos y subtítulos en la pantalla del jugador. Soporta tres modos: simple (una vez), permanent (continuo con actualizaciones), y countdown (cuenta regresiva). Incluye configuración de tiempos de fade in/stay/fade out y soporte completo para placeholders.

## API Principal

### Envío Simple
- `send(Player player, String title, String subtitle)` → `CompletableFuture<String>` - Muestra título una vez
- `send(Player player, String title, String subtitle, PlaceholderContext context)` → `CompletableFuture<String>` - Con contexto
- `send(Player player, TitleConfig config)` → `CompletableFuture<String>` - Con configuración completa
- `send(Player player, TitleConfig config, PlaceholderContext context)` → `CompletableFuture<String>` - Config + contexto

### Envío Permanent
- `sendPermanent(Player player, String title, String subtitle)` → `CompletableFuture<String>` - Título permanente
- `sendPermanent(Player player, String title, String subtitle, PlaceholderContext context)` → `CompletableFuture<String>` - Con contexto
- `sendPermanent(Player player, TitleConfig config, PlaceholderContext context)` → `CompletableFuture<String>` - Config completa

### Countdown
- `countdown(Player player, int durationSeconds)` → `CompletableFuture<String>` - Countdown simple (60 segundos)
- `countdown(Player player, int durationSeconds, String title, String subtitle)` → `CompletableFuture<String>` - Con textos
- `countdown(Player player, int durationSeconds, String title, String subtitle, PlaceholderContext context)` → `CompletableFuture<String>`
- `countdown(Player player, int durationSeconds, TitleConfig config, PlaceholderContext context)` → `CompletableFuture<String>`

### Builder
- `builder()` → `TitleBuilder` - Crea un builder de configuración
- `countdownBuilder(Player player, int durationSeconds)` → `CountdownTitleBuilder` - Builder de countdown con callbacks

### Cancelación
- `cancel(Player player, String titleId)` → `boolean` - Cancela título específico
- `cancelAll(Player player)` - Cancela todos los títulos del jugador

## TitleBuilder

Constructor fluido para configuración de títulos:

```java
TitleBuilder.create()
    .title(String title)
    .subtitle(String subtitle)
    .fadeIn(int ticks)
    .stay(int ticks)
    .fadeOut(int ticks)
    .times(int fadeIn, int stay, int fadeOut)
    .permanent()
    .updateInterval(long ticks)
    .build() → TitleConfig
```

**Métodos:**
- `title(String)` - Texto del título
- `subtitle(String)` - Texto del subtítulo
- `fadeIn(int)` - Ticks de fade in (default: 10)
- `stay(int)` - Ticks que permanece (default: 70)
- `fadeOut(int)` - Ticks de fade out (default: 20)
- `times(int, int, int)` - Configura los tres tiempos a la vez
- `permanent()` - Marca como permanente
- `updateInterval(long)` - Intervalo de actualización en ticks (default: 20)

## CountdownTitleBuilder

Builder especializado para countdowns con callbacks:

```java
TitleAPI.countdownBuilder(player, 60)
    .title(String title)
    .subtitle(String subtitle)
    .times(int fadeIn, int stay, int fadeOut)
    .context(PlaceholderContext context)
    .onComplete(Runnable callback)
    .onCancel(Runnable callback)
    .start() → CompletableFuture<String>
```

**Métodos:**
- `title(String)` - Título del countdown
- `subtitle(String)` - Subtítulo (puede usar `{time_formatted}`, `{time}`, etc.)
- `times(int, int, int)` - Tiempos de visualización
- `context(PlaceholderContext)` - Contexto de placeholders
- `onComplete(Runnable)` - Callback al completar
- `onCancel(Runnable)` - Callback al cancelar
- `start()` - Inicia el countdown

## Placeholders de Countdown

Cuando usas countdown, estos placeholders están disponibles:
- `{time}` - Segundos restantes (ej: "60")
- `{time_formatted}` - Tiempo formateado (ej: "1m 0s")
- `{time_minutes}` - Minutos restantes
- `{time_seconds}` - Segundos restantes sin minutos

## Características Principales
- **Tres Modos**: Simple, Permanent, Countdown
- **Async Operations**: Todas las operaciones retornan CompletableFuture
- **Placeholders**: Soporte completo para placeholders con contexto
- **Tiempos Configurables**: Fade in, stay, fade out personalizables
- **Callbacks**: onComplete y onCancel para countdowns
- **Auto-update**: Permanent mode actualiza según updateInterval
- **Cancelación**: Individual o todos los títulos del jugador

## Notas Importantes
- Los tiempos se especifican en **ticks** (20 ticks = 1 segundo)
- Permanent mode muestra el título continuamente hasta cancelar
- Los títulos se cancelan automáticamente al desconectar el jugador
- El procesamiento de placeholders es asíncrono
- Los callbacks se ejecutan en el main thread de Bukkit
- Si no especificas tiempos, usa defaults: fadeIn=10, stay=70, fadeOut=20
- Countdown actualiza automáticamente los placeholders de tiempo
- Puedes cancelar un countdown antes de que termine

## Ver También
- [VISUAL_OVERVIEW](VISUAL_OVERVIEW.md) - Arquitectura común del sistema
- [ACTIONBAR](ACTIONBAR.md) - Alternativa para mensajes persistentes
- [BOSSBAR](BOSSBAR.md) - Otra opción para mostrar información
- PLACEHOLDERS_SYSTEM - Sistema de placeholders
