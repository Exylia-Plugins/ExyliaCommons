package net.exylia.commons.menu;

import net.exylia.commons.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builder específico para items de toggle (boolean rules)
 */
public class ToggleItemBuilder {
    private String name;
    private String material;
    private String colorSuccess = "§a";
    private String colorError = "§c";
    private String colorSecondary = "§7";
    private Consumer<Boolean> setter;
    private Supplier<Boolean> getter;
    private Player player;
    private Runnable onUpdate;

    public ToggleItemBuilder(String name, String material) {
        this.name = name;
        this.material = material;
    }

    public ToggleItemBuilder colors(String success, String error, String secondary) {
        this.colorSuccess = success;
        this.colorError = error;
        this.colorSecondary = secondary;
        return this;
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

    public MenuItem build() {
        MenuItem item = new MenuItem(material);
        updateItemState(item);

        item.setClickHandler(clickInfo -> {
            boolean currentValue = getter.get();
            boolean newValue = !currentValue;
            setter.accept(newValue);

            // Crear un nuevo item con el estado actualizado
            MenuItem updatedItem = new MenuItem(material);
            updateItemState(updatedItem);

            // CRITICAL: Preserve the click handler by recreating it with the same logic
            updatedItem.setClickHandler(clickInfo.item().getClickHandler());

            // Actualizar el item en el menú inmediatamente
            clickInfo.updateThisItem(updatedItem);

            // Notificar al jugador
            if (player != null) {
                MessageUtils.sendMessageAsync(player, colorSuccess + name + " " +
                        (newValue ? "enabled" : "disabled") + "!");
            }

            // Ejecutar callback si existe
            if (onUpdate != null) {
                onUpdate.run();
            }
        });

        return item;
    }

    private void updateItemState(MenuItem item) {
        boolean currentValue = getter.get();
        item.setName((currentValue ? colorSuccess : colorError) + name);
        item.setLore(
                "Status: " + (currentValue ? colorSuccess + "Enabled" : colorError + "Disabled"),
                "",
                colorSecondary + "Click to " + (currentValue ? "disable" : "enable")
        );
    }
}