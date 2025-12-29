package net.exylia.commons.v2.command.core;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.command.model.Command;
import net.exylia.commons.v2.command.model.CommandContext;
import net.exylia.commons.v2.command.model.CommandResult;
import net.exylia.commons.v2.command.model.CommandSource;
import net.exylia.commons.v2.command.processor.CommandParser;
import net.exylia.commons.v2.command.proxy.ProxyCommandSender;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class CommandExecutor {
    private final ProxyCommandSender proxyCommandSender;

    public CompletableFuture<CommandResult> executeAsync(
            String commandString,
            CommandContext context
    ) {
        DebugAPI.logLibDebug(DebugCategory.COMMAND,
                String.format("[EXECUTE] Executing command '%s' for player '%s'", commandString, context.getPlayer().getName()));
        return CommandParser.parseAsync(
                commandString,
                context.getPlayer(),
                context.getPlaceholderContext()
        ).thenCompose(command -> executeCommand(command, context));
    }

    public CompletableFuture<CommandResult> executeCommand(
            Command command,
            CommandContext context
    ) {
        DebugAPI.logLibDebug(DebugCategory.COMMAND,
                String.format("[EXECUTE] Executing command '%s' for player '%s'", command.getProcessedCommand(), context.getPlayer().getName()));
        if (command.getSource() == CommandSource.PROXY) {
            return executeProxyCommand(command, context);
        } else {
            return executeLocalCommand(command, context);
        }
    }

    private CompletableFuture<CommandResult> executeLocalCommand(
            Command command,
            CommandContext context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();

                Schedulers.sync(() -> {
                    Player player = context.getPlayer();

                    switch (command.getType()) {
                        case PLAYER:
                            if (player != null && player.isOnline()) {
                                player.performCommand(command.getProcessedCommand());
                            }
                            break;

                        case CONSOLE:
                            Bukkit.getServer().dispatchCommand(
                                    Bukkit.getConsoleSender(),
                                    command.getProcessedCommand()
                            );
                            break;

                        default:
                            throw new IllegalStateException("Invalid command type for local execution: " + command.getType());
                    }
                });

                long executionTime = System.currentTimeMillis() - startTime;

                return CommandResult.builder()
                        .success(true)
                        .command(command)
                        .message("Local command executed")
                        .executionTimeMillis(executionTime)
                        .build();

            } catch (Exception e) {
                return CommandResult.failure(command, e);
            }
        });
    }

    private CompletableFuture<CommandResult> executeProxyCommand(
            Command command,
            CommandContext context
    ) {
        return proxyCommandSender.sendAsync(command, context);
    }

    public CompletableFuture<CommandResult> executeWithDelay(
            Command command,
            CommandContext context,
            long delayTicks
    ) {
        CompletableFuture<CommandResult> future = new CompletableFuture<>();

        Schedulers.syncLater(() -> {
            executeCommand(command, context)
                    .thenAccept(future::complete)
                    .exceptionally(throwable -> {
                        future.completeExceptionally(throwable);
                        return null;
                    });
        }, delayTicks);

        return future;
    }
}
