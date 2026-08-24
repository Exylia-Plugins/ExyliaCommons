package net.exylia.commons.v2.scoreboard.core;

import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Broker global de sidebars entre todos los plugins Exylia.
 *
 * Sus metodos publicos usan solo tipos compartidos por el servidor (Object,
 * Player, Component y primitivos), por lo que se pueden invocar por reflexion
 * desde plugins que relocalizan scoreboard-library en classloaders distintos.
 *
 * Mantiene una pila por jugador: el ultimo scoreboard mostrado reemplaza al
 * anterior; al ocultarlo, el anterior se restaura. Esto reproduce el
 * comportamiento esperado entre PracticeCore, Events y Capture.
 */
public final class SharedSidebarBroker {

    private final ScoreboardLibrary library;
    private final Map<UUID, Deque<Sidebar>> stacks = new ConcurrentHashMap<>();

    SharedSidebarBroker(ScoreboardLibrary library) {
        this.library = library;
    }

    public Object create(int maxLines, String objectiveName) {
        return library.createSidebar(Math.max(1, Math.min(maxLines, 15)), null, objectiveName);
    }

    public synchronized void show(Player player, Object rawSidebar) {
        Sidebar sidebar = requireSidebar(rawSidebar);
        Deque<Sidebar> stack = stacks.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        Sidebar current = stack.peekFirst();

        if (current == sidebar) {
            return;
        }
        if (current != null && !current.closed()) {
            current.removePlayer(player);
        }

        stack.remove(sidebar);
        stack.addFirst(sidebar);
        if (!sidebar.closed()) {
            sidebar.addPlayer(player);
        }
    }

    public synchronized void hide(Player player, Object rawSidebar) {
        Sidebar sidebar = requireSidebar(rawSidebar);
        Deque<Sidebar> stack = stacks.get(player.getUniqueId());
        if (stack == null) {
            return;
        }

        boolean wasCurrent = stack.peekFirst() == sidebar;
        if (wasCurrent && !sidebar.closed()) {
            sidebar.removePlayer(player);
        }
        stack.remove(sidebar);

        if (stack.isEmpty()) {
            stacks.remove(player.getUniqueId());
            return;
        }

        if (wasCurrent) {
            Sidebar next = stack.peekFirst();
            if (next != null && !next.closed()) {
                next.addPlayer(player);
            }
        }
    }

    public synchronized void closeSidebar(Player player, Object rawSidebar) {
        Sidebar sidebar = requireSidebar(rawSidebar);
        hide(player, sidebar);
        if (!sidebar.closed()) {
            sidebar.close();
        }
    }

    public void title(Object rawSidebar, Component title) {
        requireSidebar(rawSidebar).title(title);
    }

    public void line(Object rawSidebar, int index, Component line) {
        requireSidebar(rawSidebar).line(index, line);
    }

    public boolean closed(Object rawSidebar) {
        return requireSidebar(rawSidebar).closed();
    }

    public int maxLines(Object rawSidebar) {
        return requireSidebar(rawSidebar).maxLines();
    }

    public synchronized void close() {
        for (Deque<Sidebar> stack : stacks.values()) {
            for (Sidebar sidebar : stack) {
                if (!sidebar.closed()) {
                    sidebar.close();
                }
            }
        }
        stacks.clear();
        if (!library.closed()) {
            library.close();
        }
    }

    private Sidebar requireSidebar(Object value) {
        if (!(value instanceof Sidebar sidebar)) {
            throw new IllegalArgumentException("Sidebar does not belong to the shared broker");
        }
        return sidebar;
    }
}
