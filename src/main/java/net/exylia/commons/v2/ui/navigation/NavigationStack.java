package net.exylia.commons.v2.ui.navigation;

import net.exylia.commons.v2.ui.model.MenuData;

import java.util.LinkedList;
import java.util.Optional;

public class NavigationStack {

    private final LinkedList<MenuData> stack;
    private static final int MAX_HISTORY_SIZE = 10;

    public NavigationStack() {
        this.stack = new LinkedList<>();
    }

    public void push(MenuData menuData) {
        if (menuData == null) {
            return;
        }

        stack.push(menuData.copy());

        if (stack.size() > MAX_HISTORY_SIZE) {
            stack.removeLast();
        }
    }

    public Optional<MenuData> pop() {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(stack.pop());
    }

    public Optional<MenuData> peek() {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(stack.peek());
    }

    public void clear() {
        stack.clear();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }
}
