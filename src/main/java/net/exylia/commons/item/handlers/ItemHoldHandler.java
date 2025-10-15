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
        
        HoldSession existingSession = activeSessions.get(sessionKey);
        if (existingSession != null) {
            String existingItemId = existingSession.getInteractiveItem().getEffectiveId();
            String newItemId = interactiveItem.getEffectiveId();
            if (existingItemId.equals(newItemId)) {
                DebugUtils.logInternalDebug("HOLD session already active for " + player.getName() + 
                        " with same item " + newItemId + ", keeping existing session");
                return;
            } else {
                 
                DebugUtils.logInternalDebug("Stopping existing HOLD session for different item. Old: " + 
                        existingItemId + ", New: " + newItemId);
                stopHoldSession(player, hand);
            }
        }
        
        int intervalTicks = config.getActionConfigInt("hold-interval", 20);  
        String allowedHand = config.getActionConfigString("hold-hand", "ANY");  
        
        if (!isItemInCorrectHand(player, interactiveItem, hand, allowedHand)) {
            return;
        }
        
        DebugUtils.logInternalDebug("Starting HOLD session for " + player.getName() + 
                " with item " + interactiveItem.getId() + " in " + hand + " hand");
        
        HoldSession session = new HoldSession(player, interactiveItem, hand, allowedHand);
        activeSessions.put(sessionKey, session);
        
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
            DebugUtils.logInternalDebug("Stopping HOLD session for " + player.getName() + 
                    " in " + hand + " hand");
            
            if (session.getTask() != null) {
                session.getTask().cancel();
            }
            
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
        
        return session.getInteractiveItem().getEffectiveId().equals(itemId);
    }
    
    private boolean isSessionValid(HoldSession session) {
        Player player = session.getPlayer();
        
        if (!player.isOnline()) {
            return false;
        }
        
        InteractiveItem currentItem = getCurrentInteractiveItem(player, session.getHand());
        if (currentItem == null) {
            return false;
        }
        
        String originalId = session.getInteractiveItem().getEffectiveId();
        String currentId = currentItem.getEffectiveId();
        
        if (!originalId.equals(currentId)) {
            return false;
        }
        
        return isItemInCorrectHand(player, currentItem, session.getHand(), session.getAllowedHand());
    }
    
    private void executeHoldAction(HoldSession session) {
        Player player = session.getPlayer();
        InteractiveItem interactiveItem = session.getInteractiveItem();
        EquipmentSlot hand = session.getHand();
        
        try {
             
            ItemClickInfo clickInfo = new ItemClickInfo(player, 
                    org.bukkit.event.inventory.ClickType.RIGHT,
                    hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                    interactiveItem.getItemStack(),
                    ActionSource.ITEM_USE)
                    .withData("source", "hold_trigger");
            
            boolean actionExecuted = interactiveItem.executeAction(clickInfo);
            
            if (actionExecuted || !interactiveItem.hasAction()) {
                ItemEffectsHandler.executeEffects(player, player.getLocation(), interactiveItem.getConfiguration());
            }
            
            DebugUtils.logInternalDebug("Executed HOLD action for " + player.getName() + 
                    " with item " + interactiveItem.getId());
                    
        } catch (ItemHoldSessionException e) {
            DebugUtils.logInternalDebug("Exception occurred during HOLD action for " + player.getName() + 
                    " with item " + interactiveItem.getId() + ": " + e.getMessage());
            
            stopHoldSession(player, hand);
        }
    }
    
    private void executeCancellationAction(HoldSession session) {
        Player player = session.getPlayer();
        InteractiveItem interactiveItem = session.getInteractiveItem();
        ItemConfiguration config = interactiveItem.getConfiguration();
        
        ItemClickInfo clickInfo = new ItemClickInfo(player, 
                org.bukkit.event.inventory.ClickType.LEFT,
                session.getHand() == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40,
                interactiveItem.getItemStack(),
                ActionSource.ITEM_USE)
                .withData("source", "hold_cancel_trigger")
                .withData("hand", session.getHand())
                .withData("triggerType", "hold_cancel");
        
        ActionContext context = new ActionContext(player, ActionSource.ITEM_USE)
                .withData("clickType", org.bukkit.event.inventory.ClickType.LEFT)
                .withData("slot", session.getHand() == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : 40)
                .withData("item", interactiveItem)
                .withData("itemStack", interactiveItem.getItemStack())
                .withData("itemConfiguration", config)
                .withData("hand", session.getHand())
                .withData("triggerType", "hold_cancel")
                .withData("holdCancel", true);
        
        String action = config.getAction();
        if (action != null && !action.trim().isEmpty()) {
            boolean actionExecuted = GlobalActionManager.executeAction(action, context);
            
            if (actionExecuted) {
                DebugUtils.logInternalDebug("Executed HOLD cancellation for action '" + action + "' for " + player.getName() + 
                        " with item " + interactiveItem.getId());
            } else {
                DebugUtils.logInternalDebug("Failed to execute HOLD cancellation for action '" + action + "' for " + player.getName() + 
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
            default -> true;  
        };
    }
    
    private String generateSessionKey(Player player, EquipmentSlot hand) {
        return player.getUniqueId().toString() + "_" + hand.name();
    }
    
    public void cleanup() {
         
        for (HoldSession session : activeSessions.values()) {
            if (session.getTask() != null) {
                session.getTask().cancel();
            }
        }
        activeSessions.clear();
    }
    
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
