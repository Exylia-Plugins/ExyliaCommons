package net.exylia.commons.wizard.selection;

import net.exylia.commons.config.components.ActionBarConfig;
import net.exylia.commons.config.components.TitleConfig;
import net.exylia.commons.placeholders.ExyliaContext;
import net.exylia.commons.selection.SelectionManager;
import net.exylia.commons.selection.events.SelectionCompleteEvent;
import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.selection.model.SelectionType;
import net.exylia.commons.utils.visuals.ActionBarUtils;
import net.exylia.commons.utils.visuals.MessageUtils;
import net.exylia.commons.utils.visuals.TitleUtils;
import net.exylia.commons.ExyliaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static Selection Wizard Manager for selecting block areas
 * Simple, lightweight and customizable - works with the existing SelectionManager
 */
public final class SelectionWizard implements Listener {

    private static SelectionWizard instance;
    private static ExyliaPlugin plugin;
    private static SelectionManager selectionManager;

    private final Map<UUID, SelectionWizardSession> activeSessions = new ConcurrentHashMap<>();

    private SelectionWizard() {}

    /**
     * Initialize the SelectionWizard
     * Requires SelectionManager to be already initialized
     */
    public static void init(ExyliaPlugin pluginInstance) {
        if (instance != null) {
            return;
        }

        plugin = pluginInstance;
        selectionManager = SelectionManager.getInstance();
        instance = new SelectionWizard();
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);

    }

    /**
     * Start selection wizard - player selects an area using wands
     *
     * @param player The player
     * @param selectionsNeeded Number of selections needed (1, 2, 3, etc.)
     * @param handler Custom handler to process selections
     * @return CompletableFuture with the result
     */
    public static <T> CompletableFuture<T> startWizard(Player player, int selectionsNeeded, SelectionWizardHandler<T> handler) {
        if (instance == null) {
            throw new IllegalStateException("SelectionWizard not initialized");
        }

        if (selectionManager == null) {
            throw new IllegalStateException("SelectionManager not initialized");
        }

        if (selectionsNeeded < 1) {
            throw new IllegalArgumentException("Selections needed must be at least 1");
        }

        // Cancel existing session if any
        cancelWizard(player);

        SelectionWizardSession session = new SelectionWizardSession(player, selectionsNeeded, handler);
        instance.activeSessions.put(player.getUniqueId(), session);

        // Start the wizard
        handler.onStart(player, selectionsNeeded);
        startSelectionProcess(player, session);

        return session.getFuture();
    }

    /**
     * Start selection wizard with automatic wand giving
     *
     * @param player The player
     * @param selectionsNeeded Number of selections needed
     * @param handler Custom handler to process selections
     * @param giveWand Whether to give the player a wand automatically
     * @return CompletableFuture with the result
     */
    public static <T> CompletableFuture<T> startWizard(Player player, int selectionsNeeded, SelectionWizardHandler<T> handler, boolean giveWand) {
        CompletableFuture<T> future = startWizard(player, selectionsNeeded, handler);

        if (giveWand) {
            giveWand(player);
        }

        return future;
    }

    /**
     * Give a wand to the player for selections
     */
    public static void giveWand(Player player) {
        if (selectionManager == null) {
            return;
        }

        SelectionWizardSession session = getSession(player);
        if (session == null) {
            return;
        }

        String selectionId = "wizard_" + player.getUniqueId().toString().substring(0, 8) + "_" + session.getCurrentSelectionIndex();
        ItemStack wand = selectionManager.createWand(selectionId);

        player.getInventory().setItemInMainHand(wand);
        MessageUtils.sendMessageAsync(player, "{primary}You have been given a selection wand!");
    }

    /**
     * Cancel active wizard for player
     */
    public static boolean cancelWizard(Player player) {
        if (instance == null) {
            return false;
        }

        SelectionWizardSession session = instance.activeSessions.remove(player.getUniqueId());
        if (session != null) {
            // Clear any active selections created during the wizard
            clearWizardSelections(player, session);
            session.cancel();
            player.resetTitle();
            return true;
        }
        return false;
    }

    /**
     * Check if player has active wizard
     */
    public static boolean hasActiveWizard(Player player) {
        return instance != null && instance.activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Get remaining selections for player
     */
    public static int getRemainingSelections(Player player) {
        if (instance == null) {
            return 0;
        }

        SelectionWizardSession session = instance.activeSessions.get(player.getUniqueId());
        return session != null ? session.getRemainingSelections() : 0;
    }

    /**
     * Get current session for player
     */
    public static SelectionWizardSession getSession(Player player) {
        if (instance == null) {
            return null;
        }
        return instance.activeSessions.get(player.getUniqueId());
    }

    /**
     * Called when a selection is completed in SelectionManager
     * This marks the selection as ready but doesn't process it until player confirms
     */
    public static void notifySelectionReady(Player player, Selection selection) {
        if (instance == null) {
            return;
        }

        SelectionWizardSession session = instance.activeSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        // Mark selection as ready for confirmation
        session.setCurrentSelection(selection);

        // Update instructions to show confirmation step
        sendConfirmationInstructions(player, session, selection);
    }

    /**
     * Manually complete current selection and move to next
     * This is called when player confirms with SHIFT + LEFT CLICK
     */
    public static void confirmCurrentSelection(Player player, SelectionWizardSession session) {
        Selection currentSelection = session.getCurrentSelection();
        if (currentSelection == null || !currentSelection.isComplete()) {
            MessageUtils.sendMessageAsync(player, "{error}No selection ready to confirm!");
            return;
        }

        try {
            session.addSelection(currentSelection);
            session.setCurrentSelection(null); // Clear current selection

            @SuppressWarnings("unchecked")
            SelectionWizardHandler<Object> handler = (SelectionWizardHandler<Object>) session.getHandler();

            SelectionWizardResult result = handler.onSelectionComplete(
                    player,
                    currentSelection,
                    session.getCompletedSelections(),
                    session.getRemainingSelections()
            );

            TitleUtils.cancelAllTitles(player);
            ActionBarUtils.cancelAllActionBars(player);
            SelectionManager.getInstance().clearSelections(player);

            switch (result.getType()) {
                case CONTINUE:
                    if (result.getMessage() != null) {
                        MessageUtils.sendMessageAsync(player, result.getMessage());
                    }
                    continueToNextSelection(player, session);
                    break;

                case COMPLETE:
                    instance.activeSessions.remove(player.getUniqueId());
                    session.complete(result.getValue());
                    if (result.getMessage() != null) {
                        MessageUtils.sendMessageAsync(player, result.getMessage());
                    }
                    break;

                case CANCEL:
                    instance.activeSessions.remove(player.getUniqueId());
                    clearWizardSelections(player, session);
                    session.cancel();
                    if (result.getMessage() != null) {
                        MessageUtils.sendMessageAsync(player, result.getMessage());
                    }
                    break;
            }
        } catch (Exception e) {
            instance.activeSessions.remove(player.getUniqueId());
            player.resetTitle();
            clearWizardSelections(player, session);
            session.completeExceptionally(e);
        }
    }

    // ===== EVENT HANDLERS =====

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        SelectionWizardSession session = activeSessions.get(player.getUniqueId());

        if (session == null) {
            return;
        }

        // Check for SHIFT + LEFT CLICK to confirm current selection
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            if (player.isSneaking()) {
                event.setCancelled(true);
                confirmCurrentSelection(player, session);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelWizard(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSelectionComplete(SelectionCompleteEvent event) {
        // Solo procesar si el jugador tiene un wizard activo
        if (!SelectionWizard.hasActiveWizard(event.getPlayer())) {
            return;
        }

        // Verificar si la selección pertenece al wizard actual
        String selectionId = event.getSelection().getSelectionId();
        if (selectionId != null && selectionId.startsWith("wizard_")) {
            // Notificar al wizard que la selección está lista (pero no la confirma automáticamente)
            SelectionWizard.notifySelectionReady(event.getPlayer(), event.getSelection());
        }
    }

    // ===== PRIVATE METHODS =====

    private static void startSelectionProcess(Player player, SelectionWizardSession session) {
        sendInstructions(player, session);

        // Create the first selection ID for the player
        String currentSelectionId = getCurrentSelectionId(player, session);
        selectionManager.createSelection(player, currentSelectionId, SelectionType.CUBOID);
        selectionManager.setActiveSelection(player, currentSelectionId);

        // Set up callback to listen for selection completion (but not auto-confirm)
        selectionManager.setSelectionCallback(player, selection -> {
            if (selection.isComplete() && selection.getSelectionId().equals(currentSelectionId)) {
                // Don't auto-confirm, just notify that selection is ready
                notifySelectionReady(player, selection);
            }
        });
    }

    private static void continueToNextSelection(Player player, SelectionWizardSession session) {
        if (session.isComplete()) {
            return;
        }

        sendInstructions(player, session);

        // Create next selection
        String nextSelectionId = getCurrentSelectionId(player, session);
        selectionManager.createSelection(player, nextSelectionId, SelectionType.CUBOID);
        selectionManager.setActiveSelection(player, nextSelectionId);

        // Update callback for new selection
        selectionManager.setSelectionCallback(player, selection -> {
            if (selection.isComplete() && selection.getSelectionId().equals(nextSelectionId)) {
                // Don't auto-confirm, just notify that selection is ready
                notifySelectionReady(player, selection);
            }
        });
    }

    private static String getCurrentSelectionId(Player player, SelectionWizardSession session) {
        return "wizard_" + player.getUniqueId().toString().substring(0, 8) + "_" + session.getCurrentSelectionIndex();
    }

    private static void sendInstructions(Player player, SelectionWizardSession session) {
        TitleUtils.cancelTitle(player, "confirm_selection_wizard");
        ActionBarUtils.cancelActionBar(player, "confirm_selection_wizard");
        int remaining = session.getRemainingSelections();
        int total = session.getTotalSelections();
        int current = total - remaining + 1;

        // Send title with progress
        TitleUtils.sendTitle(player,
                "selection_wizard",
                new TitleConfig(
                        "{warning}⚡ Select Area " + current + "/" + total,
                        "{info}Use wand: Left click pos1, Right click pos2",
                        true,
                        20L),
                ExyliaContext.create());

        ActionBarUtils.sendActionBar(player,
                "selection_wizard",
                new ActionBarConfig(
                        "{warning}Selecting area " + current + "/" + total + " {secondary}| Use /sel wand for wand",
                        20L),
                ExyliaContext.create());
    }

    private static void sendConfirmationInstructions(Player player, SelectionWizardSession session, Selection selection) {
        TitleUtils.cancelTitle(player, "selection_wizard");
        ActionBarUtils.cancelActionBar(player, "selection_wizard");
        int remaining = session.getRemainingSelections();
        int total = session.getTotalSelections();
        int current = total - remaining + 1;

        // Send title with confirmation instructions
        TitleUtils.sendTitle(player,
                "confirm_selection_wizard",
                new TitleConfig(
                        "{success}✓ Area " + current + " Ready",
                        "{warning}SHIFT + LEFT CLICK to confirm",
                        true,
                        20L),
                ExyliaContext.create());

        ActionBarUtils.sendActionBar(player,
                "confirm_selection_wizard",
                new ActionBarConfig(
                        "{success}Volume: " + selection.getVolume() + " blocks {warning}| SHIFT + LEFT CLICK to confirm",
                        20L),
                ExyliaContext.create());

        // Optional: Send chat message with selection info
        MessageUtils.sendMessageAsync(player, "{success}Selection completed! Volume: {info}" + selection.getVolume() + " blocks");
        MessageUtils.sendMessageAsync(player, "{warning}Use SHIFT + LEFT CLICK to confirm and continue");
    }

    private static void clearWizardSelections(Player player, SelectionWizardSession session) {
        // Clear all selections created during this wizard session
        for (int i = 1; i <= (session.getTotalSelections() - session.getRemainingSelections()); i++) {
            String selectionId = "wizard_" + player.getUniqueId().toString().substring(0, 8) + "_" + i;
            selectionManager.clearSelection(player, selectionId);
        }

        // Remove selection callback
        selectionManager.removeSelectionCallback(player);
    }

    /**
     * Shutdown the SelectionWizard
     */
    public static void shutdown() {
        if (instance != null) {
            instance.activeSessions.values().forEach(session -> {
                clearWizardSelections(session.getPlayer(), session);
                session.cancel();
            });
            instance.activeSessions.clear();
            instance = null;
            plugin = null;
            selectionManager = null;
        }
    }
}