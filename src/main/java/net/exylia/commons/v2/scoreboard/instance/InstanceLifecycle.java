package net.exylia.commons.v2.scoreboard.instance;

import lombok.Getter;

@Getter
public class InstanceLifecycle {

    public enum State {
        CREATED,
        ACTIVE,
        PAUSED,
        CANCELLED
    }

    private State state;

    public InstanceLifecycle() {
        this.state = State.CREATED;
    }

    public void activate() {
        if (state == State.CREATED || state == State.PAUSED) {
            state = State.ACTIVE;
        }
    }

    public void pause() {
        if (state == State.ACTIVE) {
            state = State.PAUSED;
        }
    }

    public void cancel() {
        state = State.CANCELLED;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public boolean isCancelled() {
        return state == State.CANCELLED;
    }

    public boolean canUpdate() {
        return state == State.ACTIVE;
    }
}
