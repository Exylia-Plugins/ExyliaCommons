package net.exylia.commons.item.handlers;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.actions.ActionContext;
import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.actions.GlobalActionManager;
import net.exylia.commons.item.InteractiveItem;
import net.exylia.commons.item.ItemClickInfo;
import net.exylia.commons.item.config.ItemConfiguration;
import net.exylia.commons.item.config.TriggerType;
import net.exylia.commons.item.exceptions.ItemException;
import net.exylia.commons.item.exceptions.ItemHoldSessionException;
import net.exylia.commons.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.exylia.commons.config.base.MainConfigBase.debug;

public class ItemHoldHandler {
    
    private final JavaPlugin plugin;
    private final Map<String, HoldSession> activeSessions = new HashMap<>();
    
    public ItemHoldHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void startHoldSession(Player player, InteractiveItem interactiveItem, EquipmentSlot hand) {
        ItemConfiguration config = interactiveItem.getConfiguration();
        
        if (config.getTriggerType() != TriggerType.HOLD) {
            return;
        }
        
        String sessionKey = generateSessionKey(player, hand);
        
        // Check if there's already an active session for the same item
        HoldSession existingSession = activeSessions.get(sessionKey);
        if (existingSession != null) {
            String existingItemId = existingSession.getInteractiveItem().getEffectiveId();
            String newItemId = interactiveItem.getEffectiveId();
            if (existingItemId.equals(newItemId)) {
                DebugUtils.logInternalDebug(debug(), "HOLD session already active for " + player.getName() + 
                        " with same item " + newItemId + ", keeping existing session");
                return;
            } else {
                // Different item, stop the existing session
                DebugUtils.logInternalDebug(debug(), "Stopping existing HOLD session for different item. Old: " + 
                        existingItemId + ", New: " + newItemId);
                stopHoldSession(player, hand);
            }
        }
        
        // Get configuration
        int intervalTicks = config.getActionConfigInt("hold-interval", 20); // Default 1 second
        String allowedHand = config.getActionConfigString("hold-hand", "ANY"); // ANY, MAIN, OFF
        
        // Check if the item is in the correct hand
        if (!isItemInCorrectHand(player, interactiveItem, hand, allowedHand)) {
            return;
        }
        
        DebugUtils.logInternalDebug(debug(), "Starting HOLD session for " + player.getName() + 
                " with item " + interactiveItem.getId() + " in " + hand + " hand");
        
        // Create hold session
        HoldSession session = new HoldSession(player, interactiveItem, hand, allowedHand);
        activeSessions.put(sessionKey, session);
        
        // Start periodic task (first execution is immediate, then follows interval)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isSessionValid(session)) {
                stopHoldSession(player, hand);
                return;
            }
            
