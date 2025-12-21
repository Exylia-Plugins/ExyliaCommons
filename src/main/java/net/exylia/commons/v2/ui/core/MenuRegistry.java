package net.exylia.commons.v2.ui.core;

import lombok.Getter;
import net.exylia.commons.v2.ui.model.MenuType;
import net.exylia.commons.v2.ui.model.MenuV2;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
public class MenuRegistry {
    private final Map<String, MenuV2> menuTemplates = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, MenuV2>> activeMenus = new ConcurrentHashMap<>();
    private final Map<String, String> namespaces = new ConcurrentHashMap<>();

    public void registerTemplate(MenuV2 menu) {
        if (menu == null || menu.getId() == null) {
            return;
        }

        menuTemplates.put(menu.getId(), menu);

        String namespace = extractNamespace(menu.getId());
        if (namespace != null) {
            namespaces.put(menu.getId(), namespace);
        }
    }

    public void unregisterTemplate(String menuId) {
        menuTemplates.remove(menuId);
        namespaces.remove(menuId);
    }

    public Optional<MenuV2> getTemplate(String menuId) {
        return Optional.ofNullable(menuTemplates.get(menuId));
    }

    public Collection<MenuV2> getAllTemplates() {
        return new ArrayList<>(menuTemplates.values());
    }

    public Collection<MenuV2> getTemplatesByNamespace(String namespace) {
        return menuTemplates.entrySet().stream()
            .filter(entry -> namespace.equals(namespaces.get(entry.getKey())))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    public Collection<MenuV2> getTemplatesByType(MenuType type) {
        return menuTemplates.values().stream()
            .filter(menu -> menu.getType() == type)
            .collect(Collectors.toList());
    }

    public void registerActiveMenu(Player player, String instanceId, MenuV2 menu) {
        if (player == null || instanceId == null || menu == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        activeMenus.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(instanceId, menu);
    }

    public Optional<MenuV2> getActiveMenu(Player player, String instanceId) {
        if (player == null || instanceId == null) {
            return Optional.empty();
        }

        Map<String, MenuV2> playerMenus = activeMenus.get(player.getUniqueId());
        if (playerMenus == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(playerMenus.get(instanceId));
    }

    public Optional<MenuV2> getCurrentMenu(Player player) {
        if (player == null) {
            return Optional.empty();
        }

        Map<String, MenuV2> playerMenus = activeMenus.get(player.getUniqueId());
        if (playerMenus == null || playerMenus.isEmpty()) {
            return Optional.empty();
        }

        return playerMenus.values().stream()
            .filter(menu -> menu.getViewer() != null
                && menu.getViewer().getUniqueId().equals(player.getUniqueId()))
            .findFirst();
    }

    public Collection<MenuV2> getPlayerMenus(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }

        Map<String, MenuV2> playerMenus = activeMenus.get(player.getUniqueId());
        if (playerMenus == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(playerMenus.values());
    }

    public void unregisterActiveMenu(Player player, String instanceId) {
        if (player == null || instanceId == null) {
            return;
        }

        Map<String, MenuV2> playerMenus = activeMenus.get(player.getUniqueId());
        if (playerMenus != null) {
            playerMenus.remove(instanceId);

            if (playerMenus.isEmpty()) {
                activeMenus.remove(player.getUniqueId());
            }
        }
    }

    public void clearPlayerMenus(UUID playerId) {
        activeMenus.remove(playerId);
    }

    public Collection<MenuV2> getAllActiveMenus() {
        return activeMenus.values().stream()
            .flatMap(map -> map.values().stream())
            .collect(Collectors.toList());
    }

    public int getActiveMenuCount() {
        return activeMenus.values().stream()
            .mapToInt(Map::size)
            .sum();
    }

    public int size() {
        return menuTemplates.size();
    }

    public void clear() {
        menuTemplates.clear();
        activeMenus.clear();
        namespaces.clear();
    }

    public void clearActiveMenus() {
        activeMenus.clear();
    }

    public Set<String> getNamespaces() {
        return new HashSet<>(namespaces.values());
    }

    private String extractNamespace(String menuId) {
        if (menuId == null || !menuId.contains(":")) {
            return null;
        }

        return menuId.substring(0, menuId.indexOf(":"));
    }
}
