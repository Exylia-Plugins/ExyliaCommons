package net.exylia.commons.v2.clientapi.team.api;

import net.exylia.commons.v2.clientapi.team.core.ClientTeamManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ClientTeamAPI {

    private static ClientTeamManager manager;

    private ClientTeamAPI() {}

    public static void initialize(ClientTeamManager clientTeamManager) {
        manager = clientTeamManager;
    }

    public static void createTeam(Player viewer, String name, NamedTextColor color, boolean seeFriendlyInvisibles, List<String> members) {
        if (manager == null) return;
        manager.createTeam(viewer, name, color, seeFriendlyInvisibles, members);
    }

    public static void ensureTeamAndAdd(Player viewer, String name, NamedTextColor color, boolean seeFriendlyInvisibles, String entityName, boolean includeViewer) {
        if (manager == null) return;
        manager.ensureTeamAndAdd(viewer, name, color, seeFriendlyInvisibles, entityName, includeViewer);
    }

    public static void addToTeam(Player viewer, String name, String entityName) {
        if (manager == null) return;
        manager.addToTeam(viewer, name, entityName);
    }

    public static void removeFromTeam(Player viewer, String name, String entityName) {
        if (manager == null) return;
        manager.removeFromTeam(viewer, name, entityName);
    }

    public static void removeOwnedEntity(Player viewer, String entityName) {
        if (manager == null) return;
        manager.removeOwnedEntity(viewer, entityName);
    }

    public static void removeTeam(Player viewer, String name) {
        if (manager == null) return;
        manager.removeTeam(viewer, name);
    }

    public static void clearViewer(Player viewer) {
        if (manager == null) return;
        manager.clearViewer(viewer);
    }

    public static void cleanupViewer(UUID viewerUuid) {
        if (manager == null) return;
        manager.cleanupViewer(viewerUuid);
    }

    public static void forgetEntity(String entityName) {
        if (manager == null) return;
        manager.forgetEntity(entityName);
    }

    public static void registerEntity(int entityId, UUID uuid) {
        if (manager == null) return;
        manager.registerEntity(entityId, uuid);
    }

    public static void unregisterEntity(int entityId) {
        if (manager == null) return;
        manager.unregisterEntity(entityId);
    }

    public static void addGlowTarget(UUID viewerUuid, UUID targetUuid) {
        if (manager == null) return;
        manager.addGlowTarget(viewerUuid, targetUuid);
    }

    public static void removeGlowTarget(UUID viewerUuid, UUID targetUuid) {
        if (manager == null) return;
        manager.removeGlowTarget(viewerUuid, targetUuid);
    }

    public static Set<String> getViewerTeams(UUID viewerUuid) {
        if (manager == null) return Collections.emptySet();
        return manager.getViewerTeams(viewerUuid);
    }

    public static boolean hasTeam(UUID viewerUuid, String teamName) {
        return manager != null && manager.hasTeam(viewerUuid, teamName);
    }

    public static String getEntityTeam(UUID viewerUuid, String entityName) {
        if (manager == null) return null;
        return manager.getEntityTeam(viewerUuid, entityName);
    }
}
