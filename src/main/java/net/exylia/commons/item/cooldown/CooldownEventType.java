package net.exylia.commons.item.cooldown;

/**
 * Tipos de eventos de cooldown
 */
public enum CooldownEventType {

    /**
     * Se estableció un nuevo cooldown
     */
    SET,

    /**
     * Se removió manualmente un cooldown
     */
    REMOVE,

    /**
     * Un cooldown expiró naturalmente
     */
    EXPIRE,

    /**
     * Se limpiaron todos los cooldowns de un jugador
     */
    CLEAR_ALL
}