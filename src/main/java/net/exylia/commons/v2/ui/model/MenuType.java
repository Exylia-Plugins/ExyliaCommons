package net.exylia.commons.v2.ui.model;

import lombok.Getter;

@Getter
public enum MenuType {
    SIMPLE("simple"),
    PAGINATION("pagination"),
    EDITABLE("editable"),
    FULL_INVENTORY("full_inventory"),
    PAGINATED_FULL_INVENTORY("paginated_full_inventory"),
    MULTI_PAGINATION("multi_pagination"),
    MULTI_PAGINATED_FULL_INVENTORY("multi_paginated_full_inventory"),
    CONFIRMATION("confirmation"),
    SELECTION("selection");

    private final String configName;

    MenuType(String configName) {
        this.configName = configName;
    }

    public static MenuType fromString(String name) {
        for (MenuType type : values()) {
            if (type.configName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return SIMPLE;
    }
}
