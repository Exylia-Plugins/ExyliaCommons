package net.exylia.commons.utils.visuals;

import net.exylia.commons.utils.AdapterFactory;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.effects.FireworkUtils;
import net.exylia.commons.utils.effects.ParticleUtils;
import net.exylia.commons.utils.effects.SoundUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

        // Process effects if message starts with special prefix
        String cleanMessage = processEffectsAndGetMessage(player, message);
        if (cleanMessage == null || cleanMessage.trim().isEmpty()) return;

        Component component = ColorUtils.parse(cleanMessage);
        sendMessage(player, component);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.trim().isEmpty()) return;

        if (sender instanceof Player player) {
            // For players, process effects
            sendMessage(player, message);
        } else {
            // For non-players, remove effects and send clean message
            String cleanMessage = message;
            if (message.startsWith("[")) {
                int effectsEnd = message.indexOf(']');
                if (effectsEnd != -1) {
                    cleanMessage = message.substring(effectsEnd + 1);
                }
            }
            Component component = ColorUtils.parse(cleanMessage);
            sendMessage(sender, component);
        }
    }

    public static void sendMessage(UUID playerUUID, Component component) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            sendMessage(player, component);
        }
    }

    public static void sendMessage(UUID playerUUID, String message) {
        if (message == null || message.trim().isEmpty()) return;
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            sendMessage(player, message);
        }
    }


    public static void broadcastMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessage(player, message);
        }
    }

    public static void broadcastMessage(Component component) {
        if (component == null) return;
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        if (plainText.trim().isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessage(player, component);
        }
    }


    public static void sendMessage(Collection<Player> players, String message) {
        if (message == null || message.trim().isEmpty()) return;
        for (Player player : players) {
            sendMessage(player, message);
        }
    }

    public static void sendMessage(Collection<Player> players, Component component) {
        if (component == null) return;
        for (Player player : players) {
            sendMessage(player, component);
        }
    }


    public static void broadcastMessageExcluding(Collection<Player> excludePlayers, String message) {
        if (message == null || message.trim().isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!excludePlayers.contains(player)) {
                sendMessage(player, message);
            }
        }
    }

    public static void broadcastMessageExcluding(Player excludePlayer, String message) {
        if (message == null || message.trim().isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(excludePlayer)) {
                sendMessage(player, message);
            }
        }
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


    public static void sendMessageToCollectionExcluding(Collection<Player> recipients, Collection<Player> excludePlayers, String message) {
        if (message == null || message.trim().isEmpty()) return;
        for (Player player : recipients) {
            if (!excludePlayers.contains(player)) {
                sendMessage(player, message);
            }
        }
    }

    public static void sendMessageToCollectionExcluding(Collection<Player> recipients, Player excludePlayer, String message) {
        if (message == null || message.trim().isEmpty()) return;
        for (Player player : recipients) {
            if (!player.equals(excludePlayer)) {
                sendMessage(player, message);
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


    public static void sendActionBar(Player player, Component message) {
        if (message == null) return;
        player.sendActionBar(message);
    }

    public static void sendActionBar(Player player, String message) {
        if (message == null) return;
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

        Bukkit.getScheduler().runTaskLater(
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

        Bukkit.getScheduler().runTaskLater(
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


    public static void sendMessageToFiltered(Predicate<Player> condition, String message) {
        if (message == null || message.trim().isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                sendMessage(player, message);
            }
        }
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


    public static void showBossBarToFiltered(Predicate<Player> condition, BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                showPlayerBossBar(player, bossBar);
            }
        }
    }


    public static void hideBossBarToFiltered(Predicate<Player> condition, BossBar bossBar) {
        if (bossBar == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (condition.test(player)) {
                hidePlayerBossBar(player, bossBar);
            }
        }
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


    public static void sendMessageInRadius(org.bukkit.Location origin, double radius, String message) {
        if (message == null || message.trim().isEmpty() || origin == null) return;
        double radiusSquared = radius * radius;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == origin.getWorld() &&
                    player.getLocation().distanceSquared(origin) <= radiusSquared) {
                sendMessage(player, message);
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

    // ==================== SPECIAL EFFECTS SYSTEM ====================

    /**
     * Processes a message with special effects prefix and returns the clean message.
     * Format: [sounds:sound1|1|1,sound2|1|2;particles:particle1;fireworks:firework1]%prefix% message
     *
     * @param player The player receiving the message
     * @param message The raw message with potential effects
     * @return The clean message without effects prefix
     */
    public static String processEffectsAndGetMessage(Player player, String message) {
        if (message == null || !message.startsWith("[")) {
            return message;
        }

        int effectsEnd = message.indexOf(']');
        if (effectsEnd == -1) {
            return message;
        }

        String effectsSection = message.substring(1, effectsEnd);
        String cleanMessage = message.substring(effectsEnd + 1);

        processEffects(player, effectsSection);
        return cleanMessage;
    }

    /**
     * Processes special effects for a player (must be called from main thread)
     *
     * @param player The player to apply effects to
     * @param effectsSection The effects configuration string
     */
    private static void processEffects(Player player, String effectsSection) {
        if (effectsSection == null || effectsSection.trim().isEmpty() || player == null) {
            return;
        }
        String[] effectTypes = effectsSection.split(";");

        for (String effectType : effectTypes) {
            String[] parts = effectType.split(":", 2);
            if (parts.length != 2) continue;

            String type = parts[0].toLowerCase().trim();
            String config = parts[1].trim();

            switch (type) {
                case "sounds":
                case "sound":
                    processSounds(player, config);
                    break;
                case "particles":
                case "particle":
                    processParticles(player, config);
                    break;
                case "fireworks":
                case "firework":
                    processFireworks(player, config);
                    break;
            }
        }
    }

    /**
     * Processes sound effects from configuration
     * Format: sound1|volume|pitch,sound2|volume|pitch
     */
    private static void processSounds(Player player, String soundsConfig) {
        if (soundsConfig == null || soundsConfig.trim().isEmpty()) return;

        String[] sounds = soundsConfig.split(",");
        for (String sound : sounds) {
            sound = sound.trim();
            if (!sound.isEmpty()) {
                SoundUtils.playSound(player, sound);
            }
        }
    }

    /**
     * Processes particle effects from configuration
     * Format: particle1|count|offsetX|offsetY|offsetZ|extra,particle2...
     */
    private static void processParticles(Player player, String particlesConfig) {
        if (particlesConfig == null || particlesConfig.trim().isEmpty()) return;

        String[] particles = particlesConfig.split(",");
        for (String particle : particles) {
            particle = particle.trim();
            if (!particle.isEmpty()) {
                ParticleUtils.spawnParticles(player, player.getLocation().add(0, 1, 0), particle);
            }
        }
    }

    /**
     * Processes firework effects from configuration
     * Format: firework1|type|colors|fade|flicker|trail|power,firework2...
     */
    private static void processFireworks(Player player, String fireworksConfig) {
        if (fireworksConfig == null || fireworksConfig.trim().isEmpty()) return;

        String[] fireworks = fireworksConfig.split(",");
        for (String firework : fireworks) {
            firework = firework.trim();
            if (!firework.isEmpty()) {
                FireworkUtils.launchFireworkForPlayer(player, firework);
            }
        }
    }

    // ==================== ENHANCED MESSAGE METHODS ====================

    /**
     * Enhanced sendMessage that processes special effects
     */
    public static void sendEnhancedMessage(Player player, String message) {
        if (message == null || message.trim().isEmpty()) return;

        String cleanMessage = processEffectsAndGetMessage(player, message);
        if (cleanMessage != null && !cleanMessage.trim().isEmpty()) {
            sendMessage(player, cleanMessage);
        }
    }

    /**
     * Enhanced sendMessage for CommandSender (effects only work for Players)
     */
    public static void sendEnhancedMessage(CommandSender sender, String message) {
        if (message == null || message.trim().isEmpty()) return;

        if (sender instanceof Player player) {
            sendEnhancedMessage(player, message);
        } else {
            // For non-players, remove effects and send clean message
            String cleanMessage = message;
            if (message.startsWith("[")) {
                int effectsEnd = message.indexOf(']');
                if (effectsEnd != -1) {
                    cleanMessage = message.substring(effectsEnd + 1);
                }
            }
            sendMessage(sender, cleanMessage);
        }
    }

    /**
     * Enhanced broadcast that processes effects for all players
     */
    public static void broadcastEnhancedMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            sendEnhancedMessage(player, message);
        }
    }

    /**
     * Enhanced collection message that processes effects
     */
    public static void sendEnhancedMessage(Collection<Player> players, String message) {
        if (message == null || message.trim().isEmpty() || players == null) return;

        for (Player player : players) {
            sendEnhancedMessage(player, message);
        }
    }

}