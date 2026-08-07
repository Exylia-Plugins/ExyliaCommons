package net.exylia.commons.v2.scoreboard.exception;

public class ScoreboardLimitException extends ScoreboardException {

    public ScoreboardLimitException(String message) {
        super(message);
    }

    public ScoreboardLimitException(String limit, int current, int max) {
        super(String.format("Scoreboard limit exceeded for '%s': %d/%d", limit, current, max));
    }
}
