package net.exylia.commons.utils.visuals;

import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Utilidad para enviar mensajes compatibles con múltiples versiones
 */
public class MessageUtils {

    /**
     * Envía un componente a un jugador
     * @param player Jugador destinatario
     * @param component Componente a enviar
     */
    public static void sendMessage(Player player, Component component) {
        AdapterFactory.getMessageAdapter().sendMessage(player, component);
    }

    /**
     * Envía un componente a un CommandSender
     * @param sender Destinatario del mensaje
     * @param component Componente a enviar
     */
    public static void sendMessage(CommandSender sender, Component component) {
        AdapterFactory.getMessageAdapter().sendMessage(sender, component);
    }

    /**
     * Envía un mensaje de texto con formato a un jugador
     * @param player Jugador destinatario
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendMessage(Player player, String message) {
        Component component = ColorUtils.parse(message);
        sendMessage(player, component);
    }

    /**
     * Envía un mensaje de texto con formato a un CommandSender
     * @param sender Destinatario del mensaje
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendMessage(CommandSender sender, String message) {
        Component component = ColorUtils.parse(message);
        sendMessage(sender, component);
    }

    public static void sendMessage(UUID playerUUID, Component component) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            sendMessage(player, component);
        }
    }

    public static void sendMessage(UUID playerUUID, String message) {
        Component component = ColorUtils.parse(message);
        sendMessage(playerUUID, component);
    }

    /**
     * Envía un mensaje asíncrono a un jugador
     * @param player Jugador destinatario
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageAsync(Player player, String message) {
        return CompletableFuture.runAsync(() -> sendMessage(player, message));
    }

    /**
     * Envía un mensaje asíncrono a un CommandSender
     * @param sender Destinatario del mensaje
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageAsync(CommandSender sender, String message) {
        return CompletableFuture.runAsync(() -> sendMessage(sender, message));
    }

    /**
     * Envía un mensaje asíncrono con componente a un jugador
     * @param player Jugador destinatario
     * @param component Componente a enviar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageAsync(Player player, Component component) {
        return CompletableFuture.runAsync(() -> sendMessage(player, component));
    }

    /**
     * Envía un mensaje asíncrono con componente a un CommandSender
     * @param sender Destinatario del mensaje
     * @param component Componente a enviar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageAsync(CommandSender sender, Component component) {
        return CompletableFuture.runAsync(() -> sendMessage(sender, component));
    }

    /**
     * Envía un mensaje a todos los jugadores en línea
     * @param message Mensaje con códigos de color (&)
     */
    public static void broadcastMessage(String message) {
        if (message.trim().isEmpty()) {
            return;
        }
        Component component = ColorUtils.parse(message);
        broadcastMessage(component);
    }

    /**
     * Envía un mensaje a todos los jugadores en línea
     * @param component Componente a enviar
     */
    public static void broadcastMessage(Component component) {
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        if (plainText.trim().isEmpty()) {
            return;

        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessage(player, component);
        }
    }

    /**
     * Envía un mensaje a todos los jugadores en línea asíncronamente
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> broadcastMessageAsync(String message) {
        return CompletableFuture.runAsync(() -> broadcastMessage(message));
    }

    /**
     * Envía un mensaje a todos los jugadores en línea asíncronamente
     * @param component Componente a enviar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> broadcastMessageAsync(Component component) {
        return CompletableFuture.runAsync(() -> broadcastMessage(component));
    }

    /**
     * Envía un mensaje a una colección de jugadores
     * @param players Colección de jugadores
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendMessage(Collection<Player> players, String message) {
        Component component = ColorUtils.parse(message);
        for (Player player : players) {
            sendMessage(player, component);
        }
    }

    /**
     * Envía un mensaje a una colección de jugadores
     * @param players Colección de jugadores
     * @param component Componente a enviar
     */
    public static void sendMessage(Collection<Player> players, Component component) {
        for (Player player : players) {
            sendMessage(player, component);
        }
    }

    /**
     * Envía un mensaje a una colección de jugadores asíncronamente
     * @param players Colección de jugadores
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageAsync(Collection<Player> players, String message) {
        return CompletableFuture.runAsync(() -> sendMessage(players, message));
    }

    /**
     * Envía un mensaje a una colección de jugadores asíncronamente
     * @param players Colección de jugadores
     * @param component Componente a enviar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageAsync(Collection<Player> players, Component component) {
        return CompletableFuture.runAsync(() -> sendMessage(players, component));
    }


    // ============== MÉTODOS DE EXCLUSIÓN MEJORADOS PARA MENSAJES ==============

    /**
     * Envía un mensaje a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendMessageToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, String message) {
        Component component = ColorUtils.parse(message);
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendMessage(player, component);
            }
        }
    }

    /**
     * Envía un mensaje a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendMessageToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, String message) {
        Component component = ColorUtils.parse(message);
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendMessage(player, component);
            }
        }
    }

    /**
     * Envía un mensaje a una colección de jugadores excluyendo jugadores específicos (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param component Componente a enviar
     */
    public static void sendMessageToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, Component component) {
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendMessage(player, component);
            }
        }
    }

    /**
     * Envía un mensaje a una colección de jugadores excluyendo un jugador específico (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param component Componente a enviar
     */
    public static void sendMessageToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, Component component) {
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendMessage(player, component);
            }
        }
    }

