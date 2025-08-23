package net.exylia.commons.item.config;

/**
 * Tipos de filtros para mundos en items
 */
public enum WorldFilterType {
    /**
     * Sin filtro de mundo - permite en todos los mundos
     */
    NONE,

    /**
     * Lista blanca - solo permite en los mundos especificados
     */
    WHITELIST,

    /**
     * Lista negra - permite en todos los mundos excepto los especificados
     */
    BLACKLIST;

    /**
     * Convierte un string a WorldFilterType
     * @param typeString String a convertir
     * @return WorldFilterType correspondiente
     */
    public static WorldFilterType fromString(String typeString) {
        if (typeString == null || typeString.trim().isEmpty()) {
            return NONE;
        }

        String normalized = typeString.trim().toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            switch (normalized) {
                case "WHITE":
                case "ALLOW":
                case "ALLOWED":
                    return WHITELIST;
                case "BLACK":
                case "DENY":
                case "DENIED":
                case "BLOCK":
                case "BLOCKED":
                    return BLACKLIST;
                case "OFF":
                case "DISABLED":
                case "DISABLE":
                    return NONE;
                default:
                    return NONE;
            }
        }
    }
}