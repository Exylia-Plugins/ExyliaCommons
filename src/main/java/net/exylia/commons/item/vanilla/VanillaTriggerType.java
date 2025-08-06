package net.exylia.commons.item.vanilla;

/**
 * Tipos de trigger para items vanilla
 */
public enum VanillaTriggerType {

    /**
     * El cooldown se aplica inmediatamente al hacer clic/interactuar
     * Útil para: escudos, pociones splash, cubos, etc.
     */
    INTERACT,

    /**
     * El cooldown se aplica después de consumir el item
     * Útil para: comida, pociones bebibles, leche, etc.
     */
    AFTER_CONSUME,

    /**
     * El cooldown se aplica después de lanzar un proyectil
     * Útil para: ender pearls, huevos, bolas de nieve, arcos, ballestas, etc.
     */
    AFTER_PROJECTILE,

    /**
     * Detecta automáticamente el tipo de trigger basado en el material
     * El sistema determinará automáticamente cuándo aplicar el cooldown
     */
    AUTO_DETECT;

    /**
     * Obtiene el tipo de trigger desde un string
     */
    public static VanillaTriggerType fromString(String triggerString) {
        if (triggerString == null || triggerString.trim().isEmpty()) {
            return AUTO_DETECT;
        }

        String normalized = triggerString.toUpperCase().replace("-", "_");

        try {
            return VanillaTriggerType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Intentar matchear con nombres alternativos
            return switch (normalized) {
                case "IMMEDIATE", "CLICK", "USE" -> INTERACT;
                case "CONSUME", "EAT", "DRINK" -> AFTER_CONSUME;
                case "PROJECTILE", "LAUNCH", "THROW", "SHOOT" -> AFTER_PROJECTILE;
                default -> AUTO_DETECT;
            };
        }
    }

    /**
     * Verifica si este trigger requiere que se cancele el evento de interact
     * cuando el item está en cooldown
     */
    public boolean shouldCancelInteract() {
        return this != AUTO_DETECT; // AUTO_DETECT se maneja caso por caso
    }

    @Override
    public String toString() {
        return name().toLowerCase().replace("_", "-");
    }
}