# SKULL SYSTEM

## Descripción
Sistema completo de cabezas de jugador (skulls) con soporte para texturas base64, URLs de texturas, nombres de jugadores, cache inteligente con Caffeine, rate limiting para evitar sobrecarga de APIs, precarga batch, y builder para personalización avanzada. Optimizado para alta performance con sistema de cache de múltiples capas.

## Inicialización
```java
SkullAPI.initialize()
SkullAPI.initialize(SkullConfig config)  // Con configuración custom
```

## API Principal

### Desde Textura Base64
- `fromTexture(String base64)` → `ItemStack` - Skull desde base64 sync
- `fromTextureAsync(String base64)` → `CompletableFuture<ItemStack>` - Async
- `fromTextureAsync(String base64, Consumer<ItemStack> consumer)` - Async con callback

### Desde URL de Textura
- `fromTextureURL(String url)` → `ItemStack` - Skull desde URL sync
- `fromTextureURLAsync(String url)` → `CompletableFuture<ItemStack>` - Async
- `fromTextureURLAsync(String url, Consumer<ItemStack> consumer)` - Async con callback

### Desde Nombre de Jugador
- `fromPlayer(String playerName)` → `ItemStack` - Skull de jugador sync
- `fromPlayerAsync(String playerName)` → `CompletableFuture<ItemStack>` - Async
- `fromPlayerAsync(String playerName, Consumer<ItemStack> consumer)` - Async con callback

### Builder
- `texture(String base64)` → `SkullBuilder` - Builder desde base64
- `textureURL(String url)` → `SkullBuilder` - Builder desde URL
- `player(String playerName)` → `SkullBuilder` - Builder desde jugador

### Precarga y Batch
- `preloadPlayers(String... playerNames)` - Precarga skulls de jugadores en cache
- `batchPlayers(String... playerNames)` → `CompletableFuture<List<ItemStack>>` - Carga batch async

### Cache y Estado
- `isPlayerCached(String playerName)` → `boolean` - Si jugador está en cache
- `isRateLimited()` → `boolean` - Si está en rate limit
- `clearTextureCache()` - Limpia cache de texturas
- `clearPlayerCache()` - Limpia cache de jugadores
- `clearAllCache()` - Limpia todo el cache

### Estadísticas
- `getStats()` → `String` - Estadísticas del sistema

## SkullBuilder

```java
SkullAPI.texture("base64...")
    .amount(int amount)
    .displayName(String name)
    .lore(List<String> lore)
    .glow(boolean glow)
    .build() → ItemStack
    .buildAsync() → CompletableFuture<ItemStack>
```

**Métodos:**
- `amount(int)` - Cantidad del item
- `displayName(String)` - Nombre del skull
- `lore(List<String>)` - Lore del skull
- `glow(boolean)` - Efecto glow
- `build()` - Construye sync
- `buildAsync()` - Construye async

## SkullConfig

Configuración del sistema:
- **cacheExpireMinutes** - Minutos antes de expirar cache (default: 30)
- **maxCacheSize** - Tamaño máximo del cache (default: 1000)
- **rateLimitPerSecond** - Requests por segundo permitidos (default: 10)
- **enableMetrics** - Habilitar métricas (default: true)

## Características Principales
- **Multi-Source**: Base64, URL, o nombre de jugador
- **Caffeine Cache**: Cache de alta performance con expiraciones
- **Rate Limiting**: Protección contra sobrecarga de API
- **Batch Loading**: Carga múltiples skulls en paralelo
- **Preloading**: Precarga skulls antes de necesitarlos
- **Builder Pattern**: Construcción fluida con personalización
- **Async-First**: Operaciones async para no bloquear
- **Multi-Layer Cache**: Cache de texturas y jugadores separados
- **Stats & Monitoring**: Estadísticas de uso y performance

## Fuentes de Texturas

### 1. Base64
Textura directa en formato base64:
```java
SkullAPI.fromTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90...");
```

### 2. URL
URL de textura de Minecraft:
```java
SkullAPI.fromTextureURL("http://textures.minecraft.net/texture/abc123...");
```

### 3. Player Name
Nombre de jugador (usa Mojang API):
```java
SkullAPI.fromPlayer("Notch");
```

## Cache System

El sistema mantiene 2 caches separados:
1. **Texture Cache**: Para texturas base64 y URLs
2. **Player Cache**: Para skulls de jugadores

Ambos usan Caffeine con:
- TTL configurable (default: 30 min)
- Max size configurable (default: 1000)
- Eviction automática

## Rate Limiting

Protege contra sobrecarga:
- Límite configurable de requests/segundo
- Aplica solo a requests que consultan APIs externas
- El cache bypass el rate limit
- Retorna skulls default si está limitado

## Notas Importantes
- Las operaciones async retornan skulls en thread async - usa callback para sync
- El player skull requiere conexión a Mojang API
- El rate limit solo afecta consultas nuevas (no cached)
- El cache persiste entre reloads (hasta expiración)
- Las texturas base64 son las más rápidas (no requieren API)
- El batch loading ejecuta requests en paralelo
- La precarga es útil para menus que usan muchos skulls
- Los skulls son ItemStacks normales (Material.PLAYER_HEAD)
- El sistema es thread-safe para uso concurrent
- Las estadísticas incluyen hit rate y tamaño del cache

## Ver También
- ITEMS_SYSTEM - Sistema de items con skulls
- VISUAL/COLOR - Colores en nombres de skulls
- CONFIG_SYSTEM - Configuración de SkullConfig
