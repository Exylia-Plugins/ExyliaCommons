package net.exylia.commons.command;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.DebugUtils.logInternalError;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

/**
 * Clase base para crear comandos de forma sencilla
 */
public abstract class ExyliaCommand implements CommandExecutor, TabCompleter {

    protected final ExyliaPlugin plugin;
    @Getter
    private final String name;
    @Getter
    private final List<String> aliases;

    /**
     * Constructor básico
     *
     * @param plugin Instancia del plugin
     * @param name Nombre del comando
     */
    public ExyliaCommand(ExyliaPlugin plugin, String name) {
        this(plugin, name, new ArrayList<>());
    }

    /**
     * Constructor con aliases
     *
     * @param plugin Instancia del plugin
     * @param name Nombre del comando
     * @param aliases Lista de aliases
     */
    public ExyliaCommand(ExyliaPlugin plugin, String name, List<String> aliases) {
        this.plugin = plugin;
        this.name = name;
        this.aliases = aliases != null ? aliases : new ArrayList<>();
    }

    /**
     * Se ejecuta cuando se llama al comando
     *
     * @param sender Quien ejecuta el comando
     * @param command Objeto Command
     * @param label Label utilizada
     * @param args Argumentos
     * @return true si se ha manejado correctamente
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return execute(sender, label, args);
    }

    /**
     * A implementar con la lógica del comando
     *
     * @param sender Quien ejecuta el comando
     * @param label Label utilizada
     * @param args Argumentos
     * @return true si se ha manejado correctamente
     */
    public abstract boolean execute(CommandSender sender, String label, String[] args);

    /**
     * Para autocompletar el comando
     *
     * @param sender Quien ejecuta el comando
     * @param command Objeto Command
     * @param alias Alias utilizado
     * @param args Argumentos actuales
     * @return Lista de sugerencias o null
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    /**
     * Registra el comando en el servidor con múltiples intentos y validación
     *
     * @return true si se registró correctamente
     */
    public boolean register() {
        return register(3, 50L); // 3 intentos con 50ms de delay
    }

    /**
     * Registra el comando con reintentos configurables
     *
     * @param maxAttempts Número máximo de intentos
     * @param delayMs Delay entre intentos en milisegundos
     * @return true si se registró correctamente
     */
    public boolean register(int maxAttempts, long delayMs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (registerCommand()) {
                    return true;
                }
            } catch (Exception e) {
                logInternalError("Error en intento " + attempt + " registrando comando " + name + ": " + e.getMessage());
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logInternalError("Falló el registro del comando " + name + " después de " + maxAttempts + " intentos");
        return false;
    }

    /**
     * Lógica de registro del comando con verificaciones adicionales
     */
    private boolean registerCommand() throws Exception {
        // Verificar que el CommandMap esté disponible
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            throw new IllegalStateException("CommandMap no está disponible");
        }

        // Verificar si el comando ya existe y desregistrarlo
        unregisterIfExists(commandMap);

        // Crear el comando usando reflexión
        Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
        constructor.setAccessible(true);
        PluginCommand cmd = constructor.newInstance(name, plugin);

        // Configurar el comando
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);

        if (!aliases.isEmpty()) {
            cmd.setAliases(aliases);
        }

        // Registrar en el CommandMap
        boolean registered = commandMap.register(plugin.getName().toLowerCase(), cmd);

        if (registered) {
            // Verificar que realmente se registró
            return verifyRegistration(commandMap);
        }

        return false;
    }

    /**
     * Obtiene el CommandMap del servidor
     */
    private CommandMap getCommandMap() {
        try {
            return Bukkit.getCommandMap();
        } catch (Exception e) {
            // Fallback para versiones más antiguas
            try {
                Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                return (CommandMap) commandMapField.get(Bukkit.getServer());
            } catch (Exception ex) {
                logInternalError("No se pudo obtener el CommandMap: " + ex.getMessage());
                return null;
            }
        }
    }

    /**
     * Desregistra el comando si ya existe
     */
    private void unregisterIfExists(CommandMap commandMap) {
        try {
            // Intentar desregistrar comando principal
            Command existingCommand = commandMap.getCommand(name);
            if (existingCommand != null) {
                unregisterCommand(commandMap, name);
            }

            // Intentar desregistrar aliases
            for (String alias : aliases) {
                Command existingAlias = commandMap.getCommand(alias);
                if (existingAlias != null) {
                    unregisterCommand(commandMap, alias);
                }
            }
        } catch (Exception e) {
            // Ignorar errores de desregistro
        }
    }

    /**
     * Desregistra un comando específico del CommandMap
     */
    private void unregisterCommand(CommandMap commandMap, String commandName) {
        try {
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            knownCommands.remove(commandName.toLowerCase());
        } catch (Exception e) {
            // Ignorar errores
        }
    }

    /**
     * Verifica que el comando se registró correctamente
     */
    private boolean verifyRegistration(CommandMap commandMap) {
        try {
            Command registeredCommand = commandMap.getCommand(name);
            if (registeredCommand == null) {
                return false;
            }

            // Verificar que el executor es correcto
            if (registeredCommand instanceof PluginCommand pluginCommand) {
                return pluginCommand.getExecutor() == this;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Métodos de ayuda para todos los comandos (sin cambios)

    /**
     * Verifica si el remitente es un jugador
     *
     * @param sender Remitente a verificar
     * @return true si es un jugador
     */
    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    /**
     * Obtiene el jugador si el remitente es un jugador
     *
     * @param sender Remitente
     * @return Jugador o null
     */
    protected Player getPlayer(CommandSender sender) {
        return isPlayer(sender) ? (Player) sender : null;
    }

    /**
     * Verifica si un jugador está online
     *
     * @param name Nombre del jugador
     * @return true si está online
     */
    protected boolean isPlayerOnline(String name) {
        return Bukkit.getPlayer(name) != null;
    }

    /**
     * Obtiene un jugador por su nombre
     *
     * @param name Nombre del jugador
     * @return Jugador o null si no está online
     */
    protected Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    /**
     * Filtra una lista de sugerencias basada en el argumento actual
     *
     * @param suggestions Lista de sugerencias
     * @param arg Argumento actual para filtrar
     * @return Lista filtrada
     */
    protected List<String> filterSuggestions(List<String> suggestions, String arg) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una lista de nombres de jugadores online
     *
     * @return Lista de nombres
     */
    protected List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una lista filtrada de nombres de jugadores online
     *
     * @param arg Argumento para filtrar
     * @return Lista filtrada
     */
    protected List<String> getOnlinePlayerNames(String arg) {
        return filterSuggestions(getOnlinePlayerNames(), arg);
    }

    /**
     * Verifica si un remitente tiene un permiso
     *
     * @param sender Remitente
     * @param permission Permiso a verificar
     * @return true si tiene el permiso
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }
}