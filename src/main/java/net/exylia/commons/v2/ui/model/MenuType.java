package net.exylia.commons.v2.ui.model;

public enum MenuType {
    SIMPLE,
    PAGINATION,
    MULTI_PAGINATION,
    FULL_INVENTORY,
    PAGINATION_FULL,
    MULTI_PAGINATION_FULL;

    public boolean isPaginationMenu() {
        return this == PAGINATION || this == MULTI_PAGINATION ||
               this == PAGINATION_FULL || this == MULTI_PAGINATION_FULL;
    }

    public boolean isMultiSectionMenu() {
        return this == MULTI_PAGINATION || this == MULTI_PAGINATION_FULL;
    }

    public boolean isFullInventoryMenu() {
        return this == FULL_INVENTORY || this == PAGINATION_FULL || this == MULTI_PAGINATION_FULL;
    }

    public static MenuType fromString(String type) {
        if (type == null) {
            return SIMPLE;
        }

        try {
            return MenuType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SIMPLE;
        }
    }
}
