package net.exylia.commons.menu;

import net.exylia.commons.utils.MessageUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.exylia.commons.utils.PredefinedColors.*;
import static net.exylia.commons.utils.PredefinedHeads.TEXTURE_DISABLE;
import static net.exylia.commons.utils.PredefinedHeads.TEXTURE_ENABLE;

@Deprecated
public class ToggleItemBuilder {
    private String name;
    private String material;
    private Consumer<Boolean> setter;
    private Supplier<Boolean> getter;
    private Player player;
    private Runnable onUpdate;
    private Supplier<Boolean> condition;
    private List<String> denyMessages;

    public ToggleItemBuilder(String name) {
        this.name = name;
    }

    public ToggleItemBuilder(String name, String material) {
        this.name = name;
        this.material = material;
    }

    public ToggleItemBuilder setter(Consumer<Boolean> setter) {
        this.setter = setter;
        return this;
    }

    public ToggleItemBuilder getter(Supplier<Boolean> getter) {
        this.getter = getter;
        return this;
    }

    public ToggleItemBuilder player(Player player) {
        this.player = player;
        return this;
    }

    public ToggleItemBuilder onUpdate(Runnable callback) {
        this.onUpdate = callback;
        return this;
    }

    public ToggleItemBuilder condition(Supplier<Boolean> condition) {
        this.condition = condition;
        return this;
    }

    public ToggleItemBuilder denyMessage(String denyMessage) {
        this.denyMessages = List.of(denyMessage);
        return this;
    }

    public ToggleItemBuilder denyMessages(List<String> denyMessages) {
        this.denyMessages = new ArrayList<>(denyMessages);
        return this;
    }

    public MenuItem build() {
        MenuItem item = new MenuItem(material != null ? material : (getter.get() ? TEXTURE_ENABLE : TEXTURE_DISABLE));
        updateItemState(item);

        item.setClickHandler(clickInfo -> {
            // Verificar condición antes de permitir el toggle
            if (condition != null && !condition.get()) {
                if (player != null && denyMessages != null && !denyMessages.isEmpty()) {
                    for (String message : denyMessages) {
                        MessageUtils.sendMessageAsync(player, COLOR_ERROR + message);
                    }
                }
                return; // No hacer nada si la condición no se cumple
            }

            boolean currentValue = getter.get();
            boolean newValue = !currentValue;
            setter.accept(newValue);

            MenuItem updatedItem = new MenuItem(material != null ? material : (!currentValue ? TEXTURE_ENABLE : TEXTURE_DISABLE));
            updateItemState(updatedItem);
            updatedItem.setClickHandler(clickInfo.item().getClickHandler());
            clickInfo.updateThisItem(updatedItem);

            if (player != null) {
                MessageUtils.sendMessageAsync(player, COLOR_SUCCESS + name + " " +
                        (newValue ? "enabled" : "disabled") + "!");
            }
            if (onUpdate != null) {
                onUpdate.run();
            }
        });

        return item;
    }

    private void updateItemState(MenuItem item) {
        boolean currentValue = getter.get();
        boolean canToggle = condition == null || condition.get();

        if (material == null) {
            item.setMaterial((currentValue ? TEXTURE_ENABLE : TEXTURE_DISABLE));
        }

        // Modificar el nombre para indicar si está deshabilitado
        String nameColor = canToggle ? (currentValue ? COLOR_SUCCESS : COLOR_ERROR) : COLOR_SECONDARY;
        item.setName(nameColor + name + (canToggle ? "" : " (Disabled)"));

        // Actualizar el lore
        if (canToggle) {
            item.setLore(
                    "Status: " + (currentValue ? COLOR_SUCCESS + "Enabled" : COLOR_ERROR + "Disabled"),
                    "",
                    COLOR_SECONDARY + "Click to " + (currentValue ? "disable" : "enable")
            );
        } else {
            List<String> loreLines = new ArrayList<>();
            loreLines.add("Status: " + (currentValue ? COLOR_SUCCESS + "Enabled" : COLOR_ERROR + "Disabled"));
            loreLines.add("");

            if (denyMessages != null && !denyMessages.isEmpty()) {
                for (String message : denyMessages) {
                    loreLines.add(COLOR_ERROR + message);
                }
            } else {
                loreLines.add(COLOR_ERROR + "Cannot toggle this item");
            }

            loreLines.add(COLOR_SECONDARY + "Condition not met");

            item.setLore(loreLines.toArray(new String[0]));
        }
    }
}