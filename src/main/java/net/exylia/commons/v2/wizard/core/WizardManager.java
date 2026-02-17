package net.exylia.commons.v2.wizard.core;

import lombok.Getter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.region.selection.Selection;
import net.exylia.commons.v2.region.selection.SelectionManager;
import net.exylia.commons.v2.visual.api.ActionBarAPI;
import net.exylia.commons.v2.visual.api.TitleAPI;
import net.exylia.commons.v2.visual.builder.ActionBarBuilder;
import net.exylia.commons.v2.visual.builder.TitleBuilder;
import net.exylia.commons.v2.wizard.config.WizardConfig;
import net.exylia.commons.v2.wizard.handler.InteractionHandler;
import net.exylia.commons.v2.wizard.handler.LocationHandler;
import net.exylia.commons.v2.wizard.handler.SelectionHandler;
import net.exylia.commons.v2.wizard.result.WizardResult;
import net.exylia.commons.v2.wizard.session.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WizardManager implements Listener {
    private static volatile WizardManager instance;
    private static final Object LOCK = new Object();

    @Getter
    private JavaPlugin plugin;
    private final Map<UUID, WizardSession<?>> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastInteractionTime = new ConcurrentHashMap<>();

    private WizardManager() {}

    public static WizardManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new WizardManager();
                }
            }
        }
        return instance;
    }

    public void init(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void shutdown() {
        activeSessions.values().forEach(session -> {
            clearDisplay(session.getPlayer());
            session.cancel();
        });
        activeSessions.clear();
        lastInteractionTime.clear();
        if (plugin != null) {
            HandlerList.unregisterAll(this);
        }
    }

    public <T> CompletableFuture<T> startInteraction(Player player, WizardConfig config, InteractionHandler<T> handler) {
        cancel(player);

        InteractionSession<T> session = new InteractionSession<>(player, config, handler);
        activeSessions.put(player.getUniqueId(), session);

        handler.onStart(player);
        showDisplay(player, config);

        if (config.isCloseInventory()) {
            player.closeInventory();
        }

        return session.getFuture();
    }

    public <T> CompletableFuture<T> startLocation(Player player, int count, WizardConfig config, LocationHandler<T> handler) {
        cancel(player);

        LocationSession<T> session = new LocationSession<>(player, config, count, handler);
        activeSessions.put(player.getUniqueId(), session);

        handler.onStart(player, count);
        showLocationDisplay(player, session);

        if (config.isCloseInventory()) {
            player.closeInventory();
        }

        return session.getFuture();
    }

    public <T> CompletableFuture<T> startSelection(Player player, int count, WizardConfig config, SelectionHandler<T> handler, boolean giveWand) {
        cancel(player);

        SelectionSession<T> session = new SelectionSession<>(player, config, count, handler);
        activeSessions.put(player.getUniqueId(), session);

        handler.onStart(player, count);
        initSelectionProcess(player, session);
        showSelectionDisplay(player, session);

        if (giveWand) {
            giveWand(player, session);
        }

        if (config.isCloseInventory()) {
            player.closeInventory();
        }

        return session.getFuture();
    }

    private <T> void initSelectionProcess(Player player, SelectionSession<T> session) {
        SelectionManager selMgr = SelectionManager.getInstance();
        String selectionId = session.generateSelectionId();
        Selection selection = selMgr.getOrCreateSelection(player);
        selection.clear();

        selMgr.setCallback(player, sel -> {
            if (sel.isComplete()) {
                session.notifySelectionReady(sel);
                showConfirmationDisplay(player, session, sel);
            }
        });
    }

    private void giveWand(Player player, SelectionSession<?> session) {
        SelectionManager selMgr = SelectionManager.getInstance();
        ItemStack wand = selMgr.createWand(session.generateSelectionId());
        player.getInventory().setItemInMainHand(wand);
    }

    public boolean cancel(Player player) {
        UUID playerId = player.getUniqueId();
        WizardSession<?> session = activeSessions.remove(playerId);
        if (session != null) {
            lastInteractionTime.remove(playerId);
            clearDisplay(player);
            cleanupSelection(player, session);
            session.cancel();
            return true;
        }
        return false;
    }

    public boolean hasActive(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public Optional<WizardSession<?>> getSession(Player player) {
        return Optional.ofNullable(activeSessions.get(player.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        WizardSession<?> session = activeSessions.get(playerId);
        if (session == null) return;

        long cooldown = session.getConfig().getInteractionCooldown();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastInteractionTime.get(playerId);
        if (lastTime != null && (currentTime - lastTime) < cooldown) return;

        lastInteractionTime.put(playerId, currentTime);

        try {
            session.handleInteraction(event);
            handlePostInteraction(player, session);
        } catch (Exception e) {
            activeSessions.remove(playerId);
            lastInteractionTime.remove(playerId);
            clearDisplay(player);
            cleanupSelection(player, session);
            session.completeExceptionally(e);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer());
    }

    private void handlePostInteraction(Player player, WizardSession<?> session) {
        if (!session.isActive()) {
            activeSessions.remove(player.getUniqueId());
            lastInteractionTime.remove(player.getUniqueId());
            clearDisplay(player);
            cleanupSelection(player, session);
            return;
        }

        if (session instanceof LocationSession<?> locSession) {
            showLocationDisplay(player, locSession);
        } else if (session instanceof SelectionSession<?> selSession) {
            if (selSession.isJustConfirmed() && selSession.needsMoreSelections()) {
                selSession.setJustConfirmed(false);
                continueSelectionProcess(player, selSession);
            }
        }
    }

    private void continueSelectionProcess(Player player, SelectionSession<?> session) {
        SelectionManager selMgr = SelectionManager.getInstance();
        selMgr.clearSelection(player);
        Selection selection = selMgr.getOrCreateSelection(player);
        selection.clear();

        selMgr.setCallback(player, sel -> {
            if (sel.isComplete()) {
                session.notifySelectionReady(sel);
                showConfirmationDisplay(player, session, sel);
            }
        });

        showSelectionDisplay(player, session);
    }

    private void cleanupSelection(Player player, WizardSession<?> session) {
        if (session instanceof SelectionSession<?>) {
            SelectionManager.getInstance().clearCallback(player);
            SelectionManager.getInstance().clearSelection(player);
        }
    }

    private void showDisplay(Player player, WizardConfig config) {
        PlaceholderContext ctx = PlaceholderContext.create().withPlayer(player);

        if (config.isShowTitle()) {
            TitleAPI.send(player, TitleBuilder.create()
                    .title(config.getTitleText())
                    .subtitle(config.getSubtitleText())
                    .times(config.getFadeIn(), config.getStay(), config.getFadeOut())
                    .permanent()
                    .build(), ctx);
        }

        if (config.isShowActionBar()) {
            ActionBarAPI.sendPermanent(player, ActionBarBuilder.create()
                    .text(config.getActionBarText())
                    .build(), ctx);
        }
    }

    private void showLocationDisplay(Player player, LocationSession<?> session) {
        WizardConfig config = session.getConfig();
        int current = session.getCurrent();
        int total = session.getTotalCount();
        int remaining = session.getRemaining();

        PlaceholderContext ctx = PlaceholderContext.create()
                .withPlayer(player)
                .put("current", current)
                .put("total", total)
                .put("remaining", remaining);

        String title = config.getTitleText()
                .replace("{current}", String.valueOf(current))
                .replace("{total}", String.valueOf(total));
        String subtitle = config.getSubtitleText()
                .replace("{remaining}", String.valueOf(remaining));
        String actionBar = config.getActionBarText()
                .replace("{current}", String.valueOf(current))
                .replace("{total}", String.valueOf(total))
                .replace("{remaining}", String.valueOf(remaining));

        clearDisplay(player);

        if (config.isShowTitle()) {
            TitleAPI.send(player, TitleBuilder.create()
                    .title(title)
                    .subtitle(subtitle)
                    .times(config.getFadeIn(), config.getStay(), config.getFadeOut())
                    .permanent()
                    .build(), ctx);
        }

        if (config.isShowActionBar()) {
            ActionBarAPI.sendPermanent(player, ActionBarBuilder.create()
                    .text(actionBar)
                    .build(), ctx);
        }
    }

    private void showSelectionDisplay(Player player, SelectionSession<?> session) {
        WizardConfig config = session.getConfig();
        int current = session.getCurrent();
        int total = session.getTotalCount();

        PlaceholderContext ctx = PlaceholderContext.create()
                .withPlayer(player)
                .put("current", current)
                .put("total", total);

        clearDisplay(player);

        if (config.isShowTitle()) {
            TitleAPI.send(player, TitleBuilder.create()
                    .title("{warning}⚡ Select Area " + current + "/" + total)
                    .subtitle("{info}Use wand: Left click pos1, Right click pos2")
                    .times(config.getFadeIn(), config.getStay(), config.getFadeOut())
                    .permanent()
                    .build(), ctx);
        }

        if (config.isShowActionBar()) {
            ActionBarAPI.sendPermanent(player, ActionBarBuilder.create()
                    .text("{warning}Selecting area " + current + "/" + total)
                    .build(), ctx);
        }
    }

    private void showConfirmationDisplay(Player player, SelectionSession<?> session, Selection selection) {
        WizardConfig config = session.getConfig();
        int current = session.getCurrent();
        int total = session.getTotalCount();
        long volume = selection.getVolume();

        PlaceholderContext ctx = PlaceholderContext.create()
                .withPlayer(player)
                .put("current", current)
                .put("total", total)
                .put("volume", volume);

        clearDisplay(player);

        if (config.isShowTitle()) {
            TitleAPI.send(player, TitleBuilder.create()
                    .title("{success}✔ Area " + current + " Ready")
                    .subtitle("{warning}SHIFT + LEFT CLICK to confirm")
                    .times(config.getFadeIn(), config.getStay(), config.getFadeOut())
                    .permanent()
                    .build(), ctx);
        }

        if (config.isShowActionBar()) {
            ActionBarAPI.sendPermanent(player, ActionBarBuilder.create()
                    .text("{success}Volume: " + volume + " blocks {warning}| SHIFT + LEFT CLICK to confirm")
                    .build(), ctx);
        }
    }

    private void clearDisplay(Player player) {
        player.resetTitle();
        TitleAPI.cancelAll(player);
        ActionBarAPI.cancelAll(player);
    }

    public void updateDisplay(Player player, WizardConfig config) {
        if (!hasActive(player)) return;
        clearDisplay(player);
        showDisplay(player, config);
    }
}
