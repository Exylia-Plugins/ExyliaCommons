package net.exylia.commons.region.model;

import lombok.Getter;

/**
 * Flags que pueden tener las regiones
 */
@Getter
public enum RegionFlag {
    // Flags de protección
    PVP("pvp", "Permite PvP", false),
    BUILD("build", "Permite construir", true),
    BREAK("break", "Permite romper bloques", true),
    INTERACT("interact", "Permite interactuar", true),

    // Flags de movimiento
    ENTRY("entry", "Permite entrada", true),
    EXIT("exit", "Permite salida", true),

    // Flags de items y entidades
    ITEM_DROP("item-drop", "Permite tirar items", true),
    ITEM_PICKUP("item-pickup", "Permite recoger items", true),

    // Flags especiales
    FLIGHT("flight", "Permite volar", false),

    // Flags de comunicación
    CHAT("chat", "Permite chatear", true),
    COMMANDS("commands", "Permite comandos", true);

    private final String key;
    private final String description;
    private final boolean defaultValue;

    RegionFlag(String key, String description, boolean defaultValue) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public static RegionFlag fromKey(String key) {
        for (RegionFlag flag : values()) {
            if (flag.key.equalsIgnoreCase(key)) {
                return flag;
            }
        }
        return null;
    }
}