package net.exylia.commons.utils.versions;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Interfaz para enviar mensajes compatible con diferentes versiones
 */
public interface MessageAdapter {
    /**
     * Envía un componente Adventure a un jugador
     * @param player Jugador destinatario
     * @param component Componente Adventure para enviar
     */
    void sendMessage(Player player, Component component);

    /**
     * Envía un componente Adventure a un CommandSender
     * @param sender CommandSender destinatario
     * @param component Componente Adventure para enviar
     */
    void sendMessage(CommandSender sender, Component component);
}
