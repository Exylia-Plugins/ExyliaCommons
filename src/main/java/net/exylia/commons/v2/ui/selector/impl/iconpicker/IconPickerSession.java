package net.exylia.commons.v2.ui.selector.impl.iconpicker;

import lombok.Getter;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@Getter
public class IconPickerSession {

    private final Player player;
    private final Runnable onCancel;
    private final Consumer<ItemSnapshot> onComplete;
    private ItemStack capturedItem;

    public IconPickerSession(Player player, Runnable onCancel, Consumer<ItemSnapshot> onComplete) {
        this.player = player;
        this.onCancel = onCancel;
        this.onComplete = onComplete;
    }

    public void setCapturedItem(ItemStack item) {
        this.capturedItem = item != null ? item.clone() : null;
    }

    public boolean hasCapturedItem() {
        return capturedItem != null && capturedItem.getType() != org.bukkit.Material.AIR;
    }

    public void complete(ItemSnapshot snapshot) {
        IconPickerRegistry.getInstance().remove(player);
        onComplete.accept(snapshot);
    }

    public void cancel() {
        IconPickerRegistry.getInstance().remove(player);
        if (onCancel != null) onCancel.run();
    }
}
