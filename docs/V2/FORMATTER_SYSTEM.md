# FORMATTER SYSTEM

## Descripción
Sistema de formateo de tiempo, fechas y precios con múltiples formatos de salida, cache inteligente, async batch operations, y estadísticas de performance. Soporta parsing inteligente de inputs (millis, segundos, strings), múltiples idiomas, y configuración personalizable.

## API Principal - Time Formatting

### Formatos de Tiempo
- `formatTime(Object input)` → `String` - Formato estándar (ej: "1h 30m 45s")
- `formatTimeClock(Object input)` → `String` - Formato reloj (ej: "01:30:45")
- `formatTimeClock(Object input, ClockFormat format)` → `String` - Con formato específico
- `formatTimeCompact(Object input)` → `String` - Compacto (ej: "1.5h")
- `formatTimeVerbal(Object input)` → `String` - Verbal (ej: "1 hour and 30 minutes")
- `formatTimeLargest(Object input)` → `String` - Solo unidad mayor (ej: "1.5h")
- `formatTimeApproximate(Object input)` → `String` - Aproximado (max 2 unidades)
- `formatTimeWithPrecision(Object input, int precision)` → `String` - Precision decimal custom

### Componentes de Tiempo
- `getTimeComponents(Object input)` → `TimeComponents` - Obtiene componentes individuales (días, horas, minutos, segundos)

## API Principal - Date Formatting

### Formatos de Fecha
- `formatDate(Object input)` → `String` - Formato estándar configurado
- `formatDate(Object input, String pattern)` → `String` - Con patrón custom
- `formatDateOnly(Object input)` → `String` - Solo fecha (sin hora)
- `formatTimeOnly(Object input)` → `String` - Solo hora (sin fecha)
- `formatDateISO(Object input)` → `String` - Formato ISO-8601

### Formatos Relativos
- `formatDateRelative(Object input)` → `String` - Relativo (ej: "hace 2 horas", "dentro de 3 días")
- `formatDateRelativeFrom(Object input, Object fromDate)` → `String` - Relativo desde fecha específica
- `formatDateRelativeCompact(Object input)` → `String` - Relativo compacto (ej: "2h ago")
- `formatDateRelativeVerbose(Object input)` → `String` - Relativo verbose

### Diferencias y Validaciones
- `getDateDifference(Object input1, Object input2, ChronoUnit unit)` → `long` - Diferencia en unidad específica
- `getDateDifferenceFormatted(Object input1, Object input2)` → `String` - Diferencia formateada
- `isDatePast(Object input)` → `boolean` - Es fecha pasada
- `isDateFuture(Object input)` → `boolean` - Es fecha futura
- `isDateToday(Object input)` → `boolean` - Es hoy
- `isDateWithin(Object input, long millis)` → `boolean` - Está dentro de X millis desde ahora

## API Principal - Price Formatting

### Formatos de Precio
- `formatPrice(Object input)` → `String` - Con símbolo de moneda (ej: "$1,234.56")
- `formatPriceCompact(Object input)` → `String` - Compacto con sufijos (ej: "$1.2M")
- `formatPriceNoSymbol(Object input)` → `String` - Sin símbolo (ej: "1,234.56")
- `formatPriceWithSymbol(Object input, String symbol)` → `String` - Con símbolo custom

## Batch Async Operations

- `formatTimeBatchAsync(List<Object> inputs)` → `CompletableFuture<List<String>>` - Batch de tiempo
- `formatDateBatchAsync(List<Object> inputs)` → `CompletableFuture<List<String>>` - Batch de fechas
- `formatPriceBatchAsync(List<Object> inputs)` → `CompletableFuture<List<String>>` - Batch de precios

## Gestión y Stats

- `reload()` - Recarga configuración
- `invalidateCaches()` - Limpia cache de resultados
- `invalidateAllCaches()` - Limpia todos los caches (incluye patrones regex y DecimalFormat)
- `getCacheStats()` → `FormatterCacheStats` - Estadísticas de cache
- `getGlobalStats()` → `GlobalFormatterStats` - Estadísticas globales de todos los formatters

## Inputs Aceptados

### Time Formatter
- `Long` - Milisegundos
- `Integer` - Segundos
- `Double/Float` - Segundos decimales
- `String` - Formato "1h30m45s", "2d", "30.5s"

### Date Formatter
- `Date` - java.util.Date
- `Instant` - java.time.Instant
- `LocalDateTime` - java.time.LocalDateTime
- `Long` - Timestamp en millis

### Price Formatter
- `BigDecimal` - Precisión decimal exacta
- `Number` - Integer, Long, Double, Float
- `String` - Parsea números con/sin símbolos

## ClockFormat (Enum)

- `FULL` - HH:MM:SS (ej: "01:30:45")
- `NO_HOURS` - MM:SS (ej: "90:45")
- `ONLY_SECONDS` - SS (ej: "5445")

## TimeComponents

Componentes individuales de tiempo:
- `getDays()` → `long`
- `getHours()` → `long`
- `getMinutes()` → `long`
- `getSeconds()` → `long`
- `getMillis()` → `long`

## Configuración (config.yml)

```yaml
formatters:
  time:
    zero-text: "0s"
    show-milliseconds: false
    compact-mode: false
    precision: 2
    language: "en"  # en/es
    force-show-zero-decimals: false
    decimal-threshold-millis: 60000
    show-decimals-under-threshold: true

  date:
    default-pattern: "dd/MM/yyyy HH:mm:ss"
    locale: "es_ES"
    timezone: "America/Mexico_City"

  price:
    currency-symbol: "$"
    symbol-before: true
    decimal-separator: "."
    thousand-separator: ","
    decimal-places: 2
    show-decimals: true
```

## Características Principales
- **Múltiples Formatos**: Standard, clock, compact, verbal, approximate, etc.
- **Smart Parsing**: Acepta múltiples tipos de input
- **Multilingual**: Soporte en/es configurable
- **Relative Dates**: Formatos relativos ("hace X tiempo")
- **Compact Numbers**: Sufijos K, M, B, T para precios
- **Cache System**: Cache de resultados, patrones regex, y DecimalFormat
- **Thread-Safe**: ThreadLocal para DecimalFormat
- **Batch Async**: Procesamiento async en lote
- **Stats & Monitoring**: Estadísticas detalladas de uso y performance
- **Configurable**: Personalización completa desde config

## Notas Importantes
- Los formatters se inicializan automáticamente con configuración por defecto
- El cache mejora significativamente el performance en formateos repetidos
- TimeFormatter acepta "1h30m" sin espacios
- DateFormatter detecta automáticamente el tipo de input
- PriceFormatter usa ThreadLocal para thread-safety
- Los formatos relativos usan el tiempo actual como referencia
- Compact price format usa K (miles), M (millones), B (billones), T (trillones)
- El precision en time formatter aplica a decimales de la unidad mayor
- Los patrones de fecha usan SimpleDateFormat
- La configuración se recarga con `reload()`
- GlobalFormatterStats incluye estadísticas agregadas de performance

## Ver También
- CONFIG_SYSTEM - Configuración de formatters
- PLACEHOLDERS_SYSTEM - Formatters útiles en placeholders
- VISUAL/MESSAGE - Usa formatters para mensajes
- SCOREBOARD_SYSTEM - Usa formatters en scoreboards
