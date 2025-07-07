package net.exylia.commons.command;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static net.exylia.commons.utils.DebugUtils.*;

/**
 * Utility class para enviar comandos al proxy (BungeeCord/Velocity)
 */
public class BungeeMessageSender {

    private static final String CHANNEL = "exylia:commands";
    private static JavaPlugin plugin;
    private static boolean initialized = false;

    public static void initialize(JavaPlugin javaPlugin) {
        if (initialized) return;

        plugin = javaPlugin;

        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);

        initialized = true;
        logInternalInfo("BungeeMessageSender inicializado. Canal '" + CHANNEL + "' registrado.");
    }

    public static void sendCommand(Player player, String command) {
        if (!initialized) {
            logInternalError("BungeeMessageSender no ha sido inicializado!");
            return;
        }

        if (player == null || command == null || command.trim().isEmpty()) {
            logInternalWarn("Intento de enviar comando inválido al proxy");
            return;
        }

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(stream);

            output.writeUTF("EXECUTE_COMMAND");
            output.writeUTF(command.trim());
            output.writeUTF(player.getName());

            String serverName = "test";
            if (serverName == null || serverName.isEmpty()) {
                serverName = "unknown-server";
            }
            output.writeUTF(serverName);

            player.sendPluginMessage(plugin, CHANNEL, stream.toByteArray());

            logInternalInfo(String.format("Comando enviado al proxy por %s: %s",
                    player.getName(), command));

        } catch (IOException e) {
            logInternalError("Error enviando comando al proxy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static String getChannel() {
        return CHANNEL;
    }
}