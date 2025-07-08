package net.exylia.commons.item;

/**
 * Tipos de filtro para regiones de WorldGuard
 */
public enum RegionFilterType {

    /**
     * Sin filtro de regiones - el item puede usarse en cualquier lugar
     */
    NONE,

    /**
     * Lista blanca - el item SOLO puede usarse en las regiones especificadas
     */
    WHITELIST,

    /**
     * Lista negra - el item puede usarse en TODAS las regiones EXCEPTO las especificadas
     */
    BLACKLIST;

    /**
     * Convierte un string a RegionFilterType de forma segura
     * @param value String a convertir
     * @return RegionFilterType correspondiente, o NONE si no es válido
     */
    public static RegionFilterType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NONE;
        }

        try {
            return valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    /**
     * Verifica si este tipo requiere una lista de regiones
     * @return true si requiere lista de regiones
     */
    public boolean requiresRegionList() {
        return this == WHITELIST || this == BLACKLIST;
    }

    /**
     * Obtiene la descripción del tipo de filtro
     * @return Descripción legible
     */
    public String getDescription() {
        return switch (this) {
            case NONE -> "Sin restricciones de región";
            case WHITELIST -> "Solo permitido en regiones específicas";
            case BLACKLIST -> "Prohibido en regiones específicas";
        };
    }
}