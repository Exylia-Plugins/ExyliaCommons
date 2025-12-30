package net.exylia.commons.v2.scoreboard.expander;

import net.exylia.commons.v2.placeholders.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LineExpander {

    private LineExpander() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static List<String> expandLines(List<String> lines, Player player, PlaceholderContext context) {
        List<String> expanded = new ArrayList<>();

        for (String line : lines) {
            String processed = Placeholders.process(line, player, context);

            if (processed.contains("\n")) {
                String[] split = processed.split("\n");
                for (String splitLine : split) {
                    expanded.add(splitLine);
                }
            } else {
                expanded.add(processed);
            }
        }

        return expanded;
    }
}
