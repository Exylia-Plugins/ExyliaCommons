package net.exylia.commons.v2.scoreboard.api;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.scoreboard.builder.ScoreboardBuilder;
import net.exylia.commons.v2.scoreboard.config.ScoreboardLoader;
import net.exylia.commons.v2.scoreboard.core.ScoreboardManager;
import net.exylia.commons.v2.scoreboard.instance.ScoreboardInstance;
import net.exylia.commons.v2.scoreboard.model.Scoreboard;
import net.exylia.commons.v2.scoreboard.model.ScoreboardStats;
import net.exylia.commons.v2.scoreboard.team.TeamManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class ScoreboardAPI {

    private ScoreboardAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ScoreboardBuilder builder() {
        return ScoreboardBuilder.create();
    }

    public static Scoreboard load(ConfigurationSection section) {
        return ScoreboardLoader.load(section);
    }

    public static CompletableFuture<String> show(Player player, Scoreboard scoreboard) {
        return show(player, scoreboard, null);
    }

    public static CompletableFuture<String> show(Player player, Scoreboard scoreboard, PlaceholderContext context) {
        return ScoreboardManager.getInstance().showScoreboard(player, scoreboard, context);
    }

    public static boolean hide(Player player) {
        return ScoreboardManager.getInstance().hideScoreboard(player);
    }

    public static boolean has(Player player) {
        return ScoreboardManager.getInstance().hasScoreboard(player);
    }

    public static Optional<ScoreboardInstance> get(Player player) {
        return ScoreboardManager.getInstance().getScoreboard(player);
    }

    public static void updateContext(Player player, PlaceholderContext context) {
        ScoreboardManager.getInstance().updateContext(player, context);
    }

    public static void forceUpdate(Player player) {
        ScoreboardManager.getInstance().forceUpdate(player);
    }

    public static boolean addToTeam(Player owner, Player target) {
        Optional<ScoreboardInstance> instanceOpt = ScoreboardManager.getInstance().getScoreboard(owner);

        if (instanceOpt.isPresent()) {
            TeamManager teamManager = instanceOpt.get().getTeamManager();
            if (teamManager != null) {
                return teamManager.addMember(target);
            }
        }

        return false;
    }

    public static boolean removeFromTeam(Player owner, Player target) {
        Optional<ScoreboardInstance> instanceOpt = ScoreboardManager.getInstance().getScoreboard(owner);

        if (instanceOpt.isPresent()) {
            TeamManager teamManager = instanceOpt.get().getTeamManager();
            if (teamManager != null) {
                return teamManager.removeMember(target);
            }
        }

        return false;
    }

    public static boolean isInTeam(Player owner, Player target) {
        Optional<ScoreboardInstance> instanceOpt = ScoreboardManager.getInstance().getScoreboard(owner);

        if (instanceOpt.isPresent()) {
            TeamManager teamManager = instanceOpt.get().getTeamManager();
            if (teamManager != null) {
                return teamManager.hasMember(target);
            }
        }

        return false;
    }

    public static Set<String> getTeamMembers(Player owner) {
        Optional<ScoreboardInstance> instanceOpt = ScoreboardManager.getInstance().getScoreboard(owner);

        if (instanceOpt.isPresent()) {
            TeamManager teamManager = instanceOpt.get().getTeamManager();
            if (teamManager != null) {
                return teamManager.getMembers();
            }
        }

        return Collections.emptySet();
    }

    public static void clearTeam(Player owner) {
        Optional<ScoreboardInstance> instanceOpt = ScoreboardManager.getInstance().getScoreboard(owner);

        instanceOpt.ifPresent(instance -> {
            TeamManager teamManager = instance.getTeamManager();
            if (teamManager != null) {
                teamManager.clearMembers();
            }
        });
    }

    public static void updateTeamPrefix(Player owner, String prefix) {
        Optional<ScoreboardInstance> instanceOpt = ScoreboardManager.getInstance().getScoreboard(owner);

        instanceOpt.ifPresent(instance -> {
            TeamManager teamManager = instance.getTeamManager();
            if (teamManager != null) {
                teamManager.updatePrefix(prefix);
            }
        });
    }

    public static void updateTeamSuffix(Player owner, String suffix) {
        Optional<ScoreboardInstance> instanceOpt = ScoreboardManager.getInstance().getScoreboard(owner);

        instanceOpt.ifPresent(instance -> {
            TeamManager teamManager = instance.getTeamManager();
            if (teamManager != null) {
                teamManager.updateSuffix(suffix);
            }
        });
    }

    public static void hideAll() {
        ScoreboardManager.getInstance().hideAll();
    }

    public static int getActiveCount() {
        return ScoreboardManager.getInstance().getActiveCount();
    }

    public static ScoreboardStats getStats() {
        return ScoreboardManager.getInstance().getStats();
    }

    public static void clearCache() {
        ScoreboardManager.getInstance().clearCache();
    }
}
