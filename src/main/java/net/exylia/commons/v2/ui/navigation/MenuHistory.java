package net.exylia.commons.v2.ui.navigation;

import lombok.Getter;
import net.exylia.commons.v2.ui.model.MenuV2;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

@Getter
public class MenuHistory {
    private final UUID playerId;
    private final Deque<MenuV2> history;
    private final int maxSize;

    public MenuHistory(UUID playerId) {
        this(playerId, 10);
    }

    public MenuHistory(UUID playerId, int maxSize) {
        this.playerId = playerId;
        this.history = new ArrayDeque<>();
        this.maxSize = maxSize;
    }

    public void push(MenuV2 menu) {
        if (history.size() >= maxSize) {
            history.removeLast();
        }
        history.push(menu);
    }

    public MenuV2 pop() {
        return history.poll();
    }

    public MenuV2 peek() {
        return history.peek();
    }

    public int size() {
        return history.size();
    }

    public void clear() {
        history.clear();
    }

    public boolean isEmpty() {
        return history.isEmpty();
    }
}
