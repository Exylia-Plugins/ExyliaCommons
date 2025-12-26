# VISUAL SYSTEM - MESSAGE

## Descripción
Sistema de mensajes formateados en chat con soporte completo para placeholders, colores, broadcast, filtrado de jugadores, radio, y centrado de mensajes. Incluye integración con sistema de rutas (Messages.yml) para mensajes centralizados, y efectos visuales embebidos (sonidos, partículas, fireworks, efectos de poción). Funciona tanto con jugadores como con CommandSender (consola).

## API Principal

### Envío a Jugador
- `send(Player player, String message)` → `CompletableFuture<Void>` - Envía mensaje simple
- `send(Player player, String message, PlaceholderContext context)` → `CompletableFuture<Void>` - Con contexto
- `send(Player player, List<String> messages)` → `CompletableFuture<Void>` - Múltiples líneas
- `send(Player player, List<String> messages, PlaceholderContext context)` → `CompletableFuture<Void>`
- `send(UUID playerUUID, String message)` → `CompletableFuture<Void>` - Envía por UUID
- `send(CommandSender sender, String message)` → `CompletableFuture<Void>` - Player o consola

### Broadcast
- `broadcast(String message)` → `CompletableFuture<Void>` - Broadcast a todos
- `broadcast(List<String> messages)` → `CompletableFuture<Void>` - Múltiples líneas a todos
- `broadcastExcluding(String message, Player excludePlayer)` → `CompletableFuture<Void>` - Excluye un jugador
- `broadcastExcluding(String message, Collection<Player> excludePlayers)` → `CompletableFuture<Void>` - Excluye varios

### Filtrado y Recipients
- `sendToFiltered(Predicate<Player> filter, String message)` → `CompletableFuture<Void>` - Con filtro
- `sendToFiltered(Predicate<Player> filter, List<String> messages)` → `CompletableFuture<Void>`
- `sendToRecipients(Collection<Player> recipients, String message)` → `CompletableFuture<Void>` - A lista específica
- `sendToRecipients(Collection<Player> recipients, List<String> messages)` → `CompletableFuture<Void>`

### Por Radio (Distancia)
- `sendInRadius(Location origin, double radius, String message)` → `CompletableFuture<Void>` - En radio
- `sendInRadius(Location origin, double radius, List<String> messages)` → `CompletableFuture<Void>`

### Mensajes Centrados
- `sendCentered(Player player, String message)` → `CompletableFuture<Void>` - Mensaje centrado
- `sendCentered(Player player, List<String> messages)` → `CompletableFuture<Void>` - Líneas centradas
- `broadcastCentered(String message)` → `CompletableFuture<Void>` - Broadcast centrado
- `broadcastCentered(List<String> messages)` → `CompletableFuture<Void>`

### Rutas (Messages.yml)
- `sendRoute(Player player, String route)` → `CompletableFuture<Void>` - Mensaje desde Messages.yml
- `sendRoute(String filePath, Player player, String route)` → `CompletableFuture<Void>` - Desde archivo específico
- `sendRoute(Player player, String route, PlaceholderContext context)` → `CompletableFuture<Void>`
- `sendRoute(String filePath, Player player, String route, PlaceholderContext context)` → `CompletableFuture<Void>`
- `sendRoute(CommandSender sender, String route)` → `CompletableFuture<Void>`
- `sendRoute(String filePath, CommandSender sender, String route)` → `CompletableFuture<Void>`
- `broadcastRoute(String route)` → `CompletableFuture<Void>`
- `broadcastRoute(String filePath, String route)` → `CompletableFuture<Void>`
- `broadcastRouteExcluding(String route, Player excludePlayer)` → `CompletableFuture<Void>`
- `broadcastRouteExcluding(String filePath, String route, Player excludePlayer)` → `CompletableFuture<Void>`
- `broadcastRouteExcluding(String route, Collection<Player> excludePlayers)` → `CompletableFuture<Void>`
- `broadcastRouteExcluding(String filePath, String route, Collection<Player> excludePlayers)` → `CompletableFuture<Void>`
- `sendRouteToFiltered(Predicate<Player> filter, String route)` → `CompletableFuture<Void>`
- `sendRouteToFiltered(String filePath, Predicate<Player> filter, String route)` → `CompletableFuture<Void>`
- `sendRouteToRecipients(Collection<Player> recipients, String route)` → `CompletableFuture<Void>`
- `sendRouteToRecipients(String filePath, Collection<Player> recipients, String route)` → `CompletableFuture<Void>`
- `sendRouteInRadius(Location origin, double radius, String route)` → `CompletableFuture<Void>`
- `sendRouteInRadius(String filePath, Location origin, double radius, String route)` → `CompletableFuture<Void>`
- `sendRouteCentered(Player player, String route)` → `CompletableFuture<Void>`
- `sendRouteCentered(String filePath, Player player, String route)` → `CompletableFuture<Void>`
- `broadcastRouteCentered(String route)` → `CompletableFuture<Void>`
- `broadcastRouteCentered(String filePath, String route)` → `CompletableFuture<Void>`

