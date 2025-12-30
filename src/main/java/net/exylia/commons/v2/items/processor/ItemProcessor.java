package net.exylia.commons.v2.items.processor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.items.api.ProcessedItem;
import net.exylia.commons.v2.items.exception.ItemValidationException;
import net.exylia.commons.v2.items.model.ItemData;
import net.exylia.commons.v2.items.skull.SkullParser;
import net.exylia.commons.v2.items.utils.ItemStackUtils;
import net.exylia.commons.v2.items.utils.PlaceholderDetector;
import net.exylia.commons.v2.items.validation.ItemValidator;
import net.exylia.commons.v2.placeholders.Placeholders;
import net.exylia.commons.v2.visual.api.ColorAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemProcessor {

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

            return processMaterialAsync(itemData, player)
                .thenCompose(itemStack -> processAllFieldsAsync(itemStack, itemData, player))
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

        ItemStack itemStack = processMaterial(itemData, player);
        return processAllFields(itemStack, itemData, player);
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

    private static ProcessedItem processAllFields(ItemStack itemStack, ItemData itemData, Player player) {
        processNameAndLore(itemStack, itemData, player);
        processAmount(itemStack, itemData, player);
        processEnchantments(itemStack, itemData, player);
        processPotionConfig(itemStack, itemData, player);
        processArmorTrim(itemStack, itemData);
        processLeatherArmorColor(itemStack, itemData, player);
        processItemModel(itemStack, itemData, player);
        processAttributes(itemStack, itemData);
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
            .build();
    }

    private static void processNameAndLore(ItemStack itemStack, ItemData itemData, Player player) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        String rawName = itemData.getRawDisplayName() != null ?
                        itemData.getRawDisplayName() : itemData.getRawName();

        if (rawName != null) {
            String processedName = Placeholders.process(rawName, player, itemData.getContext());
            meta.displayName(ColorAPI.parse(processedName));
        }

        List<String> rawLore = itemData.getLoreDynamicSupplier() != null ?
                              itemData.getLoreDynamicSupplier().get() : itemData.getRawLore();

        if (rawLore != null && !rawLore.isEmpty()) {
            List<net.kyori.adventure.text.Component> processedLore = rawLore.stream()
                .map(line -> Placeholders.process(line, player, itemData.getContext()))
                .map(ColorAPI::parse)
                .collect(Collectors.toList());
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
            itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        } catch (NumberFormatException ignored) {
        }
    }

    private static void processEnchantments(ItemStack itemStack, ItemData itemData, Player player) {
        EnchantmentProcessor.apply(itemStack, itemData.getRawEnchantments(), player, itemData.getContext());
    }

    private static void processPotionConfig(ItemStack itemStack, ItemData itemData, Player player) {
        PotionProcessor.apply(itemStack, itemData.getPotionConfig(), player, itemData.getContext());
    }

    private static void processArmorTrim(ItemStack itemStack, ItemData itemData) {
        ArmorTrimProcessor.apply(itemStack, itemData.getArmorTrimConfig());
    }

    private static void processLeatherArmorColor(ItemStack itemStack, ItemData itemData, Player player) {
        LeatherArmorProcessor.apply(itemStack, itemData.getLeatherArmorConfig(), player, itemData.getContext());
    }

    private static void processItemModel(ItemStack itemStack, ItemData itemData, Player player) {
        ItemModelProcessor.apply(itemStack, itemData.getRawItemModel(), player, itemData.getContext());
    }

    private static void processAttributes(ItemStack itemStack, ItemData itemData) {
        if (itemData.isGlowing()) {
            AttributeProcessor.applyGlowing(itemStack, true);
        }
        if (itemData.isHideAttributes()) {
            AttributeProcessor.applyHideAttributes(itemStack);
        }
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
        if (itemData.getMaxStackSize() != -1) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setMaxStackSize(itemData.getMaxStackSize());
                itemStack.setItemMeta(meta);
            }
        }
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
