package net.exylia.commons.utils.versions;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface MessageAdapter {
     
    void sendMessage(Player player, Component component);

    void sendMessage(CommandSender sender, Component component);
}
