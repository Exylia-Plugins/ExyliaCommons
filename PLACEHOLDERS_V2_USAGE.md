# PlaceholdersV2 - Sistema Profesional de Placeholders

Sistema super eficiente de placeholders con anotaciones, auto-discovery, caché Caffeine, async nativo y integración automática con PlaceholderAPI.

## Inicialización

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        PlaceholdersV2.initialize(this);
        PlaceholdersV2.registerPapiExpander("myplugin");
    }

    @Override
    public void onDisable() {
        PlaceholdersV2.shutdown();
    }
}
```

## Uso con Anotaciones (Recomendado)

### Placeholder Global

```java
@Placeholder(name = "server_time", description = "Hora actual del servidor")
public String getServerTime() {
    return new SimpleDateFormat("HH:mm:ss").format(new Date());
}
```

### Placeholder de Jugador

```java
@Placeholder(
    name = "player_name",
    description = "Nombre del jugador",
    scope = PlaceholderScope.PLAYER,
    cacheable = true,
    cacheTtlMs = 5000
)
public String getPlayerName(Player player) {
    return player.getName();
}
```

### Placeholder de Contexto

```java
@Placeholder(
    name = "player_health",
    description = "Vida del jugador desde el contexto",
    scope = PlaceholderScope.CONTEXT
)
public String getPlayerHealth(PlaceholderContext context, Player player) {
    Player contextPlayer = context.getPlayer();
    if (contextPlayer != null) {
        return String.format("%.1f", contextPlayer.getHealth());
    }
    return "N/A";
}
```

### Placeholder Asincrónico

```java
@Placeholder(
    name = "player_balance",
    description = "Balance del jugador (async)",
    scope = PlaceholderScope.PLAYER,
    async = true,
    cacheable = false
)
public String getPlayerBalance(Player player) {
    return getBalanceFromDatabase(player.getUniqueId());
}
```

## Registrar Clases Anotadas

```java
MyPlaceholderHandler handler = new MyPlaceholderHandler();
PlaceholdersV2.registerAnnotatedClass(handler);

PlaceholdersV2.registerAnnotatedClasses(handler1, handler2, handler3);
```

## Registro Manual

Para casos donde no quieras usar anotaciones:

```java
PlaceholdersV2.registerGlobal("status", () -> "Online");

PlaceholdersV2.registerPlayer("kills", player ->
    getPlayerKills(player.getUniqueId())
);

PlaceholdersV2.registerContext("rank", (context, player) -> {
    User user = context.find(User.class);
    return user != null ? user.getRank() : "Visitor";
});
```

## Procesamiento de Placeholders

### Síncrono

```java
String template = "Hola %player_name%, tu balance es: %player_balance%";

String result = PlaceholdersV2.process(template, player);

String resultWithContext = PlaceholdersV2.process(
    template,
    player,
    PlaceholderContext.create().with(userData)
);
```

### Asincrónico

```java
PlaceholdersV2.processAsync(template, player)
    .thenAccept(result -> {
        player.sendMessage(result);
    });

PlaceholdersV2.processAsync(template)
    .thenAccept(result -> {
        System.out.println(result);
    });
```

## Contexto de Placeholders

```java
PlaceholderContext context = PlaceholdersV2.createContext()
    .with(player)
    .with(userData)
    .put("custom_key", "custom_value");

String result = PlaceholdersV2.process(template, context);

String resultWithPlayer = PlaceholdersV2.process(
    template,
    player,
    context
);
```

## Extracción y Validación

```java
List<String> placeholders = PlaceholdersV2.extractPlaceholders(template);

if (PlaceholdersV2.containsPlaceholders(template)) {
    System.out.println("Template contiene placeholders");
}

if (PlaceholdersV2.hasResolver("player_name")) {
    System.out.println("Placeholder existe");
}

Set<String> registered = PlaceholdersV2.getRegisteredPlaceholders();
```

## Monitoreo y Estadísticas

```java
PlaceholderRegistryV2.PlaceholderRegistryStats stats = PlaceholdersV2.getStats();
System.out.println(stats);

PlaceholdersV2.clearCache();
```

## Ejemplo Completo

```java
public class UserPlaceholders {

    @Placeholder(name = "player_name", scope = PlaceholderScope.PLAYER)
    public String getName(Player player) {
        return player.getName();
    }

    @Placeholder(name = "player_level", scope = PlaceholderScope.PLAYER, cacheable = true)
    public int getLevel(Player player) {
        return getUserLevel(player.getUniqueId());
    }

    @Placeholder(name = "server_online", scope = PlaceholderScope.GLOBAL)
    public int getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Placeholder(
        name = "player_rank",
        scope = PlaceholderScope.CONTEXT,
        async = true
    )
    public String getRank(PlaceholderContext context, Player player) {
        User user = context.find(User.class);
        return user != null ? user.getRank() : "N/A";
    }

    @Placeholder(name = "custom_data", scope = PlaceholderScope.CONTEXT)
    public String getCustomData(PlaceholderContext context) {
        return context.get("data_key", String.class);
    }
}

// En tu plugin
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        PlaceholdersV2.initialize(this);
        PlaceholdersV2.registerAnnotatedClass(new UserPlaceholders());
        PlaceholdersV2.registerPapiExpander("myplugin");
    }
}
```

## Características

✅ **Sistema de Anotaciones** - Simple y limpio
✅ **Auto-Discovery** - Escanea clases automáticamente
✅ **Caché Caffeine** - Super eficiente
✅ **Async Nativo** - Soporte total para placeholders async
✅ **PlaceholderAPI** - Integración automática
✅ **Contexto Flexible** - Pasa cualquier dato
✅ **Type-Safe** - Resolución segura de tipos
✅ **Estadísticas** - Monitoreo en tiempo real

