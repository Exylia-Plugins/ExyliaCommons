package net.exylia.commons.v2.scoreboard.core;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Un unico timer asincrono conduce todos los scoreboards.
 * <p>
 * El diseño anterior creaba una tarea <em>sincrona</em> por cada intervalo
 * distinto y ademas volvia a comprobar el tiempo en milisegundos dentro de cada
 * una, con lo que el intervalo se aplicaba dos veces. Aqui hay un solo tick a
 * 50ms que consulta {@link ScoreboardInstance#shouldUpdate(long)}: el ritmo real
 * lo decide cada instancia, y el desfase por jugador reparte la carga.
 * <p>
 * Corre fuera del hilo principal: resolver placeholders y encolar packets no
 * necesita el main thread, y scoreboard-library es explicitamente segura ahi.
 */
public class ScoreboardScheduler {

    private static final long TICK_PERIOD_TICKS = 1L;

    private final Map<String, ScoreboardInstance> instances = new ConcurrentHashMap<>();
    private final AtomicLong renderCount = new AtomicLong();

    private volatile ScheduledTask task;

    public void schedule(ScoreboardInstance instance) {
        if (instance == null) {
            return;
        }
        instances.put(instance.getId(), instance);
        ensureRunning();
    }

    public void unschedule(ScoreboardInstance instance) {
        if (instance == null) {
            return;
        }
        instances.remove(instance.getId());
    }

    public long getRenderCount() {
        return renderCount.get();
    }

    public void shutdown() {
        ScheduledTask current = task;
        task = null;
        if (current != null && !current.isCancelled()) {
            current.cancel();
        }
        instances.clear();
    }

    private void ensureRunning() {
        if (task != null) {
            return;
        }
        synchronized (this) {
            if (task != null) {
                return;
            }
            task = Tasks.asyncTimer(this::tick, TICK_PERIOD_TICKS, TICK_PERIOD_TICKS);
        }
    }

    private void tick() {
        if (instances.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<ScoreboardInstance> iterator = instances.values().iterator();

        while (iterator.hasNext()) {
            ScoreboardInstance instance = iterator.next();

            if (instance.getLifecycle().isCancelled() || !instance.getPlayer().isOnline()) {
                iterator.remove();
                continue;
            }

            if (!instance.shouldUpdate(now)) {
                continue;
            }

            try {
                instance.render();
                renderCount.incrementAndGet();
            } catch (Throwable t) {
                DebugAPI.logLibError(DebugCategory.SCOREBOARD,
                        "Fallo en el ciclo de scoreboard: " + t.getMessage(), t);
            }
        }
    }
}
