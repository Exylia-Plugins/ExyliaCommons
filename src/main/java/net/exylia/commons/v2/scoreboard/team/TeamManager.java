package net.exylia.commons.v2.scoreboard.team;

import lombok.Getter;
import net.exylia.commons.v2.scoreboard.config.TeamConfig;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TeamManager {

    private final Player owner;
    private final TeamConfig config;
    private final Scoreboard bukkitScoreboard;
    private final Team mainTeam;
    private final Set<String> members;

    public TeamManager(Player owner, TeamConfig config, Scoreboard bukkitScoreboard) {
        this.owner = owner;
        this.config = config;
        this.bukkitScoreboard = bukkitScoreboard;
        this.members = ConcurrentHashMap.newKeySet();
        this.mainTeam = setupMainTeam();
    }

    public boolean addMember(Player player) {
        if (player == null || mainTeam == null) {
            return false;
        }

        String playerName = player.getName();
        if (members.contains(playerName)) {
            return false;
        }

        try {
            mainTeam.addEntry(playerName);
            members.add(playerName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean removeMember(Player player) {
        if (player == null || mainTeam == null) {
            return false;
        }

        String playerName = player.getName();
        if (!members.contains(playerName)) {
            return false;
        }

        try {
            mainTeam.removeEntry(playerName);
            members.remove(playerName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasMember(Player player) {
        return player != null && members.contains(player.getName());
    }

    public void clearMembers() {
        if (mainTeam == null) {
            return;
        }

        try {
            for (String member : members) {
                mainTeam.removeEntry(member);
            }
            members.clear();
        } catch (Exception ignored) {
        }
    }

    public void updatePrefix(String prefix) {
        if (mainTeam != null) {
            try {
                mainTeam.setPrefix(prefix != null ? prefix : "");
            } catch (Exception ignored) {
            }
        }
    }

    public void updateSuffix(String suffix) {
        if (mainTeam != null) {
            try {
                mainTeam.setSuffix(suffix != null ? suffix : "");
            } catch (Exception ignored) {
            }
        }
    }

    public void updateColor(ChatColor color) {
        if (mainTeam != null) {
            try {
                mainTeam.setColor(color != null ? color : ChatColor.WHITE);
            } catch (Exception ignored) {
            }
        }
    }

    public void cleanup() {
        clearMembers();

        if (mainTeam != null) {
            try {
                mainTeam.unregister();
            } catch (Exception ignored) {
            }
        }
    }

    private Team setupMainTeam() {
        if (config == null) {
            return null;
        }

        try {
            String teamName = config.getName();

            Team existingTeam = bukkitScoreboard.getTeam(teamName);
            if (existingTeam != null) {
                existingTeam.unregister();
            }

            Team team = bukkitScoreboard.registerNewTeam(teamName);
            applyTeamSettings(team);
            return team;

        } catch (Exception e) {
            return null;
        }
    }

    private void applyTeamSettings(Team team) {
        if (team == null || config == null) {
            return;
        }

        try {
            team.setPrefix(config.getPrefix() != null ? config.getPrefix() : "");
            team.setSuffix(config.getSuffix() != null ? config.getSuffix() : "");
            team.setColor(config.getColor() != null ? config.getColor() : ChatColor.WHITE);
            team.setOption(Team.Option.COLLISION_RULE, config.getCollisionRule());
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, config.getNametagVisibility());
            team.setAllowFriendlyFire(config.isFriendlyFire());
            team.setCanSeeFriendlyInvisibles(config.isSeeFriendlyInvisibles());
        } catch (Exception ignored) {
        }
    }
}
