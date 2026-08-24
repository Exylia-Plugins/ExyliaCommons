package net.exylia.commons.v2.scoreboard.renderer;

import lombok.Getter;
import net.exylia.commons.v2.scoreboard.core.ScoreboardLibraryProvider;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Handle agnostico al classloader para un sidebar compartido.
 *
 * El objeto sidebar real nunca se castea localmente: todas las operaciones pasan
 * por ScoreboardLibraryProvider y se ejecutan en el broker propietario.
 */
public class SidebarHandle {

    private final ScoreboardLibraryProvider provider;
    private final Object sidebar;

    @Getter
    private final Player player;

    private String[] lastLines;
    private int visibleLines;
    private String lastTitle;

    @Getter
    private volatile boolean deleted;

    public SidebarHandle(ScoreboardLibraryProvider provider, Player player,
                         int maxLines, String objectiveName) {
        this.provider = provider;
        this.player = player;
        this.sidebar = provider.createSidebar(maxLines, objectiveName);
        this.lastLines = new String[provider.maxLines(sidebar)];
        this.visibleLines = 0;
        this.deleted = false;
        provider.show(player, sidebar);
    }

    public int maxLines() {
        return provider.maxLines(sidebar);
    }

    public void applyTitle(String raw) {
        if (deleted || raw.equals(lastTitle)) {
            return;
        }
        lastTitle = raw;
        provider.title(sidebar, ColorAPI.parse(raw));
    }

    public void applyLines(String[] raw, int count) {
        if (deleted) {
            return;
        }

        int size = Math.min(count, maxLines());
        for (int i = 0; i < size; i++) {
            String value = raw[i];
            if (i < visibleLines && value.equals(lastLines[i])) {
                continue;
            }
            lastLines[i] = value;
            provider.line(sidebar, i, ColorAPI.parse(value));
        }

        for (int i = size; i < visibleLines; i++) {
            lastLines[i] = null;
            provider.line(sidebar, i, null);
        }
        visibleLines = size;
    }

    public void invalidate() {
        lastTitle = null;
        Arrays.fill(lastLines, null);
    }

    public void reinitialize() {
        if (deleted) {
            return;
        }
        provider.hide(player, sidebar);
        provider.show(player, sidebar);
        invalidate();
    }

    public void delete() {
        if (deleted) {
            return;
        }
        deleted = true;
        provider.closeSidebar(player, sidebar);
        lastTitle = null;
        lastLines = null;
        visibleLines = 0;
    }
}
