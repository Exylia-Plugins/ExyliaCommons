package net.exylia.commons.selection.wand;

import net.exylia.commons.selection.model.WandConfig;
import net.exylia.commons.selection.model.SelectionType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory para crear wands de selección
 */
public class WandFactory {
    private static final String WAND_KEY = "exylia_selection_wand";
    private static final String SELECTION_ID_KEY = "exylia_selection_id";
    private static final String SELECTION_TYPE_KEY = "exylia_selection_type";

    private final JavaPlugin plugin;
    private final NamespacedKey wandKey;
    private final NamespacedKey selectionIdKey;
    private final NamespacedKey selectionTypeKey;

    public WandFactory(JavaPlugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, WAND_KEY);
        this.selectionIdKey = new NamespacedKey(plugin, SELECTION_ID_KEY);
        this.selectionTypeKey = new NamespacedKey(plugin, SELECTION_TYPE_KEY);
    }

    /**
     * Crea una wand con configuración por defecto
     */
    public ItemStack createWand() {
        return createWand(new WandConfig());
    }

    /**
     * Crea una wand básica con ID de selección
     */
    public ItemStack createWand(String selectionId) {
        return createWand(new WandConfig().selectionId(selectionId));
    }

    /**
     * Crea una wand con configuración personalizada
     */
    public ItemStack createWand(WandConfig config) {
        ItemStack wand = new ItemStack(config.getMaterial());
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            // Nombre y lore
            meta.setDisplayName(config.getDisplayName());
            meta.setLore(config.getLore());

            // Configurar unbreakable
            if (config.isUnbreakable()) {
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }

            // Configurar encantamiento
            if (config.isEnchanted()) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // Custom model data
            if (config.getCustomModelData() > 0) {
                meta.setCustomModelData(config.getCustomModelData());
            }

            // Ocultar flags adicionales
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            // Datos persistentes
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(selectionIdKey, PersistentDataType.STRING, config.getSelectionId());
            meta.getPersistentDataContainer().set(selectionTypeKey, PersistentDataType.STRING, config.getSelectionType().name());

            wand.setItemMeta(meta);
        }

        return wand;
    }

    /**
     * Verifica si un item es una wand
     */
    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    /**
     * Obtiene el ID de selección de una wand
     */
    public String getSelectionId(ItemStack wand) {
        if (!isWand(wand)) {
            return null;
        }

        ItemMeta meta = wand.getItemMeta();
        return meta.getPersistentDataContainer().get(selectionIdKey, PersistentDataType.STRING);
    }

    /**
     * Obtiene el tipo de selección de una wand
     */
    public SelectionType getSelectionType(ItemStack wand) {
        if (!isWand(wand)) {
            return null;
        }

        ItemMeta meta = wand.getItemMeta();
        String typeName = meta.getPersistentDataContainer().get(selectionTypeKey, PersistentDataType.STRING);

        try {
            return SelectionType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return SelectionType.CUBOID;
        }
    }

    /**
     * Actualiza el lore de una wand con información de selección
     */
    public ItemStack updateWandLore(ItemStack wand, String selectionInfo) {
        if (!isWand(wand)) {
            return wand;
        }

        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>(meta.getLore());

            // Remover líneas de información anterior
            lore.removeIf(line -> line.startsWith("{success}✓") || line.startsWith("{error}✗"));

            // Agregar nueva información
            lore.add("");
            lore.add(selectionInfo);

            meta.setLore(lore);
            wand.setItemMeta(meta);
        }

        return wand;
    }

    /**
     * Configuraciones predefinidas de wands
     */
    public static class Presets {
        public static WandConfig basicWand() {
            return new WandConfig()
                    .material(Material.GOLDEN_AXE)
                    .displayName("§6§lWand Básica")
                    .selectionId("main");
        }

        public static WandConfig adminWand() {
            return new WandConfig()
                    .material(Material.DIAMOND_AXE)
                    .displayName("§c§lWand de Administrador")
                    .addLore("§7Click izquierdo: §ePrimer punto")
                    .addLore("§7Click derecho: §eSegundo punto")
                    .addLore("§7Shift + Click: §eInformación detallada")
                    .addLore("§7Shift + Click derecho: §eLimpiar selección")
                    .addLore("")
                    .addLore("§c§lModo Administrador")
                    .selectionId("admin");
        }

        public static WandConfig customWand(Material material, String name, String selectionId) {
            return new WandConfig()
                    .material(material)
                    .displayName(name)
                    .selectionId(selectionId);
        }
    }
}