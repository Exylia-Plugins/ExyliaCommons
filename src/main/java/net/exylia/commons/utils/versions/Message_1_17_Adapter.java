package net.exylia.commons.utils.versions;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Message_1_17_Adapter implements MessageAdapter {
    private final BukkitAudiences audience;

    public Message_1_17_Adapter(JavaPlugin plugin) {
        // Inicializar BukkitAudiences para enviar mensajes
        this.audience = BukkitAudiences.create(plugin);
    }

    @Override
    public void sendMessage(Player player, Component component) {
        audience.player(player).sendMessage(component);
    }

    @Override
    public void sendMessage(CommandSender sender, Component component) {
        audience.sender(sender).sendMessage(component);
    }

    /**
     * Cierra correctamente el manejador de audiencia
     */
    public void close() {
        if (audience != null) {
            audience.close();
        }
    }
}