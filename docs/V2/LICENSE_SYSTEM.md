# LICENSE SYSTEM

## Descripción
Sistema de validación de licencias para plugins premium que verifica keys contra servidor de licencias de Exylia, genera HWID único por servidor, valida async sin bloquear startup, soporta modo gratuito (sin licencia), y proporciona mensajes claros de error. Protege plugins comerciales mientras permite desarrollo libre.

## Inicialización

El sistema se inicializa automáticamente con ExyliaPlugin:

```java
public class MyPlugin extends ExyliaPlugin {
    public MyPlugin() {
        super(true);  // true = requiere licencia
        // super(false) = plugin gratuito
    }
}
```

## LicenseManager

Manager interno del sistema (uso avanzado):

### Creación
```java
LicenseManager manager = new LicenseManager(ExyliaPlugin plugin, boolean isRequired);
```

### Verificación
- `initializeAndVerify()` → `CompletableFuture<Void>` - Inicializa y verifica async

### Estado
- `isRequired()` → `boolean` - Si requiere licencia
- `isVerified()` → `boolean` - Si la licencia fue verificada
- `isValidating()` → `boolean` - Si está validando actualmente
- `getLicenseConfig()` → `LicenseConfig` - Configuración de licencia

## LicenseConfig

Configuración automática desde `license.yml`:

```yaml
license-key: "XXXXX-XXXXX-XXXXX-XXXXX-XXXXX"
```

**Propiedades:**
- `license-key` - Key de licencia del plugin

El archivo se crea automáticamente al iniciar el plugin.

## LicenseResult

Resultado de validación:

### Métodos
- `isValid()` → `boolean` - Si la licencia es válida
- `getMessage()` → `String` - Mensaje descriptivo
- `getErrorType()` → `ErrorType` - Tipo de error si falló

### ErrorType (Enum)
1. **NONE** - Sin error (licencia válida)
2. **INVALID_LICENSE** - Licencia inválida o expirada
3. **MISSING_LICENSE** - Licencia requerida pero no configurada
4. **CONNECTION_ERROR** - Error de conexión al servidor
5. **SERVER_ERROR** - Error en servidor de licencias
6. **VALIDATION_IN_PROGRESS** - Validación ya en progreso
7. **UNEXPECTED_ERROR** - Error inesperado

## Proceso de Validación

1. **Lectura de Config**: Lee license.yml del plugin
2. **Generación de HWID**: Genera ID único del servidor
3. **Envío al Servidor**: POST a API de licencias
4. **Validación**: Verifica respuesta del servidor
5. **Resultado**: Permite o bloquea el plugin

## Payload de Validación

El sistema envía:
- **key**: License key del plugin
- **hwid**: Hardware ID del servidor
- **plugin_version**: Versión del plugin
- **server_name**: Nombre del servidor Minecraft
- **minecraft_version**: Versión de Minecraft

## HWID (Hardware ID)

Se genera desde:
- COMPUTERNAME (variable de entorno)
- User name (system property)
- PROCESSOR_IDENTIFIER (variable de entorno)
- PROCESSOR_LEVEL (variable de entorno)

Hash MD5 de la combinación asegura unicidad por servidor.

## Modos de Operación

### Plugin Premium (isRequired = true)
- Requiere license key válida
- Valida contra servidor de Exylia
- Bloquea plugin si falla validación
- Muestra mensajes de error claros

### Plugin Gratuito (isRequired = false)
- No requiere license key
- Salta validación completamente
- Siempre retorna éxito
- Permite uso sin restricciones

## Mensajes de Error

El sistema proporciona mensajes específicos:

### Invalid License
```
INVALID LICENSE FOR [Plugin]
Reason: License key invalid or expired
Join our Discord to get a new license key
https://discord.exylia.net/
```

### Connection Error
```
Error with connection to license server
Check your internet connection and try again
```

### Server Error
```
Error in the license server
Try again later or contact support
https://discord.exylia.net/
```

### Missing License
```
LICENSE REQUIRED - CONFIG REQUIRED
This plugin requires a license key
Configure in plugins/[Plugin]/license.yml
Join our Discord for help: https://discord.exylia.net/
```

## Características Principales
- **Async Validation**: No bloquea el startup del servidor
- **HWID Generation**: ID único por servidor
- **Multi-Error Types**: Errores específicos y claros
- **Free Mode**: Soporta plugins gratuitos
- **Auto-Configuration**: Crea license.yml automáticamente
- **API Integration**: Conecta con servidor de Exylia
- **Clear Messages**: Mensajes de error descriptivos
- **Version Tracking**: Registra versión del plugin
- **Server Info**: Envía info del servidor para analytics

## API del Servidor

**Endpoint**: `https://license-api.exylia.net/api/licenses/verify/`
**Método**: POST
**Headers**:
- Content-Type: application/json
- User-Agent: MinecraftPlugin/1.0

**Response Codes**:
- 200: Validación exitosa
- 404: Licencia no encontrada o inválida
- 500: Error del servidor

## Notas Importantes
- La validación ocurre async durante el startup
- El plugin se deshabilita automáticamente si falla validación
- El HWID es único por máquina física (no por servidor MC)
- Las licencias pueden estar atadas a HWID específico
- El modo gratuito (isRequired=false) salta toda validación
- Los errores de conexión no bloquean en modo desarrollo
- El sistema usa timeout de 10s para conexión y 15s para lectura
- La license key se almacena en plaintext en license.yml
- El servidor de licencias puede retornar JSON con detalles
- El sistema cachea resultado de validación (no re-valida cada vez)
- Use modo gratuito para plugins open-source o de prueba
- El HWID puede cambiar si cambia hardware del servidor
- Los errores se loggean en consola con detalles completos
- La validación solo ocurre una vez al startup

## Ver También
- LIFECYCLE_SYSTEM - Integración con ciclo de vida
- CONFIG_SYSTEM - Configuración de licencia
- DEBUG_SYSTEM - Logs de validación
