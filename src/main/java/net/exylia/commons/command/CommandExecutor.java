package net.exylia.commons.command;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static net.exylia.commons.ExyliaPlugin.isPlaceholderAPIEnabled;
import static net.exylia.commons.utils.DebugUtils.logWarn;

/**
 * Utilidad para ejecutar comandos con soporte para placeholders y diferentes tipos de ejecución
 */
public class CommandExecutor {

    /**
     * Ejecuta una lista de comandos
     * @param commands Lista de comandos a ejecutar
     * @param player Jugador que ejecuta los comandos
     */
    public static void executeCommands(List<String> commands, Player player) {
        executeCommands(commands, player, null, null);
    }

    /**
     * Ejecuta una lista de comandos con contexto de placeholders
     * @param commands Lista de comandos a ejecutar
     * @param player Jugador que ejecuta los comandos
     * @param placeholderPlayer Jugador específico para procesar placeholders (o null para usar player)
     */
    public static void executeCommands(List<String> commands, Player player, Player placeholderPlayer) {
        executeCommands(commands, player, placeholderPlayer, null);
    }

    /**
     * Ejecuta una lista de comandos con contexto completo
     * @param commands Lista de comandos a ejecutar
     * @param player Jugador que ejecuta los comandos
     * @param placeholderPlayer Jugador específico para procesar placeholders (o null para usar player)
     * @param placeholderContext Objeto de contexto para placeholders personalizados
     */
    public static void executeCommands(List<String> commands, Player player, Player placeholderPlayer, Object placeholderContext) {
        if (commands == null || commands.isEmpty()) return;

        for (String cmd : commands) {
            executeCommand(cmd, player, placeholderPlayer, placeholderContext);
        }
    }

    /**
     * Ejecuta un comando individual
     * @param command Comando a ejecutar
     * @param player Jugador que ejecuta el comando
     */
    public static void executeCommand(String command, Player player) {
        executeCommand(command, player, null, null);
    }

    /**
     * Ejecuta un comando individual con contexto de placeholders
     * @param command Comando a ejecutar
     * @param player Jugador que ejecuta el comando
     * @param placeholderPlayer Jugador específico para procesar placeholders (o null para usar player)
     */
    public static void executeCommand(String command, Player player, Player placeholderPlayer) {
        executeCommand(command, player, placeholderPlayer, null);
    }

    /**
     * Ejecuta un comando individual con contexto completo
     * @param command Comando a ejecutar (formato: "player: /comando", "console: /comando", "bungee: /comando")
     * @param player Jugador que ejecuta el comando
     * @param placeholderPlayer Jugador específico para procesar placeholders (o null para usar player)
     * @param placeholderContext Objeto de contexto para placeholders personalizados
     */
    public static void executeCommand(String command, Player player, Player placeholderPlayer, Object placeholderContext) {
        if (command == null || command.trim().isEmpty()) return;

        String processedCmd = command.trim();

        if (placeholderContext != null) {
//            processedCmd = PlaceholderProcessor.process(processedCmd, placeholderContext);
        }

        if (isPlaceholderAPIEnabled()) {
            Player targetPlayer = (placeholderPlayer != null) ? placeholderPlayer : player;
            processedCmd = PlaceholderAPI.setPlaceholders(targetPlayer, processedCmd);
        }

        if (processedCmd.startsWith("player: ")) {
            executePlayerCommand(processedCmd, player);
        } else if (processedCmd.startsWith("console: ")) {
            executeConsoleCommand(processedCmd);
        } else if (processedCmd.startsWith("bungee: ")) {
            executeBungeeCommand(processedCmd, player);
        } else {
            player.performCommand(processedCmd);
        }
    }

    /**
     * Ejecuta un comando como jugador
     * @param command Comando completo (con prefijo "player: ")
     * @param player Jugador que ejecutará el comando
     */
    private static void executePlayerCommand(String command, Player player) {
        String playerCmd = command.substring(8).trim(); // Remover "player: "
        if (!playerCmd.isEmpty()) {
            player.performCommand(playerCmd);
        }
    }

    /**
     * Ejecuta un comando desde la consola
     * @param command Comando completo (con prefijo "console: ")
     */
    private static void executeConsoleCommand(String command) {
        String consoleCmd = command.substring(9).trim(); // Remover "console: "
        if (!consoleCmd.isEmpty()) {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(console, consoleCmd);
        }
    }

    /**
     * Ejecuta un comando a través de BungeeCord
     * @param command Comando completo (con prefijo "bungee: ")
     * @param player Jugador que será enviado al servidor
     */
    private static void executeBungeeCommand(String command, Player player) {
        String bungeeCmd = command.substring(8).trim(); // Remover "bungee: "
        if (bungeeCmd.isEmpty()) return;

        if (!BungeeMessageSender.isInitialized()) {
            logWarn("BungeeMessageSender no está inicializado. No se puede ejecutar comando bungee: " + bungeeCmd);
            return;
        }

        BungeeMessageSender.sendCommand(player, bungeeCmd);
    }

    /**
     * Clase Builder para facilitar la configuración y ejecución de comandos
     */
    public static class Builder {
        private final Player player;
        private Player placeholderPlayer;
        private Object placeholderContext;

        public Builder(Player player) {
            this.player = player;
        }

        /**
         * Establece el jugador específico para procesar placeholders
         * @param placeholderPlayer Jugador para procesar placeholders
         * @return Builder para encadenamiento
         */
        public Builder withPlaceholderPlayer(Player placeholderPlayer) {
            this.placeholderPlayer = placeholderPlayer;
            return this;
        }

        /**
         * Establece el contexto para placeholders personalizados
         * @param context Objeto de contexto
         * @return Builder para encadenamiento
         */
        public Builder withPlaceholderContext(Object context) {
            this.placeholderContext = context;
            return this;
        }

        /**
         * Ejecuta un comando
         * @param command Comando a ejecutar
         */
        public void execute(String command) {
            CommandExecutor.executeCommand(command, player, placeholderPlayer, placeholderContext);
        }

        /**
         * Ejecuta una lista de comandos
         * @param commands Lista de comandos a ejecutar
         */
        public void execute(List<String> commands) {
            CommandExecutor.executeCommands(commands, player, placeholderPlayer, placeholderContext);
        }
    }

    public static Builder builder(Player player) {
        return new Builder(player);
    }
}