# CONFIG SYSTEM

## Descripción
Sistema centralizado de gestión de archivos de configuración YAML. Proporciona una API estática para acceder, modificar y recargar archivos de configuración del plugin. Incluye cache automático, auto-creación de archivos desde resources, y métodos utilitarios para mapeo funcional de secciones a objetos.

## Inicialización
```java
Configs.init(JavaPlugin plugin)
```

## API Principal

### Gestión de Archivos
- `get(String fileName)` → `Config` - Obtiene o crea un archivo de configuración
- `file(String fileName)` → `Config` - Alias de get()
- `raw()` → `FileConfiguration` - Obtiene FileConfiguration del config.yml principal
- `raw(String fileName)` → `FileConfiguration` - Obtiene FileConfiguration de un archivo específico

### Lectura de Valores (config.yml principal)
- `string(String path)` → `String` - Lee string
- `string(String path, String defaultValue)` → `String` - Lee string con default
- `integer(String path)` → `int` - Lee entero
- `integer(String path, int defaultValue)` → `int` - Lee entero con default
- `bool(String path)` → `boolean` - Lee boolean
- `bool(String path, boolean defaultValue)` → `boolean` - Lee boolean con default
- `decimal(String path)` → `double` - Lee double
- `decimal(String path, double defaultValue)` → `double` - Lee double con default
- `longValue(String path)` → `long` - Lee long
- `longValue(String path, long defaultValue)` → `long` - Lee long con default
- `stringList(String path)` → `List<String>` - Lee lista de strings
- `intList(String path)` → `List<Integer>` - Lee lista de enteros

### Lectura Avanzada
- `section(String path)` → `ConfigurationSection` - Obtiene sección de configuración
- `getKeys(String path)` → `Set<String>` - Obtiene keys de una sección
- `getValue(String path, Class<T> type)` → `T` - Obtiene valor tipado
- `map(String path, Function<ConfigurationSection, T> mapper)` → `Map<String, T>` - Mapea sección a Map
- `list(String path, Function<ConfigurationSection, T> mapper)` → `List<T>` - Mapea sección a List
- `exists(String path)` → `boolean` - Verifica si existe un path

### Modificación y Guardado
- `set(String path, Object value)` - Asigna valor al config.yml principal
- `save()` - Guarda config.yml principal
- `save(String fileName)` - Guarda archivo específico
- `saveAll()` - Guarda todos los archivos cacheados

### Recarga
- `reload(String fileName)` - Recarga archivo específico
- `reloadAll()` - Recarga todos los archivos cacheados

### Gestión de Cache
- `unload(String fileName)` - Remueve archivo del cache
- `unloadAll()` - Limpia todo el cache

## Clase Config
Representa un archivo de configuración individual:
- `string(String path)` - Lee string
- `integer(String path)` - Lee int
- `bool(String path)` - Lee boolean
- `decimal(String path)` - Lee double
- `longValue(String path)` - Lee long
- `stringList(String path)` - Lee lista
- `section(String path)` - Obtiene sección
- `map(String path, Function)` - Mapea a Map
- `list(String path, Function)` - Mapea a List
- `set(String path, Object value)` - Asigna valor
- `save()` - Guarda archivo
- `reload()` - Recarga archivo

## Mapeo Funcional
El sistema permite transformar secciones de configuración en objetos complejos usando lambdas:

```java
Map<String, MyObject> objects = Configs.map("objects", section ->
    new MyObject(section.getString("name"), section.getInt("value"))
);

List<MyObject> list = Configs.list("items", section ->
    new MyObject(section.getString("name"), section.getInt("value"))
);
```

## Características Principales
- **Cache Automático**: Los archivos se cachean en memoria con ConcurrentHashMap
- **Auto-creación**: Si el archivo no existe, se copia desde resources
- **API Estática**: Acceso global sin necesidad de instancias
- **Thread-Safe**: Uso de ConcurrentHashMap para concurrencia
- **Mapeo Funcional**: Transformación de secciones a objetos con lambdas
- **Múltiples Archivos**: Gestión de múltiples archivos YAML simultáneamente
- **Valores por Defecto**: Métodos sobrecargados con defaults

## Notas Importantes
- Debe llamarse `Configs.init(plugin)` en `onEnable()` antes de usar
- El archivo principal es `config.yml` (accesible sin especificar nombre)
- Los archivos se buscan en la carpeta del plugin
- Si un archivo no existe, se copia automáticamente desde `resources/`
- El cache es compartido globalmente - cambios en un lugar afectan a todos
- Los métodos de lectura retornan valores por defecto si el config no está inicializado

## Ver También
- DATABASE_SYSTEM - Usa Config para configuración de database
- YAML_SYSTEM - Sistema de persistencia en YAML (diferente propósito)
