package net.exylia.commons.selection.model;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración para crear wands personalizadas
 */
@Getter
public class WandConfig {
    private Material material;
    private String displayName;
    private List<String> lore;
    private boolean enchanted;
    private boolean unbreakable;
    private int customModelData;
    private String selectionId;
    private SelectionType selectionType;

    public WandConfig() {
        this.material = Material.GOLDEN_AXE;
        this.displayName = "§6§lWand de Selección";
        this.lore = new ArrayList<>();
        this.enchanted = true;
        this.unbreakable = true;
        this.customModelData = 0;
        this.selectionId = "main";
        this.selectionType = SelectionType.CUBOID;

        // Lore por defecto
        this.lore.add("§7Click izquierdo: §ePrimer punto");
        this.lore.add("§7Click derecho: §eSegundo punto");
        this.lore.add("§7Shift + Click: §eInformación");
        this.lore.add("");
        this.lore.add("§8Wand de Selección Exylia");
    }

    // Builder pattern
    public WandConfig material(Material material) {
        this.material = material;
        return this;
    }

    public WandConfig displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public WandConfig lore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
        return this;
    }

    public WandConfig addLore(String line) {
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