// ============== MÉTODOS ASÍNCRONOS DE EXCLUSIÓN PARA MENSAJES ==============

    /**
     * Envía un mensaje asíncronamente a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, String message) {
        return CompletableFuture.runAsync(() -> sendMessageToCollectionExcluding(recipients, excludePlayers, message));
    }

    /**
     * Envía un mensaje asíncronamente a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, String message) {
        return CompletableFuture.runAsync(() -> sendMessageToCollectionExcluding(recipients, excludePlayer, message));
    }

    /**
     * Envía un mensaje asíncronamente a una colección de jugadores excluyendo jugadores específicos (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param component Componente a enviar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, Component component) {
        return CompletableFuture.runAsync(() -> sendMessageToCollectionExcluding(recipients, excludePlayers, component));
    }

    /**
     * Envía un mensaje asíncronamente a una colección de jugadores excluyendo un jugador específico (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param component Componente a enviar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, Component component) {
        return CompletableFuture.runAsync(() -> sendMessageToCollectionExcluding(recipients, excludePlayer, component));
    }

// ============== MÉTODOS DE EXCLUSIÓN PARA TÍTULOS ==============

    /**
     * Envía un título a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitleToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = ColorUtils.parse(title);
        Component subtitleComponent = ColorUtils.parse(subtitle);
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
            }
        }
    }

    /**
     * Envía un título a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitleToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = ColorUtils.parse(title);
        Component subtitleComponent = ColorUtils.parse(subtitle);
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
            }
        }
    }

    /**
     * Envía un título a una colección de jugadores excluyendo jugadores específicos (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitleToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }

    /**
     * Envía un título a una colección de jugadores excluyendo un jugador específico (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitleToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }

// ============== MÉTODOS ASÍNCRONOS DE EXCLUSIÓN PARA TÍTULOS ==============

    /**
     * Envía un título asíncronamente a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> sendTitleToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitleToCollectionExcluding(recipients, excludePlayers, title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Envía un título asíncronamente a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> sendTitleToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitleToCollectionExcluding(recipients, excludePlayer, title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Envía un título asíncronamente a una colección de jugadores excluyendo jugadores específicos (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> sendTitleToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitleToCollectionExcluding(recipients, excludePlayers, title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Envía un título asíncronamente a una colección de jugadores excluyendo un jugador específico (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> sendTitleToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitleToCollectionExcluding(recipients, excludePlayer, title, subtitle, fadeIn, stay, fadeOut));
    }

// ============== MÉTODOS DE EXCLUSIÓN PARA ACTION BAR ==============

    /**
     * Envía un mensaje de acción a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendActionBarToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, String message) {
        Component component = ColorUtils.parse(message);
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendActionBar(player, component);
            }
        }
    }

    /**
     * Envía un mensaje de acción a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendActionBarToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, String message) {
        Component component = ColorUtils.parse(message);
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendActionBar(player, component);
            }
        }
    }

    /**
     * Envía un mensaje de acción a una colección de jugadores excluyendo jugadores específicos (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param component Componente a enviar
     */
    public static void sendActionBarToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, Component component) {
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendActionBar(player, component);
            }
        }
    }

    /**
     * Envía un mensaje de acción a una colección de jugadores excluyendo un jugador específico (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param component Componente a enviar
     */
    public static void sendActionBarToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, Component component) {
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendActionBar(player, component);
            }
        }
    }

// ============== MÉTODOS ASÍNCRONOS DE EXCLUSIÓN PARA ACTION BAR ==============

    /**
     * Envía un mensaje de acción asíncronamente a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendActionBarToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, String message) {
        return CompletableFuture.runAsync(() -> sendActionBarToCollectionExcluding(recipients, excludePlayers, message));
    }

    /**
     * Envía un mensaje de acción asíncronamente a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendActionBarToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, String message) {
        return CompletableFuture.runAsync(() -> sendActionBarToCollectionExcluding(recipients, excludePlayer, message));
    }

    /**
     * Envía un mensaje de acción asíncronamente a una colección de jugadores excluyendo jugadores específicos (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param component Componente a enviar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendActionBarToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, Component component) {
        return CompletableFuture.runAsync(() -> sendActionBarToCollectionExcluding(recipients, excludePlayers, component));
    }

    /**
     * Envía un mensaje de acción asíncronamente a una colección de jugadores excluyendo un jugador específico (con Component)
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param component Componente a enviar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendActionBarToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, Component component) {
        return CompletableFuture.runAsync(() -> sendActionBarToCollectionExcluding(recipients, excludePlayer, component));
    }

// ============== MÉTODOS DE EXCLUSIÓN PARA BOSS BAR ==============

    /**
     * Muestra una barra de jefe a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param bossBar Barra de jefe a mostrar
     */
    public static void showBossBarToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, BossBar bossBar) {
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                showPlayerBossBar(player, bossBar);
            }
        }
    }

    /**
     * Muestra una barra de jefe a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param bossBar Barra de jefe a mostrar
     */
    public static void showBossBarToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, BossBar bossBar) {
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                showPlayerBossBar(player, bossBar);
            }
        }
    }

    /**
     * Oculta una barra de jefe para una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param bossBar Barra de jefe a ocultar
     */
    public static void hideBossBarToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, BossBar bossBar) {
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                hidePlayerBossBar(player, bossBar);
            }
        }
    }

    /**
     * Oculta una barra de jefe para una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param bossBar Barra de jefe a ocultar
     */
    public static void hideBossBarToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, BossBar bossBar) {
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                hidePlayerBossBar(player, bossBar);
            }
        }
    }

