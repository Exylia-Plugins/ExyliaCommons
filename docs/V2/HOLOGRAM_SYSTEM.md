# HOLOGRAM SYSTEM

## Descripción
Sistema completo de hologramas usando Display Entities (1.19.4+) con soporte per-player, templates reutilizables, visibilidad condicional, optimización por chunks, actualización automática de placeholders, y persistencia en base de datos. Incluye configuración avanzada de propiedades visuales (billboard, alignment, scale, colors, brightness, etc.).

## Inicialización
```java
HologramAPI.initialize(JavaPlugin plugin)
```

## API Principal

### Creación
- `create(String id, Location location)` → `HologramBuilder` - Crea builder
- `createAsync(String id, Location location, String... lines)` → `CompletableFuture<Hologram>` - Async con líneas

### Consulta
- `get(String id)` → `Optional<Hologram>` - Obtiene por ID
- `getAll()` → `Collection<Hologram>` - Todos los hologramas
- `getNearby(Location location, double radius)` → `List<Hologram>` - Cercanos a ubicación

### Eliminación
- `remove(String id)` → `CompletableFuture<Boolean>` - Elimina async
- `removeAll()` - Elimina todos async
- `removeAllSync()` - Elimina todos sync

### Gestión
- `reload()` - Recarga el sistema
- `shutdown()` - Cierra sin forzar
- `shutdown(boolean isServerShutdown)` - Cierre con flag de server shutdown
- `getManager()` → `HologramManager` - Manager para acceso avanzado
- `isInitialized()` → `boolean` - Verifica inicialización

### Configuración YAML
- `loadFromConfig(String id, ConfigurationSection section)` → `Hologram` - Carga desde config
- `loadFromConfigAsync(String id, ConfigurationSection section)` → `CompletableFuture<Hologram>` - Async
- `loadFromConfig(String id, ConfigurationSection section, Location location)` → `Hologram` - Con ubicación override
- `loadFromConfigAsync(String id, ConfigurationSection section, Location location)` → `CompletableFuture<Hologram>`
- `saveToConfig(Hologram hologram, ConfigurationSection section)` - Guarda en config

### Templates
- `loadTemplateFromConfig(ConfigurationSection section)` → `HologramTemplate` - Carga template
- `loadTemplateFromConfigAsync(ConfigurationSection section)` → `CompletableFuture<HologramTemplate>`
- `saveTemplateToConfig(HologramTemplate template, ConfigurationSection section)` - Guarda template
- `createFromTemplate(String id, Location location, HologramTemplate template)` → `HologramBuilder`
- `createFromTemplateAsync(String id, Location location, HologramTemplate template)` → `CompletableFuture<Hologram>`

## HologramBuilder

```java
HologramAPI.create("hologram-id", location)
    .line(String line)
    .lines(String... lines)
    .billboard(Display.Billboard billboard)
    .alignment(TextDisplay.TextAlignment alignment)
    .scale(float x, float y, float z)
    .shadow(boolean shadow)
    .seeThrough(boolean seeThrough)
    .lineWidth(int lineWidth)
    .backgroundColor(Color color)
    .backgroundAlpha(int alpha)
    .textOpacity(int opacity)
    .lineSpacing(double spacing)
    .brightness(int brightness)
    .updateInterval(long ticks)
    .autoUpdate(boolean autoUpdate)
    .persistent(boolean persistent)
    .perPlayer(boolean perPlayer)
    .viewDistance(double viewDistance)
    .visibilityCondition(BiPredicate<Player, Hologram> condition)
    .placeholderContext(PlaceholderContext context)
    .build() → Hologram
    .buildAsync() → CompletableFuture<Hologram>
```

**Propiedades Visuales:**
- `billboard(Display.Billboard)` - CENTER, VERTICAL, HORIZONTAL, FIXED
- `alignment(TextDisplay.TextAlignment)` - LEFT, CENTER, RIGHT
- `scale(float, float, float)` - Escala X, Y, Z
- `shadow(boolean)` - Sombra del texto
- `seeThrough(boolean)` - Visible a través de bloques
- `lineWidth(int)` - Ancho máximo de línea (default: 200)
- `backgroundColor(Color)` - Color de fondo
- `backgroundAlpha(int)` - Transparencia fondo 0-255
- `textOpacity(int)` - Opacidad texto
- `lineSpacing(double)` - Espacio entre líneas (default: 0.25)
- `brightness(int)` - Nivel de brillo (-1 para default)

