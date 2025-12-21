package net.exylia.commons.v2.ui.navigation;

import lombok.Getter;
import net.exylia.commons.v2.ui.cache.MenuCacheManager;
import net.exylia.commons.v2.ui.model.MenuV2;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class NavigationManager {
    private final ConcurrentHashMap<UUID, MenuHistory> playerHistories = new ConcurrentHashMap<>();
    private final MenuCacheManager cacheManager;

    public NavigationManager(MenuCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void pushMenu(Player player, MenuV2 menu) {
        if (player == null || menu == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        MenuHistory history = playerHistories.computeIfAbsent(
            playerId,
            k -> cacheManager.getHistory(playerId)
                .orElseGet(() -> new MenuHistory(playerId))
        );

        history.push(menu);
        cacheManager.cacheHistory(playerId, history);
    }

    public Optional<MenuV2> popMenu(Player player) {
        if (player == null) {
            return Optional.empty();
        }

        MenuHistory history = playerHistories.get(player.getUniqueId());
        if (history == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(history.pop());
    }

    public Optional<MenuV2> peekMenu(Player player) {
        if (player == null) {
            return Optional.empty();
        }

        MenuHistory history = playerHistories.get(player.getUniqueId());
        if (history == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(history.peek());
    }

    public void openParent(Player player) {
        if (player == null) {
            return;
        }

        MenuHistory history = playerHistories.get(player.getUniqueId());
        if (history != null && history.size() > 1) {
            history.pop();
            MenuV2 parent = history.pop();
            if (parent != null) {
                parent.open(player, parent.getContext());
            }
        }
    }

    public boolean goBack(Player player) {
        if (player == null) {
            return false;
        }

        MenuHistory history = playerHistories.get(player.getUniqueId());
        if (history != null && history.size() > 1) {
            history.pop();
            MenuV2 parent = history.pop();
            if (parent != null) {
                parent.open(player, parent.getContext());
                return true;
            }
        }
        return false;
    }

    public void navigateBack(Player player) {
        goBack(player);
    }

    public CompletableFuture<Boolean> goBackAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> goBack(player));
    }

    public void clearHistory(UUID playerId) {
        playerHistories.remove(playerId);
        cacheManager.invalidateHistory(playerId);
    }

    public int getHistorySize(UUID playerId) {
        MenuHistory history = playerHistories.get(playerId);
        return history != null ? history.size() : 0;
    }
}
