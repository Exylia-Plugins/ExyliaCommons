# LIFECYCLE SYSTEM

## Descripción
Sistema de gestión del ciclo de vida del plugin que coordina la inicialización, validación de licencia, bootstrap de sistemas core, enable del plugin, y shutdown ordenado. Define etapas claras del ciclo de vida y asegura que los sistemas se inicialicen y cierren en el orden correcto.

## Etapas del Ciclo de Vida

### LifecycleStage (Enum)
1. **PRE_INIT** (0) - Estado inicial antes de cualquier inicialización
2. **LICENSE_VALIDATION** (1) - Validación de licencia en progreso
3. **CORE_INIT** (2) - Inicialización de sistemas core (bootstrap)
4. **PLUGIN_INIT** (3) - Inicialización específica del plugin
5. **POST_INIT** (4) - Post-inicialización
6. **RUNNING** (5) - Plugin completamente inicializado y ejecutándose
7. **PRE_SHUTDOWN** (6) - Preparación para shutdown
8. **SHUTDOWN** (7) - Shutdown en progreso

## API Principal

### LifecycleManager

#### Constructor
```java
new LifecycleManager(ExyliaPlugin plugin)
```

#### Métodos de Ejecución
- `executeLicenseValidation()` → `boolean` - Ejecuta validación de licencia (retorna false si falla)
- `executeBootstrap()` - Ejecuta bootstrap de sistemas core asíncronamente
- `executePluginEnable()` - Ejecuta lógica de enable específica del plugin
- `executeShutdown()` - Ejecuta shutdown ordenado de todos los sistemas

#### Consulta de Estado
- `getCurrentStage()` → `LifecycleStage` - Obtiene la etapa actual del lifecycle

### Componentes Internos

#### SystemBootstrapper
- Inicializa sistemas core de forma asíncrona
- Verifica dependencias opcionales
- Prepara el entorno para el plugin

#### ShutdownCoordinator
- Ejecuta shutdown ordenado de subsistemas
- Coordina el cierre de conexiones y recursos
- Asegura que no queden tareas pendientes

## Flujo de Ejecución

El flujo típico del lifecycle es:

1. **PRE_INIT** → Constructor del LifecycleManager
2. **LICENSE_VALIDATION** → `executeLicenseValidation()`
   - Valida con SunLicenseAPI
   - Retorna false si falla (el plugin debe detenerse)
3. **CORE_INIT** → `executeBootstrap()`
   - Inicializa sistemas core async
   - Verifica dependencias opcionales
4. **PLUGIN_INIT** → `executePluginEnable()`
   - Llama a `callOnExyliaEnable()` del plugin
5. **POST_INIT** → Automático después de PLUGIN_INIT
6. **RUNNING** → Plugin completamente funcional
7. **PRE_SHUTDOWN** → `executeShutdown()` inicia
   - Llama a `callOnExyliaDisable()` del plugin
8. **SHUTDOWN** → ShutdownCoordinator ejecuta cierre ordenado

## Características Principales
- **Validación de Licencia**: Integrado con SunLicenseAPI
- **Bootstrap Asíncrono**: Inicialización de sistemas core fuera del main thread
- **Shutdown Ordenado**: Cierre coordinado de todos los subsistemas
- **Estados Claros**: Etapas bien definidas del ciclo de vida
- **Manejo de Errores**: RuntimeException si bootstrap o enable fallan
- **Logging Detallado**: Debug logging en cada paso del proceso

## Notas Importantes
- El LifecycleManager es usado internamente por ExyliaPlugin
- **NO** debes instanciar LifecycleManager manualmente en plugins normales
- Si `executeLicenseValidation()` retorna `false`, el plugin debe detenerse
- Los errores en `executeBootstrap()` o `executePluginEnable()` lanzan RuntimeException
- El shutdown siempre intenta ejecutarse completamente, incluso si hay errores
- Los sistemas core se inicializan asíncronamente para mejorar tiempo de carga
- El orden de shutdown es importante - ShutdownCoordinator se encarga de esto

## Uso en ExyliaPlugin

El lifecycle es manejado automáticamente por `ExyliaPlugin`:

```java
public class MyPlugin extends ExyliaPlugin {
    @Override
    protected void onExyliaEnable() {
        // Tu lógica de enable aquí
        // Se ejecuta en la etapa PLUGIN_INIT
    }

    @Override
    protected void onExyliaDisable() {
        // Tu lógica de disable aquí
        // Se ejecuta en la etapa PRE_SHUTDOWN
    }
}
```

## Ver También
- LICENSE_SYSTEM - Sistema de validación de licencias
- DATABASE_SYSTEM - Se inicializa durante bootstrap
- RELOAD_SYSTEM - Permite recargar sistemas sin reiniciar
