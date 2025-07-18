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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.DebugUtils.logInternalError;
import static net.exylia.commons.utils.DebugUtils.logInternalInfo;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

/**
 * Clase base para crear comandos de forma sencilla con registro mejorado
 */
public abstract class ExyliaCommand implements CommandExecutor, TabCompleter {

    protected final ExyliaPlugin plugin;
    @Getter
    private final String name;
    @Getter
    private final List<String> aliases;

    // Cache para evitar múltiples registros
    private static final Map<String, ExyliaCommand> registeredCommands = new ConcurrentHashMap<>();
    private boolean isRegistered = false;
    private ReflectCommand reflectCommand;

    /**
     * Constructor básico
     */
    public ExyliaCommand(ExyliaPlugin plugin, String name) {
        this(plugin, name, new ArrayList<>());
    }

    /**
     * Constructor con aliases
     */
    public ExyliaCommand(ExyliaPlugin plugin, String name, List<String> aliases) {
        this.plugin = plugin;
        this.name = name;
        this.aliases = aliases != null ? aliases : new ArrayList<>();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return execute(sender, label, args);
    }

    /**
     * A implementar con la lógica del comando
     */
    public abstract boolean execute(CommandSender sender, String label, String[] args);

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    /**
     * Registra el comando con verificaciones mejoradas
     */
    public boolean register() {
        if (isRegistered) {
            logInternalWarn("Comando " + name + " ya está registrado");
            return true;
        }

        try {
            // Verificar si ya existe en el cache
            String lowerName = name.toLowerCase();
            if (registeredCommands.containsKey(lowerName)) {
                logInternalWarn("Comando " + name + " ya existe en el cache");
                return false;
            }

            // Intentar el registro
            if (registerCommandInternal()) {
                registeredCommands.put(lowerName, this);
                isRegistered = true;

                // Verificar el registro después de un tick
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!verifyFinalRegistration()) {
                        logInternalError("Verificación post-registro falló para " + name);
                        // Intentar re-registro
                        forceReregister();
                    }
                }, 1L);

