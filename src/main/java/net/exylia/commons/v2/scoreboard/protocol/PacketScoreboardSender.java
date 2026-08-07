package net.exylia.commons.v2.scoreboard.protocol;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.util.LegacyComponent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerResetScore;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * The only class in the scoreboard module that knows about PacketEvents.
 * Every update is sent to one player only; no Bukkit scoreboard is touched.
 */
public final class PacketScoreboardSender {

    private static final String LOG_PREFIX = "[Scoreboard] [PacketScoreboardSender] ";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private PacketScoreboardSender() {
    }

    public static boolean isReady(Player player) {
        if (player == null) {
            Bukkit.getLogger().warning(LOG_PREFIX + "isReady()=false -> player is null");
            return false;
        }
        if (!player.isOnline()) {
            Bukkit.getLogger().warning(LOG_PREFIX + "isReady(" + player.getName() + ")=false -> player is not online");
            return false;
        }
        if (!PacketScoreboardSupport.isAvailable()) {
            Bukkit.getLogger().warning(LOG_PREFIX + "isReady(" + player.getName() + ")=false -> PacketScoreboardSupport.isAvailable()=false (see previous log)");
            return false;
        }
        User user = getUser(player);
        if (user == null) {
            Bukkit.getLogger().warning(LOG_PREFIX + "isReady(" + player.getName() + ")=false -> PacketEvents User is null (player connection not tracked by PacketEvents yet)");
            return false;
        }
        Bukkit.getLogger().info(LOG_PREFIX + "isReady(" + player.getName() + ")=true (clientVersion=" + user.getClientVersion() + ")");
        return true;
    }

    public static void createObjective(Player player, String objective, Component title) {
        Bukkit.getLogger().info(LOG_PREFIX + "createObjective(" + playerName(player) + ", objective=" + objective + ", title=" + plain(title) + ")");
        send(player, new WrapperPlayServerScoreboardObjective(
                objective,
                WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
                title == null ? Component.empty() : title,
                WrapperPlayServerScoreboardObjective.RenderType.INTEGER));
        send(player, new WrapperPlayServerDisplayScoreboard(1, objective));
    }

    public static void updateObjective(Player player, String objective, Component title) {
        Bukkit.getLogger().info(LOG_PREFIX + "updateObjective(" + playerName(player) + ", objective=" + objective + ", title=" + plain(title) + ")");
        send(player, new WrapperPlayServerScoreboardObjective(
                objective,
                WrapperPlayServerScoreboardObjective.ObjectiveMode.UPDATE,
                title == null ? Component.empty() : title,
                WrapperPlayServerScoreboardObjective.RenderType.INTEGER));
    }

    public static void removeObjective(Player player, String objective) {
        Bukkit.getLogger().info(LOG_PREFIX + "removeObjective(" + playerName(player) + ", objective=" + objective + ")");
        send(player, new WrapperPlayServerScoreboardObjective(
                objective,
                WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE,
                Component.empty(),
                WrapperPlayServerScoreboardObjective.RenderType.INTEGER));
    }

    public static void createLine(Player player, String objective, String team, String entry, int score, Component line) {
        Bukkit.getLogger().info(LOG_PREFIX + "createLine(" + playerName(player) + ", objective=" + objective + ", team=" + team + ", entry=" + escape(entry) + ", score=" + score + ", text=" + plain(line) + ")");
        send(player, new WrapperPlayServerTeams(
                team,
                WrapperPlayServerTeams.TeamMode.CREATE,
                teamInfo(player, line),
                Collections.singletonList(entry)));
        // BUGFIX: this used to pass `team` where the objective name was
        // expected, so scores were registered against a "b<hash>_<index>"
        // objective that was never created instead of the real objective
        // (the one actually set as the sidebar via DisplayScoreboard). The
        // title showed up but the sidebar itself stayed completely empty.
        updateScore(player, entry, objective, score);
    }

    /**
     * Updates only the team text. Scores are immutable for a configured board,
     * so sending an UpdateScore packet on every refresh would be wasteful.
     */
    public static void updateLine(Player player, String team, String entry, int score, Component line) {
        Bukkit.getLogger().info(LOG_PREFIX + "updateLine(" + playerName(player) + ", team=" + team + ", entry=" + escape(entry) + ", text=" + plain(line) + ")");
        send(player, new WrapperPlayServerTeams(
                team,
                WrapperPlayServerTeams.TeamMode.UPDATE,
                teamInfo(player, line),
                Collections.emptyList()));
    }

    public static void removeLine(Player player, String team, String entry, String objective) {
        Bukkit.getLogger().info(LOG_PREFIX + "removeLine(" + playerName(player) + ", team=" + team + ", entry=" + escape(entry) + ", objective=" + objective + ")");
        send(player, new WrapperPlayServerResetScore(entry, objective));
        send(player, new WrapperPlayServerTeams(
                team,
                WrapperPlayServerTeams.TeamMode.REMOVE,
                Optional.empty(),
                Collections.emptyList()));
    }

    private static void updateScore(Player player, String entry, String objective, int score) {
        Bukkit.getLogger().info(LOG_PREFIX + "updateScore(" + playerName(player) + ", entry=" + escape(entry) + ", objective=" + objective + ", score=" + score + ")");
        send(player, new WrapperPlayServerUpdateScore(
                entry,
                WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
                objective,
                Optional.of(score)));
    }