### Builder
- `builder()` → `MessageBuilder` - Builder de configuración

## Sistema de Rutas

Las rutas permiten centralizar mensajes en `Messages.yml`:

```yaml
messages:
  welcome: "&aWelcome %player_name%!"
  goodbye: "&cGoodbye!"

  # Soporte para listas
  announcement:
    - "&f"
    - "&6&lIMPORTANT ANNOUNCEMENT"
    - "&f"
    - "&7This is a multi-line message"
    - "&f"
```

```java
// Mensaje simple
MessageAPI.sendRoute(player, "messages.welcome");

// Mensaje multilínea
MessageAPI.sendRoute(player, "messages.announcement");

// Desde archivo específico
MessageAPI.sendRoute("modules/freeze/messages", player, "player.frozen", context);

// Broadcast
MessageAPI.broadcastRoute("messages.goodbye");
```

## Efectos Visuales Embebidos

Los mensajes soportan efectos visuales mediante tags especiales en el formato `[tipo:configuración]`.

### Tag: `[sound:...]` / `[sounds:...]`

Reproduce sonidos al jugador.

**Sintaxis**: `[sound:SOUND_NAME|volume|pitch]`

**Parámetros**:
- `SOUND_NAME`: Nombre del sonido de Bukkit (ej: `ENTITY_EXPERIENCE_ORB_PICKUP`)
- `volume` (opcional, default: 1.0): Volumen del sonido
- `pitch` (opcional, default: 1.0): Tono del sonido (0.5-2.0)

**Ejemplos**:
```yaml
# Sonido simple
message: "[sound:ENTITY_EXPERIENCE_ORB_PICKUP] &aMensaje con sonido"

# Sonido con volumen y pitch
message: "[sound:BLOCK_NOTE_BLOCK_PLING|2.0|1.5] &bSonido fuerte y agudo"

# Múltiples sonidos separados por coma
message: "[sound:ENTITY_PLAYER_LEVELUP|1.0|1.0, ENTITY_FIREWORK_ROCKET_BLAST|1.0|1.0] &d¡Level Up!"
```

### Tag: `[particle:...]` / `[particles:...]`

Genera partículas en la ubicación del jugador.

**Sintaxis**: `[particle:PARTICLE_NAME|count|offsetX|offsetY|offsetZ|extra|color]`

**Parámetros**:
- `PARTICLE_NAME`: Nombre de la partícula de Bukkit (ej: `HEART`, `FLAME`, `VILLAGER_HAPPY`)
- `count` (opcional, default: 1): Cantidad de partículas
- `offsetX/Y/Z` (opcional, default: 0.0): Desplazamiento en cada eje
- `extra` (opcional, default: 0.0): Velocidad/dato extra
- `color` (opcional): Color RGB (formato: `255,0,0` para rojo) - **Requerido para DUST**

