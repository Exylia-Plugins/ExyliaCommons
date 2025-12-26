# VISUAL SYSTEM - PARTICLE

## Descripción
Sistema para spawning de partículas de Minecraft. Soporta spawn simple, en ubicación específica, a jugadores cercanos, con filtros, en radio, y modos continuous y countdown. Incluye parsing desde strings para configuración fácil desde YAML.

## API Principal

### Spawn Simple
- `spawn(Player player, Particle particle)` → `CompletableFuture<Void>` - 1 partícula al jugador
- `spawn(Player player, Particle particle, int count)` → `CompletableFuture<Void>` - N partículas
- `spawn(Location location, Particle particle)` → `CompletableFuture<Void>` - En ubicación
- `spawn(Location location, Particle particle, int count)` → `CompletableFuture<Void>`

### Desde String
- `spawn(Player player, String particleString)` → `CompletableFuture<Void>` - Parse desde string
- `spawn(Player player, ParticleConfig config)` → `CompletableFuture<Void>` - Con configuración

### A Jugadores Cercanos
- `spawnNearby(Player player, Particle particle, int count)` → `CompletableFuture<Void>` - Jugadores cercanos

### Filtrado
- `spawnToFiltered(Predicate<Player> filter, Particle particle, int count)` → `CompletableFuture<Void>` - Con filtro

### Por Radio
- `spawnInRadius(Location origin, double radius, Particle particle, int count)` → `CompletableFuture<Void>` - En radio

### Continuous y Countdown
- `spawnContinuous(Player player, ParticleConfig config)` → `CompletableFuture<String>` - Spawn continuo
- `spawnCountdown(Player player, ParticleConfig config, long durationTicks)` → `CompletableFuture<String>` - Countdown

### Builder
- `builder()` → `ParticleBuilder` - Builder de configuración

## ParticleBuilder

```java
ParticleBuilder.create()
    .particle(Particle particle)
    .count(int count)
    .offset(double offsetX, double offsetY, double offsetZ)
    .speed(double speed)
    .atLocation(Location location)
    .nearby()
    .build() → ParticleConfig
```

**Métodos:**
- `particle(Particle)` - Tipo de partícula
- `count(int)` - Cantidad de partículas
- `offset(double, double, double)` - Offset en X, Y, Z
- `speed(double)` - Velocidad de las partículas
- `atLocation(Location)` - Ubicación específica (si no, usa ubicación del jugador)
- `nearby()` - Spawn a jugadores cercanos

## Formato String

```java
"FLAME:10:0.5:0.5:0.5:0.1"
// PARTICLE:count:offsetX:offsetY:offsetZ:speed
```

Ejemplo:
```java
ParticleAPI.spawn(player, "HEART:5:0.3:0.3:0.3:0.05")
```

## Tipos de Partículas Comunes

- `Particle.FLAME` - Llamas
- `Particle.HEART` - Corazones
- `Particle.VILLAGER_HAPPY` - Felicidad de aldeano
- `Particle.SMOKE_NORMAL` - Humo
- `Particle.CRIT` - Crítico
- `Particle.ENCHANTMENT_TABLE` - Encantamiento
- `Particle.EXPLOSION_NORMAL` - Explosión
- `Particle.PORTAL` - Portal
- `Particle.CLOUD` - Nube
- Y muchos más...

## Características Principales
- **Spawn Simple**: A jugador o ubicación
- **Nearby**: Spawn a jugadores cercanos automáticamente
- **Filtrado**: Spawn con predicates
- **Radio**: Spawn en área de radio
- **Continuous**: Spawn continuo con intervalo
- **Countdown**: Spawn durante countdown
- **Configuración**: Offset, speed, count personalizables
- **String Parsing**: Configuración desde strings
- **Async**: Operaciones asíncronas

## Notas Importantes
- Si spawn en Location y no hay jugadores en el mundo, no hace nada
- "Nearby" spawna a todos los jugadores en rango de visualización
- El offset afecta el área de spawn de las partículas
- Speed afecta la velocidad de movimiento de las partículas
- Algunas partículas ignoran offset y speed
- El radio usa distanceSquared para performance
- Continuous requiere cancelación manual
- Las partículas son client-side - cada jugador las ve
- El count afecta la cantidad, no la duración
- Diferentes partículas tienen comportamientos únicos

## Ver También
- [VISUAL_OVERVIEW](VISUAL_OVERVIEW.md) - Arquitectura común
- [SOUND](SOUND.md) - Sistema de sonidos complementario
- [FIREWORK](FIREWORK.md) - Efectos visuales más complejos
