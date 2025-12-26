# VISUAL SYSTEM - FIREWORK

## Descripción
Sistema para lanzar fuegos artificiales con efectos personalizados. Soporta diferentes tipos de explosión, colores, fade colors, flicker, trail, power, filtrado, radio, y modos continuous y countdown. Ideal para celebraciones, efectos visuales llamativos, o recompensas.

## API Principal

### Lanzamiento Simple
- `launch(Location location, FireworkEffect.Type type, Color... colors)` → `CompletableFuture<Void>` - Lanza en ubicación
- `launch(Location location, FireworkEffect.Type type, List<Color> colors)` → `CompletableFuture<Void>`
- `launch(Player player, FireworkEffect.Type type, Color... colors)` → `CompletableFuture<Void>` - En ubicación del jugador
- `launch(Player player, String fireworkString)` → `CompletableFuture<Void>` - Desde string
- `launch(Player player, FireworkConfig config)` → `CompletableFuture<Void>` - Con configuración

### Filtrado
- `launchToFiltered(Predicate<Player> filter, FireworkEffect.Type type, List<Color> colors)` → `CompletableFuture<Void>`

### Por Radio
- `launchInRadius(Location origin, double radius, FireworkEffect.Type type, List<Color> colors)` → `CompletableFuture<Void>`

### Continuous y Countdown
- `launchContinuous(Player player, FireworkConfig config)` → `CompletableFuture<String>` - Lanzamiento continuo
- `launchCountdown(Player player, FireworkConfig config, long durationTicks)` → `CompletableFuture<String>` - Durante countdown

### Builder
- `builder()` → `FireworkBuilder` - Builder de configuración

## FireworkBuilder

```java
FireworkBuilder.create()
    .type(FireworkEffect.Type type)
    .colors(List<Color> colors)
    .fadeColors(List<Color> fadeColors)
    .flicker(boolean flicker)
    .trail(boolean trail)
    .power(int power)
    .location(Location location)
    .build() → FireworkConfig
```

**Métodos:**
- `type(FireworkEffect.Type)` - Tipo de explosión
- `colors(List<Color>)` - Colores principales
- `fadeColors(List<Color>)` - Colores de fade
- `flicker(boolean)` - Efecto de centelleo
- `trail(boolean)` - Estela de partículas
- `power(int)` - Altura antes de explotar (1-3)
- `location(Location)` - Ubicación de lanzamiento

## Tipos de Explosión

- `FireworkEffect.Type.BALL` - Bola estándar
- `FireworkEffect.Type.BALL_LARGE` - Bola grande
- `FireworkEffect.Type.BURST` - Ráfaga (explosión rápida)
- `FireworkEffect.Type.CREEPER` - Forma de cara de creeper
- `FireworkEffect.Type.STAR` - Forma de estrella

## Colores

Los colores usan `org.bukkit.Color`:

```java
Color.RED
Color.BLUE
Color.GREEN
Color.YELLOW
Color.PURPLE
Color.ORANGE
Color.WHITE
Color.BLACK

// Custom
Color.fromRGB(255, 100, 50)
```

## Formato String

```java
"BALL:RED,BLUE:YELLOW:true:true:2"
// TYPE:colors:fadeColors:flicker:trail:power
```

Ejemplo:
```java
FireworkAPI.launch(player, "STAR:RED,BLUE,GREEN:YELLOW,WHITE:true:true:3")
```

## Parámetros

### Power (Potencia)
- **1**: Explota rápido (bajo)
- **2**: Altura media (default)
- **3**: Sube alto antes de explotar

### Flicker
- **true**: Efecto de centelleo/chispa
- **false**: Sin centelleo

### Trail
- **true**: Deja estela de partículas
- **false**: Sin estela

## Características Principales
- **Tipos de Explosión**: 5 tipos diferentes (BALL, BALL_LARGE, BURST, CREEPER, STAR)
- **Colores Personalizados**: Múltiples colores y fade colors
- **Efectos Especiales**: Flicker y trail
- **Potencia**: Control de altura de explosión
- **Filtrado**: Launch con predicates
- **Radio**: En área de radio
- **Continuous**: Lanzamiento continuo
- **Countdown**: Durante countdown
- **String Parsing**: Configuración desde strings
- **Async**: Operaciones asíncronas

## Notas Importantes
- Los fireworks lanzan desde la ubicación especificada
- Power determina qué tan alto sube antes de explotar
- Puedes combinar múltiples colores en el mismo firework
- Fade colors son los colores al desaparecer
- Flicker añade efecto de chispa/destello
- Trail deja estela de partículas mientras sube
- El radio se calcula desde el origen
- Los fireworks generan sonido automáticamente
- Continuous lanza fireworks repetidamente
- Los colores se interpolan si hay múltiples
- Los fireworks son visibles para todos los jugadores en rango

## Ver También
- [VISUAL_OVERVIEW](VISUAL_OVERVIEW.md) - Arquitectura común
- [PARTICLE](PARTICLE.md) - Efectos de partículas
- [SOUND](SOUND.md) - Sonidos complementarios
