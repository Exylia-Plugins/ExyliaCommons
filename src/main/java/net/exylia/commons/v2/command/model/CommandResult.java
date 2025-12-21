package net.exylia.commons.v2.command.model;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class CommandResult {
    private final boolean success;
    private final String message;
    private final Command command;
    private final Throwable error;

    @Builder.Default
    private final long executionTimeMillis = 0;

    @Builder.Default
    private final List<String> details = new ArrayList<>();

    public static CommandResult success(Command command) {
        return CommandResult.builder()
                .success(true)
                .command(command)
                .message("Command executed successfully")
                .build();
    }

    public static CommandResult success(Command command, String message) {
        return CommandResult.builder()
                .success(true)
                .command(command)
                .message(message)
                .build();
    }

    public static CommandResult failure(Command command, String message) {
        return CommandResult.builder()
                .success(false)
                .command(command)
                .message(message)
                .build();
    }

    public static CommandResult failure(Command command, Throwable error) {
        return CommandResult.builder()
                .success(false)
                .command(command)
                .message(error.getMessage())
                .error(error)
                .build();
    }
}
