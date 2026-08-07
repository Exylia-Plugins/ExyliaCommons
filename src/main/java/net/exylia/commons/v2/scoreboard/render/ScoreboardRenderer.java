package net.exylia.commons.v2.scoreboard.render;

import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;
import net.exylia.commons.v2.scoreboard.protocol.PacketScoreboardSender;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ScoreboardRenderer {

    private static final String LOG_PREFIX = "[Scoreboard] [ScoreboardRenderer] ";

    private final Scoreboard scoreboard;
    private final String objective;
    private final String teamBase;
    private final List<ScoreboardLine> configuredLines;

    public ScoreboardRenderer(Scoreboard scoreboard, String objective) {
        this.scoreboard = scoreboard;
        this.objective = objective;
        // Team names must be independent from the (already 16-char-truncated)
        // objective name: appending an index suffix to an already-maxed-out
        // objective name left zero room for the suffix, silently truncating
        // back to the objective itself and producing 15 IDENTICAL team names
        // (client discards/overwrites duplicate CREATE packets -> invisible
        // scoreboard). A short, deterministic 9-char base always leaves room
        // for a 1-char line-index suffix while staying under the legacy
        // 16-character team name limit (pre-1.13 clients).
        this.teamBase = "b" + String.format("%08x", objective.hashCode());
        this.configuredLines = scoreboard.getLines() == null ? List.of() : List.copyOf(scoreboard.getLines());
        Bukkit.getLogger().info(LOG_PREFIX + "init -> objective=" + objective + " (len=" + objective.length()
                + "), teamBase=" + teamBase + " (len=" + teamBase.length() + ")");
    }

    public RenderedBoard render(Player player, PlaceholderContext context) {
        PlaceholderContext ctx = context == null ? PlaceholderContext.create() : context;
        String rawTitle = scoreboard.getTitle();
        String processedTitle = process(rawTitle, player, ctx);
        Component title = parseColor(processedTitle, player);
        List<RenderedLine> lines = new ArrayList<>(configuredLines.size());

        for (int index = 0; index < configuredLines.size(); index++) {
            ScoreboardLine configured = configuredLines.get(index);
            String value = process(configured.getContent(), player, ctx);
            lines.add(new RenderedLine(index, parseColor(value, player)));
        }
        Bukkit.getLogger().info(LOG_PREFIX + "render(" + player.getName() + ") -> title=\"" + processedTitle
                + "\", " + lines.size() + " line(s) rendered");
        return new RenderedBoard(title, lines);
    }

    private Component parseColor(String value, Player player) {
        try {
            return ColorAPI.parse(value == null ? "" : value);
        } catch (Throwable t) {
            Bukkit.getLogger().warning(LOG_PREFIX + "ColorAPI.parse() FAILED for player="
                    + (player == null ? "null" : player.getName()) + ", value=\"" + value + "\" -> "
                    + t.getClass().getSimpleName() + " - " + t.getMessage() + " (falling back to empty component)");
            return Component.empty();
        }
    }

    public void create(Player player, RenderedBoard board) {
        PacketScoreboardSender.createObjective(player, objective, board.title());
        for (RenderedLine line : board.lines()) {
            PacketScoreboardSender.createLine(
                    player,
                    objective,
                    teamName(line.index()),
                    entryName(line.index()),
                    scoreFor(line.index(), board.lines().size()),
                    line.content());
        }
    }

    public void update(Player player, RenderedBoard previous, RenderedBoard current) {
        if (previous == null) {
            create(player, current);
            return;
        }
        if (!current.title().equals(previous.title())) {
            PacketScoreboardSender.updateObjective(player, objective, current.title());
        }

        int previousSize = previous.lines().size();
        int currentSize = current.lines().size();
        int shared = Math.min(previousSize, currentSize);
        for (int index = 0; index < shared; index++) {
            RenderedLine oldLine = previous.lines().get(index);
            RenderedLine newLine = current.lines().get(index);
            if (!oldLine.content().equals(newLine.content())) {
                PacketScoreboardSender.updateLine(player, teamName(index), entryName(index), scoreFor(index, currentSize), newLine.content());
            }
        }
        for (int index = shared; index < currentSize; index++) {
            RenderedLine line = current.lines().get(index);
            PacketScoreboardSender.createLine(player, objective, teamName(index), entryName(index), scoreFor(index, currentSize), line.content());
        }
        for (int index = currentSize; index < previousSize; index++) {
            PacketScoreboardSender.removeLine(player, teamName(index), entryName(index), objective);
        }
    }

    public void destroy(Player player, int lineCount) {
        List<PacketScoreboardSender.LineHandle> lines = new ArrayList<>(lineCount);
        for (int index = 0; index < lineCount; index++) {
            lines.add(new PacketScoreboardSender.LineHandle(teamName(index), entryName(index)));
        }
        PacketScoreboardSender.removeObjectiveAndLines(player, objective, lines);
    }

    private String process(String value, Player player, PlaceholderContext context) {
        if (value == null || value.isEmpty()) return "";
        // Placeholder processing is intentionally done before ColorAPI.parse:
        // context values may themselves contain {preset} color tags.
        try {
            return Placeholders.process(value, player, context);
        } catch (Throwable t) {
            Bukkit.getLogger().warning(LOG_PREFIX + "process() FAILED for player="
                    + (player == null ? "null" : player.getName()) + ", raw=\"" + value + "\" -> "
                    + t.getClass().getSimpleName() + " - " + t.getMessage() + " (returning raw value unprocessed)");
            return value;
        }
    }

    private String teamName(int index) {
        // teamBase is fixed at 9 chars, leaving up to 7 chars for "_" + index,
        // comfortably covering the 0-14 range allowed by the 15-line limit
        // while staying under the legacy 16-character team name cap.
        String name = teamBase + "_" + index;
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    private String entryName(int index) {
        // The entry is invisible in the team packet and unique per line.
        return "§" + Integer.toHexString(index);
    }

    private int scoreFor(int index, int size) {
        return size - index;
    }

    public record RenderedBoard(Component title, List<RenderedLine> lines) {
        public RenderedBoard {
            lines = List.copyOf(lines);
        }
    }

    public record RenderedLine(int index, Component content) {
    }
}
