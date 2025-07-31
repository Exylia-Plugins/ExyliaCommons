package net.exylia.commons.region.model;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Flags que pueden tener las regiones con soporte completo para validación y aplicación
 */
@Getter
public enum RegionFlag {
    // === FLAGS DE PROTECCIÓN ===
    PVP("pvp", "Permite PvP entre jugadores", true, FlagType.PROTECTION),
    BUILD("build", "Permite construir y colocar bloques", true, FlagType.PROTECTION),
    BREAK("break", "Permite romper bloques", true, FlagType.PROTECTION),
    INTERACT("interact", "Permite interactuar con bloques y entidades", true, FlagType.PROTECTION),
    PLAYER_BUILD_ONLY("player-build-only", "Solo permite romper/explotar bloques colocados por jugadores", false, FlagType.PROTECTION),
    TRACK_PLAYER_BLOCKS("track-player-blocks", "Rastrea bloques colocados por jugadores en esta región", false, FlagType.PROTECTION),
    ALLOWED_BLOCKS_ONLY("allowed-blocks-only", "Solo permite colocar bloques específicos de una lista", false, FlagType.PROTECTION),
    TEMPORARY_BLOCKS("temporary-blocks", "Los bloques colocados por jugadores desaparecen automáticamente", false, FlagType.SPECIAL),
    RE_GIVE_BLOCKS("re-give-blocks", "Devuelve los bloques temporales al inventario del jugador cuando desaparecen", false, FlagType.SPECIAL),

    // Nueva flag para restricción regional
    REGION_MEMBERS_ONLY("region-members-only", "Solo jugadores dentro de la región pueden realizar acciones que afecten la región", false, FlagType.PROTECTION),

    // Protección específica de bloques
    CHEST_ACCESS("chest-access", "Permite acceder a cofres y contenedores", true, FlagType.PROTECTION),
    USE_DOORS("use-doors", "Permite usar puertas y compuertas", true, FlagType.PROTECTION),
    USE_BUTTONS("use-buttons", "Permite usar botones y palancas", true, FlagType.PROTECTION),

    // === FLAGS DE MOVIMIENTO ===
    ENTRY("entry", "Permite entrada a la región", true, FlagType.MOVEMENT),
    EXIT("exit", "Permite salida de la región", true, FlagType.MOVEMENT),
    TELEPORT_IN("teleport-in", "Permite teletransportarse hacia la región", true, FlagType.MOVEMENT),
    TELEPORT_OUT("teleport-out", "Permite teletransportarse desde la región", true, FlagType.MOVEMENT),

    // === FLAGS DE ITEMS Y ENTIDADES ===
    ITEM_DROP("item-drop", "Permite tirar items al suelo", true, FlagType.ITEM),
    ITEM_PICKUP("item-pickup", "Permite recoger items del suelo", true, FlagType.ITEM),
    ITEM_FRAME("item-frame", "Permite usar marcos de items", true, FlagType.ITEM),

    // Entidades
    MOB_SPAWNING("mob-spawning", "Permite aparición natural de mobs", true, FlagType.ENTITY),
    MOB_DAMAGE("mob-damage", "Permite que mobs hagan daño", true, FlagType.ENTITY),
    ANIMAL_SPAWNING("animal-spawning", "Permite aparición de animales", true, FlagType.ENTITY),

    // === FLAGS ESPECIALES ===
    FLIGHT("flight", "Permite volar", false, FlagType.SPECIAL),
    INVINCIBLE("invincible", "Hace a los jugadores invencibles", false, FlagType.SPECIAL),
    HEAL("heal", "Regenera vida automáticamente", false, FlagType.SPECIAL),
    FEED("feed", "Mantiene la comida llena", false, FlagType.SPECIAL),

    // === FLAGS DE TIEMPO Y CLIMA ===
    TIME_LOCK("time-lock", "Bloquea el tiempo en la región", false, FlagType.ENVIRONMENT),
    WEATHER_LOCK("weather-lock", "Bloquea el clima en la región", false, FlagType.ENVIRONMENT),

    // === FLAGS DE GAME MODE ===
    GAMEMODE_CHANGE("gamemode-change", "Permite cambiar modo de juego", true, FlagType.GAMEMODE),
    FORCE_ADVENTURE("force-adventure", "Fuerza modo aventura", false, FlagType.GAMEMODE),
    FORCE_SURVIVAL("force-survival", "Fuerza modo supervivencia", false, FlagType.GAMEMODE),
    FORCE_CREATIVE("force-creative", "Fuerza modo creativo", false, FlagType.GAMEMODE),

    // === FLAGS DE EXPLOSIONES ===
    TNT("tnt", "Permite explosiones de TNT", true, FlagType.EXPLOSION),
    CREEPER_EXPLOSION("creeper-explosion", "Permite explosiones de creepers", true, FlagType.EXPLOSION),
    OTHER_EXPLOSION("other-explosion", "Permite otras explosiones", true, FlagType.EXPLOSION),

    // === FLAGS DE FUEGO ===
    FIRE_SPREAD("fire-spread", "Permite propagación del fuego", true, FlagType.FIRE),
    FIRE_DAMAGE("fire-damage", "Permite daño por fuego", true, FlagType.FIRE),
    LAVA_FIRE("lava-fire", "Permite que la lava cause fuego", true, FlagType.FIRE),

    // === FLAGS DE LÍQUIDOS ===
    WATER_FLOW("water-flow", "Permite flujo de agua", true, FlagType.LIQUID),
    LAVA_FLOW("lava-flow", "Permite flujo de lava", true, FlagType.LIQUID),

    // === FLAGS ECONÓMICAS ===
    SHOP_CREATE("shop-create", "Permite crear tiendas", true, FlagType.ECONOMY),
    SHOP_USE("shop-use", "Permite usar tiendas", true, FlagType.ECONOMY);

