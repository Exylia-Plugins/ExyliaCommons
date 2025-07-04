package net.exylia.commons.selection.model;

import lombok.Getter;

/**
 * Tipos de selección disponibles
 */
@Getter
public enum SelectionType {
    CUBOID("Cuboid", "Selección rectangular"),
    EXTEND("Extend", "Selección extendida"),
    CUSTOM("Custom", "Selección personalizada");

    private final String displayName;
    private final String description;

    SelectionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

}