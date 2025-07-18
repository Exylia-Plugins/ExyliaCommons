package net.exylia.commons.utils.versions;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Message_1_18_Adapter implements MessageAdapter {

    public Message_1_18_Adapter(JavaPlugin plugin) {
    }

    @Override
    public void sendMessage(Player player, Component component) {
        player.sendMessage(component);
    }

    @Override
    public void sendMessage(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }

    /**
     * No se necesita cerrar nada en esta versión
     */
    public void close() {
    }
}