package net.exylia.commons.v2.region.selection;

import lombok.Getter;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.exylia.commons.v2.region.model.Region;
import net.exylia.commons.v2.region.visual.RegionSelector;
import net.exylia.commons.v2.region.visual.SelectionSession;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SelectionManager {
    private static SelectionManager instance;

    private final JavaPlugin plugin;
    private final Map<UUID, Selection> playerSelections;
    private final Map<UUID, Consumer<Selection>> selectionCallbacks;
    private final Map<UUID, SelectionSession> activeVisualizations;

    @Getter
    private final WandManager wandManager;

    private SelectionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerSelections = new ConcurrentHashMap<>();
        this.selectionCallbacks = new ConcurrentHashMap<>();
        this.activeVisualizations = new ConcurrentHashMap<>();
        this.wandManager = new WandManager(plugin);
    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (SelectionManager.class) {
            if (instance != null && instance.plugin == plugin) {
                return;
            }

            if (instance != null) {
                instance.cleanupAll();
            }

            instance = new SelectionManager(plugin);
        }
    }

    public static SelectionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SelectionManagerV2 has not been initialized");
        }
        return instance;
    }

    public ItemStack createWand() {
        return wandManager.createWand();
    }

    public ItemStack createWand(String selectionId) {
        return wandManager.createWand(selectionId);
    }

    public boolean isWand(ItemStack item) {
        return wandManager.isWand(item);
    }

    public Selection getOrCreateSelection(Player player) {
        return playerSelections.computeIfAbsent(player.getUniqueId(),
            k -> new Selection(player.getUniqueId(), "region-" + player.getName()));
    }

    public Optional<Selection> getSelection(Player player) {
        return Optional.ofNullable(playerSelections.get(player.getUniqueId()));
    }

    public void setPos1(Player player, Location location) {
        Selection selection = getOrCreateSelection(player);
        selection.setPos1(location);

        player.sendMessage(ColorAPI.parse(String.format(
            "{success}• Position 1 set to {info}%d, %d, %d",
            location.getBlockX(), location.getBlockY(), location.getBlockZ()
        )));

        if (selection.isComplete()) {
            onSelectionComplete(player, selection);
        }
    }

    public void setPos2(Player player, Location location) {
        Selection selection = getOrCreateSelection(player);
        selection.setPos2(location);

        player.sendMessage(ColorAPI.parse(String.format(
            "{error}• Position 2 set to {info}%d, %d, %d",
            location.getBlockX(), location.getBlockY(), location.getBlockZ()
        )));

        if (selection.isComplete()) {
            onSelectionComplete(player, selection);
        }
    }

    private void onSelectionComplete(Player player, Selection selection) {
        player.sendMessage(ColorAPI.parse(String.format(
            "{success}• Selection complete! {letters}Volume: {info}%d blocks",
            selection.getVolume()
        )));

        startVisualization(player, selection);

        Consumer<Selection> callback = selectionCallbacks.get(player.getUniqueId());
        if (callback != null) {
            callback.accept(selection);
        }
    }

    public void setCallback(Player player, Consumer<Selection> callback) {
        selectionCallbacks.put(player.getUniqueId(), callback);
    }

    public void clearCallback(Player player) {
        selectionCallbacks.remove(player.getUniqueId());
    }

    public void clearSelection(Player player) {
        Selection selection = playerSelections.remove(player.getUniqueId());
        selectionCallbacks.remove(player.getUniqueId());

        SelectionSession session = activeVisualizations.remove(player.getUniqueId());
        if (session != null) {
            session.stop();
        }

        if (selection != null) {
            player.sendMessage(ColorAPI.parse("{info}• Selection cleared"));
        }
    }

    public void showSelection(Player player) {
        Selection selection = playerSelections.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            player.sendMessage(ColorAPI.parse("{error}• No complete selection to show"));
            return;
        }

        startVisualization(player, selection);
        player.sendMessage(ColorAPI.parse("{info}• Showing selection visualization..."));
    }

    private void startVisualization(Player player, Selection selection) {
        SelectionSession existingSession = activeVisualizations.get(player.getUniqueId());
        if (existingSession != null) {
            existingSession.stop();
        }

        Region tempRegion = new Region("temp-selection-" + player.getUniqueId(), selection);
        SelectionSession session = RegionSelector.getInstance().showSelector(player, tempRegion, Color.AQUA);
        activeVisualizations.put(player.getUniqueId(), session);
    }

    public void hideSelection(Player player) {
        SelectionSession session = activeVisualizations.remove(player.getUniqueId());
        if (session != null) {
            session.stop();
            player.sendMessage(ColorAPI.parse("{info}• Selection visualization hidden"));
        }
    }

    public void cleanup(Player player) {
        playerSelections.remove(player.getUniqueId());
        selectionCallbacks.remove(player.getUniqueId());

        SelectionSession session = activeVisualizations.remove(player.getUniqueId());
        if (session != null) {
            session.stop();
        }
    }

    public void cleanupAll() {
        for (SelectionSession session : activeVisualizations.values()) {
            session.stop();
        }

        playerSelections.clear();
        selectionCallbacks.clear();
        activeVisualizations.clear();
    }

    public void shutdown() {
        cleanupAll();
        synchronized (SelectionManager.class) {
            if (instance == this) {
                instance = null;
            }
        }
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("active_selections", playerSelections.size());
        stats.put("active_callbacks", selectionCallbacks.size());
        stats.put("active_visualizations", activeVisualizations.size());
        return stats;
    }
}
