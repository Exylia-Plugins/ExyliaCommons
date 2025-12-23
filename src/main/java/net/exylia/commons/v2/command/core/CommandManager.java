package net.exylia.commons.v2.command.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import net.exylia.commons.v2.command.config.CommandConfigLoader;
import net.exylia.commons.v2.command.model.CommandContext;
import net.exylia.commons.v2.command.model.CommandResult;
import net.exylia.commons.v2.command.proxy.ProxyCommandSender;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class CommandManager {
    private static volatile CommandManager instance;
    private static final Object LOCK = new Object();

    private JavaPlugin plugin;
    private CommandExecutor executor;
    private CommandConfigLoader configLoader;
    private ProxyCommandSender proxyCommandSender;
    private Cache<String, CommandResult> resultCache;
    private boolean initialized = false;

    private CommandManager() {
    }

    public static CommandManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new CommandManager();
                }
            }
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        if (initialized) {
            DebugAPI.logLibWarn(DebugCategory.COMMAND, "CommandManager already initialized, skipping");
            throw new IllegalStateException("CommandManager already initialized");
        }

        DebugAPI.logLibInfo(DebugCategory.COMMAND, "Initializing CommandManager for plugin: " + plugin.getName());

        this.plugin = plugin;
        this.proxyCommandSender = new ProxyCommandSender(plugin);
        this.executor = new CommandExecutor(proxyCommandSender);
        this.configLoader = new CommandConfigLoader();

        this.resultCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        this.proxyCommandSender.initialize();
        this.initialized = true;

        DebugAPI.logLibSuccess(DebugCategory.COMMAND, "CommandManager initialized (cache size: 1000, TTL: 5min, proxy: " + proxyCommandSender.isEnabled() + ")");
    }

    public CompletableFuture<CommandResult> executeAsync(
            String commandString,
            CommandContext context
    ) {
        validateInitialized();
        DebugAPI.logLibDebug(DebugCategory.COMMAND, "Executing command: " + commandString);
        return executor.executeAsync(commandString, context);
    }

    public CompletableFuture<List<CommandResult>> executeBatch(
            Player player,
            List<String> commands,
            PlaceholderContext context
    ) {
        validateInitialized();

        DebugAPI.logLibDebug(DebugCategory.COMMAND,
            "Executing batch of " + commands.size() + " commands for player: " + player.getName());

        CommandContext cmdContext = CommandContext.builder()
                .player(player)
                .placeholderContext(context)
                .build();

        List<CompletableFuture<CommandResult>> futures = commands.stream()
                .map(cmd -> executor.executeAsync(cmd, cmdContext))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<CommandResult> results = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());
                    long successful = results.stream().filter(CommandResult::isSuccess).count();
                    DebugAPI.logLibDebug(DebugCategory.COMMAND,
                        "Batch execution completed: " + successful + "/" + results.size() + " successful");
                    return results;
                });
    }

    public CompletableFuture<List<CommandResult>> executeFromConfig(
            Player player,
            ConfigurationSection section,
            PlaceholderContext context
    ) {
        validateInitialized();
        List<String> commands = configLoader.loadCommands(section);
        DebugAPI.logLibDebug(DebugCategory.COMMAND,
            "Loaded " + commands.size() + " commands from config for player: " + player.getName());
        return executeBatch(player, commands, context);
    }

    public CompletableFuture<List<CommandResult>> executeFromConfigKey(
            Player player,
            ConfigurationSection section,
            String key,
            PlaceholderContext context
    ) {
        validateInitialized();
        List<String> commands = configLoader.loadCommandsFromKey(section, key);
        return executeBatch(player, commands, context);
    }

    private void validateInitialized() {
        if (!initialized) {
            throw new IllegalStateException("CommandManager not initialized");
        }
    }

    public CommandStats getStats() {
        return CommandStats.builder()
                .cacheSize(resultCache.estimatedSize())
                .proxyEnabled(proxyCommandSender.isEnabled())
                .build();
    }

    public void reload() {
        validateInitialized();
        DebugAPI.logLibInfo(DebugCategory.COMMAND, "Reloading CommandManager (clearing " + (resultCache != null ? resultCache.estimatedSize() : 0) + " cached results)");

        if (resultCache != null) {
            resultCache.invalidateAll();
        }

        DebugAPI.logLibSuccess(DebugCategory.COMMAND, "CommandManager reloaded successfully");
    }

    public void shutdown() {
        DebugAPI.logLibInfo(DebugCategory.COMMAND, "Shutting down CommandManager (cached results: " + (resultCache != null ? resultCache.estimatedSize() : 0) + ")");
        if (resultCache != null) {
            resultCache.invalidateAll();
        }
        initialized = false;
        DebugAPI.logLibInfo(DebugCategory.COMMAND, "CommandManager shutdown complete");
    }
}