**Ejemplos**:
```yaml
# Partícula simple
message: "[particle:HEART] &c¡Te amo!"

# Partículas con cantidad y offset
message: "[particle:VILLAGER_HAPPY|10|0.5|0.5|0.5] &a¡Éxito!"

# Partícula DUST con color rojo
message: "[particle:DUST|5|0.3|0.3|0.3|1.0|255,0,0] &4¡Sangre!"

# Múltiples partículas
message: "[particle:HEART|5, VILLAGER_HAPPY|10] &dDoble efecto"
```

### Tag: `[firework:...]` / `[fireworks:...]`

Lanza fuegos artificiales en la ubicación del jugador.

**Sintaxis**: `[firework:TYPE|colors|fadeColors|flicker|trail|power]`

**Parámetros**:
- `TYPE`: Tipo de explosión (`BALL`, `BALL_LARGE`, `STAR`, `BURST`, `CREEPER`)
- `colors`: Colores principales separados por `;` (nombres o RGB)
- `fadeColors` (opcional): Colores de desvanecimiento separados por `;`
- `flicker` (opcional): `true`/`false` para parpadeo
- `trail` (opcional): `true`/`false` para estela
- `power` (opcional, 0-3): Potencia del cohete

**Colores predefinidos**: `RED`, `GREEN`, `BLUE`, `YELLOW`, `ORANGE`, `PURPLE`, `WHITE`, `BLACK`, `PINK`, `LIME`, `CYAN`, `MAGENTA`

**Ejemplos**:
```yaml
# Firework simple
message: "[firework:BALL|RED;YELLOW] &c¡BOOM!"

# Firework complejo con fade y efectos
message: "[firework:BURST|RED;ORANGE;YELLOW|WHITE|true|true|2] &6¡Explosión épica!"

# Con colores RGB personalizados
message: "[firework:STAR|255,0,0;0,255,0;0,0,255] &dArcoíris!"
```

### Tag: `[effect:...]` / `[effects:...]`

Aplica efectos de poción al jugador.

**Sintaxis**: `[effect:EFFECT_NAME|amplifier|duration]`

**Parámetros**:
- `EFFECT_NAME`: Nombre del efecto de poción (ej: `SPEED`, `REGENERATION`, `BLINDNESS`)
- `amplifier` (opcional, default: 0): Nivel del efecto (0 = nivel 1)
- `duration` (opcional, default: 10): Duración en **segundos**

**Ejemplos**:
```yaml
# Efecto simple
message: "[effect:SPEED] &b¡Corre rápido!"

# Efecto con nivel y duración
message: "[effect:REGENERATION|2|30] &a¡Regeneración nivel 3 por 30 segundos!"

# Efecto negativo
message: "[effect:BLINDNESS|0|5] &8¡Estás cegado!"

# Múltiples efectos
message: "[effect:SPEED|1|10, JUMP_BOOST|1|10] &e¡Super poderes!"
```

### Tag: `[center]` / `[centered]`

Centra la línea del mensaje en el chat.

**Sintaxis**: `[center]` o `[centered]`

**Ejemplos**:
```yaml
# Solo centrar
message: "[center] &c&lTÍTULO CENTRADO"

# También funciona con "centered"
message: "[centered] &eSubtítulo centrado"

# Mensajes multilínea mixtos
messages:
  - "[center] &6&l━━━━━━━━━━━━━━━"
  - "[center] &c&lTÍTULO"
  - "[center] &6&l━━━━━━━━━━━━━━━"
  - "&7Esta línea NO está centrada"
  - "[center] &aGracias!"
```

### Combinación de Efectos

Puedes combinar múltiples efectos separándolos con `;`:

```yaml
# Sonido + Partículas
message: "[sound:ENTITY_PLAYER_LEVELUP; particle:VILLAGER_HAPPY|20] &a¡Nivel completado!"

# Centrado + Sonido
message: "[center; sound:BLOCK_NOTE_BLOCK_PLING] &b&lMENSAJE IMPORTANTE"

# Todos los efectos juntos
message: "[sound:BLOCK_BELL_USE|2.0|0.8; particle:HEART|10|0.5|0.5|0.5; effect:REGENERATION|1|5; firework:STAR|RED;GOLD] &c&l¡RECOMPENSA ÉPICA!"

# Centrado + Múltiples efectos
message: "[center; sound:ENTITY_PLAYER_LEVELUP; particle:FIREWORK_SPARK|20] &e&l⭐ PREMIUM UPGRADE ⭐"
```

