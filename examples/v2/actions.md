---

```md
# Sistema de Acciones

---

## ★ Insight
- **Pipeline con middlewares:** PRE_VALIDATE → PRE_EXECUTE → EXECUTE → POST_EXECUTE → ERROR
- **Namespaces:** Evita conflictos entre plugins (`myplugin:heal` vs `otherplugin:heal`)
- **Built-in:** Cooldowns, permisos (Vault), rate limits (token bucket), audit log

---

## API Principal

```java
ActionAPI.initialize(plugin);   // onEnable()
ActionAPI.execute("namespace:action arg1", context);
ActionAPI.shutdown();           // onDisable()
```

---

## Crear Acciones

### 1. ActionBuilder (recomendado)

```java
new ActionBuilder("heal", myPlugin)
    .namespace("myplugin")
    .description("Cura al jugador")
    .sync()  // o .async()
    .permission("myplugin.heal")
    .cooldown(30, TimeUnit.SECONDS)
    .rateLimit(5)  // 5 req/min
    .auditable()
    .handler((ctx, args) -> {
        Player p = ctx.getPlayer();
        TaskAPI.runSync(() -> p.setHealth(20));
    })
    .build();
```

### 2. Factory

```java
ActionFactory factory = new ActionFactory();

Action action = factory.createSync("test", plugin, (ctx, args) -> {
    // handler
});

ActionManager.getInstance().registerAction(action);
```

### 3. Chain (secuencial)

```java
ActionAPI.createChain("reward-chain", plugin)
    .then("myplugin:heal")
    .then("myplugin:give-item")
    .then("myplugin:broadcast")
    .build();
```

---

## Ejecutar Acciones

### Simple

```java
ActionContext ctx = ActionContext.builder()
    .player(player)
    .source(ActionSource.COMMAND)
    .build();

ActionResult result = ActionAPI.execute("myplugin:heal", ctx);
```

### Con argumentos

```java
ActionResult result = ActionAPI.execute("myplugin:give diamond 64", ctx);
```

### Async

```java
ActionAPI.executeAsync("myplugin:heavy-task", ctx)
    .thenAccept(result -> {
        if (result.isSuccess()) {
            // OK
        }
    });
```

---

## ActionContext

```java
ActionContext ctx = ActionContext.builder()
    .player(player)
    .source(ActionSource.COMMAND)  // COMMAND, EVENT, PLUGIN, SCHEDULED, etc.
    .defaultNamespace("myplugin")
    .data("custom_key", "value")
    .build();

// Acceder datos
ctx.getPlayer();
ctx.getData("custom_key");
ctx.getSource();
```

---

## ActionResult

```java
ActionResult result = ActionAPI.execute(...);

result.isSuccess();           // boolean
result.getMessage();          // String
result.getError();            // Throwable (si falló)
result.getExecutionTimeMillis();
result.getMetadata("key");    // Datos adicionales
```

---

## Pipeline y Middlewares

```
PRE_VALIDATE   → NamespaceMiddleware, ValidationMiddleware
               → action.canExecute() check
PRE_EXECUTE    → PermissionMiddleware, CooldownMiddleware (valida), RateLimitMiddleware
EXECUTE        → Action.execute()
POST_EXECUTE   → CooldownApplyMiddleware (aplica solo si success), LoggingMiddleware
ERROR          → Manejo de errores
```

> **Nota:** El cooldown se valida en PRE_EXECUTE pero solo se aplica en POST_EXECUTE si la acción fue exitosa.

### Middleware personalizado

```java
public class CustomMiddleware implements Middleware {
    @Override
    public void execute(Action action, ActionContext context) {
        // Validar o modificar
    }

    @Override
    public int getPriority() {
        return 50; // Menor = ejecuta primero
    }
}

ActionManager.getInstance()
    .getPipeline()
    .registerMiddleware(PipelineStage.PRE_EXECUTE, new CustomMiddleware());
```

---

## Excepciones

| Excepción | Causa |
|-----------|-------|
| `ActionNotFoundException` | Acción no registrada |
| `ActionCooldownException` | En cooldown (tiene `getRemainingMillis()`) |
| `ActionRateLimitException` | Rate limit excedido |
| `ActionPermissionException` | Sin permiso (tiene `getPermission()`) |
| `ActionValidationException` | Validación fallida |
| `ActionParseException` | Error parseando argumentos |

```java
ActionResult result = ActionAPI.execute(...);
if (!result.isSuccess()) {
    Throwable error = result.getError();
    if (error instanceof ActionCooldownException e) {
        player.sendMessage("Espera " + e.getRemainingMillis() + "ms");
    }
}
```

---

## Argumentos

```java
// Sintaxis: "action:id arg1 arg2 --flag 'quoted string'"

.handler((ctx, args) -> {
    args.getActionId();           // "action:id"
    args.getArguments();          // List de ArgumentValue
    args.getArgumentAt(0);        // Primer argumento
    args.getFlag("--verbose");    // boolean
})
```

---

## Sync vs Async

| Tipo | Uso | Ejemplo |
|------|-----|---------|
| `sync()` | Bukkit API (world, entities, inventory) | Modificar inventario |
| `async()` | I/O, DB, red, cálculos pesados | Guardar en DB |

```java
// SYNC - se ejecuta en hilo principal
new ActionBuilder("give-item", plugin)
    .sync()
    .handler((ctx, args) -> {
        ctx.getPlayer().getInventory().addItem(...);
    })
    .build();

// ASYNC - se ejecuta en pool
new ActionBuilder("save-stats", plugin)
    .async()
    .handler((ctx, args) -> {
        database.save(ctx.getPlayer());
    })
    .build();
```

---

## ★ Insight
- **Resolución de IDs:** Si hay conflicto, usa `namespace:id`. Si no, basta con `id`
- **Cooldowns/RateLimits:** Se validan en `PRE_EXECUTE`, se aplican automáticamente si `ActionMetadata` los define
- **Caché Caffeine:** Cooldowns y rate limits expiran automáticamente
- **Audit log:** Deshabilitado por defecto, habilitar con `getAuditLogger().setEnabled(true)`

---

## Flujo Completo

```
ActionAPI.execute("myplugin:heal", context)
    ↓
ArgumentParser.parse() → ParsedArguments
    ↓
ActionRegistry.resolve("myplugin:heal") → Action
    ↓
ActionPipeline.execute()
    ├─ PRE_VALIDATE: namespace, estructura
    ├─ PRE_EXECUTE: permiso, cooldown, rate limit
    ├─ Action.execute(context)
    └─ POST_EXECUTE: audit log
    ↓
ActionResult (success/error, message, metadata)
```

---

## Gestión

```java
// Consultar
Optional<Action> action = ActionAPI.get("myplugin:heal");
Collection<Action> all = ActionAPI.getAll();
Collection<Action> byNs = ActionAPI.getAllByNamespace("myplugin");

// Limpiar
ActionAPI.unregister("myplugin:heal", plugin);
ActionAPI.unregisterAll(plugin);  // Todas del plugin

// Reload
ActionAPI.reload();  // Invalida caches
```
```

