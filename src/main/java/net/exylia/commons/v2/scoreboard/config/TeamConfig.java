package net.exylia.commons.v2.scoreboard.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Team;

@Getter
@Builder
@With
@AllArgsConstructor
public class TeamConfig {

    @Builder.Default
    private final String name = "main";

    @Builder.Default
    private final String prefix = "";

    @Builder.Default
    private final String suffix = "";

    @Builder.Default
    private final ChatColor color = ChatColor.WHITE;

    @Builder.Default
    private final Team.OptionStatus collisionRule = Team.OptionStatus.ALWAYS;

    @Builder.Default
    private final Team.OptionStatus nametagVisibility = Team.OptionStatus.ALWAYS;

    @Builder.Default
    private final boolean friendlyFire = true;

    @Builder.Default
    private final boolean seeFriendlyInvisibles = true;

    public static TeamConfig defaults() {
        return TeamConfig.builder().build();
    }
}
