package net.exylia.commons.scoreboard.config;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.scoreboard.Team;

/**
 * Configuración avanzada para scoreboards y teams de Minecraft
 * Permite personalizar comportamientos específicos del sistema de scoreboards
 */
@Getter
@Builder
public class ScoreboardSettings {

    // ==================== CONFIGURACIÓN DE TEAMS ====================

    /**
     * Configuración de colisiones entre jugadores
     * - ALWAYS: Siempre colisionan
     * - NEVER: Nunca colisionan
     * - PUSH_OTHER_TEAMS: Solo empujan a otros teams
     * - PUSH_OWN_TEAM: Solo empujan a su propio team
     */
    @Builder.Default
    private Team.OptionStatus collisionRule = Team.OptionStatus.ALWAYS;

    /**
     * Si los miembros del team pueden verse cuando están invisibles
     */
    @Builder.Default
    private boolean canSeeFriendlyInvisibles = true;

    /**
     * Configuración de visibilidad de nametags
     * - ALWAYS: Siempre visibles
     * - NEVER: Nunca visibles
     * - HIDE_FOR_OTHER_TEAMS: Ocultos para otros teams
     * - HIDE_FOR_OWN_TEAM: Ocultos para su propio team
     */
    @Builder.Default
    private Team.OptionStatus nametagVisibility = Team.OptionStatus.ALWAYS;

    /**
     * Configuración de visibilidad en el death screen
     * - ALWAYS: Siempre visibles
     * - NEVER: Nunca visibles
     * - HIDE_FOR_OTHER_TEAMS: Ocultos para otros teams
     * - HIDE_FOR_OWN_TEAM: Ocultos para su propio team
     */
    @Builder.Default
    private Team.OptionStatus deathMessageVisibility = Team.OptionStatus.ALWAYS;

    // ==================== CONFIGURACIÓN DE SCOREBOARD ====================

    /**
     * Si se debe crear un team principal para todos los jugadores del scoreboard
     */
    @Builder.Default
    private boolean createMainTeam = false;

    /**
     * Nombre del team principal (si está habilitado)
     */
    @Builder.Default
    private String mainTeamName = "exylia_main";

    /**
     * Prefijo del team principal
     */
    @Builder.Default
    private String mainTeamPrefix = "";

    /**
     * Sufijo del team principal
     */
    @Builder.Default
    private String mainTeamSuffix = "";

    /**
     * Color del team principal
     */
    @Builder.Default
    private org.bukkit.ChatColor mainTeamColor = org.bukkit.ChatColor.WHITE;

    /**
     * Si está habilitado el fuego amigo en el team
     */
    @Builder.Default
    private boolean allowFriendlyFire = true;

    // ==================== CONFIGURACIÓN DE RENDIMIENTO ====================

    /**
     * Si se debe limpiar automáticamente teams vacíos
     */
    @Builder.Default
    private boolean autoCleanupEmptyTeams = true;

    /**
     * Si se debe compartir el scoreboard entre jugadores con la misma configuración
     * ADVERTENCIA: Esto puede causar conflictos si los jugadores tienen contextos diferentes
     */
    @Builder.Default
    private boolean shareScoreboards = false;

    /**
     * Intervalo en ticks para limpiar teams vacíos (solo si autoCleanupEmptyTeams está habilitado)
     */
    @Builder.Default
    private long cleanupInterval = 1200L; // 1 minuto

    // ==================== CONFIGURACIÓN DE COMPATIBILIDAD ====================

    /**
     * Si se debe preservar el scoreboard original del jugador al ocultar
     */
    @Builder.Default
    private boolean preserveOriginalScoreboard = true;

    /**
     * Si se debe crear un backup del scoreboard original
     */
    @Builder.Default
    private boolean backupOriginalScoreboard = false;

    // ==================== MÉTODOS DE CONFIGURACIÓN RÁPIDA ====================

    /**
     * Configuración para PvP sin colisiones
     */
    public static ScoreboardSettings pvpNoCollision() {
        return ScoreboardSettings.builder()
                .collisionRule(Team.OptionStatus.NEVER)
                .canSeeFriendlyInvisibles(false)
                .nametagVisibility(Team.OptionStatus.ALWAYS)
                .allowFriendlyFire(false)
                .createMainTeam(true)
                .mainTeamName("pvp_players")
                .build();
    }

    /**
     * Configuración para mini-juegos con teams ocultos
     */
    public static ScoreboardSettings hiddenTeams() {
        return ScoreboardSettings.builder()
                .collisionRule(Team.OptionStatus.FOR_OTHER_TEAMS)
                .canSeeFriendlyInvisibles(true)
                .nametagVisibility(Team.OptionStatus.FOR_OTHER_TEAMS)
                .deathMessageVisibility(Team.OptionStatus.FOR_OTHER_TEAMS)
                .allowFriendlyFire(false)
                .createMainTeam(true)
                .build();
    }

    /**
     * Configuración para lobby/hub
     */
    public static ScoreboardSettings lobby() {
        return ScoreboardSettings.builder()
                .collisionRule(Team.OptionStatus.NEVER)
                .canSeeFriendlyInvisibles(true)
                .nametagVisibility(Team.OptionStatus.ALWAYS)
                .allowFriendlyFire(true)
                .createMainTeam(true)
                .mainTeamName("lobby_players")
                .autoCleanupEmptyTeams(true)
                .shareScoreboards(true)
                .build();
    }

    /**
     * Configuración por defecto (comportamiento vanilla de Minecraft)
     */
    public static ScoreboardSettings defaultSettings() {
        return ScoreboardSettings.builder().build();
    }

    /**
     * Verifica si alguna configuración de team está personalizada
     */
    public boolean hasCustomTeamSettings() {
        return collisionRule != Team.OptionStatus.ALWAYS ||
                !canSeeFriendlyInvisibles ||
                nametagVisibility != Team.OptionStatus.ALWAYS ||
                deathMessageVisibility != Team.OptionStatus.ALWAYS ||
                !allowFriendlyFire ||
                createMainTeam;
    }
}