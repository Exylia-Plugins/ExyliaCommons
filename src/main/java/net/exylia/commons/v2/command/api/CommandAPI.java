package net.exylia.commons.v2.command.api;

import net.exylia.commons.v2.command.core.CommandManager;
import net.exylia.commons.v2.command.core.CommandStats;
import net.exylia.commons.v2.command.model.CommandContext;
import net.exylia.commons.v2.command.model.CommandResult;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CommandAPI {

    private CommandAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        CommandManager.getInstance().initialize(plugin);
    }

    public static boolean isInitialized() {
        return CommandManager.getInstance().isInitialized();
    }

    public static CompletableFuture<CommandResult> execute(Player player, String commandString) {
        return execute(player, commandString, PlaceholderContext.create());
    }

    public static CompletableFuture<CommandResult> execute(
            Player player,
            String commandString,
            PlaceholderContext context
    ) {
        CommandContext cmdContext = CommandContext.builder()
                .player(player)
                .placeholderContext(context)
                .build();

        return CommandManager.getInstance().executeAsync(commandString, cmdContext);
    }

    public static CompletableFuture<List<CommandResult>> executeAll(
            Player player,
            List<String> commands
    ) {
        return executeAll(player, commands, PlaceholderContext.create());
    }

    public static CompletableFuture<List<CommandResult>> executeAll(
            Player player,
            List<String> commands,
            PlaceholderContext context
    ) {
        return CommandManager.getInstance().executeBatch(player, commands, context);
    }

    public static CompletableFuture<List<CommandResult>> fromConfig(
            Player player,
            ConfigurationSection section
    ) {
        return fromConfig(player, section, PlaceholderContext.create());
    }

    public static CompletableFuture<List<CommandResult>> fromConfig(
            Player player,
            ConfigurationSection section,
            PlaceholderContext context
    ) {
        return CommandManager.getInstance().executeFromConfig(player, section, context);
    }

    public static CompletableFuture<List<CommandResult>> fromConfigKey(
            Player player,
            ConfigurationSection section,
            String key,
            PlaceholderContext context
    ) {
        return CommandManager.getInstance().executeFromConfigKey(player, section, key, context);
    }

    public static CommandBuilder builder() {
        return CommandBuilder.create();
    }

    public static void shutdown() {
        CommandManager.getInstance().shutdown();
    }

    public static CompletableFuture<CommandResult> executeAsync(
            String commandString,
            CommandContext context
    ) {
        return CommandManager.getInstance().executeAsync(commandString, context);
    }

    public static CommandStats getStats() {
        return CommandManager.getInstance().getStats();
    }
}
