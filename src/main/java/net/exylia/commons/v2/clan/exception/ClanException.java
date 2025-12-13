package net.exylia.commons.v2.clan.exception;

public class ClanException extends RuntimeException {

    public ClanException(String message) {
        super(message);
    }

    public ClanException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class ProviderNotAvailableException extends ClanException {
        public ProviderNotAvailableException(String provider) {
            super("Clan provider not available: " + provider);
        }
    }

    public static class ClanNotFoundException extends ClanException {
        public ClanNotFoundException(String identifier) {
            super("Clan not found: " + identifier);
        }
    }
}