// ============== MÉTODOS ASÍNCRONOS DE EXCLUSIÓN PARA BOSS BAR ==============

    /**
     * Muestra una barra de jefe asíncronamente a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param bossBar Barra de jefe a mostrar
     * @return CompletableFuture que completa cuando la barra ha sido mostrada
     */
    public static CompletableFuture<Void> showBossBarToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> showBossBarToCollectionExcluding(recipients, excludePlayers, bossBar));
    }

    /**
     * Muestra una barra de jefe asíncronamente a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param bossBar Barra de jefe a mostrar
     * @return CompletableFuture que completa cuando la barra ha sido mostrada
     */
    public static CompletableFuture<Void> showBossBarToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> showBossBarToCollectionExcluding(recipients, excludePlayer, bossBar));
    }

    /**
     * Oculta una barra de jefe asíncronamente para una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param bossBar Barra de jefe a ocultar
     * @return CompletableFuture que completa cuando la barra ha sido ocultada
     */
    public static CompletableFuture<Void> hideBossBarToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> hideBossBarToCollectionExcluding(recipients, excludePlayers, bossBar));
    }

    /**
     * Oculta una barra de jefe asíncronamente para una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param bossBar Barra de jefe a ocultar
     * @return CompletableFuture que completa cuando la barra ha sido ocultada
     */
    public static CompletableFuture<Void> hideBossBarToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> hideBossBarToCollectionExcluding(recipients, excludePlayer, bossBar));
    }

// ============== MÉTODOS DE EXCLUSIÓN PARA SONIDOS ==============

    /**
     * Reproduce un sonido a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     */
    public static void playSoundToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, Sound sound, float volume, float pitch) {
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                playSound(player, sound, volume, pitch);
            }
        }
    }

    /**
     * Reproduce un sonido a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     */
    public static void playSoundToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, Sound sound, float volume, float pitch) {
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                playSound(player, sound, volume, pitch);
            }
        }
    }

    /**
     * Reproduce un sonido asíncronamente a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     * @return CompletableFuture que completa cuando el sonido ha sido reproducido
     */
    public static CompletableFuture<Void> playSoundToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, Sound sound, float volume, float pitch) {
        return CompletableFuture.runAsync(() -> playSoundToCollectionExcluding(recipients, excludePlayers, sound, volume, pitch));
    }

    /**
     * Reproduce un sonido asíncronamente a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     * @return CompletableFuture que completa cuando el sonido ha sido reproducido
     */
    public static CompletableFuture<Void> playSoundToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, Sound sound, float volume, float pitch) {
        return CompletableFuture.runAsync(() -> playSoundToCollectionExcluding(recipients, excludePlayer, sound, volume, pitch));
    }

