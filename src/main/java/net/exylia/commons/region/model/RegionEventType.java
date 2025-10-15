package net.exylia.commons.region.model;

import lombok.Getter;

@Getter
public enum RegionEventType {
    ENTER("enter", "Entrada a región"),
    EXIT("exit", "Salida de región"),
    MOVE("move", "Movimiento dentro de región"),
    TELEPORT_IN("teleport-in", "Teletransporte hacia región"),
    TELEPORT_OUT("teleport-out", "Teletransporte desde región");

    private final String key;
    private final String description;

    RegionEventType(String key, String description) {
        this.key = key;
        this.description = description;
    }
}
