package net.exylia.commons.item.config;

public enum RegionCheckerType {

    CONTAINS,

    PRIORITY;

    public static RegionCheckerType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return CONTAINS;  
        }

        try {
            return valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return CONTAINS;
        }
    }

    public String getDescription() {
        return switch (this) {
            case CONTAINS -> "Verifica todas las regiones del jugador";
            case PRIORITY -> "Solo verifica la región de mayor prioridad";
        };
    }
}
