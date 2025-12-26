# VISUAL SYSTEM - OVERVIEW

## Descripción
Suite completa de efectos visuales para Minecraft que incluye: títulos, actionbars, bossbars, mensajes, partículas, sonidos, fireworks, efectos de poción y utilidades de color. Todos los subsistemas comparten arquitectura común con tres modos de visualización: simple, permanent (continuous), y countdown. Todas las operaciones son asíncronas con CompletableFuture.

## Subsistemas

1. **TITLE** - Títulos y subtítulos
2. **ACTIONBAR** - Mensajes en action bar
3. **BOSSBAR** - Barras de boss personalizadas
4. **MESSAGE** - Sistema de mensajes formateados
5. **PARTICLE** - Efectos de partículas
6. **SOUND** - Reproducción de sonidos
7. **FIREWORK** - Fuegos artificiales
8. **EFFECT** - Efectos de poción
9. **COLOR** - Utilidades de color y formateo

## Modos de Visualización

### 1. Simple
Muestra el visual una vez y se detiene automáticamente:
```java
TitleAPI.send(player, "Title", "Subtitle")
ActionBarAPI.send(player, "Message")
BossBarAPI.send(player, "Boss Bar")
```

### 2. Permanent (Continuous)
Se muestra continuamente y se actualiza según el `updateInterval`:
```java
TitleAPI.sendPermanent(player, "Title", "Subtitle")
ActionBarAPI.sendPermanent(player, "Message")
BossBarAPI.sendPermanent(player, "Boss Bar")
```

### 3. Countdown
Cuenta regresiva con placeholders de tiempo especiales:
```java
TitleAPI.countdown(player, 60) // 60 segundos
ActionBarAPI.countdown(player, 30)
BossBarAPI.countdown(player, 120)
```

## Arquitectura Común

### Builders
Todos los subsistemas usan patrón builder:
```java
TitleAPI.builder()
    .title("Title")
    .subtitle("Subtitle")
    .permanent()
    .build()
```

### PlaceholderContext
Todos soportan contextos de placeholders:
```java
PlaceholderContext context = PlaceholderContext.create()
    .with(myObject)
    .withPlayer(player);

TitleAPI.send(player, "Title", "Subtitle", context)
```

### CompletableFuture
Todas las operaciones retornan un ID asíncronamente:
```java
CompletableFuture<String> future = TitleAPI.send(player, "Title", "Subtitle");
future.thenAccept(id -> {
    // Usar el ID para cancelar después si es necesario
});
```

### Cancelación
Todos los subsistemas permiten cancelar visuales:
```java
TitleAPI.cancel(player, titleId)
TitleAPI.cancelAll(player)

ActionBarAPI.cancel(player, actionBarId)
BossBarAPI.cancelAll(player)
```

## Countdown Builders

Los subsistemas Title, ActionBar y BossBar tienen builders especiales para countdowns con callbacks:

```java
TitleAPI.countdownBuilder(player, 60)
    .title("Countdown")
    .subtitle("{time_formatted}")
    .onComplete(() -> {
        // Ejecutado cuando termina
    })
    .onCancel(() -> {
        // Ejecutado si se cancela
    })
    .start();
```

## Global Countdowns

ActionBar y BossBar soportan countdowns globales (broadcast):

```java
ActionBarAPI.broadcastCountdown("event-id", 300)
    .text("Evento: {time_formatted}")
    .filter(player -> player.hasPermission("event.see"))
    .onComplete(() -> {
        // Evento terminó
    })
    .start();

// Gestión
ActionBarAPI.pauseGlobalCountdown("event-id")
ActionBarAPI.resumeGlobalCountdown("event-id")
ActionBarAPI.restartGlobalCountdown("event-id")
ActionBarAPI.cancelGlobalCountdown("event-id")
```

## Placeholders de Tiempo en Countdowns

Los countdowns automáticamente añaden estos placeholders:
- `{time}` - Segundos restantes
- `{time_formatted}` - Tiempo formateado (ej: "1m 30s")
- `{time_minutes}` - Minutos restantes
- `{time_seconds}` - Segundos restantes (sin minutos)

## Características Comunes

### Async Operations
- Todas las operaciones usan CompletableFuture
- No bloquean el main thread
- Procesamiento paralelo de placeholders

### Cache System
- Cache de componentes procesados
- Cache de colores parseados
- Cache de placeholders resueltos
- Mejora significativa de rendimiento

### Validation
- Validación automática de configuraciones
- Valores por defecto sensatos
- Manejo de errores graceful

### Lifecycle Management
- Gestión automática del ciclo de vida
- Limpieza automática al desconectar jugador
- Prevención de memory leaks

### Thread-Safe
- Todos los subsistemas son thread-safe
- Uso de concurrent collections
- Sincronización apropiada

## Integración con Placeholders

Todos los subsistemas se integran con el PLACEHOLDERS_SYSTEM:
- Procesamiento automático de placeholders
- Soporte para PlaceholderAPI
- Contexts personalizados
- Update automático en permanent mode

## Sistema de Colores

El subsistema COLOR proporciona utilidades usadas por todos los demás:
- Parsing de hex colors (<#FF5733>)
- Legacy colors (&6, &c, etc.)
- Gradientes de color
- Presets de colores
- Centrado de mensajes
- Transformación de fonts

## Inicialización

El sistema Visual se inicializa automáticamente con ExyliaCommons. Solo el subsistema COLOR requiere inicialización manual si se usan presets personalizados:

```java
ColorAPI.initialize(plugin)
ColorAPI.initialize(plugin, customPresets)
```

## Notas Importantes

- Todos los IDs retornados son únicos por jugador
- Los visuales permanent se actualizan según `updateInterval`
- Los countdowns usan ticks (20 ticks = 1 segundo)
- Global countdowns afectan a todos los jugadores online
- El cache se limpia automáticamente para evitar memory leaks
- Los visuales se cancelan automáticamente al desconectar el jugador
- PlaceholderContext es inmutable - usar `.with()` crea nuevas instancias
- Los builders son reutilizables pero no thread-safe
- Los renders ejecutan en el main thread de Bukkit
- El procesamiento de placeholders es async

## Ver También

- [TITLE](TITLE.md) - Títulos y subtítulos
- [ACTIONBAR](ACTIONBAR.md) - Action bars
- [BOSSBAR](BOSSBAR.md) - Boss bars
- [MESSAGE](MESSAGE.md) - Mensajes formateados
- [PARTICLE](PARTICLE.md) - Partículas
- [SOUND](SOUND.md) - Sonidos
- [FIREWORK](FIREWORK.md) - Fuegos artificiales
- [EFFECT](EFFECT.md) - Efectos de poción
- [COLOR](COLOR.md) - Utilidades de color
- PLACEHOLDERS_SYSTEM - Sistema de placeholders integrado
