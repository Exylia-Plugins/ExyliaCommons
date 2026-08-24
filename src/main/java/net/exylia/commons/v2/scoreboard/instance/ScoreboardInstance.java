package net.exylia.commons.v2.scoreboard.instance;

import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.renderer.RenderBuffer;
import net.exylia.commons.v2.scoreboard.renderer.ScoreboardRenderer;
import net.exylia.commons.v2.scoreboard.renderer.SidebarHandle;
import net.exylia.commons.v2.tasks.api.Tasks;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class ScoreboardInstance {

    private final String id;
    private final Player player;
    private final Scoreboard scoreboard;
    private final SidebarHandle sidebar;
    private final InstanceLifecycle lifecycle;
    private final ScoreboardRenderer renderer;
    private final long createdAt;
    private final long intervalMs;

    /**
     * Desfase fijo derivado del UUID del jugador. Reparte los updates a lo largo
     * del intervalo en lugar de concentrarlos todos en el mismo tick, que era lo
     * que producia el pico periodico de CPU.
     */
    private final long stagger;

    /**
     * Buffer reutilizable: evita asignar listas/arrays en cada render.
     * Solo lo toca el hilo que consiguio el permiso de {@link #rendering}.
     */
    private final RenderBuffer buffer;

    /**
     * Impide que se solapen dos renders de la misma instancia. Es la garantia de
     * exclusion mutua que necesita {@link SidebarHandle}, que no es thread-safe.
     * Si un render se retrasa (PAPI lento) el siguiente ciclo simplemente se
     * salta en vez de encolarse y amplificar la carga.
     */
    private final AtomicBoolean rendering = new AtomicBoolean(false);

    private volatile PlaceholderContext context;
    private volatile long lastUpdate;

    public ScoreboardInstance(
            String id,
            Player player,
            Scoreboard scoreboard,
            SidebarHandle sidebar,
            ScoreboardRenderer renderer,
            PlaceholderContext context
    ) {
        this.id = id;
        this.player = player;
        this.scoreboard = scoreboard;
        this.sidebar = sidebar;
        this.renderer = renderer;
        this.context = context != null ? context : PlaceholderContext.create();
        this.lifecycle = new InstanceLifecycle();
        this.createdAt = System.currentTimeMillis();
        this.lastUpdate = 0;
        this.intervalMs = Math.max(50L, scoreboard.getUpdateInterval() * 50L);
        this.buffer = new RenderBuffer(Math.max(1, renderer.lineCount()));

        long hash = player.getUniqueId().getLeastSignificantBits();
        this.stagger = Math.floorMod(hash, this.intervalMs);
    }

    /**
     * Primer render. Se despacha a un hilo asincrono en vez de ejecutarse aqui:
     * {@code show} lo suele llamar el hilo principal (al entrar el jugador, al
     * cambiar de estado) y resolver PAPI ahi es precisamente lo que se busca
     * evitar.
     */
    public void show() {
        lifecycle.activate();
        Tasks.run(this::render);
    }

    public void hide() {
        lifecycle.cancel();
        sidebar.delete();
    }

    /**
     * Resuelve y aplica el scoreboard. Se ejecuta ya en un hilo asincrono: no
     * hace ningun salto de scheduler ni toca el hilo principal.
     */
    public void render() {
        if (!lifecycle.canUpdate() || !player.isOnline() || sidebar.isDeleted()) {
            return;
        }

        // Un render previo sigue en curso: saltamos este ciclo.
        if (!rendering.compareAndSet(false, true)) {
            return;
        }

        try {
            lastUpdate = System.currentTimeMillis();
            renderer.render(player, context, buffer);

            if (sidebar.isDeleted()) {
                return;
            }
            sidebar.applyTitle(buffer.getTitle());
            sidebar.applyLines(buffer.lines(), buffer.getLineCount());
        } catch (Throwable t) {
            DebugAPI.logLibError(DebugCategory.SCOREBOARD,
                    "Fallo al renderizar el scoreboard de " + player.getName() + ": " + t.getMessage(), t);
        } finally {
            rendering.set(false);
        }
    }

    /**
     * Rejilla temporal global desplazada por {@link #stagger}: la instancia se
     * renderiza cuando el reloj cruza a una ranura nueva de <em>su</em> rejilla.
     * <p>
     * Compararlo asi en lugar de {@code now - lastUpdate >= intervalMs} hace que
     * el reparto entre jugadores sea estable aunque todos arranquen en el mismo
     * tick (p.ej. un reload que recrea todos los scoreboards a la vez), y a la
     * vez mantiene el periodo exacto. Con {@code lastUpdate == 0}
     * ({@link #forceUpdate}) el resultado es siempre true, o sea render en el
     * tick inmediato, sin penalizacion de desfase.
     */
    public boolean shouldUpdate(long now) {
        if (!lifecycle.canUpdate() || !player.isOnline()) {
            return false;
        }
        return Math.floorDiv(now - stagger, intervalMs)
                > Math.floorDiv(lastUpdate - stagger, intervalMs);
    }

    public boolean shouldUpdate() {
        return shouldUpdate(System.currentTimeMillis());
    }

    public void updateContext(PlaceholderContext newContext) {
        this.context = newContext != null ? newContext : PlaceholderContext.create();
    }

    /** Marca la instancia para que se renderice en el proximo tick del scheduler. */
    public void forceUpdate() {
        this.lastUpdate = 0;
    }

    /**
     * Descarta el diff y programa un reenvio completo. Lo usa el sistema de
     * reload cuando cambian presets de color o plantillas y el contenido crudo
     * podria ser identico pero su render no.
     */
    public void invalidateAndRefresh() {
        if (rendering.compareAndSet(false, true)) {
            try {
                sidebar.invalidate();
            } finally {
                rendering.set(false);
            }
        }
        forceUpdate();
    }

    /**
     * Reconstruye el sidebar en el cliente. Necesario cuando otro plugin (TAB) o
     * el propio servidor (cambio de mundo, respawn) pisa el objetivo del jugador.
     */
    public void reinitialize() {
        if (!lifecycle.isActive() || sidebar.isDeleted()) {
            return;
        }
        // Se llama desde listeners de Bukkit y desde el hook de TAB, ambos en el
        // hilo principal: el re-render se despacha fuera.
        Tasks.run(() -> {
            // Se toma el mismo permiso que render() porque reinitialize() muta
            // el estado de diff de SidebarHandle, que no es thread-safe.
            if (!rendering.compareAndSet(false, true)) {
                // Hay un render en curso; basta con pedir uno nuevo despues.
                forceUpdate();
                return;
            }
            try {
                if (sidebar.isDeleted()) {
                    return;
                }
                sidebar.reinitialize();
            } catch (Throwable t) {
                DebugAPI.logLibError(DebugCategory.SCOREBOARD,
                        "Fallo al reinicializar el scoreboard de " + player.getName() + ": " + t.getMessage(), t);
            } finally {
                rendering.set(false);
            }
            forceUpdate();
            render();
        });
    }

    public boolean isActive() {
        return lifecycle.isActive() && player.isOnline();
    }

    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }
}
