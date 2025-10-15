package net.exylia.commons.command;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static net.exylia.commons.ExyliaPlugin.isPlaceholderAPIEnabled;
import static net.exylia.commons.utils.DebugUtils.logInternalWarn;

public class CommandExecutor {

    public static void executeCommands(List<String> commands, Player player) {
        executeCommands(commands, player, null, null);
    }

    public static void executeCommands(List<String> commands, Player player, Player placeholderPlayer) {
        executeCommands(commands, player, placeholderPlayer, null);
    }

    public static void executeCommands(List<String> commands, Player player, Player placeholderPlayer, Object placeholderContext) {
        if (commands == null || commands.isEmpty()) return;

        for (String cmd : commands) {
            executeCommand(cmd, player, placeholderPlayer, placeholderContext);
        }
    }

    public static void executeCommand(String command, Player player) {
        executeCommand(command, player, null, null);
    }

    public static void executeCommand(String command, Player player, Player placeholderPlayer) {
        executeCommand(command, player, placeholderPlayer, null);
    }

    public static void executeCommand(String command, Player player, Player placeholderPlayer, Object placeholderContext) {
        if (command == null || command.trim().isEmpty()) return;

        String processedCmd = command.trim();

        if (placeholderContext != null) {
 
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

    private static void executePlayerCommand(String command, Player player) {
        String playerCmd = command.substring(8).trim();  
        if (!playerCmd.isEmpty()) {
            player.performCommand(playerCmd);
        }
    }

    private static void executeConsoleCommand(String command) {
        String consoleCmd = command.substring(9).trim();  
        if (!consoleCmd.isEmpty()) {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(console, consoleCmd);
        }
    }

    private static void executeBungeeCommand(String command, Player player) {
        String bungeeCmd = command.substring(8).trim();  
        if (bungeeCmd.isEmpty()) return;

        if (!BungeeMessageSender.isInitialized()) {
            logInternalWarn("BungeeMessageSender no está inicializado. No se puede ejecutar comando bungee: " + bungeeCmd);
            return;
        }

        BungeeMessageSender.sendCommand(player, bungeeCmd);
    }

    public static class Builder {
        private final Player player;
        private Player placeholderPlayer;
        private Object placeholderContext;

        public Builder(Player player) {
            this.player = player;
        }

        public Builder withPlaceholderPlayer(Player placeholderPlayer) {
            this.placeholderPlayer = placeholderPlayer;
            return this;
        }

        public Builder withPlaceholderContext(Object context) {
            this.placeholderContext = context;
            return this;
        }

        public void execute(String command) {
            CommandExecutor.executeCommand(command, player, placeholderPlayer, placeholderContext);
        }

        public void execute(List<String> commands) {
            CommandExecutor.executeCommands(commands, player, placeholderPlayer, placeholderContext);
        }
    }

    public static Builder builder(Player player) {
        return new Builder(player);
    }
}
