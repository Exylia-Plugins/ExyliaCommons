package net.exylia.commons.v2.region.model;

import lombok.Getter;

@Getter
public enum RegionFlag {
    PVP("pvp", "Permite combate entre jugadores", true),
    BUILD("build", "Permite colocar bloques", true),
    BREAK("break", "Permite romper bloques", true),
    INTERACT("interact", "Permite interactuar con bloques y entidades", true),
    PLAYER_BUILD_ONLY("player_build_only", "Solo permite romper bloques colocados por jugadores", false),
    ALLOWED_BLOCKS_ONLY("allowed_blocks_only", "Solo permite colocar materiales específicos", false),
    BREAKABLE_BLOCKS_ONLY("breakable_blocks_only", "Solo permite romper materiales específicos", false),
    TEMPORARY_BLOCKS("temporary_blocks", "Los bloques colocados desaparecen después de X segundos", false),
    RE_GIVE_BLOCKS("re_give_blocks", "Devuelve bloques temporales al inventario al desaparecer", false),
    REGION_MEMBERS_ONLY("region_members_only", "Solo miembros pueden realizar acciones", false),
    ENTRY("entry", "Permite entrar a la región (incluye teleports)", true),
    EXIT("exit", "Permite salir de la región (incluye teleports)", true),
    ITEM_DROP("item_drop", "Permite dropear items", true),
    ITEM_PICKUP("item_pickup", "Permite recoger items", true),
    FALL_DAMAGE("fall_damage", "Permite daño por caída a jugadores", true);

    private final String key;
    private final String description;
    private final boolean defaultValue;

    RegionFlag(String key, String description, boolean defaultValue) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public boolean isProtectionFlag() {
        return this == PVP || this == BUILD || this == BREAK || this == INTERACT;
    }

    public boolean isMovementFlag() {
        return this == ENTRY || this == EXIT;
    }

    public boolean isItemFlag() {
        return this == ITEM_DROP || this == ITEM_PICKUP;
    }

    public boolean isSpecialFlag() {
        return this == PLAYER_BUILD_ONLY || this == ALLOWED_BLOCKS_ONLY ||
               this == BREAKABLE_BLOCKS_ONLY || this == TEMPORARY_BLOCKS ||
               this == RE_GIVE_BLOCKS || this == REGION_MEMBERS_ONLY;
    }
}
