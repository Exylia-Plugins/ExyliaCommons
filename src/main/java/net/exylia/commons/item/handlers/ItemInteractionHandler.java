package net.exylia.commons.item.handlers;

import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.ItemClickInfo;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.config.TriggerType;
import net.exylia.commons.item.cooldown.CooldownManager;
import net.exylia.commons.item.vanilla.VanillaItemCooldownManager;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.commons.utils.visuals.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import static net.exylia.commons.config.base.MainConfigBase.debug;
import static net.exylia.commons.utils.TimeFormatter.timeFormatter;

public class ItemInteractionHandler {

    private final JavaPlugin plugin;

    public ItemInteractionHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void processItemInteractionWithHand(Player player, ItemStack itemStack, InteractiveItem interactiveItem,
                                               ItemClickInfo clickInfo, EquipmentSlot hand) {

        ItemConfiguration config = interactiveItem.getConfiguration();

        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        TriggerType triggerType = config.getTriggerType();

        if (triggerType == TriggerType.IMMEDIATE || triggerType == TriggerType.RADIUS) {
            boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);
            if (actionExecuted || !interactiveItem.hasAction()) {
                ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
            }
            if (shouldConsumeUse(interactiveItem, actionExecuted)) {
                processItemConsumption(player, itemStack, interactiveItem, hand);
            }
        }
    }

    public void processItemInteractionFromInventory(Player player, InventoryClickEvent event,
                                                    InteractiveItem interactiveItem, ItemClickInfo clickInfo) {

        ItemConfiguration config = interactiveItem.getConfiguration();

        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        TriggerType triggerType = config.getTriggerType();
        if (triggerType == TriggerType.IMMEDIATE || triggerType == TriggerType.RADIUS) {
            boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);
            if (actionExecuted || !interactiveItem.hasAction()) {
                ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
            }
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

        boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);
        if (actionExecuted || !interactiveItem.hasAction()) {
            ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
        }
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
            String effectiveId = interactiveItem.getEffectiveId();
            setCooldown(player, effectiveId, cooldownSeconds);
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

        if (!ItemRegionHandler.canPlayerUseItemInCurrentRegion(hitPlayer, config)) {
            handleRegionDeniedMessage(player);
            return;
        }

        boolean actionExecuted = executeItemActionsWithHitPlayer(player, hitPlayer, interactiveItem, clickInfo);

        if (actionExecuted || !interactiveItem.hasAction()) {
            ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
        }

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

        if (config.getTriggerType() != TriggerType.ON_PROJECTILE_LAUNCH &&
                config.getTriggerType() != TriggerType.ON_PROJECTILE_HIT) {
            return;
        }

        if (!preValidateItemUsage(player, interactiveItem)) {
            return;
        }

        DebugUtils.logInternalDebug("Processing projectile launch for " + player.getName());

        if (config.getTriggerType() == TriggerType.ON_PROJECTILE_LAUNCH) {
            ItemEffectsHandler.executeEffects(player, player.getLocation(), config);
            boolean actionExecuted = executeItemActions(player, interactiveItem, clickInfo);
        }

        if (config.hasCooldown()) {
            double cooldownSeconds = ItemRegionHandler.getCooldownForPlayerRegion(player, config);
            String effectiveId = interactiveItem.getEffectiveId();
            setCooldown(player, effectiveId, cooldownSeconds);

            if (interactiveItem.hasForceId()) {
                Material material = itemStack.getType();

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setCooldown(material, (int) (cooldownSeconds * 20));
                }, 1);

                DebugUtils.logInternalDebug(
                        "Force-ID immediate override: Material " + material + " -> " + effectiveId +
                                " with cooldown " + cooldownSeconds + "s for player " + player.getName() +
                                " (vanilla cooldown prevented)");
            }
        }

        boolean hasUsesLeft = interactiveItem.consumeUse();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (hasUsesLeft) {
                ItemStack updatedItem = interactiveItem.getItemStack();
                updatedItem.setAmount(1);

                interactiveItem.placeholderPlayer(player);
                interactiveItem.updatePlaceholders(player, null);

                updatedItem = interactiveItem.getItemStack();
                updatedItem.setAmount(1);

                if (hand == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(updatedItem);
                } else if (hand == EquipmentSlot.OFF_HAND) {
                    player.getInventory().setItemInOffHand(updatedItem);
                }

                DebugUtils.logInternalDebug(
                        "Returned updated item to inventory for " + player.getName() +
                                " with " + interactiveItem.getCurrentUses() + " uses remaining");
            } else {
                if (hand == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(null);
                } else if (hand == EquipmentSlot.OFF_HAND) {
                    player.getInventory().setItemInOffHand(null);
                }
                MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.consumed"));
                DebugUtils.logInternalDebug(
                        "Item completely consumed for " + player.getName() + " - removed from inventory");
            }
        });
    }

    public void processProjectileHit(Player player, Player hitPlayer, ItemStack itemStack,
                                     InteractiveItem interactiveItem, ItemClickInfo clickInfo, EquipmentSlot hand) {
        ItemConfiguration config = interactiveItem.getConfiguration();

        if (config.getTriggerType() != TriggerType.ON_PROJECTILE_HIT) {
            return;
        }

        if (!ItemRegionHandler.canPlayerUseItemInCurrentRegion(player, config)) {
            handleRegionDeniedMessage(player);
            return;
        }

        if (hitPlayer != null && !ItemRegionHandler.canPlayerUseItemInCurrentRegion(hitPlayer, config)) {
            handleRegionDeniedMessage(player);
            return;
        }

        DebugUtils.logInternalDebug("Processing projectile hit for " + player.getName() +
                (hitPlayer != null ? " hitting " + hitPlayer.getName() : " hitting block/entity"));

        Location hitLocation = hitPlayer != null ? hitPlayer.getLocation() : player.getLocation();

        boolean actionExecuted;
        if (hitPlayer != null) {
            actionExecuted = executeItemActionsWithHitPlayer(player, hitPlayer, interactiveItem, clickInfo);
        } else {
            actionExecuted = executeItemActions(player, interactiveItem, clickInfo);
        }

        if (actionExecuted || !interactiveItem.hasAction()) {
            ItemEffectsHandler.executeEffects(player, hitLocation, config);
        }

        DebugUtils.logInternalDebug(
                "Projectile hit action executed: " + actionExecuted + " for " + player.getName());
    }

    private boolean preValidateItemUsage(Player player, InteractiveItem interactiveItem) {
        ItemConfiguration config = interactiveItem.getConfiguration();

        if (!ItemRegionHandler.canPlayerUseItemInCurrentRegion(player, config)) {
            handleRegionDeniedMessage(player);
            return false;
        }

        if (config.hasCooldown()) {
            String effectiveId = interactiveItem.getEffectiveId();
            if (!canPlayerUseItem(player, effectiveId)) {
                double remainingSeconds = getRemainingCooldown(player, effectiveId);
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

        if (interactiveItem.clickHandler() != null) {
            interactiveItem.clickHandler().accept(clickInfo);
        }

        return actionExecuted;
    }

    private boolean executeItemActionsWithHitPlayer(Player player, Player hitPlayer, InteractiveItem interactiveItem, ItemClickInfo clickInfo) {
        boolean actionExecuted = false;

        if (interactiveItem.hasAction()) {
            clickInfo.withData("hitPlayer", hitPlayer);
            actionExecuted = interactiveItem.executeAction(clickInfo);
        }

        if (!actionExecuted && !interactiveItem.getCommands().isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> interactiveItem.executeCommands(player));
        }

        if (interactiveItem.clickHandler() != null) {
            interactiveItem.clickHandler().accept(clickInfo);
        }

        return actionExecuted;
    }

    private boolean shouldConsumeUse(InteractiveItem interactiveItem, boolean actionExecuted) {
        return actionExecuted || !interactiveItem.getCommands().isEmpty() ||
                interactiveItem.clickHandler() != null;
    }

    private void processItemConsumptionFromInventory(InventoryClickEvent event, InteractiveItem interactiveItem) {
        Player player = (Player) event.getWhoClicked();

        ItemConfiguration config = interactiveItem.getConfiguration();
        if (config.hasCooldown()) {
            double cooldownSeconds = ItemRegionHandler.getCooldownForPlayerRegion(player, config);
            String effectiveId = interactiveItem.getEffectiveId();
            setCooldown(player, effectiveId, cooldownSeconds);
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
            String effectiveId = interactiveItem.getEffectiveId();
            setCooldown(player, effectiveId, cooldownSeconds);
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

    public void handleRegionDeniedMessage(Player player) {
        MessageUtils.sendMessageAsync(player, MessagesBase.get("system.items.region_denied"));
    }
}