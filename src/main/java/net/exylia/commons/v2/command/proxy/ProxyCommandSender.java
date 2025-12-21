package net.exylia.commons.v2.command.proxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.exylia.commons.v2.command.model.Command;
import net.exylia.commons.v2.command.model.CommandContext;
import net.exylia.commons.v2.command.model.CommandResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@RequiredArgsConstructor
@Getter
public class ProxyCommandSender {
    private static final String CHANNEL = "exylia:commands";
    private static final String LEGACY_CHANNEL = "BungeeCord";

    private final JavaPlugin plugin;
    private boolean enabled = false;

    public void initialize() {
        try {
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, LEGACY_CHANNEL);
            enabled = true;
            plugin.getLogger().info("Proxy command channels registered successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to register proxy channels", e);
            enabled = false;
        }
    }

    public CompletableFuture<CommandResult> sendAsync(Command command, CommandContext context) {
        return CompletableFuture.supplyAsync(() -> {
            if (!enabled) {
                return CommandResult.failure(command, "Proxy messaging not enabled");
            }

            Player player = context.getPlayer();
            if (player == null || !player.isOnline()) {
                return CommandResult.failure(command, "Player not online for proxy command");
            }

            try {
                ProxyCommandData data = ProxyCommandData.builder()
                        .type(command.getType().name())
                        .command(command.getProcessedCommand())
                        .playerName(player.getName())
                        .playerId(player.getUniqueId().toString())
                        .build();

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Forward");
                out.writeUTF("ALL");
                out.writeUTF("ExyliaCommand");

                out.writeUTF(data.toJson());

                player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());

                return CommandResult.success(command, "Proxy command sent successfully");

            } catch (Exception e) {
                return CommandResult.failure(command, e);
            }
        });
    }
}
