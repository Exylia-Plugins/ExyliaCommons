package net.exylia.commons.v2.scoreboard.renderer;

import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardLine;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Resuelve placeholders sobre la plantilla del scoreboard.
 * <p>
 * El parseo de color ya no ocurre aqui: se delega a {@link SidebarHandle}, que
 * solo lo hace para las lineas que realmente cambiaron. En un scoreboard tipico
 * la mayoria de lineas son estaticas o su placeholder devuelve el mismo valor
 * entre ciclos, asi que esto elimina casi todo el trabajo de parseo recurrente.
 */
public class ComponentScoreboardRenderer implements ScoreboardRenderer {

    private final int lineCount;
    private final String[] lineContents;
    private final boolean[] lineDynamic;
    private final String titleContent;
    private final boolean titleDynamic;

    public ComponentScoreboardRenderer(Scoreboard scoreboard) {
        List<ScoreboardLine> lines = scoreboard.getLines();
        this.lineCount = lines != null ? lines.size() : 0;
        this.lineContents = new String[lineCount];
        this.lineDynamic = new boolean[lineCount];

        for (int i = 0; i < lineCount; i++) {
            ScoreboardLine line = lines.get(i);
            String content = line.getContent() != null ? line.getContent() : "";
            lineContents[i] = content;
            lineDynamic[i] = line.isDynamic();
        }

        String title = scoreboard.getTitle();
        this.titleContent = title != null ? title : "";
        this.titleDynamic = titleContent.indexOf('%') >= 0;
    }

    @Override
    public int lineCount() {
        return lineCount;
    }

    @Override
    public void render(Player player, PlaceholderContext context, RenderBuffer out) {
        out.setTitle(titleDynamic
                ? Placeholders.process(titleContent, player, context)
                : titleContent);

        out.reset();

        int max = Sidebar.MAX_LINES;
        for (int i = 0; i < lineCount && out.getLineCount() < max; i++) {
            if (!lineDynamic[i]) {
                out.add(lineContents[i]);
                continue;
            }

            String processed = Placeholders.process(lineContents[i], player, context);

            // Un placeholder puede expandirse a varias lineas. Se capa a
            // MAX_LINES en vez de reventar como hacia FastBoard.checkLineNumber.
            if (processed.indexOf('\n') < 0) {
                out.add(processed);
                continue;
            }

            int start = 0;
            while (start <= processed.length() && out.getLineCount() < max) {
                int end = processed.indexOf('\n', start);
                if (end < 0) {
                    out.add(processed.substring(start));
                    break;
                }
                out.add(processed.substring(start, end));
                start = end + 1;
            }
        }
    }
}
