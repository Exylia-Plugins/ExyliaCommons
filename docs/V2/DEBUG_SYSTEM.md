# DEBUG SYSTEM

## Descripción
Sistema de debug y logging categorizado con múltiples niveles de severidad, separación entre logs del plugin y de la librería, categorías organizadas, formateo colorizado para consola, y configuración para habilitar/deshabilitar categorías específicas. Proporciona logging estructurado y fácil de filtrar para debugging y monitoreo.

## Inicialización
```java
DebugAPI.initialize(JavaPlugin plugin)
DebugAPI.init(JavaPlugin plugin)  // Alias
```

## API Principal

### Logs del Plugin

#### Debug
- `logPluginDebug(String message)` - Debug sin categoría
- `logPluginDebug(DebugCategory category, String message)` - Con categoría

#### Error
- `logPluginError(String message)` - Error sin categoría
- `logPluginError(String message, Throwable throwable)` - Con excepción
- `logPluginError(DebugCategory category, String message)` - Con categoría
- `logPluginError(DebugCategory category, String message, Throwable throwable)` - Completo

#### Warning
- `logPluginWarn(String message)` - Warning sin categoría
- `logPluginWarn(DebugCategory category, String message)` - Con categoría

#### Info
- `logPluginInfo(String message)` - Info sin categoría
- `logPluginInfo(DebugCategory category, String message)` - Con categoría

#### Success
- `logPluginSuccess(String message)` - Success sin categoría
- `logPluginSuccess(DebugCategory category, String message)` - Con categoría

### Logs de la Librería

#### Debug
- `logLibDebug(String message)` - Debug sin categoría
- `logLibDebug(DebugCategory category, String message)` - Con categoría

#### Error
- `logLibError(String message)` - Error sin categoría
- `logLibError(String message, Throwable throwable)` - Con excepción
- `logLibError(DebugCategory category, String message)` - Con categoría
- `logLibError(DebugCategory category, String message, Throwable throwable)` - Completo

#### Warning
- `logLibWarn(String message)` - Warning sin categoría
- `logLibWarn(DebugCategory category, String message)` - Con categoría

#### Info
- `logLibInfo(String message)` - Info sin categoría
- `logLibInfo(DebugCategory category, String message)` - Con categoría

#### Success
- `logLibSuccess(String message)` - Success sin categoría
- `logLibSuccess(DebugCategory category, String message)` - Con categoría

### Utilidades
- `sendPluginMOTD(JavaPlugin plugin)` - Envía MOTD del plugin a consola

## DebugType (Enum)

Niveles de severidad:
1. **DEBUG** - Información de debugging detallada
2. **INFO** - Información general
3. **WARN** - Advertencias
4. **ERROR** - Errores
5. **SUCCESS** - Operaciones exitosas

## DebugCategory (Enum)

Categorías organizadas:
1. **DATABASE** - Operaciones de base de datos
2. **CACHE** - Operaciones de cache
3. **CONFIG** - Carga/guardado de configuración
4. **NETWORK** - Operaciones de red/Redis
5. **ASYNC** - Operaciones asíncronas
6. **LIFECYCLE** - Ciclo de vida de sistemas
7. **VISUAL** - Efectos visuales
8. **REGION** - Sistema de regiones
9. **HOLOGRAM** - Sistema de hologramas
10. **SCOREBOARD** - Sistema de scoreboards
11. **ACTION** - Sistema de acciones
12. **REWARD** - Sistema de recompensas
13. **PERMISSION** - Verificaciones de permisos
14. **API** - Llamadas a APIs externas
15. **OTHER** - Otros logs

## DebugSource (Enum)

Fuente del log:
- **PLUGIN** - Log del plugin que usa la librería
- **LIBRARY** - Log interno de ExyliaCommons

## Formato de Logs

Los logs se formatean con:
- **Color** según tipo y categoría
- **Prefijo** con fuente (PLUGIN/LIBRARY)
- **Categoría** entre corchetes [CATEGORY]
- **Tipo** (DEBUG/INFO/WARN/ERROR/SUCCESS)
- **Mensaje** del log
- **Stack trace** para excepciones

Ejemplo:
```
[ExyliaCommons] [DATABASE] INFO: Connected to MySQL database
[MyPlugin] [CACHE] WARN: Cache hit rate below 50%
[ExyliaCommons] [ASYNC] ERROR: Failed to execute async task
```

## DebugConfig

Configuración del sistema de debug:
- **enabled** - Habilita/deshabilita sistema completo
- **enabledCategories** - Set de categorías habilitadas
- **showStackTraces** - Mostrar stack traces completos
- **colorized** - Usar colores en consola
- **logToFile** - Guardar logs en archivo

## Características Principales
- **Multi-Level Logging**: 5 niveles de severidad
- **Categorized**: 15 categorías predefinidas
- **Source Separation**: Separa logs de plugin vs librería
- **Colorized Output**: Colores en consola para legibilidad
- **Exception Support**: Stack traces completos
- **Configurable**: Habilitar/deshabilitar por categoría
- **MOTD Display**: Banner de inicio personalizado
- **Performance**: Logging eficiente sin lag

## Diferencia Plugin vs Library

**Plugin Logs** (`logPlugin*`):
- Para logs del plugin que usa ExyliaCommons
- Usa el nombre del plugin en el prefijo
- Se controla desde la config del plugin

**Library Logs** (`logLib*`):
- Para logs internos de ExyliaCommons
- Usa "ExyliaCommons" en el prefijo
- Se controla desde la config de la librería

## Notas Importantes
- Los logs se procesan solo si la categoría está habilitada
- El nivel DEBUG se puede deshabilitar en producción
- Los stack traces se muestran solo para ERROR por defecto
- Los colores usan códigos ANSI (pueden no funcionar en todos los consoles)
- El sistema es thread-safe para logging concurrente
- Los logs se envían a consola inmediatamente (no buffered)
- Las categorías se pueden habilitar/deshabilitar dinámicamente
- El MOTD se muestra una vez al inicializar el plugin
- Los logs SUCCESS son útiles para operaciones críticas completadas
- El sistema no afecta el performance si está deshabilitado
- Los logs a archivo requieren configuración adicional
- Use categorías apropiadas para filtrado efectivo
- Los logs WARN no detienen ejecución - solo alertan
- Los logs ERROR deberían revisarse en producción

## Ver También
- CONFIG_SYSTEM - Configuración de DebugConfig
- LIFECYCLE_SYSTEM - Logs de ciclo de vida
- DATABASE_SYSTEM - Logs de base de datos