    public static void clearScore(Player player, String entry, String objective) {
        send(player, new WrapperPlayServerResetScore(entry, objective));
    }

    public static void removeObjectiveAndLines(Player player, String objective, Collection<LineHandle> lines) {
        Bukkit.getLogger().info(LOG_PREFIX + "removeObjectiveAndLines(" + playerName(player) + ", objective=" + objective + ", lineCount=" + (lines == null ? 0 : lines.size()) + ")");
        if (player == null || !player.isOnline()) {
            Bukkit.getLogger().warning(LOG_PREFIX + "removeObjectiveAndLines aborted -> player is null or offline");
            return;
        }
        if (lines != null) {
            for (LineHandle line : lines) {
                clearScore(player, line.entry(), objective);
                send(player, new WrapperPlayServerTeams(
                        line.team(),
                        WrapperPlayServerTeams.TeamMode.REMOVE,
                        Optional.empty(),
                        Collections.emptyList()));
            }
        }
        removeObjective(player, objective);
    }

    private static Optional<WrapperPlayServerTeams.ScoreBoardTeamInfo> teamInfo(Player player, Component line) {
        User user = getUser(player);
        if (user == null || !isLegacyClient(user.getClientVersion())) {
            return Optional.of(new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                    Component.empty(),
                    line == null ? Component.empty() : line,
                    Component.empty(),
                    WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
                    WrapperPlayServerTeams.CollisionRule.NEVER,
                    null,
                    WrapperPlayServerTeams.OptionData.NONE));
        }

        LegacyParts parts = splitLegacy(line == null ? "" : LEGACY.serialize(line), 16);
        return Optional.of(new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                LegacyComponent.empty(),
                new LegacyComponent(parts.prefix()),
                new LegacyComponent(parts.suffix()),
                WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
                WrapperPlayServerTeams.CollisionRule.NEVER,
                null,
                WrapperPlayServerTeams.OptionData.NONE));
    }

    private static boolean isLegacyClient(ClientVersion version) {
        return version != null && version.isOlderThan(ClientVersion.V_1_13);
    }

    private static LegacyParts splitLegacy(String value, int maxLength) {
        if (value.length() <= maxLength) return new LegacyParts(value, "");

        int split = maxLength;
        if (value.charAt(split - 1) == '\u00a7') split--;
        if (split > 0 && split < value.length() && value.charAt(split - 1) == '\u00a7') split--;

        String prefix = value.substring(0, split);
        String suffix = activeLegacyCodes(prefix) + value.substring(split);
        if (suffix.length() > maxLength) suffix = suffix.substring(0, maxLength);
        return new LegacyParts(prefix, suffix);
    }

    private static String activeLegacyCodes(String value) {
        StringBuilder active = new StringBuilder();
        for (int index = 0; index + 1 < value.length(); index++) {
            if (value.charAt(index) != '\u00a7') continue;
            char code = Character.toLowerCase(value.charAt(++index));
            if (code == 'r' || isColor(code)) active.setLength(0);
            active.append('\u00a7').append(code);
        }
        return active.toString();
    }

    private static boolean isColor(char code) {
        return code >= '0' && code <= '9' || code >= 'a' && code <= 'f';
    }

    private static void send(Player player, Object packet) {
        if (player == null) {
            Bukkit.getLogger().warning(LOG_PREFIX + "send() aborted -> player is null (packet=" + packet.getClass().getSimpleName() + ")");
            return;
        }
        if (!player.isOnline()) {
            Bukkit.getLogger().warning(LOG_PREFIX + "send(" + player.getName() + ") aborted -> player is not online (packet=" + packet.getClass().getSimpleName() + ")");
            return;
        }
        User user = getUser(player);
        if (user == null) {
            Bukkit.getLogger().warning(LOG_PREFIX + "send(" + player.getName() + ") aborted -> PacketEvents User is null (packet=" + packet.getClass().getSimpleName() + ")");
            return;
        }
        try {
            user.sendPacketSilently(packet);
            Bukkit.getLogger().info(LOG_PREFIX + "send(" + player.getName() + ") OK -> " + packet.getClass().getSimpleName());
        } catch (Throwable t) {
            Bukkit.getLogger().warning(LOG_PREFIX + "send(" + player.getName() + ") FAILED -> " + packet.getClass().getSimpleName()
                    + " : " + t.getClass().getSimpleName() + " - " + t.getMessage());
        }
    }

    private static User getUser(Player player) {
        try {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) {
                Bukkit.getLogger().warning(LOG_PREFIX + "getUser(" + player.getName() + ") -> PacketEvents returned null User");
            }
            return user;
        } catch (Throwable t) {
            Bukkit.getLogger().warning(LOG_PREFIX + "getUser(" + player.getName() + ") -> exception: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return null;
        }
    }

    private static String playerName(Player player) {
        return player == null ? "null" : player.getName();
    }

    private static String plain(Component component) {
        return component == null ? "" : LEGACY.serialize(component);
    }

    private static String escape(String entry) {
        return entry == null ? "null" : entry.replace("\u00a7", "&");
    }

    private record LegacyParts(String prefix, String suffix) {
    }

    public record LineHandle(String team, String entry) {
    }
}
