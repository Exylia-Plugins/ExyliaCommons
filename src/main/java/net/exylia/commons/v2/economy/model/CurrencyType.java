package net.exylia.commons.v2.economy.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CurrencyType {
    VAULT("Vault", "money"),
    PLAYER_POINTS("PlayerPoints", "points");

    private final String displayName;
    private final String identifier;

    public static CurrencyType fromName(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
