package net.exylia.commons.v2.scoreboard.core;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Acceso al broker de sidebars compartido entre plugins Exylia.
 *
 * Nunca cruza objetos Sidebar tipados entre classloaders. Los plugins invitados
 * conservan el sidebar como Object y todas sus operaciones se ejecutan dentro
 * del classloader propietario mediante metodos reflectivos del broker.
 */
public final class ScoreboardLibraryProvider {

    private final Object broker;
    private final boolean owner;
    private final boolean supported;

    private final Method createMethod;
    private final Method showMethod;
    private final Method hideMethod;
    private final Method closeSidebarMethod;
    private final Method titleMethod;
    private final Method lineMethod;
    private final Method closedMethod;
    private final Method maxLinesMethod;
    private final Method closeMethod;

    static ScoreboardLibraryProvider resolve(Plugin plugin) {
        Object existing = findBroker();
        if (existing != null) {
            DebugAPI.logLibDebug(DebugCategory.SCOREBOARD,
                    "Reutilizando el broker global de scoreboards");
            return new ScoreboardLibraryProvider(existing, false, true);
        }

        ScoreboardLibrary library;
        boolean supported;
        try {
            library = ScoreboardLibrary.loadScoreboardLibrary(plugin);
            supported = true;
        } catch (NoPacketAdapterAvailableException e) {
            library = new NoopScoreboardLibrary();
            supported = false;
            DebugAPI.logLibWarn(DebugCategory.SCOREBOARD,
                    "No hay packet adapter para esta version. Los scoreboards no seran visibles.");
        } catch (Throwable t) {
            library = new NoopScoreboardLibrary();
            supported = false;
            DebugAPI.logLibError(DebugCategory.SCOREBOARD,
                    "Fallo al inicializar scoreboard-library: " + t.getMessage(), t);
        }

        SharedSidebarBroker created = new SharedSidebarBroker(library);
        Bukkit.getServicesManager().register(Object.class, created, plugin, ServicePriority.Highest);
        return new ScoreboardLibraryProvider(created, true, supported);
    }

    private ScoreboardLibraryProvider(Object broker, boolean owner, boolean supported) {
        this.broker = broker;
        this.owner = owner;
        this.supported = supported;
        try {
            Class<?> type = broker.getClass();
            this.createMethod = type.getMethod("create", int.class, String.class);
            this.showMethod = type.getMethod("show", Player.class, Object.class);
            this.hideMethod = type.getMethod("hide", Player.class, Object.class);
            this.closeSidebarMethod = type.getMethod("closeSidebar", Player.class, Object.class);
            this.titleMethod = type.getMethod("title", Object.class, Component.class);
            this.lineMethod = type.getMethod("line", Object.class, int.class, Component.class);
            this.closedMethod = type.getMethod("closed", Object.class);
            this.maxLinesMethod = type.getMethod("maxLines", Object.class);
            this.closeMethod = type.getMethod("close");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Broker de scoreboards incompatible", e);
        }
    }

    private static Object findBroker() {
        for (RegisteredServiceProvider<?> registration :
                Bukkit.getServicesManager().getRegistrations(Object.class)) {
            Object candidate = registration.getProvider();
            if (candidate != null && candidate.getClass().getName().endsWith(".SharedSidebarBroker")) {
                return candidate;
            }
        }
        return null;
    }

    public boolean isSupported() {
        return supported;
    }

    public Object createSidebar(int maxLines, String objectiveName) {
        return invoke(createMethod, maxLines, objectiveName);
    }

    public void show(Player player, Object sidebar) {
        invoke(showMethod, player, sidebar);
    }

    public void hide(Player player, Object sidebar) {
        invoke(hideMethod, player, sidebar);
    }

    public void closeSidebar(Player player, Object sidebar) {
        invoke(closeSidebarMethod, player, sidebar);
    }

    public void title(Object sidebar, Component title) {
        invoke(titleMethod, sidebar, title);
    }

    public void line(Object sidebar, int index, Component line) {
        invoke(lineMethod, sidebar, index, line);
    }

    public boolean closed(Object sidebar) {
        return (boolean) invoke(closedMethod, sidebar);
    }

    public int maxLines(Object sidebar) {
        return (int) invoke(maxLinesMethod, sidebar);
    }

    public void close() {
        if (owner) {
            invoke(closeMethod);
        }
    }

    private Object invoke(Method method, Object... arguments) {
        try {
            return method.invoke(broker, arguments);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("No se puede acceder al broker de scoreboards", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Fallo en el broker de scoreboards", cause);
        }
    }
}
