package net.exylia.commons.v2.items.model;

import lombok.Getter;
import net.exylia.commons.v2.items.processor.*;
import net.exylia.commons.v2.items.utils.ItemStackUtils;
import net.exylia.commons.v2.items.utils.PlaceholderDetector;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.exylia.commons.utils.effects.SoundUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class ExyliaItem {

    protected static Boolean maxStackSizeAvailable = null;
    protected static Method setMaxStackSizeMethod = null;

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
        applyTooltipStyle(player);
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
        applyTooltipStyle(null);
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

        String processedMaterial = processPlaceholdersWithContext(rawMaterial, player);

        if (!processedMaterial.equals(rawMaterial)) {
            updateMaterial(processedMaterial);
        }
    }

    private void processName(Player player) {
        String rawName = itemData.getRawName() != null ? itemData.getRawName() : itemData.getRawDisplayName();
        if (rawName == null) {
            return;
        }

        String processedName = processPlaceholdersWithContext(rawName, player);

        updateName(processedName);
    }

    private void processLore(Player player) {
        List<String> currentLore = getCurrentLore();
        if (currentLore == null || currentLore.isEmpty()) {
            return;
        }

        List<net.kyori.adventure.text.Component> processedLore = new ArrayList<>();
        for (String line : currentLore) {
            String processedLine = processPlaceholdersWithContext(line, player);
            processedLore.add(ColorAPI.parse(processedLine));
        }

        updateLore(processedLore);
    }

    private void processAmount(Player player) {
        String rawAmount = itemData.getRawAmount();
        if (rawAmount == null) {
            return;
        }

        String processedAmount = processPlaceholdersWithContext(rawAmount, player);

        updateAmount(processedAmount);
    }

    private String processPlaceholdersWithContext(String text, Player player) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        if (player != null && itemData.getContext() != null) {
            try {
                net.exylia.commons.placeholders.ExyliaContext exyliaContext = itemData.getContext().toExyliaContext();
                if (player != null) {
                    exyliaContext.withPlayer(player);
                }
                result = exyliaContext.processPlaceholders(result, player);
            } catch (Exception e) {
                result = Placeholders.process(text, player, itemData.getContext());
            }
        } else if (player != null) {
            result = Placeholders.process(text, player, itemData.getContext());
        }

        return result;
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

    private void applyTooltipStyle(Player player) {
        TooltipStyleProcessor.apply(itemStack, itemData.getRawTooltipStyle(), player, itemData.getContext());
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
            meta.displayName(ColorAPI.parse(name));
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
            int clampedAmount = Math.max(1, Math.min(99, amount));
            itemStack.setAmount(clampedAmount);
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
            net.exylia.commons.v2.items.utils.AttributeManager.applyAttributes(itemStack, itemData.getRawAttributes());
        }
    }

    private void applyCustomNBT() {
        if (!itemData.getCustomNBT().isEmpty()) {
            net.exylia.commons.v2.items.utils.NBTManager.applyCustomNBT(itemStack, itemData.getCustomNBT());
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
        if (!isMaxStackSizeAvailable()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            int targetMaxStackSize;

            if (itemData.getMaxStackSize() != -1) {
                targetMaxStackSize = itemData.getMaxStackSize();
            } else {
                targetMaxStackSize = itemStack.getAmount();
            }

            int clampedMaxStackSize = Math.max(1, Math.min(99, targetMaxStackSize));
            try {
                setMaxStackSizeMethod.invoke(meta, clampedMaxStackSize);
                itemStack.setItemMeta(meta);
            } catch (Exception ignored) {
            }
        }
    }

    protected static boolean isMaxStackSizeAvailable() {
        if (maxStackSizeAvailable != null) {
            return maxStackSizeAvailable;
        }
        try {
            setMaxStackSizeMethod = ItemMeta.class.getMethod("setMaxStackSize", Integer.class);
            maxStackSizeAvailable = true;
        } catch (NoSuchMethodException e) {
            maxStackSizeAvailable = false;
        }
        return maxStackSizeAvailable;
    }
}
