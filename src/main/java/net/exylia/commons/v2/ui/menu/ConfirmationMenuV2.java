package net.exylia.commons.v2.ui.menu;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.ui.model.*;
import net.exylia.commons.v2.ui.refresh.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.UUID;
import java.util.function.Consumer;

@Getter
@Setter
public class ConfirmationMenuV2 extends MenuV2 {
    private MenuItemV2 infoItem;
    private int infoItemSlot = 13;

    private MenuItemV2 confirmButton;
    private int confirmButtonSlot = 11;

    private MenuItemV2 cancelButton;
    private int cancelButtonSlot = 15;

    private Consumer<Player> onConfirm;
    private Consumer<Player> onCancel;

    private boolean closeOnConfirm = true;
    private boolean closeOnCancel = true;

    public ConfirmationMenuV2(String title) {
        this(title, 3);
    }

    public ConfirmationMenuV2(String title, int rows) {
        super(UUID.randomUUID().toString(), MenuType.CONFIRMATION);
        this.rawTitle = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.size = this.rows * 9;
        initializeRefreshStrategy();
    }

    private void initializeRefreshStrategy() {
        this.refreshStrategy = switch (refreshMode) {
            case DISABLED -> new DisabledRefreshStrategy();
            case SLOT_ONLY -> new SlotOnlyRefreshStrategy();
            case SMART -> new SmartRefreshStrategy();
            case FULL -> new FullRefreshStrategy();
        };
    }

    @Override
    public void open(Player player, MenuContext context) {
        this.viewer = player;
        this.context = context != null ? context : MenuContext.create(player);

        enrichContext();
        processTitle();
        createInventory();
        applyFillers();
        populateInventory();

        player.openInventory(inventory);
        this.state = MenuState.OPEN;

        handleOpen();

        if (dynamicUpdates) {
            startDynamicUpdates();
        }
    }

    @Override
    public void populateInventory() {
        if (inventory == null) {
            return;
        }

        items.forEach((slot, item) -> {
            if (slot != infoItemSlot && slot != confirmButtonSlot && slot != cancelButtonSlot) {
                inventory.setItem(slot, item.build(viewer, context.getPlaceholderContext()));
            }
        });

        if (infoItem != null && infoItemSlot >= 0 && infoItemSlot < size) {
            inventory.setItem(infoItemSlot, infoItem.build(viewer, context.getPlaceholderContext()));
            items.put(infoItemSlot, infoItem);
        }

        if (confirmButton != null && confirmButtonSlot >= 0 && confirmButtonSlot < size) {
            MenuItemV2 confirm = confirmButton.clone();
            confirm.addAction(ClickType.LEFT, "ui:confirm");
            inventory.setItem(confirmButtonSlot, confirm.build(viewer, context.getPlaceholderContext()));
            items.put(confirmButtonSlot, confirm);
        }

        if (cancelButton != null && cancelButtonSlot >= 0 && cancelButtonSlot < size) {
            MenuItemV2 cancel = cancelButton.clone();
            cancel.addAction(ClickType.LEFT, "ui:cancel");
            inventory.setItem(cancelButtonSlot, cancel.build(viewer, context.getPlaceholderContext()));
            items.put(cancelButtonSlot, cancel);
        }
    }

    public void handleConfirm(Player player) {
        if (onConfirm != null) {
            onConfirm.accept(player);
        }

        if (closeOnConfirm) {
            close();
        }
    }

    public void handleCancel(Player player) {
        if (onCancel != null) {
            onCancel.accept(player);
        }

        if (closeOnCancel) {
            close();
        }
    }

    public ConfirmationMenuV2 setInfoItem(MenuItemV2 item, int slot) {
        this.infoItem = item;
        this.infoItemSlot = slot;
        return this;
    }

    public ConfirmationMenuV2 setConfirmButton(MenuItemV2 button, int slot) {
        this.confirmButton = button;
        this.confirmButtonSlot = slot;
        return this;
    }

    public ConfirmationMenuV2 setCancelButton(MenuItemV2 button, int slot) {
        this.cancelButton = button;
        this.cancelButtonSlot = slot;
        return this;
    }

    public ConfirmationMenuV2 setOnConfirm(Consumer<Player> callback) {
        this.onConfirm = callback;
        return this;
    }

    public ConfirmationMenuV2 setOnCancel(Consumer<Player> callback) {
        this.onCancel = callback;
        return this;
    }

    public ConfirmationMenuV2 setCloseOnConfirm(boolean close) {
        this.closeOnConfirm = close;
        return this;
    }

    public ConfirmationMenuV2 setCloseOnCancel(boolean close) {
        this.closeOnCancel = close;
        return this;
    }
}
