package net.exylia.commons.items.model;

import lombok.Getter;
import net.exylia.commons.items.processor.*;
import net.exylia.commons.items.utils.ItemStackUtils;
import net.exylia.commons.items.utils.PlaceholderDetector;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.effects.SoundUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ExyliaItem {

    @Getter
    protected ItemData itemData;

    protected ItemStack itemStack;

    public ExyliaItem() {
        this.itemData = ItemData.builder().build();
        this.itemStack = ItemStackUtils.createFromString(itemData.getRawMaterial());
    }

    public ExyliaItem(ItemData itemData) {
        this.itemData = itemData != null ? itemData : ItemData.builder().build();
        this.itemStack = ItemStackUtils.createFromString(this.itemData.getRawMaterial());
    }

    protected final void processItem(Player player) {
        if (player == null && itemData.getContext() == null) {
            processItem();
            return;
        }

        processMaterial(player);
        processName(player);
        processLore(player);
        processAmount(player);
        processEnchantments(player);
        processPotionConfig(player);
        processArmorTrim(player);
        processLeatherArmorColor(player);
        applyItemModel(player);
        applyAttributes();
        applyCustomAttributes();
        applyCustomNBT();
        applyUnbreakable();
        applyMaxStackSize();
        playClickSounds(player);
    }

    protected final void processItem() {
        processMaterial(null);
        processName(null);
        processLore(null);
        processAmount(null);
        processEnchantments(null);
        processPotionConfig(null);
        processArmorTrim(null);
        processLeatherArmorColor(null);
        applyItemModel(null);
        applyAttributes();
        applyCustomAttributes();
        applyCustomNBT();
        applyUnbreakable();
        applyMaxStackSize();
    }

    private void processMaterial(Player player) {
        String rawMaterial = itemData.getRawMaterial();
        if (rawMaterial == null) {
            return;
        }

        String processedMaterial = rawMaterial;
        if (player != null && itemData.getContext() != null) {
            processedMaterial = itemData.getContext().processPlaceholders(rawMaterial, player);
        }

        if (!processedMaterial.equals(rawMaterial)) {
            updateMaterial(processedMaterial);
        }
    }

    private void processName(Player player) {
        String rawName = itemData.getRawDisplayName() != null ? itemData.getRawDisplayName() : itemData.getRawName();
        if (rawName == null) {
            return;
        }

        String processedName = rawName;
        if (player != null && itemData.getContext() != null) {
            processedName = itemData.getContext().processPlaceholders(rawName, player);
        }

        updateName(processedName);
    }

    private void processLore(Player player) {
        List<String> currentLore = getCurrentLore();
        if (currentLore == null || currentLore.isEmpty()) {
            return;
        }

        List<net.kyori.adventure.text.Component> processedLore = new ArrayList<>();
        for (String line : currentLore) {
            String processedLine = line;
            if (player != null && itemData.getContext() != null) {
                processedLine = itemData.getContext().processPlaceholders(line, player);
            }
            processedLore.add(ColorUtils.parse(processedLine));
        }

        updateLore(processedLore);
    }

    private void processAmount(Player player) {
        String rawAmount = itemData.getRawAmount();
        if (rawAmount == null) {
            return;
        }

        String processedAmount = rawAmount;
        if (player != null && itemData.getContext() != null) {
            processedAmount = itemData.getContext().processPlaceholders(rawAmount, player);
        }

        updateAmount(processedAmount);
    }

    private void processEnchantments(Player player) {
        EnchantmentProcessor.apply(itemStack, itemData.getRawEnchantments(), player, itemData.getContext());
    }

    private void processPotionConfig(Player player) {
        PotionProcessor.apply(itemStack, itemData.getPotionConfig(), player, itemData.getContext());
    }

    private void processArmorTrim(Player player) {
        ArmorTrimProcessor.apply(itemStack, itemData.getArmorTrimConfig());
    }

    private void processLeatherArmorColor(Player player) {
        LeatherArmorProcessor.apply(itemStack, itemData.getLeatherArmorConfig(), player, itemData.getContext());
    }

    private void applyItemModel(Player player) {
        ItemModelProcessor.apply(itemStack, itemData.getRawItemModel(), player, itemData.getContext());
    }

    private void applyAttributes() {
        if (itemData.isGlowing()) {
            AttributeProcessor.applyGlowing(itemStack, true);
        }

        if (itemData.isHideAttributes()) {
            AttributeProcessor.applyHideAttributes(itemStack);
        }
    }

    private void playClickSounds(Player player) {
        if (player != null && !itemData.getClickSounds().isEmpty()) {
            for (String sound : itemData.getClickSounds()) {
                SoundUtils.playSound(player, sound);
            }
        }
    }

    protected void updateMaterial(String materialString) {
        ItemStack newStack = ItemStackUtils.createFromString(materialString);
        ItemMeta currentMeta = itemStack.getItemMeta();

        if (currentMeta != null) {
            ItemMeta newMeta = newStack.getItemMeta();
            if (newMeta != null) {
                if (currentMeta.hasDisplayName()) {
                    newMeta.displayName(currentMeta.displayName());
                }
                if (currentMeta.hasLore()) {
                    newMeta.lore(currentMeta.lore());
                }

                newMeta.addItemFlags(currentMeta.getItemFlags().toArray(new org.bukkit.inventory.ItemFlag[0]));
                currentMeta.getEnchants().forEach((enchant, level) ->
                        newMeta.addEnchant(enchant, level, true));

                newStack.setItemMeta(newMeta);
            }
        }

        newStack.setAmount(itemStack.getAmount());
        this.itemStack = newStack;
    }

    protected void updateName(String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.parse(name));
            itemStack.setItemMeta(meta);
        }
    }

    protected void updateLore(List<net.kyori.adventure.text.Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
            itemStack.setItemMeta(meta);
        }
    }

    protected void updateAmount(String amountString) {
        try {
            int amount = Integer.parseInt(amountString.trim());
            itemStack.setAmount(Math.max(1, Math.min(64, amount)));
            if (amount > 0) {
                itemStack.getItemMeta().setMaxStackSize(amount);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    protected List<String> getCurrentLore() {
        if (itemData.getLoreDynamicSupplier() != null) {
            try {
                return itemData.getLoreDynamicSupplier().get();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return itemData.getRawLore();
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    public ItemData getItemDataCopy() {
        return itemData.copy();
    }

    public abstract ItemStack buildProcessed(Player player);

    public abstract ItemStack build();

    public boolean hasDynamicContent() {
        return itemData.isDynamicUpdate() || itemData.getLoreDynamicSupplier() != null ||
               PlaceholderDetector.contains(itemData.getRawName()) ||
               PlaceholderDetector.contains(itemData.getRawDisplayName()) ||
               (itemData.getRawLore() != null && itemData.getRawLore().stream().anyMatch(PlaceholderDetector::contains));
    }

    private void applyCustomAttributes() {
        if (!itemData.getRawAttributes().isEmpty()) {
            net.exylia.commons.items.utils.AttributeManager.applyAttributes(itemStack, itemData.getRawAttributes());
        }
    }

    private void applyCustomNBT() {
        if (!itemData.getCustomNBT().isEmpty()) {
            net.exylia.commons.items.utils.NBTManager.applyCustomNBT(itemStack, itemData.getCustomNBT());
        }
    }

    public int getAmount() {
        try {
            if (itemData.getRawAmount() != null) {
                return Integer.parseInt(itemData.getRawAmount());
            }
        } catch (NumberFormatException ignored) {
        }
        return itemStack.getAmount();
    }

    private void applyUnbreakable() {
        UnbreakableProcessor.apply(itemStack, itemData.isUnbreakable());
    }

    protected void applyMaxStackSize() {
        if (itemData.getMaxStackSize() != -1) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setMaxStackSize(itemData.getMaxStackSize());
                itemStack.setItemMeta(meta);
            }
        }
    }
}
