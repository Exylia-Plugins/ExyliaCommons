package net.exylia.commons.item.config;

/**
 * Tipos de verificador para regiones de WorldGuard
 * Define cómo se evalúan las regiones cuando un jugador está en múltiples regiones
 */
public enum RegionCheckerType {

    /**
     * CONTAINS - Verifica si cualquiera de las regiones del jugador está en la lista
     * Comportamiento tradicional: si está en al menos una región de la lista
     */
    CONTAINS,

    /**
     * PRIORITY - Solo verifica la región de mayor prioridad del jugador
     * Útil para regiones superpuestas donde solo importa la región principal
     */
    PRIORITY;

    /**
     * Convierte un string a RegionCheckerType de forma segura
     * @param value String a convertir
     * @return RegionCheckerType correspondiente, o CONTAINS si no es válido
     */
    public static RegionCheckerType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return CONTAINS; // Comportamiento por defecto
        }

        try {
            return valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return CONTAINS;
        }
    }

    /**
     * Obtiene la descripción del tipo de verificador
     * @return Descripción legible
     */
    public String getDescription() {
        return switch (this) {
            case CONTAINS -> "Verifica todas las regiones del jugador";
            case PRIORITY -> "Solo verifica la región de mayor prioridad";
        };
    }
}