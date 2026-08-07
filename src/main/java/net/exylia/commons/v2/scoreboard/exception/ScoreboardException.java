package net.exylia.commons.v2.scoreboard.exception;

public class ScoreboardException extends RuntimeException {

    public ScoreboardException(String message) {
        super(message);
    }

    public ScoreboardException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScoreboardException(Throwable cause) {
        super(cause);
    }
}
