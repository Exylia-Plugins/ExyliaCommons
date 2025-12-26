# YAML SYSTEM

## Descripción
Sistema de persistencia en archivos YAML que implementa la interfaz Repository, similar a Database pero usando archivos en lugar de base de datos. Permite guardar entities en archivos YAML organizados por carpetas, con auto-serialización, operaciones async, y sistema de backup automático. Ideal para datos que no requieren una base de datos completa.

## Inicialización

```java
Yaml.initialize()
Yaml.initialize(Path baseDir)
Yaml.initialize(YamlConfig config)
```

Por defecto usa el directorio `data/` si no se especifica.

## API Principal

### Registro de Entities
- `registerEntity(Class<T> entityClass)` - Registra entity sync
- `registerEntityAsync(Class<T> entityClass)` → `CompletableFuture<Void>` - Registra async

### Obtención de Repositories
- `getRepository(Class<T> entityClass)` → `YamlRepository<T>` - Obtiene repository para entity

### Gestión
- `shutdown()` - Cierra el sistema y guarda cambios pendientes
- `getManager()` → `YamlManager` - Obtiene manager para acceso avanzado
- `isInitialized()` → `boolean` - Verifica si está inicializado

## YamlRepository<T> Interface

Extiende `Repository<T>`, proporcionando las mismas operaciones que Database.

### Operaciones de Lectura (Sync)
- `findById(String id)` → `Optional<T>` - Busca por ID
- `findAll()` → `List<T>` - Obtiene todas las entities
- `exists(String id)` → `boolean` - Verifica si existe
- `count()` → `long` - Cuenta total de entities

### Operaciones de Lectura (Async)
- `findByIdAsync(String id)` → `CompletableFuture<Optional<T>>`
- `findAllAsync()` → `CompletableFuture<List<T>>`
- `existsAsync(String id)` → `CompletableFuture<Boolean>`
- `countAsync()` → `CompletableFuture<Long>`

### Operaciones de Escritura (Sync)
- `save(T entity)` → `T` - Guarda entity en archivo YAML
- `delete(String id)` → `boolean` - Elimina archivo YAML
- `deleteAll()` → `boolean` - Elimina todos los archivos

### Operaciones de Escritura (Async)
- `saveAsync(T entity)` → `CompletableFuture<T>`
- `deleteAsync(String id)` → `CompletableFuture<Boolean>`

## YamlConfig

Configuración del sistema YAML:

```java
YamlConfig.builder()
    .baseDir(Paths.get("data"))
    .autoBackup(true)
    .backupInterval(3600000L)
    .maxBackups(5)
    .build()
```

**Parámetros:**
- `baseDir` - Directorio base para archivos YAML
- `autoBackup` - Habilitar backups automáticos
- `backupInterval` - Intervalo de backup en milisegundos
- `maxBackups` - Número máximo de backups a mantener

## Estructura de Archivos

Las entities se guardan en la siguiente estructura:

```
data/
└── <entity_class_name>/
    ├── <entity_id_1>.yml
    ├── <entity_id_2>.yml
    └── <entity_id_3>.yml
```

Cada entity se guarda en su propio archivo YAML con su ID como nombre.

## Serialización

El sistema serializa automáticamente las entities a YAML:
- Campos primitivos → valores YAML directos
- Objetos complejos → secciones YAML anidadas
- Collections → listas YAML
- Maps → secciones YAML con keys

## Backups

Si `autoBackup` está habilitado:
- Se crean backups periódicamente según `backupInterval`
- Los backups se guardan en `<baseDir>/backups/`
- Se mantienen solo los últimos `maxBackups` backups
- Formato: `<entity>_<timestamp>.yml`

## Diferencia con Config System

| Aspecto | Config System | YAML System |
|---------|--------------|-------------|
| Propósito | Archivos de configuración del plugin | Persistencia de entities |
| Estructura | Archivos únicos con múltiples keys | Un archivo por entity |
| API | Lectura/escritura de valores | Repository pattern (CRUD) |
| Uso típico | Settings, mensajes, configuración | Datos de jugadores, registros |
| Entities | No | Sí (extiende Entity) |

## Características Principales
- **Implementa Repository**: Misma interfaz que Database para fácil migración
- **Un Archivo por Entity**: Cada entity en su propio archivo YAML
- **Async Operations**: Operaciones asíncronas con CompletableFuture
- **Auto-serialización**: Conversión automática de objetos a YAML
- **Sistema de Backups**: Backups automáticos periódicos
- **Organización por Carpetas**: Entities organizadas en carpetas por tipo
- **No Requiere Base de Datos**: Ideal para datos pequeños o desarrollo
- **Thread-Safe**: Seguro para uso concurrente

## Notas Importantes
- Las entities deben extender `Entity` al igual que con Database
- El ID de la entity se usa como nombre del archivo YAML
- Los archivos se crean automáticamente al guardar
- **NO** es tan eficiente como una base de datos real para grandes volúmenes
- Ideal para:
  - Desarrollo y testing
  - Configuraciones complejas por jugador
  - Datos pequeños que se benefician de ser legibles
  - Plugins simples sin necesidad de base de datos
- **NO** recomendado para:
  - Grandes volúmenes de datos (>1000 entities)
  - Consultas complejas o joins
  - Alta frecuencia de escritura
  - Datos críticos que requieren transacciones
- Los backups pueden consumir espacio en disco
- La lectura de archivos es relativamente lenta comparada con DB

## Ver También
- DATABASE_SYSTEM - Sistema de persistencia con base de datos real
- CONFIG_SYSTEM - Sistema de configuración (diferente propósito)
- SNAPSHOT_SYSTEM - Puede usar YAML como backend
