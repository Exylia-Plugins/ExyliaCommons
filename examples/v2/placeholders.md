---

````md
# Sistema de Placeholders

---

## ★ Insight
- **Resolución jerárquica:** Context → Player → Global → PAPI (el primero encontrado gana)  
- **Inyección automática:** el sistema detecta qué parámetros necesita tu método y los inyecta  
- **Caché con Caffeine:** evita recalcular placeholders frecuentes  

---

## Resumen

### API Principal

```java
Placeholders.initialize(plugin);
Placeholders.process("Hola %player_name%", player);
Placeholders.processAsync("...", player, context);
````

---

## 3 Formas de Registrar

### 1. Con anotaciones (recomendado)

```java
public class MisPlaceholders {

    @Placeholder(name = "server_online", scope = PlaceholderScope.GLOBAL)
    public int online() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Placeholder(name = "player_level", scope = PlaceholderScope.PLAYER)
    public int level(Player player) {
        return player.getLevel();
    }

    @Placeholder(name = "kit_name", scope = PlaceholderScope.CONTEXT)
    public String kitName(PlaceholderContext ctx) {
        return ctx.get("kit_name", String.class);
    }
}
```

**Registrar:**

```java
Placeholders.registerAnnotatedClass(new MisPlaceholders());
```

---

### 2. Con lambdas

```java
Placeholders.registerGlobal("server_tps", () -> Bukkit.getTPS()[0]);
Placeholders.registerPlayer("player_ping", player -> player.getPing());
Placeholders.registerContext("custom_value", ctx -> ctx.get("value"));
```

---

### 3. Directo en `PlaceholderContext`

```java
PlaceholderContext ctx = PlaceholderContext.create()
    .put("item_name", "Espada")
    .put("item_price", 100)
    .putDynamic("timestamp", System::currentTimeMillis);
```

---

## PlaceholderContext

```java
PlaceholderContext ctx = PlaceholderContext.create()
    .withPlayer(player)
    .with(Kit.class, myKit)           // Por tipo
    .put("custom_key", "value")       // Por clave
    .putDynamic("time", () -> now()); // Dinámico
```

### Obtener

```java
ctx.get("custom_key");                // Object
ctx.get("custom_key", String.class);  // Tipado
ctx.find(Kit.class);                  // Busca en todos los maps
```

---

## Flujo de Resolución

```
%placeholder% encontrado
↓
¿Existe en context.get("placeholder")? → Usa ese valor
↓ no
¿Hay ContextResolver registrado? → Ejecuta
↓ no
¿Hay PlayerResolver y player != null? → Ejecuta
↓ no
¿Hay GlobalResolver? → Ejecuta
↓ no
¿PAPI disponible? → Delega a PAPI
↓ no
Deja %placeholder% sin cambiar
```

---

## Caché

```java
@Placeholder(
    name = "expensive_calc",
    cacheable = true,
    cacheTtlMs = 5000 // 5 segundos
)
public String calc() { ... }
```

---

## Scope

El scope indica qué dependencias necesita tu placeholder para resolverse:

| Scope   | Necesita           | Ejemplo                        | Cuándo usar                         |
| ------- | ------------------ | ------------------------------ | ----------------------------------- |
| GLOBAL  | Nada               | %server_tps%, %online_players% | Valor igual para todos              |
| PLAYER  | Player             | %player_name%, %player_health% | Valor específico por jugador        |
| CONTEXT | PlaceholderContext | %item_price%, %kit_name%       | Datos dinámicos pasados manualmente |

---

## Ejemplo práctico

```java
// GLOBAL
@Placeholder(name = "server_tps", scope = PlaceholderScope.GLOBAL)
public double tps() {
    return Bukkit.getTPS()[0];
}

// PLAYER
@Placeholder(name = "player_kills", scope = PlaceholderScope.PLAYER)
public int kills(Player player) {
    return statsManager.getKills(player);
}

// CONTEXT
@Placeholder(name = "product_price", scope = PlaceholderScope.CONTEXT)
public int price(PlaceholderContext ctx) {
    Product product = ctx.find(Product.class);
    return product.getPrice();
}
```

---

## ★ Insight

* El scope también afecta el caché:

    * GLOBAL → se cachea una vez
    * PLAYER → por jugador
    * CONTEXT → nunca
* Usar un scope incorrecto hará que el placeholder **falle silenciosamente**

---

## Uso

### 1. Placeholders con Argumentos

```java
public class MisPlaceholders {

    @Placeholder(name = "player_has_perm_*", scope = PlaceholderScope.PLAYER)
    public boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    @Placeholder(name = "player_stat_*", scope = PlaceholderScope.PLAYER)
    public int getStat(Player player, String statName) {
        return statsManager.getStat(player, statName);
    }

    @Placeholder(name = "config_*", scope = PlaceholderScope.GLOBAL, hasArgument = true)
    public String getConfig(String path) {
        return plugin.getConfig().getString(path, "N/A");
    }
}
```

#### Ejemplo lore

```yml
lore:
  - '&7Permiso admin: %player_has_perm_admin%'
  - '&7Permiso vip.fly: %player_has_perm_vip.fly%'
  - '&7Kills: %player_stat_kills%'
  - '&7Deaths: %player_stat_deaths%'
  - '&7Server: %config_server.name%'
```

---
  
### 2. Placeholders Anidados

```java
PlaceholderContext ctx = PlaceholderContext.create()
    .put("selected_stat", "kills")
    .put("kills", 150)
    .put("deaths", 30);

String text = "Tu stat %selected_stat%: %kills%";
```

**Pase 1:**
`Tu stat kills: %kills%`

**Pase 2:**
`Tu stat kills: 150`

```yml
lore:
  - 'Valor: %%dynamic_key%%'
```

Si `%dynamic_key%` → `player_level`
Resultado final → `%player_level%` → `25`

---

## ★ Insight

* `_*` al final indica que acepta argumentos (o usa `hasArgument = true`)
* El argumento se inyecta en el primer `String` que no sea `Player` o `Context`
* `MAX_NESTING_DEPTH = 10` previene loops infinitos
* El procesamiento async usa `thenCompose` para encadenar pasadas recursivas

---

```

Si quieres, en el siguiente paso puedo:
- Separarlo por archivos (`api.md`, `scope.md`, `examples.md`), o  
- Adaptarlo a formato de documentación tipo GitBook o Docusaurus.
```
