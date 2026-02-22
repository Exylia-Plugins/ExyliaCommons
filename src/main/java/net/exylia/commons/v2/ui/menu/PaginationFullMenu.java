package net.exylia.commons.v2.ui.menu;

import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.tasks.api.Tasks;
import net.exylia.commons.v2.ui.snapshot.AutoSnapshotHandler;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class PaginationFullMenu extends PaginationMenu {

    public PaginationFullMenu(Player player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    public CompletableFuture<Void> openAsync() {
        if (menuData.isSnapshotEnabled()) {
            String snapshotId = menuData.getSnapshotId();
            return AutoSnapshotHandler.createSnapshot(player, snapshotId)
                    .thenCompose(snapshot -> super.openAsync());
        }

        return super.openAsync();
    }

    @Override
    public void close() {
        if (menuData.isSnapshotEnabled() && menuData.isRestoreOnClose()) {
            AutoSnapshotHandler.restoreSnapshot(player)
                    .whenComplete((result, throwable) -> Tasks.sync(super::close));
        } else {
            super.close();
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        if (menuData.isSnapshotEnabled()) {
            AutoSnapshotHandler.clearSnapshot(player);
        }
    }
}