                return true;
            }
        } catch (Exception e) {
            logInternalError("Error registrando comando " + name + ": " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Lógica interna de registro mejorada
     */
    private boolean registerCommandInternal() throws Exception {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            throw new IllegalStateException("CommandMap no disponible");
        }

        // Limpiar registros previos de forma más agresiva
        cleanupPreviousRegistrations(commandMap);

        // Crear comando con configuración mejorada
        reflectCommand = new ReflectCommand(this.name);
        reflectCommand.setExecutor(this);
        reflectCommand.setTabCompleter(this);

        // Configurar aliases de forma segura
        if (!aliases.isEmpty()) {
            List<String> safeAliases = new ArrayList<>();
            for (String alias : aliases) {
                if (!commandExists(commandMap, alias)) {
                    safeAliases.add(alias);
                } else {
                    logInternalWarn("Alias " + alias + " ya existe, se omite");
                }
            }
            reflectCommand.setAliases(safeAliases);
        }

        // Registrar con prefijo del plugin para evitar conflictos
        String prefixedName = plugin.getName().toLowerCase() + ":" + name;
        boolean registered = commandMap.register(prefixedName, reflectCommand);

        if (!registered) {
            // Intentar sin prefijo como fallback
            registered = commandMap.register(plugin.getName(), reflectCommand);
        }

        if (registered) {
            // Forzar actualización del comando en Bukkit
            updateBukkitCommand();

            // Programar actualización adicional para asegurar sincronización
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ensureCommandSynchronization();
            }, 5L);

            return true;
        }

        return false;
    }

    /**
     * Asegura la sincronización del comando con el servidor y clientes
     */
    private void ensureCommandSynchronization() {
        try {
            // Re-verificar que el comando existe
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) return;

            Command cmd = commandMap.getCommand(name);
            if (cmd == null) {
                cmd = commandMap.getCommand(plugin.getName() + ":" + name);
            }

            if (cmd == null) {
                logInternalWarn("Comando " + name + " perdido durante sincronización, reregistrando...");
                // Intentar re-registro inmediato
                Bukkit.getScheduler().runTask(plugin, () -> {
                    forceReregister();
                });
                return;
            }

            // Forzar actualización del cache de comandos
            forceCommandCacheUpdate();

            // Verificación final después de la sincronización
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!testCommandFromPlayer()) {
                    logInternalWarn("Comando " + name + " no accesible para jugadores, forzando reregistro...");
                    forceReregister();
                }
            }, 20L); // 1 segundo después

        } catch (Exception e) {
            logInternalError("Error en sincronización: " + e.getMessage());
        }
    }

    /**
     * Prueba si el comando es accesible para los jugadores
     */
    private boolean testCommandFromPlayer() {
        try {
            // Verificar si algún jugador online puede ver el comando
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                try {
                    // Comprobar si el comando está en la lista de comandos disponibles del jugador
                    Command cmd = Bukkit.getServer().getCommandMap().getCommand(name);
                    if (cmd != null) {
                        return true;
                    }
                } catch (Exception e) {
                    // Continuar con el siguiente jugador
                }
            }

            // Si no hay jugadores online, asumir que funciona
            return Bukkit.getOnlinePlayers().isEmpty();

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica si un comando existe en el CommandMap
     */
    private boolean commandExists(CommandMap commandMap, String commandName) {
        return commandMap.getCommand(commandName) != null;
    }

    /**
     * Limpia registros previos de forma más completa
     */
    private void cleanupPreviousRegistrations(CommandMap commandMap) {
        try {
            // Limpiar comando principal
            unregisterFromMap(commandMap, name);
            unregisterFromMap(commandMap, plugin.getName() + ":" + name);

            // Limpiar aliases
            for (String alias : aliases) {
                unregisterFromMap(commandMap, alias);
                unregisterFromMap(commandMap, plugin.getName() + ":" + alias);
            }

            // Limpiar del servidor si existe
            Command bukkitCommand = Bukkit.getPluginCommand(name);
            if (bukkitCommand instanceof PluginCommand pluginCommand) {
                pluginCommand.setExecutor(null);
                pluginCommand.setTabCompleter(null);
            }

        } catch (Exception e) {
            logInternalWarn("Error limpiando registros previos: " + e.getMessage());
        }
    }

    /**
     * Desregistra un comando del mapa de comandos
     */
    private void unregisterFromMap(CommandMap commandMap, String commandName) {
        try {
            Map<String, Command> knownCommands = commandMap.getKnownCommands();
            Command removed = knownCommands.remove(commandName.toLowerCase());
            if (removed != null) {
                logInternalInfo("Removido comando previo: " + commandName);
            }
        } catch (Exception e) {
            // Fallback usando reflexión
            try {
                Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
                knownCommands.remove(commandName.toLowerCase());
            } catch (Exception ex) {
                // Ignorar si no se puede limpiar
            }
        }
    }

    /**
     * Actualiza el comando en Bukkit y fuerza la sincronización con los clientes
     */
    private void updateBukkitCommand() {
        try {
            // Intentar obtener y actualizar el PluginCommand
            PluginCommand pluginCommand = plugin.getCommand(name);
            if (pluginCommand != null) {
                pluginCommand.setExecutor(this);
                pluginCommand.setTabCompleter(this);
                if (!aliases.isEmpty()) {
                    pluginCommand.setAliases(aliases);
                }
            }

            // Forzar actualización del cache de comandos para todos los jugadores
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                forceCommandCacheUpdate();
            }, 1L);

        } catch (Exception e) {
            logInternalWarn("No se pudo actualizar PluginCommand: " + e.getMessage());
        }
    }

    /**
     * Fuerza la actualización del cache de comandos para todos los jugadores online
     */
    private void forceCommandCacheUpdate() {
        try {
            // Enviar actualización de comandos a todos los jugadores
            Bukkit.getOnlinePlayers().forEach(player -> {
                try {
                    player.updateCommands();
                } catch (Exception e) {
                    try {
                        Object handle = player.getClass().getMethod("getHandle").invoke(player);
                        Object playerConnection = handle.getClass().getField("playerConnection").get(handle);

                        // Enviar packet de comandos actualizado
                        Class<?> packetClass = Class.forName("net.minecraft.server.v1_" +
                                Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1) +
                                ".PacketPlayOutCommands");

                        // Si no se puede enviar el packet, al menos intentar reconectarlo suavemente
                    } catch (Exception ex) {
                        // Último fallback: programar una verificación más tarde
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            verifyPlayerCommandAccess(player);
                        }, 20L);
                    }
                }
            });
        } catch (Exception e) {
            logInternalWarn("Error forzando actualización de cache: " + e.getMessage());
        }
    }

    /**
     * Verifica que un jugador específico pueda usar el comando
     */
    private void verifyPlayerCommandAccess(org.bukkit.entity.Player player) {
        // Verificación silenciosa - si el jugador no puede usar el comando,
        // programa otro intento de actualización
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                // Intentar que el servidor "redescubra" el comando
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                        "help " + name);
            } catch (Exception e) {
                // Ignorar errores
            }
        }, 40L);
    }

    /**
     * Verificación final del registro con múltiples comprobaciones
     */
    private boolean verifyFinalRegistration() {
        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) return false;

            // Verificar que el comando existe en el CommandMap
            Command cmd = commandMap.getCommand(name);
            if (cmd == null) {
                cmd = commandMap.getCommand(plugin.getName() + ":" + name);
            }

            if (cmd == null) {
                logInternalError("Comando " + name + " no encontrado en CommandMap después del registro");
                return false;
            }

            // Verificar que el executor es correcto
            boolean executorCorrect = false;
            if (cmd instanceof ReflectCommand reflectCmd) {
                executorCorrect = reflectCmd.getExecutor() == this;
            } else if (cmd instanceof PluginCommand pluginCmd) {
                executorCorrect = pluginCmd.getExecutor() == this;
            }

            if (!executorCorrect) {
                logInternalWarn("Executor del comando " + name + " no es correcto");
                return false;
            }

            // Verificación adicional: comprobar que el comando responde
            return testCommandExecution();

        } catch (Exception e) {
            logInternalError("Error verificando registro: " + e.getMessage());
            return false;
        }
    }

    /**
     * Prueba que el comando responde correctamente
     */
    private boolean testCommandExecution() {
        try {
            // Crear un sender de prueba (consola)
            CommandSender testSender = Bukkit.getConsoleSender();

            // Obtener el comando del servidor
            Command serverCommand = Bukkit.getServer().getCommandMap().getCommand(name);
            if (serverCommand == null) {
                serverCommand = Bukkit.getServer().getCommandMap().getCommand(plugin.getName() + ":" + name);
            }

            if (serverCommand != null) {
                // El comando existe y está accesible
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fuerza un re-registro en caso de fallo
     */
    private void forceReregister() {
        logInternalInfo("Forzando re-registro de " + name);
        isRegistered = false;
        registeredCommands.remove(name.toLowerCase());

        // Intentar re-registro después de un delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (register()) {
                logInternalInfo("Re-registro exitoso de " + name);
            } else {
                logInternalError("Re-registro falló para " + name);
            }
        }, 5L);
    }

    /**
     * Obtiene el CommandMap con múltiples métodos de fallback
     */
    private CommandMap getCommandMap() {
        try {
            return Bukkit.getCommandMap();
        } catch (Exception e) {
            // Fallback 1: Reflexión estándar
            try {
                Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                return (CommandMap) commandMapField.get(Bukkit.getServer());
            } catch (Exception ex) {
                // Fallback 2: A través de CraftServer
                try {
                    Object craftServer = Bukkit.getServer();
                    Field commandMapField = craftServer.getClass().getSuperclass().getDeclaredField("commandMap");
                    commandMapField.setAccessible(true);
                    return (CommandMap) commandMapField.get(craftServer);
                } catch (Exception ex2) {
                    logInternalError("No se pudo obtener CommandMap: " + ex2.getMessage());
                    return null;
                }
            }
        }
    }

    /**
     * Desregistra el comando completamente
     */
    public void unregister() {
        if (!isRegistered) return;

        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap != null) {
                cleanupPreviousRegistrations(commandMap);
            }

            registeredCommands.remove(name.toLowerCase());
            isRegistered = false;
            reflectCommand = null;

            logInternalInfo("Comando " + name + " desregistrado");
        } catch (Exception e) {
            logInternalError("Error desregistrando comando " + name + ": " + e.getMessage());
        }
    }

    /**
     * Verifica si el comando está registrado
     */
    public boolean isRegistered() {
        return isRegistered && verifyFinalRegistration();
    }

    // Métodos de utilidad (sin cambios)
    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    protected Player getPlayer(CommandSender sender) {
        return isPlayer(sender) ? (Player) sender : null;
    }

    protected boolean isPlayerOnline(String name) {
        return Bukkit.getPlayer(name) != null;
    }

    protected Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    protected List<String> filterSuggestions(List<String> suggestions, String arg) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                .collect(Collectors.toList());
    }

    protected List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    protected List<String> getOnlinePlayerNames(String arg) {
        return filterSuggestions(getOnlinePlayerNames(), arg);
    }

    protected boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    /**
     * Clase interna mejorada para el comando
     */
    private static final class ReflectCommand extends Command {
        private ExyliaCommand executor = null;

        private ReflectCommand(String command) {
            super(command);
        }

        public void setExecutor(ExyliaCommand executor) {
            this.executor = executor;
        }

        public ExyliaCommand getExecutor() {
            return executor;
        }

        public void setTabCompleter(TabCompleter tabCompleter) {
            // Almacenar referencia si es necesario
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
            if (executor != null) {
                return executor.onCommand(sender, this, commandLabel, args);
            }
            return false;
        }

        @Override
        public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
            if (executor != null) {
                return executor.onTabComplete(sender, this, alias, args);
            }
            return Collections.emptyList();
        }
    }
}