    private final String key;
    private final String description;
    private final boolean defaultValue;
    private final FlagType type;

    RegionFlag(String key, String description, boolean defaultValue, FlagType type) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.type = type;
    }

    /**
     * Busca una flag por su clave
     */
    public static RegionFlag fromKey(String key) {
        for (RegionFlag flag : values()) {
            if (flag.key.equalsIgnoreCase(key)) {
                return flag;
            }
        }
        return null;
    }

    /**
     * Obtiene todas las flags de un tipo específico
     */
    public static List<RegionFlag> getFlagsByType(FlagType type) {
        return Arrays.stream(values())
                .filter(flag -> flag.type == type)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todas las flags de protección
     */
    public static Set<RegionFlag> getProtectionFlags() {
        return Arrays.stream(values())
                .filter(flag -> flag.type == FlagType.PROTECTION)
                .collect(Collectors.toSet());
    }

    /**
     * Verifica si esta flag afecta la construcción
     */
    public boolean affectsBuilding() {
        return this == BUILD || this == BREAK || this == INTERACT ||
                this == CHEST_ACCESS || this == USE_DOORS || this == USE_BUTTONS ||
                this == PLAYER_BUILD_ONLY || this == TRACK_PLAYER_BLOCKS ||
                this == REGION_MEMBERS_ONLY || this == ALLOWED_BLOCKS_ONLY ||
                this == TEMPORARY_BLOCKS || this == RE_GIVE_BLOCKS;
    }

    /**
     * Verifica si esta flag afecta el movimiento
     */
    public boolean affectsMovement() {
        return type == FlagType.MOVEMENT;
    }

    /**
     * Verifica si esta flag afecta el combate
     */
    public boolean affectsCombat() {
        return this == PVP || this == MOB_DAMAGE || this == INVINCIBLE || this == REGION_MEMBERS_ONLY;
    }

    /**
     * Verifica si esta flag requiere que el jugador esté dentro de la región
     */
    public boolean requiresPlayerInRegion() {
        return this == REGION_MEMBERS_ONLY;
    }

    /**
     * Verifica si esta flag es afectada por REGION_MEMBERS_ONLY
     */
    public boolean isAffectedByRegionMembersOnly() {
        return this == BUILD || this == BREAK || this == INTERACT ||
                this == PVP || this == CHEST_ACCESS || this == USE_DOORS ||
                this == USE_BUTTONS || this == ITEM_FRAME;
    }

    /**
     * Verifica si esta flag requiere permisos especiales para ser modificada
     */
    public boolean requiresSpecialPermission() {
        return type == FlagType.SPECIAL || type == FlagType.GAMEMODE ||
                this == INVINCIBLE || this == FORCE_ADVENTURE ||
                this == FORCE_SURVIVAL || this == FORCE_CREATIVE ||
                this == PLAYER_BUILD_ONLY || this == TRACK_PLAYER_BLOCKS ||
                this == REGION_MEMBERS_ONLY || this == ALLOWED_BLOCKS_ONLY ||
                this == TEMPORARY_BLOCKS || this == RE_GIVE_BLOCKS;
    }

    /**
     * Obtiene el permiso necesario para modificar esta flag
     */
    public String getRequiredPermission(String regionId) {
        String base = "exylia.region.flag.";

        if (requiresSpecialPermission()) {
            base += "special.";
        }

        return base + key.replace("-", "_") + "." + regionId;
    }

    /**
     * Verifica si dos flags son incompatibles
     */
    public boolean isIncompatibleWith(RegionFlag other) {
        // Game modes son mutuamente exclusivos
        if (this.type == FlagType.GAMEMODE && other.type == FlagType.GAMEMODE) {
            return this != other &&
                    (this == FORCE_ADVENTURE || this == FORCE_SURVIVAL || this == FORCE_CREATIVE) &&
                    (other == FORCE_ADVENTURE || other == FORCE_SURVIVAL || other == FORCE_CREATIVE);
        }

        if (this == RE_GIVE_BLOCKS && other == TEMPORARY_BLOCKS) {
            return false;
        }

        // Otras incompatibilidades específicas
        if (this == INVINCIBLE && other == PVP) return true;
        return this == PVP && other == INVINCIBLE;
    }

    public boolean requiresFlag(RegionFlag other) {
        // RE_GIVE_BLOCKS requiere TEMPORARY_BLOCKS para funcionar
        if (this == RE_GIVE_BLOCKS && other == TEMPORARY_BLOCKS) {
            return true;
        }

        return false;
    }

    public boolean isTemporaryBlockRelated() {
        return this == TEMPORARY_BLOCKS || this == RE_GIVE_BLOCKS;
    }

    @Getter
    public enum FlagType {
        PROTECTION("Protección", "Flags que controlan la protección de bloques y entidades"),
        MOVEMENT("Movimiento", "Flags que controlan el movimiento de jugadores"),
        ITEM("Items", "Flags que controlan el manejo de items"),
        ENTITY("Entidades", "Flags que controlan el comportamiento de entidades"),
        SPECIAL("Especiales", "Flags con efectos especiales en jugadores"),
        COMMUNICATION("Comunicación", "Flags que controlan la comunicación"),
        ENVIRONMENT("Ambiente", "Flags que controlan el ambiente del mundo"),
        GAMEMODE("Modo de Juego", "Flags que controlan el modo de juego"),
        EXPLOSION("Explosiones", "Flags que controlan las explosiones"),
        FIRE("Fuego", "Flags que controlan el fuego"),
        LIQUID("Líquidos", "Flags que controlan los líquidos"),
        ECONOMY("Economía", "Flags relacionadas con el sistema económico");

        private final String displayName;
        private final String description;

        FlagType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }
}