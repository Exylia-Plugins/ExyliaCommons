package net.exylia.commons.v2.clientapi.team.core;

import net.exylia.commons.v2.clientapi.team.adapter.TeamTrackerAdapter;
import net.exylia.commons.v2.clientapi.team.adapter.impl.ApolloTeamTrackerAdapter;
import net.exylia.commons.v2.clientapi.team.adapter.impl.FeatherTeamTrackerAdapter;
import net.exylia.commons.v2.clientapi.team.model.TrackingTeam;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.exylia.commons.v2.tasks.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TeamTrackerManager {

    private final List<TeamTrackerAdapter> adapters = new ArrayList<>();
    private final Map<UUID, TrackingTeam> teams = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToTeam = new ConcurrentHashMap<>();
    private ScheduledTask refreshTask;

    public void initialize() {
        tryRegisterApollo();
        tryRegisterFeather();

        boolean needsRefresh = adapters.stream().anyMatch(a -> !(a instanceof FeatherTeamTrackerAdapter));
        if (needsRefresh) {
            refreshTask = TaskAPI.asyncScheduledTimer(this::refreshAll, 50, 50, TimeUnit.MILLISECONDS);
        }
    }

    private void tryRegisterApollo() {
        if (!isAnyPluginEnabled("Apollo", "Apollo-Folia", "Apollo-Bukkit")) {
            DebugAPI.logLibWarn("Apollo (Lunar Client) API not found. ExyliaCommons supports it — add apollo-api as a dependency to enable team tracking.");
            return;
        }
        try {
            adapters.add(new ApolloTeamTrackerAdapter());
            DebugAPI.logLibSuccess("Apollo (Lunar Client) team tracker support enabled.");
        } catch (NoClassDefFoundError | Exception e) {
            DebugAPI.logLibError("Failed to initialize Apollo team tracker adapter: " + e.getMessage());
        }
    }

    private void tryRegisterFeather() {
        if (!isAnyPluginEnabled("FeatherServerAPI")) {
            DebugAPI.logLibWarn("Feather Client API not found. ExyliaCommons supports it — add feather-server-api as a dependency to enable team tracking.");
            return;
        }
        try {
            adapters.add(new FeatherTeamTrackerAdapter());
            DebugAPI.logLibSuccess("Feather Client team tracker support enabled.");
        } catch (NoClassDefFoundError | Exception e) {
            DebugAPI.logLibError("Failed to initialize Feather team tracker adapter: " + e.getMessage());
        }
    }

    private boolean isAnyPluginEnabled(String... names) {
        for (String name : names) {
            if (Bukkit.getPluginManager().isPluginEnabled(name)) return true;
        }
        return false;
    }

    public TrackingTeam createTeam() {
        TrackingTeam team = new TrackingTeam();
        teams.put(team.getTeamId(), team);
        return team;
    }

    public void deleteTeam(UUID teamId) {
        TrackingTeam team = teams.remove(teamId);
        if (team == null) return;
        for (Player member : team.getMembers()) {
            playerToTeam.remove(member.getUniqueId());
        }
        for (TeamTrackerAdapter adapter : adapters) {
            if (!adapter.isAvailable()) continue;
            try {
                adapter.dissolve(team);
            } catch (Exception e) {
                DebugAPI.logLibError("Error dissolving team via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    public void addMember(UUID teamId, Player player) {
        TrackingTeam team = teams.get(teamId);
        if (team == null) return;

        UUID previousTeamId = playerToTeam.put(player.getUniqueId(), teamId);
        if (previousTeamId != null && !previousTeamId.equals(teamId)) {
            removeMemberFromTeam(previousTeamId, player);
        }

        team.addMember(player);
        for (TeamTrackerAdapter adapter : adapters) {
            if (!adapter.isAvailable() || !adapter.supportsPlayer(player)) continue;
            try {
                adapter.onMemberAdded(team, player);
            } catch (Exception e) {
                DebugAPI.logLibError("Error adding team member via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    public void removeMember(Player player) {
        UUID teamId = playerToTeam.remove(player.getUniqueId());
        if (teamId == null) return;
        removeMemberFromTeam(teamId, player);
    }

    private void removeMemberFromTeam(UUID teamId, Player player) {
        TrackingTeam team = teams.get(teamId);
        if (team == null) return;
        team.removeMember(player.getUniqueId());
        for (TeamTrackerAdapter adapter : adapters) {
            if (!adapter.isAvailable()) continue;
            try {
                adapter.onMemberRemoved(team, player);
            } catch (Exception e) {
                DebugAPI.logLibError("Error removing team member via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    private void refreshAll() {
        for (TrackingTeam team : teams.values()) {
            for (TeamTrackerAdapter adapter : adapters) {
                if (!adapter.isAvailable()) continue;
                try {
                    adapter.refresh(team);
                } catch (Exception e) {
                    DebugAPI.logLibError("Error refreshing team via " + adapter.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        for (TrackingTeam team : teams.values()) {
            for (TeamTrackerAdapter adapter : adapters) {
                if (!adapter.isAvailable()) continue;
                try {
                    adapter.dissolve(team);
                } catch (Exception ignored) {}
            }
        }
        teams.clear();
        playerToTeam.clear();
    }

    public List<TeamTrackerAdapter> getAdapters() {
        return Collections.unmodifiableList(adapters);
    }
}
