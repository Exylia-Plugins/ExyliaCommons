package net.exylia.commons.v2.combat.exception;

public class CombatException extends RuntimeException {

    public CombatException(String message) {
        super(message);
    }

    public CombatException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class ProviderNotAvailableException extends CombatException {
        public ProviderNotAvailableException(String provider) {
            super("Combat provider not available: " + provider);
        }
    }

    public static class PlayerNotFoundException extends CombatException {
        public PlayerNotFoundException(String identifier) {
            super("Player not found: " + identifier);
        }
    }
}
