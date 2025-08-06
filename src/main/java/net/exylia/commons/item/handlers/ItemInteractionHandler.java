package net.exylia.commons.item.handlers;

import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.ItemClickInfo;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.config.TriggerType;
import net.exylia.commons.item.cooldown.CooldownManager;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.visuals.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import static net.exylia.commons.utils.TimeFormatter.timeFormatter;

public class ItemInteractionHandler {

    private final JavaPlugin plugin;

    public ItemInteractionHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void processItemInteractionWithHand(Player player, ItemStack itemStack, InteractiveItem interactiveItem,
                                               ItemClickInfo clickInfo, EquipmentSlot hand) {

        ItemConfiguration config = interactiveItem.getConfiguration();

        // Validaciones previas
        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        // Manejar según el tipo de trigger
        TriggerType triggerType = config.getTriggerType();

        if (triggerType == TriggerType.IMMEDIATE || triggerType == TriggerType.RADIUS) {
            ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
            boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);

            if (shouldConsumeUse(interactiveItem, actionExecuted)) {
                processItemConsumption(player, itemStack, interactiveItem, hand);
            }
        }
    }

    /**
     * Procesa interacción desde inventario
     */
    public void processItemInteractionFromInventory(Player player, InventoryClickEvent event,
                                                    InteractiveItem interactiveItem, ItemClickInfo clickInfo) {

        ItemConfiguration config = interactiveItem.getConfiguration();

        // Validaciones previas
        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        TriggerType triggerType = config.getTriggerType();
        if (triggerType == TriggerType.IMMEDIATE || triggerType == TriggerType.RADIUS) {
            ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
            boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);
            if (shouldConsumeUse(interactiveItem, actionExecuted)) {
                processItemConsumptionFromInventory(event, interactiveItem);
            }
        }
    }

    public void processItemConsumptionEvent(Player player, ItemStack itemStack, InteractiveItem interactiveItem,
                                            ItemClickInfo clickInfo, EquipmentSlot hand) {
        ItemConfiguration config = interactiveItem.getConfiguration();

        if (config.getTriggerType() != TriggerType.AFTER_CONSUME) {
            return;
        }

        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
        boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);

        boolean shouldConsume = shouldConsumeUse(interactiveItem, actionExecuted);
        if (shouldConsume) {
            processItemConsumption(player, itemStack, interactiveItem, hand);
        } else {
            if (interactiveItem.shouldConsumeOnUse()) {
                ItemInventoryHandler.removeOrReduceItemByEquipmentSlot(player, itemStack, hand);
            }
        }
    }

    private void processItemConsumption(Player player, ItemStack itemStack,
                                        InteractiveItem interactiveItem, EquipmentSlot hand) {
        ItemConfiguration config = interactiveItem.getConfiguration();

        if (config.hasCooldown()) {
            double cooldownSeconds = ItemRegionHandler.getCooldownForPlayerRegion(player, config);
            setCooldown(player, interactiveItem.getId(), cooldownSeconds);
        }

        boolean hasUsesLeft = interactiveItem.consumeUse();

        boolean shouldRemoveItem = false;
        String removalReason = null;

        if (!hasUsesLeft) {
            shouldRemoveItem = true;
            removalReason = "no_uses";
        } else if (interactiveItem.shouldConsumeOnUse()) {
            shouldRemoveItem = true;
            removalReason = "consume_on_use";
        }

        if (shouldRemoveItem) {
            ItemInventoryHandler.removeOrReduceItemByEquipmentSlot(player, itemStack, hand);
            if ("no_uses".equals(removalReason)) {
                MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.consumed"));
            }
        } else {
            ItemInventoryHandler.updateItemByEquipmentSlot(player, itemStack, interactiveItem, hand);
            interactiveItem.updatePlaceholders(player, hand);
        }
    }

    public void processHitPlayer(Player player, Player hitPlayer, ItemStack itemStack,
                                 InteractiveItem interactiveItem, ItemClickInfo clickInfo, EquipmentSlot hand) {
        ItemConfiguration config = interactiveItem.getConfiguration();

        if (config.getTriggerType() != TriggerType.ON_HIT_PLAYER) {
            return;
        }

        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
        boolean actionExecuted = executeItemActionsWithHitPlayer(player, hitPlayer, interactiveItem, clickInfo);
        if (shouldConsumeUse(interactiveItem, actionExecuted)) {
            processItemConsumption(player, itemStack, interactiveItem, hand);
        }
        if (interactiveItem.shouldConsumeOnUse()) {
            ItemInventoryHandler.removeOrReduceItemByEquipmentSlot(player, itemStack, hand);
        }
    }

    public void processProjectileLaunch(Player player, ItemStack itemStack, InteractiveItem interactiveItem,
                                        ItemClickInfo clickInfo, EquipmentSlot hand) {
        ItemConfiguration config = interactiveItem.getConfiguration();

        if (config.getTriggerType() != TriggerType.ON_PROJECTILE_LAUNCH) {
            return;
        }

        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
        boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);
        if (shouldConsumeUse(interactiveItem, actionExecuted)) {
            processItemConsumption(player, itemStack, interactiveItem, hand);
        }
        if (interactiveItem.shouldConsumeOnUse()) {
            ItemInventoryHandler.removeOrReduceItemByEquipmentSlot(player, itemStack, hand);
        }
    }

    public void processProjectileHit(Player player, Player hitPlayer, ItemStack itemStack,
                                     InteractiveItem interactiveItem, ItemClickInfo clickInfo, EquipmentSlot hand) {
        ItemConfiguration config = interactiveItem.getConfiguration();

        if (config.getTriggerType() != TriggerType.ON_PROJECTILE_HIT) {
            return;
        }

        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
        boolean actionExecuted = executeItemActionsWithHitPlayer(player, hitPlayer, interactiveItem, clickInfo);
        if (shouldConsumeUse(interactiveItem, actionExecuted)) {
            processItemConsumption(player, itemStack, interactiveItem, hand);
        }
        if (interactiveItem.shouldConsumeOnUse()) {
            ItemInventoryHandler.removeOrReduceItemByEquipmentSlot(player, itemStack, hand);
        }
    }

    private boolean preValidateItemUsage(Player player, InteractiveItem interactiveItem) {
        ItemConfiguration config = interactiveItem.getConfiguration();

        if (!ItemRegionHandler.canPlayerUseItemInCurrentRegion(player, config)) {
            handleRegionDeniedMessage(player);
            return false;
        }

        if (config.hasCooldown()) {
            if (!canPlayerUseItem(player, interactiveItem.getId())) {
                double remainingSeconds = getRemainingCooldown(player, interactiveItem.getId());
                handleCooldownMessage(player, remainingSeconds);
                return false;
            }
        }

        if (!interactiveItem.hasUsesRemaining()) {
            MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.no_uses_remaining"));
            return false;
        }

        return true;
    }

    private boolean executeItemActions(Player player, InteractiveItem interactiveItem, ItemClickInfo clickInfo) {
        boolean actionExecuted = false;

        if (interactiveItem.hasAction()) {
            actionExecuted = interactiveItem.executeAction(clickInfo);
        }

        if (!actionExecuted && !interactiveItem.getCommands().isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> interactiveItem.executeCommands(player));
        }

        if (interactiveItem.getClickHandler() != null) {
            interactiveItem.getClickHandler().accept(clickInfo);
        }

        return actionExecuted;
    }

    private boolean executeItemActionsWithHitPlayer(Player player, Player hitPlayer, InteractiveItem interactiveItem, ItemClickInfo clickInfo) {
        boolean actionExecuted = false;

        if (interactiveItem.hasAction()) {
            clickInfo.withData("hitPlayer", hitPlayer); // Añadir jugador golpeado al contexto
            actionExecuted = interactiveItem.executeAction(clickInfo);
        }

        if (!actionExecuted && !interactiveItem.getCommands().isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> interactiveItem.executeCommands(player));
        }

        if (interactiveItem.getClickHandler() != null) {
            interactiveItem.getClickHandler().accept(clickInfo);
        }

        return actionExecuted;
    }

    private boolean shouldConsumeUse(InteractiveItem interactiveItem, boolean actionExecuted) {
        return actionExecuted || !interactiveItem.getCommands().isEmpty() ||
                interactiveItem.getClickHandler() != null;
    }

    private void processItemConsumptionFromInventory(InventoryClickEvent event, InteractiveItem interactiveItem) {
        Player player = (Player) event.getWhoClicked();

        ItemConfiguration config = interactiveItem.getConfiguration();
        if (config.hasCooldown()) {
            double cooldownSeconds = ItemRegionHandler.getCooldownForPlayerRegion(player, config);
            setCooldown(player, interactiveItem.getId(), cooldownSeconds);
        }

        boolean hasUsesLeft = interactiveItem.consumeUse();

        boolean shouldRemoveItem = false;
        String removalReason = null;

        if (!hasUsesLeft) {
            shouldRemoveItem = true;
            removalReason = "no_uses";
        } else if (interactiveItem.shouldConsumeOnUse()) {
            shouldRemoveItem = true;
            removalReason = "consume_on_use";
        }

        if (shouldRemoveItem) {
            ItemInventoryHandler.removeOrReduceItemFromInventory(event);

            if ("no_uses".equals(removalReason)) {
                MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.consumed"));
            }
        } else {
            ItemInventoryHandler.updateItemInInventory(event, interactiveItem);
        }
    }

    private void setCooldownForRegion(Player player, InteractiveItem interactiveItem) {
        ItemConfiguration config = interactiveItem.getConfiguration();
        if (config.hasCooldown()) {
            double cooldownSeconds = ItemRegionHandler.getCooldownForPlayerRegion(player, config);
            setCooldown(player, interactiveItem.getId(), cooldownSeconds);
        }
    }

    public static boolean canPlayerUseItem(Player player, String itemId) {
        if (!CooldownManager.isInitialized()) {
            return true;
        }
        return !CooldownManager.getInstance().hasCooldown(player, itemId);
    }

    public static void setCooldown(Player player, String itemId, double seconds) {
        if (CooldownManager.isInitialized()) {
            CooldownManager.getInstance().setCooldown(player, itemId, seconds);
        }
    }

    public static double getRemainingCooldown(Player player, String itemId) {
        if (!CooldownManager.isInitialized()) {
            return 0.0;
        }
        return CooldownManager.getInstance().getRemainingCooldown(player, itemId);
    }

    public static void removeCooldown(Player player, String itemId) {
        if (CooldownManager.isInitialized()) {
            CooldownManager.getInstance().removeCooldown(player, itemId);
        }
    }

    public void handleCooldownMessage(Player player, double remainingSeconds) {
        String formattedTime = timeFormatter.format(remainingSeconds);
        MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.in_cooldown",
                "%cooldown_formatted%", formattedTime,
                "%cooldown_seconds%", String.valueOf(remainingSeconds)));
    }

    private void handleRegionDeniedMessage(Player player) {
        MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.region_denied"));
    }
}