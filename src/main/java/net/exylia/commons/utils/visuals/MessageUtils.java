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

public class MessageUtils {

    public static void sendMessage(Player player, Component component) {
        if (component == null) return;
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        if (plainText.trim().isEmpty()) return;
        AdapterFactory.getMessageAdapter().sendMessage(player, component);
    }

    public static void sendMessage(CommandSender sender, Component component) {
        if (component == null) return;
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        if (plainText.trim().isEmpty()) return;
        AdapterFactory.getMessageAdapter().sendMessage(sender, component);
    }

    public static void sendMessage(Player player, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        sendMessage(player, component);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.trim().isEmpty()) return;
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
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        sendMessage(playerUUID, component);
    }

    public static CompletableFuture<Void> sendMessageAsync(Player player, String message) {
        return CompletableFuture.runAsync(() -> sendMessage(player, message));
    }

    public static CompletableFuture<Void> sendMessageAsync(CommandSender sender, String message) {
        return CompletableFuture.runAsync(() -> sendMessage(sender, message));
    }

    public static CompletableFuture<Void> sendMessageAsync(Player player, Component component) {
        return CompletableFuture.runAsync(() -> sendMessage(player, component));
    }

    public static CompletableFuture<Void> sendMessageAsync(CommandSender sender, Component component) {
        return CompletableFuture.runAsync(() -> sendMessage(sender, component));
    }

    public static void broadcastMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        broadcastMessage(component);
    }

    public static void broadcastMessage(Component component) {
        if (component == null) return;
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        if (plainText.trim().isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessage(player, component);
        }
    }

    public static CompletableFuture<Void> broadcastMessageAsync(String message) {
        return CompletableFuture.runAsync(() -> broadcastMessage(message));
    }

    public static CompletableFuture<Void> broadcastMessageAsync(Component component) {
        return CompletableFuture.runAsync(() -> broadcastMessage(component));
    }

    public static void sendMessage(Collection<Player> players, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        for (Player player : players) {
            sendMessage(player, component);
        }
    }

    public static void sendMessage(Collection<Player> players, Component component) {
        if (component == null) return;
        for (Player player : players) {
            sendMessage(player, component);
        }
    }

    public static CompletableFuture<Void> sendMessageAsync(Collection<Player> players, String message) {
        return CompletableFuture.runAsync(() -> sendMessage(players, message));
    }

    public static CompletableFuture<Void> sendMessageAsync(Collection<Player> players, Component component) {
        return CompletableFuture.runAsync(() -> sendMessage(players, component));
    }

    public static void broadcastMessageExcluding(Collection<Player> excludePlayers, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        broadcastMessageExcluding(excludePlayers, component);
    }

    public static void broadcastMessageExcluding(Player excludePlayer, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        broadcastMessageExcluding(excludePlayer, component);
    }

    public static void broadcastMessageExcluding(Collection<Player> excludePlayers, Component component) {
        if (component == null) return;
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        if (plainText.trim().isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!excludePlayers.contains(player)) {
                sendMessage(player, component);
            }
        }
    }

    public static void broadcastMessageExcluding(Player excludePlayer, Component component) {
        if (component == null) return;
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        if (plainText.trim().isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(excludePlayer)) {
                sendMessage(player, component);
            }
        }
    }

    public static CompletableFuture<Void> broadcastMessageExcludingAsync(Collection<Player> excludePlayers, String message) {
        return CompletableFuture.runAsync(() -> broadcastMessageExcluding(excludePlayers, message));
    }

    public static CompletableFuture<Void> broadcastMessageExcludingAsync(Player excludePlayer, String message) {
        return CompletableFuture.runAsync(() -> broadcastMessageExcluding(excludePlayer, message));
    }

    public static CompletableFuture<Void> broadcastMessageExcludingAsync(Collection<Player> excludePlayers, Component component) {
        return CompletableFuture.runAsync(() -> broadcastMessageExcluding(excludePlayers, component));
    }

    public static CompletableFuture<Void> broadcastMessageExcludingAsync(Player excludePlayer, Component component) {
        return CompletableFuture.runAsync(() -> broadcastMessageExcluding(excludePlayer, component));
    }

    public static void sendMessageToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendMessage(player, component);
            }
        }
    }

    public static void sendMessageToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendMessage(player, component);
            }
        }
    }

    public static void sendMessageToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, Component component) {
        if (component == null) return;
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendMessage(player, component);
            }
        }
    }

    public static void sendMessageToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, Component component) {
        if (component == null) return;
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendMessage(player, component);
            }
        }
    }

    public static CompletableFuture<Void> sendMessageToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, String message) {
        return CompletableFuture.runAsync(() -> sendMessageToCollectionExcluding(recipients, excludePlayers, message));
    }

    public static CompletableFuture<Void> sendMessageToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, String message) {
        return CompletableFuture.runAsync(() -> sendMessageToCollectionExcluding(recipients, excludePlayer, message));
    }

    public static CompletableFuture<Void> sendMessageToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, Component component) {
        return CompletableFuture.runAsync(() -> sendMessageToCollectionExcluding(recipients, excludePlayers, component));
    }

    public static CompletableFuture<Void> sendMessageToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, Component component) {
        return CompletableFuture.runAsync(() -> sendMessageToCollectionExcluding(recipients, excludePlayer, component));
    }

    public static void sendTitleToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if ((title == null || title.trim().isEmpty()) && (subtitle == null || subtitle.trim().isEmpty())) return;
        Component titleComponent = ColorUtils.parse(title != null ? title : "");
        Component subtitleComponent = ColorUtils.parse(subtitle != null ? subtitle : "");
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
            }
        }
    }

    public static void sendTitleToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if ((title == null || title.trim().isEmpty()) && (subtitle == null || subtitle.trim().isEmpty())) return;
        Component titleComponent = ColorUtils.parse(title != null ? title : "");
        Component subtitleComponent = ColorUtils.parse(subtitle != null ? subtitle : "");
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
            }
        }
    }

    public static void sendTitleToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (title == null && subtitle == null) return;
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }

    public static void sendTitleToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (title == null && subtitle == null) return;
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }

    public static CompletableFuture<Void> sendTitleToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitleToCollectionExcluding(recipients, excludePlayers, title, subtitle, fadeIn, stay, fadeOut));
    }

    public static CompletableFuture<Void> sendTitleToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitleToCollectionExcluding(recipients, excludePlayer, title, subtitle, fadeIn, stay, fadeOut));
    }

    public static CompletableFuture<Void> sendTitleToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitleToCollectionExcluding(recipients, excludePlayers, title, subtitle, fadeIn, stay, fadeOut));
    }

    public static CompletableFuture<Void> sendTitleToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitleToCollectionExcluding(recipients, excludePlayer, title, subtitle, fadeIn, stay, fadeOut));
    }

    public static void sendActionBarToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendActionBar(player, component);
            }
        }
    }

    public static void sendActionBarToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendActionBar(player, component);
            }
        }
    }

    public static void sendActionBarToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, Component component) {
        if (component == null) return;
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendActionBar(player, component);
            }
        }
    }

    public static void sendActionBarToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, Component component) {
        if (component == null) return;
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendActionBar(player, component);
            }
        }
    }

    public static CompletableFuture<Void> sendActionBarToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, String message) {
        return CompletableFuture.runAsync(() -> sendActionBarToCollectionExcluding(recipients, excludePlayers, message));
    }

    public static CompletableFuture<Void> sendActionBarToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, String message) {
        return CompletableFuture.runAsync(() -> sendActionBarToCollectionExcluding(recipients, excludePlayer, message));
    }

    public static CompletableFuture<Void> sendActionBarToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, Component component) {
        return CompletableFuture.runAsync(() -> sendActionBarToCollectionExcluding(recipients, excludePlayers, component));
    }

    public static CompletableFuture<Void> sendActionBarToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, Component component) {
        return CompletableFuture.runAsync(() -> sendActionBarToCollectionExcluding(recipients, excludePlayer, component));
    }

    public static void showBossBarToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                showPlayerBossBar(player, bossBar);
            }
        }
    }

    public static void showBossBarToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                showPlayerBossBar(player, bossBar);
            }
        }
    }

    public static void hideBossBarToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                hidePlayerBossBar(player, bossBar);
            }
        }
    }

    public static void hideBossBarToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                hidePlayerBossBar(player, bossBar);
            }
        }
    }

    public static CompletableFuture<Void> showBossBarToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> showBossBarToCollectionExcluding(recipients, excludePlayers, bossBar));
    }

    public static CompletableFuture<Void> showBossBarToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> showBossBarToCollectionExcluding(recipients, excludePlayer, bossBar));
    }

    public static CompletableFuture<Void> hideBossBarToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> hideBossBarToCollectionExcluding(recipients, excludePlayers, bossBar));
    }

    public static CompletableFuture<Void> hideBossBarToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> hideBossBarToCollectionExcluding(recipients, excludePlayer, bossBar));
    }

    public static void playSoundToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, Sound sound, float volume, float pitch) {
        if (sound == null) return;
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                playSound(player, sound, volume, pitch);
            }
        }
    }

    public static void playSoundToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, Sound sound, float volume, float pitch) {
        if (sound == null) return;
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                playSound(player, sound, volume, pitch);
            }
        }
    }

    public static CompletableFuture<Void> playSoundToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, Sound sound, float volume, float pitch) {
        return CompletableFuture.runAsync(() -> playSoundToCollectionExcluding(recipients, excludePlayers, sound, volume, pitch));
    }

    public static CompletableFuture<Void> playSoundToCollectionExcludingAsync(Collection<Player> recipients, Player excludePlayer, Sound sound, float volume, float pitch) {
        return CompletableFuture.runAsync(() -> playSoundToCollectionExcluding(recipients, excludePlayer, sound, volume, pitch));
    }

    public static void sendMessageWithSoundToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, String message, Sound sound, float volume, float pitch) {
        if (message == null || message.trim().isEmpty() || sound == null) return;
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendMessageWithSound(player, message, sound, volume, pitch);
            }
        }
    }

    public static void sendMessageWithSoundToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, String message, Sound sound, float volume, float pitch) {
        if (message == null || message.trim().isEmpty() || sound == null) return;
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendMessageWithSound(player, message, sound, volume, pitch);
            }
        }
    }

    public static CompletableFuture<Void> sendMessageWithSoundToCollectionExcludingAsync(Collection<Player> recipients, Collection<Player> excludePlayers, String message, Sound sound, float volume, float pitch) {
        return CompletableFuture.runAsync(() -> sendMessageWithSoundToCollectionExcluding(recipients, excludePlayers, message, sound, volume, pitch));
    }

    public static void sendTitle(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (title == null && subtitle == null) return;
        player.showTitle(Title.title(
                title != null ? title : Component.empty(),
                subtitle != null ? subtitle : Component.empty(),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        ));
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if ((title == null || title.trim().isEmpty()) && (subtitle == null || subtitle.trim().isEmpty())) return;
        Component titleComponent = ColorUtils.parse(title != null ? title : "");
        Component subtitleComponent = ColorUtils.parse(subtitle != null ? subtitle : "");
        sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
    }

    public static void broadcastTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (title == null && subtitle == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public static void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if ((title == null || title.trim().isEmpty()) && (subtitle == null || subtitle.trim().isEmpty())) return;
        Component titleComponent = ColorUtils.parse(title != null ? title : "");
        Component subtitleComponent = ColorUtils.parse(subtitle != null ? subtitle : "");
        broadcastTitle(titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
    }

    public static void sendTitle(Collection<Player> players, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (title == null && subtitle == null) return;
        for (Player player : players) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public static void sendTitle(Collection<Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if ((title == null || title.trim().isEmpty()) && (subtitle == null || subtitle.trim().isEmpty())) return;
        Component titleComponent = ColorUtils.parse(title != null ? title : "");
        Component subtitleComponent = ColorUtils.parse(subtitle != null ? subtitle : "");
        sendTitle(players, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
    }

    public static CompletableFuture<Void> sendTitleAsync(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitle(player, title, subtitle, fadeIn, stay, fadeOut));
    }

    public static CompletableFuture<Void> sendTitleAsync(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitle(player, title, subtitle, fadeIn, stay, fadeOut));
    }

    public static CompletableFuture<Void> broadcastTitleAsync(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> broadcastTitle(title, subtitle, fadeIn, stay, fadeOut));
    }

    public static CompletableFuture<Void> broadcastTitleAsync(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> broadcastTitle(title, subtitle, fadeIn, stay, fadeOut));
    }

    public static CompletableFuture<Void> sendTitleAsync(Collection<Player> players, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitle(players, title, subtitle, fadeIn, stay, fadeOut));
    }

    public static CompletableFuture<Void> sendTitleAsync(Collection<Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitle(players, title, subtitle, fadeIn, stay, fadeOut));
    }

    public static void sendActionBar(Player player, Component message) {
        if (message == null) return;
        String plainText = PlainTextComponentSerializer.plainText().serialize(message);
        if (plainText.trim().isEmpty()) return;
        player.sendActionBar(message);
    }

    public static void sendActionBar(Player player, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        sendActionBar(player, component);
    }

    public static void broadcastActionBar(Component message) {
        if (message == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendActionBar(player, message);
        }
    }

    public static void broadcastActionBar(String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        broadcastActionBar(component);
    }

    public static void sendActionBar(Collection<Player> players, Component message) {
        if (message == null) return;
        for (Player player : players) {
            sendActionBar(player, message);
        }
    }

    public static void sendActionBar(Collection<Player> players, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        sendActionBar(players, component);
    }

    public static CompletableFuture<Void> sendActionBarAsync(Player player, Component message) {
        return CompletableFuture.runAsync(() -> sendActionBar(player, message));
    }

    public static CompletableFuture<Void> sendActionBarAsync(Player player, String message) {
        return CompletableFuture.runAsync(() -> sendActionBar(player, message));
    }

    public static CompletableFuture<Void> broadcastActionBarAsync(Component message) {
        return CompletableFuture.runAsync(() -> broadcastActionBar(message));
    }

    public static CompletableFuture<Void> broadcastActionBarAsync(String message) {
        return CompletableFuture.runAsync(() -> broadcastActionBar(message));
    }

    public static CompletableFuture<Void> sendActionBarAsync(Collection<Player> players, Component message) {
        return CompletableFuture.runAsync(() -> sendActionBar(players, message));
    }

    public static CompletableFuture<Void> sendActionBarAsync(Collection<Player> players, String message) {
        return CompletableFuture.runAsync(() -> sendActionBar(players, message));
    }

    public static void showPlayerBossBar(Player player, BossBar bossBar) {
        if (bossBar == null) return;
        player.showBossBar(bossBar);
    }

    public static void showPlayersBossBar(BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
        }
    }

    public static void showPlayersBossBar(Collection<Player> players, BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : players) {
            player.showBossBar(bossBar);
        }
    }

    public static CompletableFuture<Void> showPlayerBossBarAsync(Player player, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> showPlayerBossBar(player, bossBar));
    }

    public static CompletableFuture<Void> showPlayersBossBarAsync(BossBar bossBar) {
        return CompletableFuture.runAsync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showBossBar(bossBar);
            }
        });
    }

    public static CompletableFuture<Void> showPlayersBossBarAsync(Collection<Player> players, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> showPlayersBossBar(players, bossBar));
    }

    public static void hidePlayerBossBar(Player player, BossBar bossBar) {
        if (bossBar == null) return;
        player.hideBossBar(bossBar);
    }

    public static void hidePlayersBossBar(BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    public static void hidePlayersBossBar(Collection<Player> players, BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : players) {
            player.hideBossBar(bossBar);
        }
    }

    public static CompletableFuture<Void> hidePlayerBossBarAsync(Player player, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> hidePlayerBossBar(player, bossBar));
    }

    public static CompletableFuture<Void> hidePlayersBossBarAsync(BossBar bossBar) {
        return CompletableFuture.runAsync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.hideBossBar(bossBar);
            }
        });
    }

    public static CompletableFuture<Void> hidePlayersBossBarAsync(Collection<Player> players, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> hidePlayersBossBar(players, bossBar));
    }

    public static BossBar createBossBar(String text, BossBar.Color color, BossBar.Overlay style, BossBar.Flag... flags) {
        if (text == null || text.trim().isEmpty()) return null;
        Component component = ColorUtils.parse(text);
        Set<BossBar.Flag> flagSet = Set.of(flags);
        return BossBar.bossBar(component, 1.0f, color, style, flagSet);
    }

    public static BossBar showTemporaryBossBar(Player player, String text, BossBar.Color color, BossBar.Overlay style, int seconds) {
        if (text == null || text.trim().isEmpty()) return null;
        BossBar bossBar = createBossBar(text, color, style);
        if (bossBar == null) return null;
        showPlayerBossBar(player, bossBar);

        Bukkit.getScheduler().runTaskLaterAsynchronously(
                Bukkit.getPluginManager().getPlugins()[0],
                () -> hidePlayerBossBar(player, bossBar),
                seconds * 20L
        );

        return bossBar;
    }

    public static BossBar showTemporaryBossBarToAll(String text, BossBar.Color color, BossBar.Overlay style, int seconds) {
        if (text == null || text.trim().isEmpty()) return null;
        BossBar bossBar = createBossBar(text, color, style);
        if (bossBar == null) return null;
        showPlayersBossBar(bossBar);

        Bukkit.getScheduler().runTaskLaterAsynchronously(
                Bukkit.getPluginManager().getPlugins()[0],
                () -> hidePlayersBossBar(bossBar),
                seconds * 20L
        );

        return bossBar;
    }

    public static void sendMessageWithSound(Player player, String message, Sound sound, float volume, float pitch) {
        if ((message == null || message.trim().isEmpty()) && sound == null) return;
        if (message != null && !message.trim().isEmpty()) {
            sendMessage(player, message);
        }
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public static CompletableFuture<Void> sendMessageWithSoundAsync(Player player, String message, Sound sound, float volume, float pitch) {
        return CompletableFuture.runAsync(() -> sendMessageWithSound(player, message, sound, volume, pitch));
    }

    public static void sendMessageToFiltered(Predicate<Player> condition, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                sendMessage(player, component);
            }
        }
    }

    public static CompletableFuture<Void> sendMessageToFilteredAsync(Predicate<Player> condition, String message) {
        return CompletableFuture.runAsync(() -> sendMessageToFiltered(condition, message));
    }

    public static void sendTitleToFiltered(Predicate<Player> condition, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if ((title == null || title.trim().isEmpty()) && (subtitle == null || subtitle.trim().isEmpty())) return;
        Component titleComponent = ColorUtils.parse(title != null ? title : "");
        Component subtitleComponent = ColorUtils.parse(subtitle != null ? subtitle : "");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
            }
        }
    }

    public static CompletableFuture<Void> sendTitleToFilteredAsync(Predicate<Player> condition, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return CompletableFuture.runAsync(() -> sendTitleToFiltered(condition, title, subtitle, fadeIn, stay, fadeOut));
    }

    public static void showBossBarToFiltered(Predicate<Player> condition, BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                showPlayerBossBar(player, bossBar);
            }
        }
    }

    public static CompletableFuture<Void> showBossBarToFilteredAsync(Predicate<Player> condition, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> showBossBarToFiltered(condition, bossBar));
    }

    public static void hideBossBarToFiltered(Predicate<Player> condition, BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                hidePlayerBossBar(player, bossBar);
            }
        }
    }

    public static CompletableFuture<Void> hideBossBarToFilteredAsync(Predicate<Player> condition, BossBar bossBar) {
        return CompletableFuture.runAsync(() -> hideBossBarToFiltered(condition, bossBar));
    }

    public static void sendActionBarToFiltered(Predicate<Player> condition, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                sendActionBar(player, component);
            }
        }
    }

    public static CompletableFuture<Void> sendActionBarToFilteredAsync(Predicate<Player> condition, String message) {
        return CompletableFuture.runAsync(() -> sendActionBarToFiltered(condition, message));
    }

    public static void sendPaginatedMessage(Player player, Component header, Component footer, int pageNumber, int itemsPerPage, List<Component> items) {
        if (items == null || items.isEmpty()) return;
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

    public static void sendPaginatedMessage(Player player, String header, String footer, int pageNumber, int itemsPerPage, List<String> items) {
        if (items == null || items.isEmpty()) return;
        Component headerComponent = header != null && !header.trim().isEmpty() ? ColorUtils.parse(header) : null;
        Component footerComponent = footer != null && !footer.trim().isEmpty() ? ColorUtils.parse(footer) : null;

        List<Component> components = new java.util.ArrayList<>();
        for (String item : items) {
            if (item != null && !item.trim().isEmpty()) {
                components.add(ColorUtils.parse(item));
            }
        }

        sendPaginatedMessage(player, headerComponent, footerComponent, pageNumber, itemsPerPage, components);
    }

    public static CompletableFuture<Void> sendPaginatedMessageAsync(Player player, Component header, Component footer, int pageNumber, int itemsPerPage, List<Component> items) {
        return CompletableFuture.runAsync(() -> sendPaginatedMessage(player, header, footer, pageNumber, itemsPerPage, items));
    }

    public static CompletableFuture<Void> sendPaginatedMessageAsync(Player player, String header, String footer, int pageNumber, int itemsPerPage, List<String> items) {
        return CompletableFuture.runAsync(() -> sendPaginatedMessage(player, header, footer, pageNumber, itemsPerPage, items));
    }

    public static void sendRepeatedMessage(Player player, String message, long intervalTicks, long durationTicks) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);

        long iterations = durationTicks / intervalTicks;

        for (long i = 0; i < iterations; i++) {
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugins()[0],
                    () -> sendMessage(player, component),
                    i * intervalTicks
            );
        }
    }

    public static void sendRepeatedActionBar(Player player, String message, long intervalTicks, long durationTicks) {
        if (message == null || message.trim().isEmpty()) return;
        Component component = ColorUtils.parse(message);

        long iterations = durationTicks / intervalTicks;

        for (long i = 0; i < iterations; i++) {
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugins()[0],
                    () -> sendActionBar(player, component),
                    i * intervalTicks
            );
        }
    }

    public static BossBar showProgressBossBar(Player player, String text, BossBar.Color color, BossBar.Overlay style, long durationTicks, boolean decreasing) {
        if (text == null || text.trim().isEmpty()) return null;
        Component component = ColorUtils.parse(text);
        BossBar bossBar = BossBar.bossBar(component, decreasing ? 1.0f : 0.0f, color, style);

        showPlayerBossBar(player, bossBar);

        int updateInterval = 2;
        long iterations = durationTicks / updateInterval;
        float progressChange = 1.0f / iterations;

        for (long i = 1; i <= iterations; i++) {
            final float progress = decreasing ? (1.0f - (progressChange * i)) : (progressChange * i);

            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugins()[0],
                    () -> bossBar.progress(Math.max(0, Math.min(1, progress))),
                    i * updateInterval
            );
        }

        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugins()[0],
                () -> hidePlayerBossBar(player, bossBar),
                durationTicks + 5
        );

        return bossBar;
    }

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        if (sound == null) return;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void broadcastSound(Sound sound, float volume, float pitch) {
        if (sound == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            playSound(player, sound, volume, pitch);
        }
    }

    public static CompletableFuture<Void> broadcastSoundAsync(Sound sound, float volume, float pitch) {
        return CompletableFuture.runAsync(() -> broadcastSound(sound, volume, pitch));
    }

    public static void sendSequence(Player player, List<String> messages, long delayBetweenMessages) {
        if (messages == null || messages.isEmpty()) return;
        for (int i = 0; i < messages.size(); i++) {
            final String message = messages.get(i);
            if (message != null && !message.trim().isEmpty()) {
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugins()[0],
                        () -> sendMessage(player, message),
                        i * delayBetweenMessages
                );
            }
        }
    }

    public static void sendTitleSequence(Player player, List<Pair<String, String>> titles, int fadeIn, int stay, int fadeOut, long delayBetweenTitles) {
        if (titles == null || titles.isEmpty()) return;
        long totalDelay = fadeIn + stay + fadeOut + delayBetweenTitles;

        for (int i = 0; i < titles.size(); i++) {
            final Pair<String, String> title = titles.get(i);
            if (title != null && ((title.key() != null && !title.key().trim().isEmpty()) || (title.value() != null && !title.value().trim().isEmpty()))) {
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugins()[0],
                        () -> sendTitle(player, title.key(), title.value(), fadeIn, stay, fadeOut),
                        i * totalDelay
                );
            }
        }
    }

    public record Pair<K, V>(K key, V value) {
    }

    public static void sendCenteredMessage(Player player, String message) {
        if (message == null || message.trim().isEmpty()) return;
        sendMessage(player, message);
    }

    public static String createBar(char symbol, int length, String color) {
        if (color == null) color = "";
        return color + String.valueOf(symbol).repeat(Math.max(0, length));
    }

    public static void sendDecoratedMessage(Player player, String message, char barSymbol, String barColor, int barLength) {
        if (message == null || message.trim().isEmpty()) return;
        String bar = createBar(barSymbol, barLength, barColor);
        sendMessage(player, bar + " &r" + message + " " + bar);
    }

    public static void sendMultiLineMessage(Player player, List<String> messages) {
        if (messages == null || messages.isEmpty()) return;
        for (String message : messages) {
            if (message != null && !message.trim().isEmpty()) {
                sendMessage(player, message);
            }
        }
    }

    public static CompletableFuture<Void> sendMultiLineMessageAsync(Player player, List<String> messages) {
        return CompletableFuture.runAsync(() -> sendMultiLineMessage(player, messages));
    }

    public static void sendMessageInRadius(org.bukkit.Location origin, double radius, String message) {
        if (message == null || message.trim().isEmpty() || origin == null) return;
        Component component = ColorUtils.parse(message);
        double radiusSquared = radius * radius;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == origin.getWorld() &&
                    player.getLocation().distanceSquared(origin) <= radiusSquared) {
                sendMessage(player, component);
            }
        }
    }

    public static void sendTitleInRadius(org.bukkit.Location origin, double radius, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (((title == null || title.trim().isEmpty()) && (subtitle == null || subtitle.trim().isEmpty())) || origin == null) return;
        Component titleComponent = ColorUtils.parse(title != null ? title : "");
        Component subtitleComponent = ColorUtils.parse(subtitle != null ? subtitle : "");
        double radiusSquared = radius * radius;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == origin.getWorld() &&
                    player.getLocation().distanceSquared(origin) <= radiusSquared) {
                sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
            }
        }
    }
}