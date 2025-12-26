# VISUAL SYSTEM - SOUND

## Descripción
Sistema para reproducción de sonidos de Minecraft. Soporta reproducción a jugador, en ubicación, a jugadores cercanos, con filtros, en radio, y modos continuous y countdown. Incluye configuración de volumen y pitch, y parsing desde strings.

## API Principal

### Reproducción Simple
- `play(Player player, Sound sound)` → `CompletableFuture<Void>` - Volumen y pitch default (1.0)
- `play(Player player, Sound sound, float volume, float pitch)` → `CompletableFuture<Void>` - Con volumen y pitch
- `play(Player player, String soundString)` → `CompletableFuture<Void>` - Desde string
- `play(Player player, SoundConfig config)` → `CompletableFuture<Void>` - Con configuración

### En Ubicación
- `playAt(Location location, Sound sound)` → `CompletableFuture<Void>` - En ubicación específica
- `playAt(Location location, Sound sound, float volume, float pitch)` → `CompletableFuture<Void>`

### A Jugadores Cercanos
- `playNearby(Player player, Sound sound)` → `CompletableFuture<Void>` - Jugadores cercanos
- `playNearby(Player player, Sound sound, float volume, float pitch)` → `CompletableFuture<Void>`

### Filtrado
- `playToFiltered(Predicate<Player> filter, Sound sound)` → `CompletableFuture<Void>` - Con filtro
- `playToFiltered(Predicate<Player> filter, Sound sound, float volume, float pitch)` → `CompletableFuture<Void>`

### Por Radio
- `playInRadius(Location origin, double radius, Sound sound)` → `CompletableFuture<Void>` - En radio
- `playInRadius(Location origin, double radius, Sound sound, float volume, float pitch)` → `CompletableFuture<Void>`

### Continuous y Countdown
- `playContinuous(Player player, SoundConfig config)` → `CompletableFuture<String>` - Reproducción continua
- `playCountdown(Player player, SoundConfig config, long durationTicks)` → `CompletableFuture<String>` - Durante countdown

### Builder
- `builder()` → `SoundBuilder` - Builder de configuración

## SoundBuilder

```java
SoundBuilder.create()
    .sound(Sound sound)
    .volume(float volume)
    .pitch(float pitch)
    .atLocation(Location location)
    .nearby()
    .build() → SoundConfig
```

**Métodos:**
- `sound(Sound)` - Tipo de sonido
- `volume(float)` - Volumen (0.0 - infinito, default: 1.0)
- `pitch(float)` - Pitch (0.5 - 2.0, default: 1.0)
- `atLocation(Location)` - Ubicación específica
- `nearby()` - Reproduce a jugadores cercanos

## Formato String

```java
"UI_BUTTON_CLICK:1.0:1.0"
// SOUND:volume:pitch
```

Ejemplo:
```java
SoundAPI.play(player, "ENTITY_EXPERIENCE_ORB_PICKUP:0.5:1.5")
```

## Parámetros de Sonido

### Volume (Volumen)
- **0.0**: Sin sonido
- **1.0**: Volumen normal (default)
- **>1.0**: Más fuerte
- El volumen también afecta el rango de audición

### Pitch (Tono)
- **0.5**: Muy grave
- **1.0**: Tono normal (default)
- **2.0**: Muy agudo
- Afecta la velocidad y tono del sonido

## Sonidos Comunes

### UI
- `Sound.UI_BUTTON_CLICK` - Click de botón
- `Sound.BLOCK_NOTE_BLOCK_PLING` - Pling
- `Sound.ENTITY_EXPERIENCE_ORB_PICKUP` - Experiencia

### Entidades
- `Sound.ENTITY_PLAYER_LEVELUP` - Level up
- `Sound.ENTITY_VILLAGER_YES` - Aldeano sí
- `Sound.ENTITY_VILLAGER_NO` - Aldeano no
- `Sound.ENTITY_FIREWORK_ROCKET_BLAST` - Explosión firework

### Ambiente
- `Sound.BLOCK_BELL_USE` - Campana
- `Sound.BLOCK_ANVIL_LAND` - Anvil
- `Sound.AMBIENT_CAVE` - Cueva

## Características Principales
- **Volumen y Pitch**: Control preciso del sonido
- **Ubicación**: Reproducción en ubicación específica
- **Nearby**: A jugadores cercanos
- **Filtrado**: Con predicates
- **Radio**: En área de radio
- **Continuous**: Reproducción continua
- **Countdown**: Durante countdown
- **String Parsing**: Configuración desde strings
- **Async**: Operaciones asíncronas

## Notas Importantes
- El volumen afecta qué tan lejos se escucha el sonido
- Pitch fuera de 0.5-2.0 puede sonar extraño
- Si playAt en ubicación sin jugadores, no hace nada
- "Nearby" reproduce a jugadores en rango de audición
- El radio usa distanceSquared para performance
- Continuous reproduce el sonido repetidamente según interval
- Los sonidos son client-side
- Volume > 1.0 aumenta el rango de audición
- Algunos sonidos tienen variaciones aleatorias de pitch
- Los sonidos custom requieren resource pack

## Ver También
- [VISUAL_OVERVIEW](VISUAL_OVERVIEW.md) - Arquitectura común
- [PARTICLE](PARTICLE.md) - Efectos visuales complementarios
- [FIREWORK](FIREWORK.md) - Efectos con sonido incluido
