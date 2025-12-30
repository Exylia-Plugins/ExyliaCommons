package net.exylia.commons.v2.ui.pagination;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PaginationTracker {

    private static final Map<UUID, PaginationContext> contexts = new ConcurrentHashMap<>();

    public static PaginationContext getContext(UUID playerId) {
        return contexts.computeIfAbsent(playerId, k -> PaginationContext.builder().build());
    }

    public static void clearPlayer(UUID playerId) {
        contexts.remove(playerId);
    }

    public static void clearAll() {
        contexts.clear();
    }

    public static int getCurrentPage(UUID playerId) {
        return getContext(playerId).getCurrentPage();
    }

    public static void setCurrentPage(UUID playerId, int page) {
        getContext(playerId).setCurrentPage(page);
    }

    public static int getSectionPage(UUID playerId, String sectionName) {
        return getContext(playerId).getSectionPage(sectionName);
    }

    public static void setSectionPage(UUID playerId, String sectionName, int page) {
        getContext(playerId).setSectionPage(sectionName, page);
    }

    public static Integer getSelectedIndex(UUID playerId, String sectionName) {
        return getContext(playerId).getSelectedIndex(sectionName);
    }

    public static void setSelectedIndex(UUID playerId, String sectionName, Integer index) {
        getContext(playerId).setSelectedIndex(sectionName, index);
    }
}
