---

```md
# Sistema de Comandos

---

## ★ Insight
- **Async-first:** Todo ejecuta con CompletableFuture, pero la ejecución final es sync (hilo principal)
- **Prefijos de tipo:** `player:`, `console:`, `player-proxy:`, `console-proxy:`
- **Integración con Placeholders:** Resuelve automáticamente con PlaceholderContext

---

## API Principal

```java
CommandAPI.initialize(plugin);   // onEnable()
CommandAPI.execute(player, "say Hola");
CommandAPI.shutdown();           // onDisable()
```

---

## Tipos de Ejecución

| Prefijo | Descripción |
|---------|-------------|
| (ninguno) | Como player |
| `player:` | Como player |
| `console:` | Como consola |
| `player-proxy:` | Como player en BungeeCord |
| `console-proxy:` | Como consola en BungeeCord |

---

## Formas de Ejecutar

### 1. Simple

```java
CommandAPI.execute(player, "tell @s Bienvenido");
CommandAPI.execute(player, "console:broadcast Hola");
```

### 2. Con Placeholders

```java
PlaceholderContext ctx = PlaceholderContext.create()
    .put("reward", "Diamante");

CommandAPI.execute(player, "give %player_name% %reward%", ctx);
```

### 3. Builder

```java
CommandAPI.builder()
    .command("broadcast %player_name% ganó")
    .asConsole()
    .build(player, ctx);
```

### 4. Batch

```java
CommandAPI.executeAll(player, List.of(
    "give %player_name% diamond 1",
    "console:broadcast %player_name% recibió premio"
));
```

### 5. Desde Config

```java
CommandAPI.fromConfig(player, configSection);
CommandAPI.fromConfigKey(player, configSection, "rewards.commands");
```

---

## Config YAML

```yaml
rewards:
  commands:
    - "give %player_name% diamond 1"
    - "console:broadcast %player_name% ganó"

on-join:
  commands: "give %player_name% bread 5"
```

---

## Flujo de Ejecución

```
CommandAPI.execute(player, command)
    ↓
CommandParser.parseAsync()  // Detecta tipo, resuelve placeholders
    ↓
CommandExecutor.executeAsync()
    ├─ PROXY → BungeeCord plugin message
    └─ LOCAL → Tasks.sync() (hilo principal)
        ├─ PLAYER: player.performCommand()
        └─ CONSOLE: Bukkit.dispatchCommand()
    ↓
CompletableFuture<CommandResult>
```

---

## CommandResult

```java
CommandAPI.execute(player, "give @s diamond")
    .thenAccept(result -> {
        if (result.isSuccess()) {
            // OK
        } else {
            // Error: result.getErrorMessage()
        }
    });
```

---

## ★ Insight
- **Caché Caffeine:** 1000 comandos parseados, TTL 5 min
- **Thread-safe:** Singleton con doble-check locking
- **Proxy commands:** Requiere canal `BungeeCord` registrado
```

