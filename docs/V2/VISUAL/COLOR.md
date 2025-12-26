# VISUAL SYSTEM - COLOR

## Descripción
Utilidades de color y formateo de texto para Minecraft. Soporta hex colors, legacy colors, gradientes, presets personalizados, centrado de mensajes, transformación de fonts, y conversión entre formatos. Usado internamente por todos los subsistemas visuales.

## Inicialización

```java
ColorAPI.initialize(JavaPlugin plugin)
ColorAPI.initialize(JavaPlugin plugin, Map<String, String> customPresets)
```

La inicialización es opcional - el sistema funciona sin ella, pero los presets personalizados requieren inicialización.

## API Principal

### Parsing de Colores
- `parse(String message)` → `Component` - Parsea a Adventure Component
- `parseAsync(String message)` → `CompletableFuture<Component>` - Parse async
- `parse(List<String> messages)` → `List<Component>` - Múltiples mensajes
- `parseAsync(List<String> messages)` → `CompletableFuture<List<Component>>` - Múltiples async
- `parseToString(String message)` → `String` - Parsea a String (legacy)

### Stripping de Colores
- `stripColors(String message)` → `String` - Remueve códigos de color
- `stripColors(Component component)` → `String` - Component a texto plano

### Normalización
- `normalizeColor(String input)` → `String` - Normaliza formato de color

### Generación de Colores
- `generateRandomHexColor()` → `String` - Genera hex random (evita muy claro/oscuro)

### Centrado de Mensajes
- `centerMessage(String message)` → `String` - Centra mensaje en chat
- `centerMessages(List<String> messages)` → `List<String>` - Centra múltiples
- `centerMessageWithWidth(String message, int maxWidth)` → `String` - Con ancho custom
- `fitsInChat(String message)` → `boolean` - Verifica si cabe en chat
- `getPixelWidth(String message)` → `int` - Obtiene ancho en píxeles

### Transformación de Fonts
- `applyFont(String message, FontTransformer.FontType fontType)` → `String` - Aplica font
- `applyFont(String message, FontTransformer.FontType fontType, boolean forceUpperCase)` → `String`
- `applyFont(String message, String fontType)` → `String` - Por nombre
- `applyFont(String message, String fontType, boolean forceUpperCase)` → `String`

### Presets de Colores
- `getColorPreset(String presetName)` → `String` - Obtiene preset
- `getAllColorPresets()` → `Map<String, String>` - Todos los presets
- `addColorPreset(String name, String colorCode)` - Añade preset custom
- `addColorPresets(Map<String, String> customPresets)` - Añade múltiples
- `reloadPresets()` - Recarga presets desde config

### Gestión de Cache
- `clearCache()` - Limpia todos los caches de color
- `isInitialized()` → `boolean` - Verifica si está inicializado

## Formatos de Color Soportados

### Hex Colors
```java
"<#FF5733>Texto en hex"
"<#00FF00>Verde</> <#FF0000>Rojo"
```

### Legacy Colors (Ampersand)
```java
"&6Texto naranja"
"&a&lVerde Negrita"
"&c&o&nRojo Cursiva Subrayado"
```

### Legacy Colors (Section)
```java
"§6Texto naranja"
"§a§lVerde Negrita"
```

### Gradientes
```java
"<gradient:#FF0000:#0000FF>Texto con gradiente"
"<gradient:red:blue>Gradiente con nombres"
```

### Presets
```java
"{primary}Texto con preset"
"{error}Mensaje de error"
```

## Códigos de Color Legacy

### Colores Básicos
- `&0` / `§0` - Negro
- `&1` / `§1` - Azul oscuro
- `&2` / `§2` - Verde oscuro
- `&3` / `§3` - Cyan oscuro
- `&4` / `§4` - Rojo oscuro
- `&5` / `§5` - Morado oscuro
- `&6` / `§6` - Dorado/Naranja
- `&7` / `§7` - Gris
- `&8` / `§8` - Gris oscuro
- `&9` / `§9` - Azul
- `&a` / `§a` - Verde
- `&b` / `§b` - Cyan
- `&c` / `§c` - Rojo
- `&d` / `§d` - Rosa
- `&e` / `§e` - Amarillo
- `&f` / `§f` - Blanco

### Formatos
- `&k` / `§k` - Ofuscado
- `&l` / `§l` - Negrita
- `&m` / `§m` - Tachado
- `&n` / `§n` - Subrayado
- `&o` / `§o` - Cursiva
- `&r` / `§r` - Reset

## FontTransformer.FontType

- `FontType.BOLD` - Negrita
- `FontType.ITALIC` - Cursiva
- `FontType.SMALL_CAPS` - Mayúsculas pequeñas
- `FontType.FANCY` - Fancy/decorativo
- `FontType.MONOSPACE` - Monoespaciado

## Sistema de Centrado

El centrado calcula el ancho en píxeles de cada carácter:
- Caracteres anchos (W, M) = más píxeles
- Caracteres delgados (i, l) = menos píxeles
- Calcula espacios necesarios para centrar
- Ancho de chat default: 154 píxeles
- Soporta colores (no afectan el ancho)

## Presets de Colores

Los presets permiten definir colores reutilizables:

```yaml
# colors.yml
colors:
  primary: "<#FF5733>"
  secondary: "<#33FF57>"
  error: "&c"
  success: "&a"
  warning: "&e"
```

Uso:
```java
ColorAPI.parse("{primary}Texto primario {error}Error")
```

## Características Principales
- **Hex Colors**: Soporte completo para colores hex
- **Legacy Colors**: Ampersand y section symbols
- **Gradientes**: Gradientes de color automáticos
- **Presets**: Sistema de presets personalizables
- **Centrado**: Centrado pixel-perfect
- **Fonts**: Transformación de fuentes
- **Cache**: Sistema de cache para performance
- **Adventure**: Integración con Adventure Components
- **Normalización**: Conversión entre formatos
- **Random Colors**: Generación de colores aleatorios balanceados

## Notas Importantes
- El parsing es case-sensitive para hex colors
- Los gradientes requieren MiniMessage (Adventure)
- El centrado asume font default de Minecraft
- Los presets se cargan desde `colors.yml` si existe
- El cache mejora significativamente el performance
- Los colores hex requieren clientes 1.16+
- `parseToString` convierte a legacy (pierde algunos efectos)
- El centrado puede fallar con fonts custom
- `forceUpperCase` en fonts convierte a mayúsculas primero
- El sistema es usado internamente por MESSAGE, TITLE, etc.

## Ver También
- [VISUAL_OVERVIEW](VISUAL_OVERVIEW.md) - Arquitectura común
- [MESSAGE](MESSAGE.md) - Usa ColorAPI para mensajes
- [TITLE](TITLE.md) - Usa ColorAPI para títulos
- CONFIG_SYSTEM - colors.yml para presets