### Ejemplo Completo con Efectos

```yaml
player:
  frozen:
    - "[sound:BLOCK_GLASS_BREAK|1.0|0.5; particle:SNOWFLAKE|15|0.5|1.0|0.5] &f"
    - "[sound:ENTITY_ELDER_GUARDIAN_CURSE|0.8|1.0] {error}&lYOUR ACCOUNT IS FROZEN"
    - "&f"
    - "{info}Follow the staff instructions from {secondary}%staff%"
    - "{info}Time frozen: {warning}%freeze_time%"
    - "&f"
    - "[effect:SLOWNESS|4|999999] {error}Do not disconnect or you will be automatically penalized."
    - "&f"

announcement:
  premium-upgrade:
    - "[center; sound:ENTITY_PLAYER_LEVELUP|2.0|1.5; particle:FIREWORK_SPARK|20] &f"
    - "[center] &6&l━━━━━━━━━━━━━━━━━━━━━━━━"
    - "[center; sound:BLOCK_NOTE_BLOCK_HARP|1.0|2.0] &e&l⭐ PREMIUM UPGRADE ⭐"
    - "[center] &6&l━━━━━━━━━━━━━━━━━━━━━━━━"
    - "&f"
    - "&7Player: &b%player%"
    - "&7Plan: &a%plan%"
    - "&f"
    - "[center] &a&lThank you for your support!"
    - "[center; particle:HEART|15|0.5|0.5|0.5] &f"
```

## Características Principales
- **Múltiples Destinatarios**: Player, UUID, CommandSender, broadcast, filtrado, recipients
- **Centrado Automático**: Mensajes centrados en el chat (API y tag `[center]`)
- **Efectos Visuales**: Sonidos, partículas, fireworks, y efectos de poción embebidos
- **Sistema de Rutas**: Integración con Messages.yml (soporta String y List<String>)
- **Rutas con Archivos Específicos**: `sendRoute(filePath, ...)` para mensajes modulares
- **Radio/Distancia**: Envío por proximidad
- **Filtros Personalizados**: Predicates para filtrar jugadores
- **Exclusión**: Broadcast excluyendo jugadores específicos
- **Placeholders**: Soporte completo con contexto
- **Colores**: Procesamiento automático con ColorAPI
- **Async**: Todas las operaciones asíncronas
- **Múltiples Líneas**: Soporte para listas de mensajes

## Notas Importantes
- Los mensajes vacíos o null se ignoran (no generan error)
- El procesamiento de placeholders es asíncrono
- Los colores se procesan automáticamente (hex, legacy, etc.)
- Los efectos visuales se procesan por línea en mensajes multilínea
- El tag `[center]` permite centrado individual por línea
- Los efectos se ejecutan en el orden: sonidos → partículas → fireworks → efectos
- CommandSender funciona con Player y Console
- Los mensajes centrados calculan ancho de píxeles
- Las rutas usan el sistema CONFIG_SYSTEM con Messages.yml
- Las rutas soportan detección automática de String vs List<String>
- El radio usa distanceSquared para performance
- Los filtros se aplican a todos los jugadores online
- El contexto de placeholders es opcional
- Los mensajes a console no procesan placeholders de jugador ni efectos visuales
- Los fireworks requieren una ubicación válida del jugador
- Los efectos con configuración inválida se ignoran silenciosamente

## Ver También
- [VISUAL_OVERVIEW](VISUAL_OVERVIEW.md) - Arquitectura común
- [COLOR](COLOR.md) - Sistema de colores usado por MESSAGE
- [SOUND](SOUND.md) - Sistema de sonidos
- [PARTICLE](PARTICLE.md) - Sistema de partículas
- [FIREWORK](FIREWORK.md) - Sistema de fireworks
- [EFFECT](EFFECT.md) - Sistema de efectos de poción
- CONFIG_SYSTEM - Messages.yml
- PLACEHOLDERS_SYSTEM - Sistema de placeholders