**Configuración:**
- `updateInterval(long)` - Ticks entre actualizaciones
- `autoUpdate(boolean)` - Actualización automática de placeholders
- `persistent(boolean)` - Persistir en base de datos
- `perPlayer(boolean)` - Un holograma único por jugador
- `viewDistance(double)` - Distancia de visualización
- `visibilityCondition(BiPredicate)` - Condición custom de visibilidad
- `placeholderContext(PlaceholderContext)` - Contexto de placeholders

## Hologram (Modelo)

### Gestión
- `spawn()` - Spawna para todos
- `spawnForPlayer(Player player)` - Spawna para jugador específico
- `despawn()` - Despawna
- `updateAsync()` → `CompletableFuture<Void>` - Actualiza placeholders
- `teleport(Location newLocation)` - Teletransporta

### Modificación de Líneas
- `setLine(int index, String text)` - Cambia línea específica
- `addLine(String text)` - Añade línea al final
- `removeLine(int index)` - Elimina línea

### Visibilidad
- `canSee(Player player)` → `boolean` - Verifica si jugador puede ver
- `showTo(Player player)` - Muestra a jugador
- `hideFrom(Player player)` - Oculta de jugador

### Estado
- `isSpawned()` → `boolean` - Está spawneado
- `isEnabled()` → `boolean` - Está habilitado
- `enable()` - Habilita
- `disable()` - Deshabilita
- `setEnabled(boolean enabled)` - Configura estado

### Getters
- `getId()` → `String`
- `getLocation()` → `Location`
- `getLines()` → `List<String>`
- `getProperties()` → `HologramProperties`
- `getConfig()` → `HologramConfig`
- `getLineCount()` → `int`

## Configuración YAML

```yaml
hologram:
  lines:
    - "&6Línea 1"
    - "&eLínea 2"
  location:
    world: "world"
    x: 0.0
    y: 64.0
    z: 0.0
  properties:
    billboard: CENTER
    alignment: CENTER
    scale:
      x: 1.0
      y: 1.0
      z: 1.0
    shadow: true
    see-through: false
    line-width: 200
    background:
      color: "#000000"
      alpha: 128
    text-opacity: 255
    line-spacing: 0.25
    brightness: 15
  config:
    update-interval: 20
    auto-update: true
    persistent: true
    per-player: false
    view-distance: 50.0
```

## Características Principales
- **Display Entities**: Usa la API moderna de Minecraft 1.19.4+
- **Per-Player**: Hologramas únicos por jugador con placeholders personalizados
- **Templates**: Sistema de plantillas reutilizables
- **Visibility Conditions**: Condiciones lambda custom
- **Chunk Optimization**: Optimización basada en chunks
- **Batched Updates**: Actualización por lotes para performance
- **Async Operations**: Todas las operaciones pesadas son async
- **Placeholder Support**: Actualización automática de placeholders
- **Persistence**: Guardar/cargar desde base de datos
- **Advanced Properties**: Control completo de propiedades visuales

## Notas Importantes
- Requiere Minecraft 1.19.4+ (Display Entities)
- Los hologramas se despawne automáticamente al descargar el chunk
- Per-player mode crea una instancia separada por jugador
- Auto-update actualiza placeholders según updateInterval
- Visibility conditions se evalúan cada vez que un jugador entra en rango
- El brightness -1 usa el brillo del mundo
- Billboard controla cómo rota el texto para mirar al jugador
- Line spacing determina la separación vertical entre líneas
- Los hologramas persistent se guardan en base de datos
- La view distance se usa para optimización de visibilidad

## Ver También
- DATABASE_SYSTEM - Persistencia de hologramas
- PLACEHOLDERS_SYSTEM - Placeholders en líneas
- VISUAL/COLOR - Colores en líneas de hologramas
- REGION_SYSTEM - Hologramas en regiones
