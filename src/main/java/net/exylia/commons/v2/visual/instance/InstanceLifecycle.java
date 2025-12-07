package net.exylia.commons.v2.visual.instance;

import lombok.Getter;
import net.exylia.commons.async.SchedulerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Getter
public class InstanceLifecycle {
    private final String id;
    private LifecycleState state;
    private final long createdAt;
    private long startedAt;
    private long completedAt;
    private final List<Consumer<LifecycleEvent>> listeners;

    public enum LifecycleState {
        CREATED, ACTIVE, PAUSED, COMPLETED, CANCELLED, ERRORED
    }

    public enum LifecycleEvent {
        STARTED, PAUSED, RESUMED, COMPLETED, CANCELLED, ERRORED
    }

    public InstanceLifecycle(String id) {
        this.id = id;
        this.state = LifecycleState.CREATED;
        this.createdAt = System.currentTimeMillis();
        this.listeners = new ArrayList<>();
    }

    public void start() {
        if (state == LifecycleState.CREATED) {
            state = LifecycleState.ACTIVE;
            startedAt = System.currentTimeMillis();
            notifyListeners(LifecycleEvent.STARTED);
        }
    }

    public void pause() {
        if (state == LifecycleState.ACTIVE) {
            state = LifecycleState.PAUSED;
            notifyListeners(LifecycleEvent.PAUSED);
        }
    }

    public void resume() {
        if (state == LifecycleState.PAUSED) {
            state = LifecycleState.ACTIVE;
            notifyListeners(LifecycleEvent.RESUMED);
        }
    }

    public void complete() {
        if (state == LifecycleState.ACTIVE) {
            state = LifecycleState.COMPLETED;
            completedAt = System.currentTimeMillis();
            notifyListeners(LifecycleEvent.COMPLETED);
            scheduleCleanup();
        }
    }

    public void cancel() {
        if (state != LifecycleState.COMPLETED && state != LifecycleState.CANCELLED) {
            state = LifecycleState.CANCELLED;
            completedAt = System.currentTimeMillis();
            notifyListeners(LifecycleEvent.CANCELLED);
            scheduleCleanup();
        }
    }

    public void error() {
        state = LifecycleState.ERRORED;
        completedAt = System.currentTimeMillis();
        notifyListeners(LifecycleEvent.ERRORED);
        scheduleCleanup();
    }

    public void addListener(Consumer<LifecycleEvent> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<LifecycleEvent> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(LifecycleEvent event) {
        for (Consumer<LifecycleEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
            }
        }
    }

    private void scheduleCleanup() {
        SchedulerManager.getInstance()
                .task(listeners::clear)
                .delay(5, TimeUnit.SECONDS)
                .schedule();
    }

    public long getDuration() {
        if (startedAt == 0) return 0;
        long endTime = completedAt > 0 ? completedAt : System.currentTimeMillis();
        return endTime - startedAt;
    }

    public boolean isFinished() {
        return state == LifecycleState.COMPLETED ||
               state == LifecycleState.CANCELLED ||
               state == LifecycleState.ERRORED;
    }
}
