package net.exylia.commons.v2.ui.menu;

import net.exylia.commons.async.Schedulers;
import net.exylia.commons.v2.ui.model.MenuData;
import net.exylia.commons.v2.ui.snapshot.AutoSnapshotHandler;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.concurrent.CompletableFuture;

public class FullInventoryMenu extends MenuBase {

    public FullInventoryMenu(Player player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, menuData.getSize(), ColorAPI.parse(menuData.getTitle()));
    }

    @Override
    protected void populateItems() {
        applyFillers();
        applyStaticItems();
    }

    @Override
    protected void handleClickInternal(int slot, ClickType clickType) {
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
                    .thenRun(() -> Schedulers.sync(() -> {
                        super.close();
                    }));
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
