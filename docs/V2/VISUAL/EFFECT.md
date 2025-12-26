# VISUAL SYSTEM - EFFECT

## Descripción
Sistema para aplicar efectos de poción (PotionEffect) a jugadores. Soporta aplicación simple, a todos los jugadores, a recipients específicos, con filtros, y modos continuous y countdown. Permite configurar amplifier, duración, ambient, particles, e icon.

## API Principal

### Aplicación Simple
- `apply(Player player, PotionEffectType effectType)` → `CompletableFuture<Void>` - Amplifier 0, 10 segundos
- `apply(Player player, PotionEffectType effectType, int amplifier, int durationTicks)` → `CompletableFuture<Void>`
- `apply(Player player, String effectString)` → `CompletableFuture<Void>` - Desde string
- `apply(Player player, EffectConfig config)` → `CompletableFuture<Void>` - Con configuración

### A Todos los Jugadores
- `applyToAll(PotionEffectType effectType, int amplifier, int durationTicks)` → `CompletableFuture<Void>`

### A Recipients
- `applyToRecipients(Collection<Player> recipients, PotionEffectType effectType, int amplifier, int durationTicks)` → `CompletableFuture<Void>`

### Filtrado
- `applyToFiltered(Predicate<Player> filter, PotionEffectType effectType, int amplifier, int durationTicks)` → `CompletableFuture<Void>`

### Continuous y Countdown
- `applyContinuous(Player player, EffectConfig config)` → `CompletableFuture<String>` - Aplicación continua
- `applyCountdown(Player player, EffectConfig config, long durationTicks)` → `CompletableFuture<String>` - Durante countdown

### Builder
- `builder()` → `EffectBuilder` - Builder de configuración

## EffectBuilder

```java
EffectBuilder.create()
    .effect(PotionEffectType effectType)
    .amplifier(int amplifier)
    .durationTicks(int durationTicks)
    .ambient(boolean ambient)
    .particles(boolean particles)
    .icon(boolean icon)
    .build() → EffectConfig
```

**Métodos:**
- `effect(PotionEffectType)` - Tipo de efecto
- `amplifier(int)` - Nivel del efecto (0 = nivel 1, 1 = nivel 2, etc.)
- `durationTicks(int)` - Duración en ticks
- `ambient(boolean)` - Efecto ambiente (menos visible)
- `particles(boolean)` - Mostrar partículas (default: true)
- `icon(boolean)` - Mostrar icono en inventario (default: true)

## Formato String

```java
"SPEED:1:200:true"
// EFFECT:amplifier:durationTicks:particles
```

Ejemplo:
```java
EffectAPI.apply(player, "REGENERATION:2:600:false")
```

## Efectos de Poción Comunes

### Positivos
- `PotionEffectType.SPEED` - Velocidad
- `PotionEffectType.JUMP` - Salto
- `PotionEffectType.REGENERATION` - Regeneración
- `PotionEffectType.INCREASE_DAMAGE` - Fuerza (Strength)
- `PotionEffectType.DAMAGE_RESISTANCE` - Resistencia
- `PotionEffectType.FIRE_RESISTANCE` - Resistencia al fuego
- `PotionEffectType.WATER_BREATHING` - Respiración acuática
- `PotionEffectType.INVISIBILITY` - Invisibilidad
- `PotionEffectType.NIGHT_VISION` - Visión nocturna
- `PotionEffectType.SATURATION` - Saturación
- `PotionEffectType.ABSORPTION` - Absorción
- `PotionEffectType.HEALTH_BOOST` - Boost de salud
- `PotionEffectType.LUCK` - Suerte

### Negativos
- `PotionEffectType.SLOW` - Lentitud
- `PotionEffectType.SLOW_DIGGING` - Fatiga minera
- `PotionEffectType.HARM` - Daño instantáneo
- `PotionEffectType.CONFUSION` - Náusea
- `PotionEffectType.BLINDNESS` - Ceguera
- `PotionEffectType.HUNGER` - Hambre
- `PotionEffectType.WEAKNESS` - Debilidad
- `PotionEffectType.POISON` - Veneno
- `PotionEffectType.WITHER` - Wither
- `PotionEffectType.LEVITATION` - Levitación
- `PotionEffectType.GLOWING` - Resplandor
- `PotionEffectType.UNLUCK` - Mala suerte

## Parámetros

### Amplifier (Nivel)
- **0**: Nivel 1 (ej: Speed I)
- **1**: Nivel 2 (ej: Speed II)
- **2**: Nivel 3 (ej: Speed III)
- Y así sucesivamente

### Duration (Duración)
- En **ticks** (20 ticks = 1 segundo)
- **200**: 10 segundos
- **600**: 30 segundos
- **1200**: 1 minuto

### Ambient
- **true**: Efecto ambiente (partículas más translúcidas)
- **false**: Efecto normal

### Particles
- **true**: Muestra partículas del efecto
- **false**: Sin partículas (sigiloso)

### Icon
- **true**: Muestra icono en inventario
- **false**: Sin icono

## Características Principales
- **Todos los Efectos**: Soporte completo para todos los PotionEffectType
- **Configuración Completa**: Amplifier, duración, ambient, particles, icon
- **Aplicación Masiva**: A todos, recipients, filtrado
- **Continuous**: Re-aplicación continua del efecto
- **Countdown**: Aplicación durante countdown
- **String Parsing**: Configuración desde strings
- **Async**: Operaciones asíncronas

## Notas Importantes
- Los efectos se acumulan - si ya tiene el efecto, lo reemplaza si es mayor
- Amplifier 0 = Nivel 1 (puede ser confuso)
- Duración en ticks, no segundos
- Continuous re-aplica el efecto antes de que expire
- Ambient hace las partículas menos visibles (para beacons)
- Particles false hace el efecto "sigiloso"
- Icon false oculta el efecto del inventario
- Algunos efectos son instantáneos (HARM, HEAL)
- Los efectos persisten al desloguear (guardados por Minecraft)
- Countdown mode útil para efectos temporales en eventos

## Ver También
- [VISUAL_OVERVIEW](VISUAL_OVERVIEW.md) - Arquitectura común
- [PARTICLE](PARTICLE.md) - Efectos visuales relacionados
- ITEMS_SYSTEM - Pociones como items