// ============== MÉTODOS DE EXCLUSIÓN PARA MENSAJES CON SONIDO ==============

    /**
     * Envía un mensaje con sonido a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param message Mensaje con códigos de color (&)
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     */
    public static void sendMessageWithSoundToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, String message, Sound sound, float volume, float pitch) {
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendMessageWithSound(player, message, sound, volume, pitch);
            }
        }
    }

    /**
     * Envía un mensaje con sonido a una colección de jugadores excluyendo un jugador específico
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayer Jugador a excluir del envío
     * @param message Mensaje con códigos de color (&)
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     */
    public static void sendMessageWithSoundToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, String message, Sound sound, float volume, float pitch) {
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendMessageWithSound(player, message, sound, volume, pitch);
            }
        }
    }

    /**
     * Envía un mensaje con sonido asíncronamente a una colección de jugadores excluyendo jugadores específicos
     * @param recipients Colección de jugadores destinatarios
     * @param excludePlayers Jugadores a excluir del envío
     * @param message Mensaje con códigos de color (&)
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageWithSoundToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, String message, Sound sound, float volume, float pitch) {
        return CompletableFuture.runAsync(() -> sendMessageWithSoundToCollectionExcluding(recipients, excludePlayers, message, sound, volume, pitch));
    }

    /**
     * Envía un título al jugador
     * @param player Jugador destinatario
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitle(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        ));
    }

    /**
     * Envía un título al jugador con texto formateado
     * @param player Jugador destinatario
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = ColorUtils.parse(title);
        Component subtitleComponent = ColorUtils.parse(subtitle);
        sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
    }

    /**
     * Envía un título a todos los jugadores
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void broadcastTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Envía un título a todos los jugadores con texto formateado
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = ColorUtils.parse(title);
        Component subtitleComponent = ColorUtils.parse(subtitle);
        broadcastTitle(titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
    }

    /**
     * Envía un título a una colección de jugadores
     * @param players Colección de jugadores
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitle(Collection<Player> players, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : players) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Envía un título a una colección de jugadores con texto formateado
     * @param players Colección de jugadores
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitle(Collection<Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = ColorUtils.parse(title);
        Component subtitleComponent = ColorUtils.parse(subtitle);
        sendTitle(players, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
    }

    /**
     * Envía un título asíncronamente al jugador
     * @param player Jugador destinatario
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> sendTitleAsync(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitle(player, title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Envía un título asíncronamente al jugador con texto formateado
     * @param player Jugador destinatario
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> sendTitleAsync(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitle(player, title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Envía un título asíncronamente a todos los jugadores
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> broadcastTitleAsync(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> broadcastTitle(title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Envía un título asíncronamente a todos los jugadores con texto formateado
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> broadcastTitleAsync(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> broadcastTitle(title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Envía un título asíncronamente a una colección de jugadores
     * @param players Colección de jugadores
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> sendTitleAsync(Collection<Player> players, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitle(players, title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Envía un título asíncronamente a una colección de jugadores con texto formateado
     * @param players Colección de jugadores
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> sendTitleAsync(Collection<Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitle(players, title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Envía un mensaje de acción (sobre la barra de ítems)
     * @param player Jugador destinatario
     * @param message Mensaje a mostrar
     */
    public static void sendActionBar(Player player, Component message) {
        player.sendActionBar(message);
    }

    /**
     * Envía un mensaje de acción con texto formateado
     * @param player Jugador destinatario
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendActionBar(Player player, String message) {
        Component component = ColorUtils.parse(message);
        sendActionBar(player, component);
    }

    /**
     * Envía un mensaje de acción a todos los jugadores
     * @param message Mensaje a mostrar
     */
    public static void broadcastActionBar(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendActionBar(player, message);
        }
    }

    /**
     * Envía un mensaje de acción a todos los jugadores con texto formateado
     * @param message Mensaje con códigos de color (&)
     */
    public static void broadcastActionBar(String message) {
        Component component = ColorUtils.parse(message);
        broadcastActionBar(component);
    }

    /**
     * Envía un mensaje de acción a una colección de jugadores
     * @param players Colección de jugadores
     * @param message Mensaje a mostrar
     */
    public static void sendActionBar(Collection<Player> players, Component message) {
        for (Player player : players) {
            sendActionBar(player, message);
        }
    }

    /**
     * Envía un mensaje de acción a una colección de jugadores con texto formateado
     * @param players Colección de jugadores
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendActionBar(Collection<Player> players, String message) {
        Component component = ColorUtils.parse(message);
        sendActionBar(players, component);
    }

    /**
     * Envía un mensaje de acción asíncronamente
     * @param player Jugador destinatario
     * @param message Mensaje a mostrar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendActionBarAsync(Player player, Component message) {
        return CompletableFuture.runAsync(() -> sendActionBar(player, message));
    }

    /**
     * Envía un mensaje de acción asíncronamente con texto formateado
     * @param player Jugador destinatario
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendActionBarAsync(Player player, String message) {
        return CompletableFuture.runAsync(() -> sendActionBar(player, message));
    }

    /**
     * Envía un mensaje de acción asíncronamente a todos los jugadores
     * @param message Mensaje a mostrar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> broadcastActionBarAsync(Component message) {
        return CompletableFuture.runAsync(() -> broadcastActionBar(message));
    }

    /**
     * Envía un mensaje de acción asíncronamente a todos los jugadores con texto formateado
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> broadcastActionBarAsync(String message) {
        return CompletableFuture.runAsync(() -> broadcastActionBar(message));
    }

    /**
     * Envía un mensaje de acción asíncronamente a una colección de jugadores
     * @param players Colección de jugadores
     * @param message Mensaje a mostrar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendActionBarAsync(Collection<Player> players, Component message) {
        return CompletableFuture.runAsync(() -> sendActionBar(players, message));
    }

    /**
     * Envía un mensaje de acción asíncronamente a una colección de jugadores con texto formateado
     * @param players Colección de jugadores
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendActionBarAsync(Collection<Player> players, String message) {
        return CompletableFuture.runAsync(() -> sendActionBar(players, message));
    }

    /**
     * Muestra una barra de jefe a un jugador
     * @param player Jugador al que mostrar la barra
     * @param bossBar Barra de jefe a mostrar
     */
    public static void showPlayerBossBar(Player player, BossBar bossBar) {
        player.showBossBar(bossBar);
    }

    /**
     * Muestra una barra de jefe a todos los jugadores
     * @param bossBar Barra de jefe a mostrar
     */
    public static void showPlayersBossBar(BossBar bossBar) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
        }
    }

    /**
     * Muestra una barra de jefe a una colección de jugadores
     * @param players Colección de jugadores
     * @param bossBar Barra de jefe a mostrar
     */
    public static void showPlayersBossBar(Collection<Player> players, BossBar bossBar) {
        for (Player player : players) {
            player.showBossBar(bossBar);
        }
    }

    /**
     * Muestra una barra de jefe a un jugador asíncronamente
     * @param player Jugador al que mostrar la barra
     * @param bossBar Barra de jefe a mostrar
     * @return CompletableFuture que completa cuando la barra ha sido mostrada
     */
    public static CompletableFuture<Void> showPlayerBossBarAsync(Player player, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> showPlayerBossBar(player, bossBar));
    }

    /**
     * Muestra una barra de jefe a todos los jugadores asíncronamente
     * @param bossBar Barra de jefe a mostrar
     * @return CompletableFuture que completa cuando la barra ha sido mostrada
     */
    public static CompletableFuture<Void> showPlayersBossBarAsync(BossBar bossBar) {
        return CompletableFuture.runAsync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showBossBar(bossBar);
            }
        });
    }

    /**
     * Muestra una barra de jefe a una colección de jugadores asíncronamente
     * @param players Colección de jugadores
     * @param bossBar Barra de jefe a mostrar
     * @return CompletableFuture que completa cuando la barra ha sido mostrada
     */
    public static CompletableFuture<Void> showPlayersBossBarAsync(Collection<Player> players, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> showPlayersBossBar(players, bossBar));
    }

    /**
     * Oculta una barra de jefe para un jugador
     * @param player Jugador al que ocultar la barra
     * @param bossBar Barra de jefe a ocultar
     */
    public static void hidePlayerBossBar(Player player, BossBar bossBar) {
        player.hideBossBar(bossBar);
    }

    /**
     * Oculta una barra de jefe para todos los jugadores
     * @param bossBar Barra de jefe a ocultar
     */
    public static void hidePlayersBossBar(BossBar bossBar) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    /**
     * Oculta una barra de jefe para una colección de jugadores
     * @param players Colección de jugadores
     * @param bossBar Barra de jefe a ocultar
     */
    public static void hidePlayersBossBar(Collection<Player> players, BossBar bossBar) {
        for (Player player : players) {
            player.hideBossBar(bossBar);
        }
    }

    /**
     * Oculta una barra de jefe para un jugador asíncronamente
     * @param player Jugador al que ocultar la barra
     * @param bossBar Barra de jefe a ocultar
     * @return CompletableFuture que completa cuando la barra ha sido ocultada
     */
    public static CompletableFuture<Void> hidePlayerBossBarAsync(Player player, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> hidePlayerBossBar(player, bossBar));
    }

    /**
     * Oculta una barra de jefe para todos los jugadores asíncronamente
     * @param bossBar Barra de jefe a ocultar
     * @return CompletableFuture que completa cuando la barra ha sido ocultada
     */
    public static CompletableFuture<Void> hidePlayersBossBarAsync(BossBar bossBar) {
        return CompletableFuture.runAsync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.hideBossBar(bossBar);
            }
        });
    }

    /**
     * Oculta una barra de jefe para una colección de jugadores asíncronamente
     * @param players Colección de jugadores
     * @param bossBar Barra de jefe a ocultar
     * @return CompletableFuture que completa cuando la barra ha sido ocultada
     */
    public static CompletableFuture<Void> hidePlayersBossBarAsync(Collection<Player> players, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> hidePlayersBossBar(players, bossBar));
    }

    /**
     * Crea un BossBar con componente y texto formateado
     * @param text Texto con códigos de color (&)
     * @param color Color de la barra
     * @param style Estilo de la barra
     * @param flags Flags opcionales para la barra
     * @return BossBar creado
     */
    public static BossBar createBossBar(String text, BossBar.Color color, BossBar.Overlay style, BossBar.Flag... flags) {
        Component component = ColorUtils.parse(text);
        Set<BossBar.Flag> flagSet = Set.of(flags);
        return BossBar.bossBar(component, 1.0f, color, style, flagSet);
    }

    /**
     * Crea un BossBar temporal que se muestra durante un tiempo determinado
     * @param player Jugador al que mostrar la barra
     * @param text Texto con códigos de color (&)
     * @param color Color de la barra
     * @param style Estilo de la barra
     * @param seconds Tiempo en segundos que se mostrará la barra
     * @return BossBar creado
     */
    public static BossBar showTemporaryBossBar(Player player, String text, BossBar.Color color, BossBar.Overlay style, int seconds) {
        BossBar bossBar = createBossBar(text, color, style);
        showPlayerBossBar(player, bossBar);

        Bukkit.getScheduler().runTaskLaterAsynchronously(
                Bukkit.getPluginManager().getPlugins()[0], // Usa el primer plugin registrado
                () -> hidePlayerBossBar(player, bossBar),
                seconds * 20L
        );

        return bossBar;
    }

    /**
     * Crea un BossBar temporal que se muestra a todos los jugadores durante un tiempo determinado
     * @param text Texto con códigos de color (&)
     * @param color Color de la barra
     * @param style Estilo de la barra
     * @param seconds Tiempo en segundos que se mostrará la barra
     * @return BossBar creado
     */
    public static BossBar showTemporaryBossBarToAll(String text, BossBar.Color color, BossBar.Overlay style, int seconds) {
        BossBar bossBar = createBossBar(text, color, style);
        showPlayersBossBar(bossBar);

        Bukkit.getScheduler().runTaskLaterAsynchronously(
                Bukkit.getPluginManager().getPlugins()[0], // Usa el primer plugin registrado
                () -> hidePlayersBossBar(bossBar),
                seconds * 20L
        );

        return bossBar;
    }

    /**
     * Envía un mensaje con sonido a un jugador
     * @param player Jugador destinatario
     * @param message Mensaje con códigos de color (&)
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     */
    public static void sendMessageWithSound(Player player, String message, Sound sound, float volume, float pitch) {
        sendMessage(player, message);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Envía un mensaje con sonido a un jugador asíncronamente
     * @param player Jugador destinatario
     * @param message Mensaje con códigos de color (&)
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageWithSoundAsync(Player player, String message, Sound sound, float volume, float pitch) {
        return CompletableFuture.runAsync(() -> sendMessageWithSound(player, message, sound, volume, pitch));
    }

    /**
     * Envía un mensaje a todos los jugadores que cumplen una condición
     * @param condition Condición que deben cumplir los jugadores
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendMessageToFiltered(Predicate<Player> condition, String message) {
        Component component = ColorUtils.parse(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                sendMessage(player, component);
            }
        }
    }

    /**
     * Envía un mensaje a todos los jugadores que cumplen una condición asíncronamente
     * @param condition Condición que deben cumplir los jugadores
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendMessageToFilteredAsync(Predicate<Player> condition, String message) {
        return CompletableFuture.runAsync(() -> sendMessageToFiltered(condition, message));
    }

    /**
     * Envía un título a todos los jugadores que cumplen una condición
     * @param condition Condición que deben cumplir los jugadores
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitleToFiltered(Predicate<Player> condition, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = ColorUtils.parse(title);
        Component subtitleComponent = ColorUtils.parse(subtitle);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
            }
        }
    }

    /**
     * Envía un título a todos los jugadores que cumplen una condición asíncronamente
     * @param condition Condición que deben cumplir los jugadores
     * @param title Título principal con códigos de color (&)
     * @param subtitle Subtítulo con códigos de color (&)
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @return CompletableFuture que completa cuando el título ha sido enviado
     */
    public static CompletableFuture<Void> sendTitleToFilteredAsync(Predicate<Player> condition, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitleToFiltered(condition, title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Muestra una barra de jefe a todos los jugadores que cumplen una condición
     * @param condition Condición que deben cumplir los jugadores
     * @param bossBar Barra de jefe a mostrar
     */
    public static void showBossBarToFiltered(Predicate<Player> condition, BossBar bossBar) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                showPlayerBossBar(player, bossBar);
            }
        }
    }

    /**
     * Muestra una barra de jefe a todos los jugadores que cumplen una condición asíncronamente
     * @param condition Condición que deben cumplir los jugadores
     * @param bossBar Barra de jefe a mostrar
     * @return CompletableFuture que completa cuando la barra ha sido mostrada
     */
    public static CompletableFuture<Void> showBossBarToFilteredAsync(Predicate<Player> condition, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> showBossBarToFiltered(condition, bossBar));
    }

    /**
     * Oculta una barra de jefe para todos los jugadores que cumplen una condición
     * @param condition Condición que deben cumplir los jugadores
     * @param bossBar Barra de jefe a ocultar
     */
    public static void hideBossBarToFiltered(Predicate<Player> condition, BossBar bossBar) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                hidePlayerBossBar(player, bossBar);
            }
        }
    }

    /**
     * Oculta una barra de jefe para todos los jugadores que cumplen una condición asíncronamente
     * @param condition Condición que deben cumplir los jugadores
     * @param bossBar Barra de jefe a ocultar
     * @return CompletableFuture que completa cuando la barra ha sido ocultada
     */
    public static CompletableFuture<Void> hideBossBarToFilteredAsync(Predicate<Player> condition, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> hideBossBarToFiltered(condition, bossBar));
    }

    /**
     * Envía un mensaje de acción a todos los jugadores que cumplen una condición
     * @param condition Condición que deben cumplir los jugadores
     * @param message Mensaje con códigos de color (&)
     */
    public static void sendActionBarToFiltered(Predicate<Player> condition, String message) {
        Component component = ColorUtils.parse(message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                sendActionBar(player, component);
            }
        }
    }

    /**
     * Envía un mensaje de acción a todos los jugadores que cumplen una condición asíncronamente
     * @param condition Condición que deben cumplir los jugadores
     * @param message Mensaje con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendActionBarToFilteredAsync(Predicate<Player> condition, String message) {
        return CompletableFuture.runAsync(() -> sendActionBarToFiltered(condition, message));
    }

    /**
     * Envía un mensaje de chat paginado a un jugador
     * @param player Jugador destinatario
     * @param header Cabecera del mensaje paginado (opcional, puede ser null)
     * @param footer Pie del mensaje paginado (opcional, puede ser null)
     * @param pageNumber Número de página a mostrar
     * @param itemsPerPage Número de elementos por página
     * @param items Lista de elementos a mostrar
     */
    public static void sendPaginatedMessage(Player player, Component header, Component footer, int pageNumber, int itemsPerPage, List<Component> items) {
        int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);

        if (pageNumber < 1) {
            pageNumber = 1;
        } else if (pageNumber > totalPages) {
            pageNumber = totalPages;
        }

        if (header != null) {
            sendMessage(player, header);
        }

        int startIndex = (pageNumber - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            sendMessage(player, items.get(i));
        }

        if (footer != null) {
            sendMessage(player, footer);
        }
    }

    /**
     * Envía un mensaje de chat paginado a un jugador con texto formateado
     * @param player Jugador destinatario
     * @param header Cabecera del mensaje paginado con códigos de color (&) (opcional, puede ser null)
     * @param footer Pie del mensaje paginado con códigos de color (&) (opcional, puede ser null)
     * @param pageNumber Número de página a mostrar
     * @param itemsPerPage Número de elementos por página
     * @param items Lista de elementos a mostrar con códigos de color (&)
     */
    public static void sendPaginatedMessage(Player player, String header, String footer, int pageNumber, int itemsPerPage, List<String> items) {
        Component headerComponent = header != null ? ColorUtils.parse(header) : null;
        Component footerComponent = footer != null ? ColorUtils.parse(footer) : null;

        List<Component> components = new java.util.ArrayList<>();
        for (String item : items) {
            components.add(ColorUtils.parse(item));
        }

        sendPaginatedMessage(player, headerComponent, footerComponent, pageNumber, itemsPerPage, components);
    }

    /**
     * Envía un mensaje de chat paginado a un jugador asíncronamente
     * @param player Jugador destinatario
     * @param header Cabecera del mensaje paginado (opcional, puede ser null)
     * @param footer Pie del mensaje paginado (opcional, puede ser null)
     * @param pageNumber Número de página a mostrar
     * @param itemsPerPage Número de elementos por página
     * @param items Lista de elementos a mostrar
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendPaginatedMessageAsync(Player player, Component header, Component footer, int pageNumber, int itemsPerPage, List<Component> items) {
        return CompletableFuture.runAsync(() -> sendPaginatedMessage(player, header, footer, pageNumber, itemsPerPage, items));
    }

    /**
     * Envía un mensaje de chat paginado a un jugador asíncronamente con texto formateado
     * @param player Jugador destinatario
     * @param header Cabecera del mensaje paginado con códigos de color (&) (opcional, puede ser null)
     * @param footer Pie del mensaje paginado con códigos de color (&) (opcional, puede ser null)
     * @param pageNumber Número de página a mostrar
     * @param itemsPerPage Número de elementos por página
     * @param items Lista de elementos a mostrar con códigos de color (&)
     * @return CompletableFuture que completa cuando el mensaje ha sido enviado
     */
    public static CompletableFuture<Void> sendPaginatedMessageAsync(Player player, String header, String footer, int pageNumber, int itemsPerPage, List<String> items) {
        return CompletableFuture.runAsync(() -> sendPaginatedMessage(player, header, footer, pageNumber, itemsPerPage, items));
    }

    /**
     * Envía un mensaje repetido a un jugador durante un tiempo determinado
     * @param player Jugador destinatario
     * @param message Mensaje con códigos de color (&)
     * @param intervalTicks Intervalo entre mensajes (en ticks)
     * @param durationTicks Duración total (en ticks)
     */
    public static void sendRepeatedMessage(Player player, String message, long intervalTicks, long durationTicks) {
        Component component = ColorUtils.parse(message);

        long iterations = durationTicks / intervalTicks;

        for (long i = 0; i < iterations; i++) {
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugins()[0], // Usa el primer plugin registrado
                    () -> sendMessage(player, component),
                    i * intervalTicks
            );
        }
    }

    /**
     * Envía un ActionBar repetido a un jugador durante un tiempo determinado
     * @param player Jugador destinatario
     * @param message Mensaje con códigos de color (&)
     * @param intervalTicks Intervalo entre mensajes (en ticks)
     * @param durationTicks Duración total (en ticks)
     */
    public static void sendRepeatedActionBar(Player player, String message, long intervalTicks, long durationTicks) {
        Component component = ColorUtils.parse(message);

        long iterations = durationTicks / intervalTicks;

        for (long i = 0; i < iterations; i++) {
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugins()[0], // Usa el primer plugin registrado
                    () -> sendActionBar(player, component),
                    i * intervalTicks
            );
        }
    }

    /**
     * Crea y muestra una BossBar que actualiza su progreso gradualmente
     * @param player Jugador al que mostrar la barra
     * @param text Texto con códigos de color (&)
     * @param color Color de la barra
     * @param style Estilo de la barra
     * @param durationTicks Duración total (en ticks)
     * @param decreasing Si la barra debe disminuir (true) o aumentar (false)
     * @return BossBar creada
     */
    public static BossBar showProgressBossBar(Player player, String text, BossBar.Color color, BossBar.Overlay style, long durationTicks, boolean decreasing) {
        Component component = ColorUtils.parse(text);
        BossBar bossBar = BossBar.bossBar(component, decreasing ? 1.0f : 0.0f, color, style);

        showPlayerBossBar(player, bossBar);

        int updateInterval = 2; // Actualizar cada 2 ticks para mayor suavidad
        long iterations = durationTicks / updateInterval;
        float progressChange = 1.0f / iterations;

        for (long i = 1; i <= iterations; i++) {
            final float progress = decreasing ? (1.0f - (progressChange * i)) : (progressChange * i);

            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugins()[0], // Usa el primer plugin registrado
                    () -> bossBar.progress(Math.max(0, Math.min(1, progress))), // Asegurar que esté entre 0 y 1
                    i * updateInterval
            );
        }

        // Ocultar la barra cuando termina
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugins()[0], // Usa el primer plugin registrado
                () -> hidePlayerBossBar(player, bossBar),
                durationTicks + 5 // +5 ticks para asegurar que se complete la animación
        );

        return bossBar;
    }

    /**
     * Envía notificaciones de sonido a un jugador
     * @param player Jugador destinatario
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     */
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Envía notificaciones de sonido a todos los jugadores
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     */
    public static void broadcastSound(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playSound(player, sound, volume, pitch);
        }
    }

    /**
     * Envía notificaciones de sonido a todos los jugadores asíncronamente
     * @param sound Sonido a reproducir
     * @param volume Volumen del sonido
     * @param pitch Tono del sonido
     * @return CompletableFuture que completa cuando el sonido ha sido enviado
     */
    public static CompletableFuture<Void> broadcastSoundAsync(Sound sound, float volume, float pitch) {
        return CompletableFuture.runAsync(() -> broadcastSound(sound, volume, pitch));
    }

    /**
     * Envía una secuencia de mensajes a un jugador con retrasos
     * @param player Jugador destinatario
     * @param messages Lista de mensajes a enviar
     * @param delayBetweenMessages Retraso entre mensajes (en ticks)
     */
    public static void sendSequence(Player player, List<String> messages, long delayBetweenMessages) {
        for (int i = 0; i < messages.size(); i++) {
            final String message = messages.get(i);
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugins()[0], // Usa el primer plugin registrado
                    () -> sendMessage(player, message),
                    i * delayBetweenMessages
            );
        }
    }

    /**
     * Envía una secuencia de títulos a un jugador con retrasos
     * @param player Jugador destinatario
     * @param titles Lista de pares título-subtítulo a enviar
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     * @param delayBetweenTitles Retraso entre títulos (en ticks, adicional al tiempo de visualización)
     */
    public static void sendTitleSequence(Player player, List<Pair<String, String>> titles, int fadeIn, int stay, int fadeOut, long delayBetweenTitles) {
        long totalDelay = fadeIn + stay + fadeOut + delayBetweenTitles;

        for (int i = 0; i < titles.size(); i++) {
            final Pair<String, String> title = titles.get(i);
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugins()[0], // Usa el primer plugin registrado
                    () -> sendTitle(player, title.key(), title.value(), fadeIn, stay, fadeOut),
                    i * totalDelay
            );
        }
    }

    /**
     * Clase de utilidad para crear pares de valores
     *
     * @param <K> Tipo del primer valor
     * @param <V> Tipo del segundo valor
     */
        public record Pair<K, V>(K key, V value) {
    }

    /**
     * Envía una notificación centrada de chat (intenta centrar el texto en el chat)
     * @param player Jugador destinatario
     * @param message Mensaje a centrar
     */
    public static void sendCenteredMessage(Player player, String message) {
        int chatWidth = 80;

//        String messagePlain = getPlainText(message);
//        int messageWidth = messagePlain.length();

//        int spacesBefore = (chatWidth - messageWidth) / 2;

//        String centeredMessage = " ".repeat(Math.max(0, spacesBefore)) + message;
//        sendMessage(player, centeredMessage);
    }

    /**
     * Crea una barra decorativa para mensajes
     * @param symbol Símbolo a usar para la barra
     * @param length Longitud de la barra
     * @param color Color de la barra (&)
     * @return Barra decorativa
     */
    public static String createBar(char symbol, int length, String color) {
        return color + String.valueOf(symbol).repeat(Math.max(0, length));
    }

    /**
     * Envía un mensaje con barra decorativa
     * @param player Jugador destinatario
     * @param message Mensaje central
     * @param barSymbol Símbolo para la barra
     * @param barColor Color de la barra (&)
     * @param barLength Longitud de cada barra
     */
    public static void sendDecoratedMessage(Player player, String message, char barSymbol, String barColor, int barLength) {
        String bar = createBar(barSymbol, barLength, barColor);
        sendMessage(player, bar + " &r" + message + " " + bar);
    }

    /**
     * Envía múltiples mensajes como un solo bloque
     * @param player Jugador destinatario
     * @param messages Lista de mensajes a enviar
     */
    public static void sendMultiLineMessage(Player player, List<String> messages) {
        for (String message : messages) {
            sendMessage(player, message);
        }
    }

    /**
     * Envía múltiples mensajes como un solo bloque asíncronamente
     * @param player Jugador destinatario
     * @param messages Lista de mensajes a enviar
     * @return CompletableFuture que completa cuando los mensajes han sido enviados
     */
    public static CompletableFuture<Void> sendMultiLineMessageAsync(Player player, List<String> messages) {
        return CompletableFuture.runAsync(() -> sendMultiLineMessage(player, messages));
    }

    /**
     * Envía un mensaje a todos los jugadores en un radio específico
     * @param origin Lugar de origen
     * @param radius Radio en bloques
     * @param message Mensaje a enviar
     */
    public static void sendMessageInRadius(org.bukkit.Location origin, double radius, String message) {
        Component component = ColorUtils.parse(message);
        double radiusSquared = radius * radius;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == origin.getWorld() &&
                    player.getLocation().distanceSquared(origin) <= radiusSquared) {
                sendMessage(player, component);
            }
        }
    }

    /**
     * Envía un título a todos los jugadores en un radio específico
     * @param origin Lugar de origen
     * @param radius Radio en bloques
     * @param title Título principal
     * @param subtitle Subtítulo
     * @param fadeIn Tiempo de aparición (ticks)
     * @param stay Tiempo de visualización (ticks)
     * @param fadeOut Tiempo de desaparición (ticks)
     */
    public static void sendTitleInRadius(org.bukkit.Location origin, double radius, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = ColorUtils.parse(title);
        Component subtitleComponent = ColorUtils.parse(subtitle);
        double radiusSquared = radius * radius;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == origin.getWorld() &&
                    player.getLocation().distanceSquared(origin) <= radiusSquared) {
                sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
            }
        }
    }
}
