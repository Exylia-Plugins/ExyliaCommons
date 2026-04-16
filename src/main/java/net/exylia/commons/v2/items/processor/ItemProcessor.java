package net.exylia.commons.v2.items.processor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.items.config.BannerConfig;
import net.exylia.commons.v2.items.exception.ItemValidationException;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.skull.SkullParser;
import net.exylia.commons.v2.items.snapshot.ItemSnapshot;
import net.exylia.commons.v2.items.utils.ItemStackUtils;
import net.exylia.commons.v2.items.utils.PlaceholderDetector;
import net.exylia.commons.v2.items.validation.ItemValidator;
import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemProcessor {

    private static Boolean hideTooltipAvailable = null;
    private static Method setHideTooltipMethod = null;
    private static Boolean maxStackSizeAvailable = null;
    private static Method setMaxStackSizeMethod = null;

    public static CompletableFuture<ProcessedItem> processAsync(ItemData itemData, Player player) {
        return processAsync(itemData, player, true);
    }

    public static CompletableFuture<ProcessedItem> processAsync(ItemData itemData, Player player, boolean validate) {
        return CompletableFuture.supplyAsync(() -> {
            if (validate) {
                try {
                    ItemValidator.validate(itemData);
                } catch (ItemValidationException e) {
                    throw new RuntimeException("Item validation failed", e);
                }
            }

            if (itemData.getItemStack() != null || itemData.getItemStackSupplier() != null) {
                ItemStack itemStack = itemData.getItemStack() != null ?
                        itemData.getItemStack().clone() :
                        itemData.getItemStackSupplier().get();
                if (itemStack == null || itemStack.getType().isAir()) {
                    itemStack = new ItemStack(Material.AIR);
                }
                return processDirectItemStack(itemStack, itemData, player);
            }

            final ItemData resolvedData = applySnapshotIfNeeded(itemData, player);
            return processMaterialAsync(resolvedData, player)
                .thenCompose(itemStack -> processAllFieldsAsync(itemStack, resolvedData, player))
                .join();
        });
    }

    public static ProcessedItem process(ItemData itemData, Player player) {
        return process(itemData, player, true);
    }

    public static ProcessedItem process(ItemData itemData, Player player, boolean validate) {
        if (validate) {
            try {
                ItemValidator.validate(itemData);
            } catch (ItemValidationException e) {
                throw new RuntimeException("Item validation failed", e);
            }
        }

        if (itemData.getItemStack() != null || itemData.getItemStackSupplier() != null) {
            ItemStack itemStack = itemData.getItemStack() != null ?
                    itemData.getItemStack().clone() :
                    itemData.getItemStackSupplier().get();
            if (itemStack == null || itemStack.getType().isAir()) {
                itemStack = new ItemStack(Material.AIR);
            }
            return processDirectItemStack(itemStack, itemData, player);
        }

        ItemData resolvedData = applySnapshotIfNeeded(itemData, player);
        ItemStack itemStack = processMaterial(resolvedData, player);
        return processAllFields(itemStack, resolvedData, player);
    }

    private static ItemData applySnapshotIfNeeded(ItemData itemData, Player player) {
        String processedMaterial = Placeholders.process(itemData.getRawMaterial(), player, itemData.getContext());
        if (!processedMaterial.startsWith("item:")) return itemData;

        ItemData snapshotData = ItemSnapshot.from(processedMaterial).toItemData();
        ItemData.ItemDataBuilder builder = itemData.toBuilder();
        builder.rawMaterial(snapshotData.getRawMaterial());

        if (itemData.getPotionConfig() == null && snapshotData.getPotionConfig() != null)
            builder.potionConfig(snapshotData.getPotionConfig());
        if (itemData.getLeatherArmorConfig() == null && snapshotData.getLeatherArmorConfig() != null)
            builder.leatherArmorConfig(snapshotData.getLeatherArmorConfig());
        if (itemData.getArmorTrimConfig() == null && snapshotData.getArmorTrimConfig() != null)
            builder.armorTrimConfig(snapshotData.getArmorTrimConfig());
        if (itemData.getBannerConfig() == null && snapshotData.getBannerConfig() != null)
            builder.bannerConfig(snapshotData.getBannerConfig());
        if (!itemData.isGlowing() && snapshotData.isGlowing())
            builder.glowing(true);

        return builder.build();
    }

    private static CompletableFuture<ItemStack> processMaterialAsync(ItemData itemData, Player player) {
        return CompletableFuture.supplyAsync(() -> {
            String rawMaterial = itemData.getRawMaterial();
            String processedMaterial = Placeholders.process(rawMaterial, player, itemData.getContext());

            if (SkullParser.isSkullString(processedMaterial)) {
                return processedMaterial;
            } else {
                return processedMaterial;
            }
        }).thenCompose(processedMaterial -> {
            if (SkullParser.isSkullString(processedMaterial)) {
                return SkullParser.parseAsync(processedMaterial, player, itemData.getContext());
            } else {
                return CompletableFuture.completedFuture(
                    ItemStackUtils.createFromString(processedMaterial)
                );
            }
        });
    }

    private static ItemStack processMaterial(ItemData itemData, Player player) {
        String rawMaterial = itemData.getRawMaterial();
        String processedMaterial = Placeholders.process(rawMaterial, player, itemData.getContext());

        if (SkullParser.isSkullString(processedMaterial)) {
            return SkullParser.parse(processedMaterial, player, itemData.getContext());
        }

        return ItemStackUtils.createFromString(processedMaterial);
    }

    private static CompletableFuture<ProcessedItem> processAllFieldsAsync(ItemStack itemStack,
                                                                           ItemData itemData,
                                                                           Player player) {
        return CompletableFuture.supplyAsync(() -> processAllFields(itemStack, itemData, player));
    }

    private static ProcessedItem processDirectItemStack(ItemStack itemStack, ItemData itemData, Player player) {
        Integer slot = processSlot(itemData, player);
        List<Integer> slots = processSlots(itemData, player);

        boolean isDynamic = itemData.getItemStackSupplier() != null || itemData.isDynamicUpdate();

        return ProcessedItem.builder()
            .itemStack(itemStack)
            .slot(slot)
            .slots(slots)
            .actions(itemData.getActions())
            .commands(itemData.getCommands())
            .rawItemData(itemData.copy())
            .hasDynamicContent(isDynamic)
            .requiresTarget(itemData.isRequiresTarget())
            .build();
    }

    private static ProcessedItem processAllFields(ItemStack itemStack, ItemData itemData, Player player) {
        processNameAndLore(itemStack, itemData, player);
        processAmount(itemStack, itemData, player);
        processEnchantments(itemStack, itemData, player);
        processPotionConfig(itemStack, itemData, player);
        processArmorTrim(itemStack, itemData, player);
        processLeatherArmorColor(itemStack, itemData, player);
        processBannerConfig(itemStack, itemData, player);
        processItemModel(itemStack, itemData, player);
        processTooltipStyle(itemStack, itemData, player);
        processAttributes(itemStack, itemData);
        processHideTooltip(itemStack, itemData);
        processCustomAttributes(itemStack, itemData);
        processCustomNBT(itemStack, itemData);
        processUnbreakable(itemStack, itemData);
        processMaxStackSize(itemStack, itemData);

        Integer slot = processSlot(itemData, player);
        List<Integer> slots = processSlots(itemData, player);

        return ProcessedItem.builder()
            .itemStack(itemStack)
            .slot(slot)
            .slots(slots)
            .actions(itemData.getActions())
            .commands(itemData.getCommands())
            .rawItemData(itemData.copy())
            .hasDynamicContent(hasDynamicContent(itemData))
            .requiresTarget(itemData.isRequiresTarget())
            .build();
    }

    private static void processNameAndLore(ItemStack itemStack, ItemData itemData, Player player) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        String rawName = itemData.getRawName() != null ?
                        itemData.getRawName() : itemData.getRawDisplayName();

        if (rawName != null) {
            String processedName = Placeholders.process(rawName, player, itemData.getContext());
            meta.displayName(ColorAPI.parse(processedName));
        }

        List<String> rawLore = itemData.getLoreDynamicSupplier() != null ?
                              itemData.getLoreDynamicSupplier().get() : itemData.getRawLore();

        if (rawLore != null && !rawLore.isEmpty()) {
            List<net.kyori.adventure.text.Component> processedLore = new ArrayList<>();
            for (String line : rawLore) {
                String processed = Placeholders.process(line, player, itemData.getContext());
                String normalized = processed.replace("\\n", "\n");
                if (normalized.contains("\n")) {
                    for (String subLine : normalized.split("\n", -1)) {
                        processedLore.add(ColorAPI.parse(subLine));
                    }
                } else {
                    processedLore.add(ColorAPI.parse(normalized));
                }
            }
            meta.lore(processedLore);
        }

        itemStack.setItemMeta(meta);
    }

    private static void processAmount(ItemStack itemStack, ItemData itemData, Player player) {
        String rawAmount = itemData.getRawAmount();
        if (rawAmount == null) return;

        String processedAmount = Placeholders.process(rawAmount, player, itemData.getContext());

        try {
            int amount = Integer.parseInt(processedAmount.trim());
            amount = Math.max(1, Math.min(99, amount));
            itemStack.setAmount(amount);

            if (amount > 1 && itemData.getMaxStackSize() == -1 && isMaxStackSizeAvailable()) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    try {
                        setMaxStackSizeMethod.invoke(meta, amount);
                        itemStack.setItemMeta(meta);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static void processEnchantments(ItemStack itemStack, ItemData itemData, Player player) {
        EnchantmentProcessor.apply(itemStack, itemData.getRawEnchantments(), player, itemData.getContext());
    }

    private static void processPotionConfig(ItemStack itemStack, ItemData itemData, Player player) {
        PotionProcessor.apply(itemStack, itemData.getPotionConfig(), player, itemData.getContext());
    }

    private static void processArmorTrim(ItemStack itemStack, ItemData itemData, Player player) {
        ArmorTrimProcessor.apply(itemStack, itemData.getArmorTrimConfig(), player, itemData.getContext());
    }

    private static void processLeatherArmorColor(ItemStack itemStack, ItemData itemData, Player player) {
        LeatherArmorProcessor.apply(itemStack, itemData.getLeatherArmorConfig(), player, itemData.getContext());
    }

    private static void processBannerConfig(ItemStack itemStack, ItemData itemData, Player player) {
        BannerConfig bannerConfig = itemData.getBannerConfig();

        if (bannerConfig == null && itemData.getRawBannerDesign() != null) {
            String resolved = Placeholders.process(itemData.getRawBannerDesign(), player, itemData.getContext());
            bannerConfig = BannerConfig.fromBase64(resolved);
        }

        BannerProcessor.apply(itemStack, bannerConfig, player, itemData.getContext());
    }

    private static void processItemModel(ItemStack itemStack, ItemData itemData, Player player) {
        ItemModelProcessor.apply(itemStack, itemData.getRawItemModel(), player, itemData.getContext());
    }

    private static void processTooltipStyle(ItemStack itemStack, ItemData itemData, Player player) {
        TooltipStyleProcessor.apply(itemStack, itemData.getRawTooltipStyle(), player, itemData.getContext());
    }

    private static void processAttributes(ItemStack itemStack, ItemData itemData) {
        if (itemData.isGlowing()) {
            AttributeProcessor.applyGlowing(itemStack, true);
        }
        if (itemData.isHideAttributes()) {
            AttributeProcessor.applyHideAttributes(itemStack);
        }
    }

    private static void processHideTooltip(ItemStack itemStack, ItemData itemData) {
        if (!itemData.isHideTooltip() || !isHideTooltipAvailable()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            try {
                setHideTooltipMethod.invoke(meta, true);
                itemStack.setItemMeta(meta);
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isHideTooltipAvailable() {
        if (hideTooltipAvailable != null) {
            return hideTooltipAvailable;
        }
        try {
            setHideTooltipMethod = ItemMeta.class.getMethod("setHideTooltip", boolean.class);
            hideTooltipAvailable = true;
        } catch (NoSuchMethodException e) {
            hideTooltipAvailable = false;
        }
        return hideTooltipAvailable;
    }

    private static void processCustomAttributes(ItemStack itemStack, ItemData itemData) {
        if (!itemData.getRawAttributes().isEmpty()) {
            net.exylia.commons.v2.items.utils.AttributeManager.applyAttributes(
                itemStack, itemData.getRawAttributes()
            );
        }
    }

    private static void processCustomNBT(ItemStack itemStack, ItemData itemData) {
        if (!itemData.getCustomNBT().isEmpty()) {
            net.exylia.commons.v2.items.utils.NBTManager.applyCustomNBT(
                itemStack, itemData.getCustomNBT()
            );
        }
    }

    private static void processUnbreakable(ItemStack itemStack, ItemData itemData) {
        UnbreakableProcessor.apply(itemStack, itemData.isUnbreakable());
    }

    private static void processMaxStackSize(ItemStack itemStack, ItemData itemData) {
        if (itemData.getMaxStackSize() == -1 || !isMaxStackSizeAvailable()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            try {
                setMaxStackSizeMethod.invoke(meta, itemData.getMaxStackSize());
                itemStack.setItemMeta(meta);
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isMaxStackSizeAvailable() {
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

    private static Integer processSlot(ItemData itemData, Player player) {
        if (itemData.getSlotConfig() == null || !itemData.getSlotConfig().isSingle()) {
            return null;
        }

        String rawSlot = itemData.getSlotConfig().getRawSlot();
        if (rawSlot == null) {
            try {
                return itemData.getSlotConfig().getSingleSlot();
            } catch (ItemValidationException e) {
                return null;
            }
        }

        String processedSlot = Placeholders.process(rawSlot, player, itemData.getContext());
        try {
            return Integer.parseInt(processedSlot.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<Integer> processSlots(ItemData itemData, Player player) {
        if (itemData.getSlotConfig() == null || !itemData.getSlotConfig().isMultiple()) {
            return null;
        }

        List<String> rawSlots = itemData.getSlotConfig().getRawSlots();
        if (rawSlots == null) {
            try {
                return itemData.getSlotConfig().getMultiSlots();
            } catch (ItemValidationException e) {
                return null;
            }
        }

        List<Integer> processedSlots = new ArrayList<>();
        for (String rawSlot : rawSlots) {
            String processed = Placeholders.process(rawSlot, player, itemData.getContext());
            try {
                processedSlots.add(Integer.parseInt(processed.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        return processedSlots.isEmpty() ? null : processedSlots;
    }

    private static boolean hasDynamicContent(ItemData itemData) {
        if (itemData.isDynamicUpdate() || itemData.getLoreDynamicSupplier() != null) {
            return true;
        }

        if (PlaceholderDetector.contains(itemData.getRawMaterial()) ||
            PlaceholderDetector.contains(itemData.getRawAmount()) ||
            PlaceholderDetector.contains(itemData.getRawName()) ||
            PlaceholderDetector.contains(itemData.getRawDisplayName())) {
            return true;
        }

        if (itemData.getRawLore() != null) {
            for (String line : itemData.getRawLore()) {
                if (PlaceholderDetector.contains(line)) {
                    return true;
                }
            }
        }

        if (itemData.getSlotConfig() != null && itemData.getSlotConfig().hasPlaceholders()) {
            return true;
        }

        return false;
    }
}
