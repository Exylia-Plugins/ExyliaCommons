package net.exylia.commons.v2.ui.selector.impl.iconpicker;

import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import net.exylia.commons.v2.ui.selector.impl.iconpicker.menu.IconPickerMenu;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public final class IconPickerAPI {

    private IconPickerAPI() {}

    public static void open(Player player, Runnable onCancel, Consumer<ItemSnapshot> onComplete) {
        IconPickerSession session = new IconPickerSession(player, onCancel, onComplete);
        IconPickerMenu.open(player, session);
    }
}
