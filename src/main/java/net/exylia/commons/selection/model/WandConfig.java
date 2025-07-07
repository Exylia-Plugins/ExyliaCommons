package net.exylia.commons.selection.model;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración para crear wands personalizadas
 */
@Getter
public class WandConfig {
    private Material material;
    private Component displayName;
    private List<Component> lore;
    private boolean enchanted;
    private boolean unbreakable;
    private int customModelData;
    private String selectionId;
    private SelectionType selectionType;

    public WandConfig() {
        this.material = Material.GOLDEN_AXE;
        this.displayName = Component.empty();
        this.lore = new ArrayList<>();
        this.enchanted = true;
        this.unbreakable = true;
        this.customModelData = 0;
        this.selectionId = "no-id";
        this.selectionType = SelectionType.CUBOID;

        // Lore por defecto
        this.lore.add(Component.empty());
    }

    // Builder pattern
    public WandConfig material(Material material) {
        this.material = material;
        return this;
    }

    public WandConfig displayName(Component displayName) {
        this.displayName = displayName;
        return this;
    }

    public WandConfig lore(List<Component> lore) {
        this.lore = new ArrayList<>(lore);
        return this;
    }

    public WandConfig addLore(Component line) {
        this.lore.add(line);
        return this;
    }

    public WandConfig enchanted(boolean enchanted) {
        this.enchanted = enchanted;
        return this;
    }

    public WandConfig unbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public WandConfig customModelData(int customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    public WandConfig selectionId(String selectionId) {
        this.selectionId = selectionId;
        return this;
    }

    public WandConfig selectionType(SelectionType selectionType) {
        this.selectionType = selectionType;
        return this;
    }

}