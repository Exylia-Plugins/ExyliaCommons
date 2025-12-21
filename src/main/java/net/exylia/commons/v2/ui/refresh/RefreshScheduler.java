package net.exylia.commons.v2.ui.refresh;

import net.exylia.commons.async.ScheduledTask;
import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.ui.model.MenuState;
import net.exylia.commons.v2.ui.model.MenuV2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RefreshScheduler {
    private final Map<String, ScheduledTask> scheduledRefreshes = new ConcurrentHashMap<>();

    public void scheduleRefresh(MenuV2 menu, long intervalTicks) {
        if (menu == null) {
            return;
        }

        String menuId = menu.getId();

        cancelRefresh(menuId);

        ScheduledTask task = Schedulers.syncTimer(() -> {
            if (menu.getState() == MenuState.OPEN &&
                menu.getViewer() != null &&
                menu.getViewer().isOnline()) {
                menu.refresh();
            } else {
                cancelRefresh(menuId);
            }
        }, intervalTicks, intervalTicks);

        scheduledRefreshes.put(menuId, task);
    }

    public void cancelRefresh(String menuId) {
        ScheduledTask task = scheduledRefreshes.remove(menuId);
        if (task != null) {
            task.cancel();
        }
    }

    public void cancelAll() {
        scheduledRefreshes.values().forEach(ScheduledTask::cancel);
        scheduledRefreshes.clear();
    }

    public boolean isScheduled(String menuId) {
        return scheduledRefreshes.containsKey(menuId);
    }

    public int getActiveRefreshCount() {
        return scheduledRefreshes.size();
    }
}