            executeHoldAction(session);
        }, 0, intervalTicks);
        
        session.setTask(task);
    }
    
    public void stopHoldSession(Player player, EquipmentSlot hand) {
        String sessionKey = generateSessionKey(player, hand);
        HoldSession session = activeSessions.remove(sessionKey);
        
        if (session != null) {
            DebugUtils.logInternalDebug(debug(), "Stopping HOLD session for " + player.getName() + 
                    " in " + hand + " hand");
            
            // Cancel the task
            if (session.getTask() != null) {
                session.getTask().cancel();
            }
            
            // Execute cancellation action if configured
            executeCancellationAction(session);
        }
    }
    
    public void stopAllSessionsForPlayer(Player player) {
        stopHoldSession(player, EquipmentSlot.HAND);
        stopHoldSession(player, EquipmentSlot.OFF_HAND);
    }
    
    public boolean hasActiveSession(Player player, EquipmentSlot hand, String itemId) {
        String sessionKey = generateSessionKey(player, hand);
        HoldSession session = activeSessions.get(sessionKey);
        
        if (session == null) {
            return false;
        }
        
        // Check if the session is for the same item
        return session.getInteractiveItem().getEffectiveId().equals(itemId);
    }
    
    private boolean isSessionValid(HoldSession session) {
        Player player = session.getPlayer();
        
        // Check if player is online
        if (!player.isOnline()) {
            return false;
        }
        
        // Check if item is still in the correct hand
        InteractiveItem currentItem = getCurrentInteractiveItem(player, session.getHand());
        if (currentItem == null) {
            return false;
        }
        
        // Check if it's the same item (by effective ID)
        String originalId = session.getInteractiveItem().getEffectiveId();
        String currentId = currentItem.getEffectiveId();
        
        if (!originalId.equals(currentId)) {
            return false;
        }
        
        // Check if hand is still allowed
        return isItemInCorrectHand(player, currentItem, session.getHand(), session.getAllowedHand());
    }
    
    private void executeHoldAction(HoldSession session) {
        Player player = session.getPlayer();
        InteractiveItem interactiveItem = session.getInteractiveItem();
        EquipmentSlot hand = session.getHand();
        
        try {
            // Create click info for the action
            ItemClickInfo clickInfo = new ItemClickInfo(player, 
                    org.bukkit.event.inventory.ClickType.RIGHT,
                    hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                    interactiveItem.getItemStack(),
                    ActionSource.ITEM_USE)
                    .withData("source", "hold_trigger");
            
            // Execute the action
            boolean actionExecuted = interactiveItem.executeAction(clickInfo);
            
            // Execute effects if action was executed or no action is configured
            if (actionExecuted || !interactiveItem.hasAction()) {
                ItemEffectsHandler.executeEffects(player, player.getLocation(), interactiveItem.getConfiguration());
            }
            
            DebugUtils.logInternalDebug(debug(), "Executed HOLD action for " + player.getName() + 
                    " with item " + interactiveItem.getId());
                    
        } catch (ItemHoldSessionException e) {
            DebugUtils.logInternalDebug(debug(), "Exception occurred during HOLD action for " + player.getName() + 
                    " with item " + interactiveItem.getId() + ": " + e.getMessage());
            
            // Stop the hold session immediately due to the exception
            stopHoldSession(player, hand);
        }
    }
    
    private void executeCancellationAction(HoldSession session) {
        Player player = session.getPlayer();
        InteractiveItem interactiveItem = session.getInteractiveItem();
        ItemConfiguration config = interactiveItem.getConfiguration();
        
        // Create click info for the cancellation action
        ItemClickInfo clickInfo = new ItemClickInfo(player, 
                org.bukkit.event.inventory.ClickType.LEFT,
                session.getHand() == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                interactiveItem.getItemStack(),
                ActionSource.ITEM_USE)
                .withData("source", "hold_cancel_trigger")
                .withData("hand", session.getHand())
                .withData("triggerType", "hold_cancel");
        
        // Create action context for the cancellation action
        ActionContext context = new ActionContext(player, ActionSource.ITEM_USE)
                .withData("clickType", org.bukkit.event.inventory.ClickType.LEFT)
                .withData("slot", session.getHand() == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40)
                .withData("item", interactiveItem)
                .withData("itemStack", interactiveItem.getItemStack())
                .withData("itemConfiguration", config)
                .withData("hand", session.getHand())
                .withData("triggerType", "hold_cancel")
                .withData("holdCancel", true);
        
        // Execute the same action but with hold_cancel context
        String action = config.getAction();
        if (action != null && !action.trim().isEmpty()) {
            boolean actionExecuted = GlobalActionManager.executeAction(action, context);
            
            if (actionExecuted) {
                DebugUtils.logInternalDebug(debug(), "Executed HOLD cancellation for action '" + action + "' for " + player.getName() + 
                        " with item " + interactiveItem.getId());
            } else {
                DebugUtils.logInternalDebug(debug(), "Failed to execute HOLD cancellation for action '" + action + "' for " + player.getName() + 
                        " with item " + interactiveItem.getId());
            }
        }
    }
    
    private InteractiveItem getCurrentInteractiveItem(Player player, EquipmentSlot hand) {
        ItemStack itemStack = switch (hand) {
            case HAND -> player.getInventory().getItemInMainHand();
            case OFF_HAND -> player.getInventory().getItemInOffHand();
            default -> null;
        };
        
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        
        return InteractiveItem.fromItemStack(itemStack);
    }
    
    private boolean isItemInCorrectHand(Player player, InteractiveItem interactiveItem, 
                                       EquipmentSlot actualHand, String allowedHand) {
        return switch (allowedHand.toUpperCase()) {
            case "MAIN" -> actualHand == EquipmentSlot.HAND;
            case "OFF" -> actualHand == EquipmentSlot.OFF_HAND;
            case "ANY" -> actualHand == EquipmentSlot.HAND || actualHand == EquipmentSlot.OFF_HAND;
            default -> true; // Default to allowing any hand
        };
    }
    
    private String generateSessionKey(Player player, EquipmentSlot hand) {
        return player.getUniqueId().toString() + "_" + hand.name();
    }
    
    public void cleanup() {
        // Stop all active sessions
        for (HoldSession session : activeSessions.values()) {
            if (session.getTask() != null) {
                session.getTask().cancel();
            }
        }
        activeSessions.clear();
    }
    
    // Inner class to hold session data
    @Getter
    private static class HoldSession {
        private final Player player;
        private final InteractiveItem interactiveItem;
        private final EquipmentSlot hand;
        private final String allowedHand;
        @Setter
        private BukkitTask task;
        
        public HoldSession(Player player, InteractiveItem interactiveItem, EquipmentSlot hand, String allowedHand) {
            this.player = player;
            this.interactiveItem = interactiveItem;
            this.hand = hand;
            this.allowedHand = allowedHand;
        }

    }
}