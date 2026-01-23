package net.exylia.commons.v2.tasks.model;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Getter
public class TaskResult<T> {
    private final String taskId;
    private final TaskState state;
    private final T value;
    private final Throwable error;
    private final Instant startTime;
    private final Instant endTime;
    private final TaskCategory category;

    private TaskResult(String taskId, TaskState state, T value, Throwable error,
                       Instant startTime, Instant endTime, TaskCategory category) {
        this.taskId = taskId;
        this.state = state;
        this.value = value;
        this.error = error;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
    }

    public static <T> TaskResult<T> success(String taskId, T value, Instant startTime, TaskCategory category) {
        return new TaskResult<>(taskId, TaskState.COMPLETED, value, null, startTime, Instant.now(), category);
    }

    public static <T> TaskResult<T> failure(String taskId, Throwable error, Instant startTime, TaskCategory category) {
        return new TaskResult<>(taskId, TaskState.FAILED, null, error, startTime, Instant.now(), category);
    }

    public static <T> TaskResult<T> timeout(String taskId, Instant startTime, TaskCategory category) {
        return new TaskResult<>(taskId, TaskState.TIMEOUT, null, null, startTime, Instant.now(), category);
    }

    public static <T> TaskResult<T> cancelled(String taskId, Instant startTime, TaskCategory category) {
        return new TaskResult<>(taskId, TaskState.CANCELLED, null, null, startTime, Instant.now(), category);
    }

    public boolean isSuccess() {
        return state == TaskState.COMPLETED;
    }

    public boolean isFailure() {
        return state == TaskState.FAILED;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

    public Duration getDuration() {
        if (startTime == null || endTime == null) return Duration.ZERO;
        return Duration.between(startTime, endTime);
    }

    public long getDurationMillis() {
        return getDuration().toMillis();
    }
}
