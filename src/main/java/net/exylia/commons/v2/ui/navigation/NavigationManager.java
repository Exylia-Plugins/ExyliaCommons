package net.exylia.commons.v2.ui.navigation;

import net.exylia.commons.v2.ui.model.MenuData;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NavigationManager {

    private final Map<UUID, NavigationStack> stacks;

    public NavigationManager() {
        this.stacks = new ConcurrentHashMap<>();
    }

    public void push(Player player, MenuData menuData) {
        stacks.computeIfAbsent(player.getUniqueId(), k -> new NavigationStack())
                .push(menuData);
    }

    public Optional<MenuData> pop(Player player) {
        NavigationStack stack = stacks.get(player.getUniqueId());
        if (stack == null) {
            return Optional.empty();
        }

        Optional<MenuData> result = stack.pop();

        if (stack.isEmpty()) {
            stacks.remove(player.getUniqueId());
        }

        return result;
    }

    public Optional<MenuData> peek(Player player) {
        NavigationStack stack = stacks.get(player.getUniqueId());
        return stack != null ? stack.peek() : Optional.empty();
    }

    public void clear(Player player) {
        stacks.remove(player.getUniqueId());
    }

    public void clearAll() {
        stacks.clear();
    }

    public boolean canGoBack(Player player) {
        NavigationStack stack = stacks.get(player.getUniqueId());
        return stack != null && !stack.isEmpty();
    }

    public int getHistorySize(Player player) {
        NavigationStack stack = stacks.get(player.getUniqueId());
        return stack != null ? stack.size() : 0;
    }
}
