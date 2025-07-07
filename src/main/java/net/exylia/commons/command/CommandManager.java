package net.exylia.commons.command;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.exylia.commons.utils.DebugUtils.logInternalInfo;

/**
 * Gestor de comandos para plugins
 * Permite registrar y gestionar comandos fácilmente
 */
public class CommandManager {

    private final JavaPlugin plugin;
    private final Map<String, ExyliaCommand> commands;
    private final Map<String, String> aliasMap;

    /**
     * Constructor
     *
     * @param plugin Plugin al que pertenece este gestor
     */
    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.commands = new HashMap<>();
        this.aliasMap = new HashMap<>();
    }

    /**
     * Registra un comando
     *
     * @param command Comando a registrar
     * @return true si se registró correctamente
     */
    public boolean registerCommand(ExyliaCommand command) {
        String cmdName = command.getName().toLowerCase();

        // Guardar comando en el mapa
        commands.put(cmdName, command);

        // Registrar aliases
        for (String alias : command.getAliases()) {
            aliasMap.put(alias.toLowerCase(), cmdName);
        }

        // Registrar en Bukkit
        boolean success = command.register();

        if (success) {
            logInternalInfo("Command /" + command.getName() + " registered");
        }

        return success;
    }

    /**
     * Registra múltiples comandos a la vez
     *
     * @param commands Comandos a registrar
     */
    public void registerCommands(ExyliaCommand... commands) {
        for (ExyliaCommand command : commands) {
            registerCommand(command);
        }
    }

    /**
     * Registra todos los comandos de una lista
     *
     * @param commands Lista de comandos
     */
    public void registerCommands(List<ExyliaCommand> commands) {
        for (ExyliaCommand command : commands) {
            registerCommand(command);
        }
    }

    /**
     * Obtiene un comando por su nombre o alias
     *
     * @param name Nombre o alias del comando
     * @return Comando o null si no existe
     */
    public ExyliaCommand getCommand(String name) {
        String lowercaseName = name.toLowerCase();

        // Verificar si es un alias
        if (aliasMap.containsKey(lowercaseName)) {
            return commands.get(aliasMap.get(lowercaseName));
        }

        return commands.get(lowercaseName);
    }

    /**
     * Obtiene todos los comandos registrados
     *
     * @return Lista con todos los comandos
     */
    public List<ExyliaCommand> getCommands() {
        return new ArrayList<>(commands.values());
    }

    /**
     * Desregistra todos los comandos
     * Útil para recargar el plugin
     */
    public void unregisterAll() {
        commands.clear();
        aliasMap.clear();
        // Nota: Los comandos seguirán registrados en Bukkit hasta que el plugin se desactive
    }
}
