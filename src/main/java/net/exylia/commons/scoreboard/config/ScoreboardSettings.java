package net.exylia.commons.scoreboard.config;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.scoreboard.Team;

@Getter
@Builder
@Deprecated
public class ScoreboardSettings {

    @Builder.Default
    private Team.OptionStatus collisionRule = Team.OptionStatus.ALWAYS;

    @Builder.Default
    private boolean canSeeFriendlyInvisibles = true;

    @Builder.Default
    private Team.OptionStatus nametagVisibility = Team.OptionStatus.ALWAYS;

    @Builder.Default
    private Team.OptionStatus deathMessageVisibility = Team.OptionStatus.ALWAYS;

    @Builder.Default
    private boolean createMainTeam = false;

    @Builder.Default
    private String mainTeamName = "exylia_main";

    @Builder.Default
    private String mainTeamPrefix = "";

    @Builder.Default
    private String mainTeamSuffix = "";

    @Builder.Default
    private org.bukkit.ChatColor mainTeamColor = org.bukkit.ChatColor.WHITE;

    @Builder.Default
    private boolean allowFriendlyFire = true;

    @Builder.Default
    private boolean autoCleanupEmptyTeams = true;

    @Builder.Default
    private boolean shareScoreboards = false;

    @Builder.Default
    private long cleanupInterval = 1200L;  

    @Builder.Default
    private boolean preserveOriginalScoreboard = true;

    @Builder.Default
    private boolean backupOriginalScoreboard = false;

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

    public static ScoreboardSettings defaultSettings() {
        return ScoreboardSettings.builder().build();
    }

    public boolean hasCustomTeamSettings() {
        return collisionRule != Team.OptionStatus.ALWAYS ||
                !canSeeFriendlyInvisibles ||
                nametagVisibility != Team.OptionStatus.ALWAYS ||
                deathMessageVisibility != Team.OptionStatus.ALWAYS ||
                !allowFriendlyFire ||
                createMainTeam;
    }
}
