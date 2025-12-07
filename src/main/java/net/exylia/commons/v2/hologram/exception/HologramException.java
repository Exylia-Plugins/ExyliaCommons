package net.exylia.commons.v2.hologram.exception;

public class HologramException extends RuntimeException {
    public HologramException(String message) {
        super(message);
    }

    public HologramException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class HologramNotFoundException extends HologramException {
        public HologramNotFoundException(String id) {
            super("Hologram not found: " + id);
        }
    }

    public static class HologramAlreadyExistsException extends HologramException {
        public HologramAlreadyExistsException(String id) {
            super("Hologram already exists: " + id);
        }
    }

    public static class HologramSpawnException extends HologramException {
        public HologramSpawnException(String message) {
            super("Failed to spawn hologram: " + message);
        }

        public HologramSpawnException(String message, Throwable cause) {
            super("Failed to spawn hologram: " + message, cause);
        }
    }

    public static class HologramPersistenceException extends HologramException {
        public HologramPersistenceException(String message) {
            super("Persistence error: " + message);
        }

        public HologramPersistenceException(String message, Throwable cause) {
            super("Persistence error: " + message, cause);
        }
    }
}
