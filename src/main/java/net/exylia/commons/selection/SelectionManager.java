package net.exylia.commons.selection;

import lombok.Getter;
import net.exylia.commons.selection.events.SelectionCompleteEvent;
import net.exylia.commons.selection.events.SelectionCreateEvent;
import net.exylia.commons.selection.listeners.WandListener;
import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.selection.model.SelectionType;
import net.exylia.commons.selection.model.WandConfig;
import net.exylia.commons.selection.visualizer.ParticleConfig;
import net.exylia.commons.selection.visualizer.ParticleVisualizer;
import net.exylia.commons.selection.wand.WandFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SelectionManager implements Listener {
    private static SelectionManager instance;
    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, Selection>> playerSelections;
    private final Map<UUID, String> activeSelections;
    private final Map<UUID, Consumer<Selection>> selectionCallbacks;
    private final Map<UUID, Boolean> visualizationEnabled;
    @Getter
    private final WandFactory wandFactory;
    @Getter
    private final ParticleVisualizer particleVisualizer;

    private SelectionManager(JavaPlugin plugin) {
        this(plugin, new ParticleConfig());
    }

    private SelectionManager(JavaPlugin plugin, ParticleConfig particleConfig) {
        this.plugin = plugin;
        this.playerSelections = new ConcurrentHashMap<>();
        this.activeSelections = new ConcurrentHashMap<>();
        this.selectionCallbacks = new ConcurrentHashMap<>();
        this.visualizationEnabled = new ConcurrentHashMap<>();
        this.wandFactory = new WandFactory(plugin);
        this.particleVisualizer = new ParticleVisualizer(plugin, particleConfig);

        plugin.getServer().getPluginManager().registerEvents(new WandListener(plugin, wandFactory, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new SelectionManager(plugin);
        }
    }

    public static void initialize(JavaPlugin plugin, ParticleConfig particleConfig) {
        if (instance == null) {
            instance = new SelectionManager(plugin, particleConfig);
        }
    }

    public static SelectionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SelectionManager no ha sido inicializado");
        }
        return instance;
    }

    public ItemStack createWand() {
        return wandFactory.createWand();
    }

    public ItemStack createWand(String selectionId) {
        return wandFactory.createWand(selectionId);
    }

    public ItemStack createWand(WandConfig config) {
        return wandFactory.createWand(config);
    }

    public boolean isWand(ItemStack item) {
        return wandFactory.isWand(item);
    }

    public Selection createSelection(Player player, String selectionId, SelectionType type) {
        UUID playerId = player.getUniqueId();
        Selection selection = new Selection(playerId, selectionId, type);

        SelectionCreateEvent event = new SelectionCreateEvent(player, selection);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return null;
        }

        playerSelections.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(selectionId, selection);

        activeSelections.put(playerId, selectionId);

        return selection;
    }

    public boolean setPos1(Player player, Location location) {
        return setPos1(player, getActiveSelectionId(player), location);
    }

    public boolean setPos1(Player player, String selectionId, Location location) {
        Optional<Selection> selectionOpt = getSelection(player, selectionId);
        if (!selectionOpt.isPresent()) {
            return false;
        }

        Selection selection = selectionOpt.get();
        selection.setPos1(location);

        updateVisualization(player, selection);

        checkSelectionComplete(player, selection);
        return true;
    }

    public boolean setPos2(Player player, Location location) {
        return setPos2(player, getActiveSelectionId(player), location);
    }

    public boolean setPos2(Player player, String selectionId, Location location) {
        Optional<Selection> selectionOpt = getSelection(player, selectionId);
        if (!selectionOpt.isPresent()) {
            return false;
        }

        Selection selection = selectionOpt.get();
        selection.setPos2(location);

        updateVisualization(player, selection);

        checkSelectionComplete(player, selection);
        return true;
    }

    public Optional<Selection> getSelection(Player player, String selectionId) {
        UUID playerId = player.getUniqueId();
        return Optional.ofNullable(
                playerSelections.getOrDefault(playerId, new HashMap<>()).get(selectionId)
        );
    }

    public Optional<Selection> getActiveSelection(Player player) {
        String activeId = getActiveSelectionId(player);
        if (activeId == null) {
            return Optional.empty();
        }
        return getSelection(player, activeId);
    }

    public String getActiveSelectionId(Player player) {
        return activeSelections.get(player.getUniqueId());
    }

    public boolean setActiveSelection(Player player, String selectionId) {
        if (getSelection(player, selectionId).isPresent()) {
            activeSelections.put(player.getUniqueId(), selectionId);
            return true;
        }
        return false;
    }

    public void clearSelections(Player player) {
        UUID playerId = player.getUniqueId();

        particleVisualizer.clearAll(player);

        playerSelections.remove(playerId);
        activeSelections.remove(playerId);
        selectionCallbacks.remove(playerId);
        visualizationEnabled.remove(playerId);
    }

    public boolean clearSelection(Player player, String selectionId) {
        UUID playerId = player.getUniqueId();
        Map<String, Selection> selections = playerSelections.get(playerId);

        if (selections == null || !selections.containsKey(selectionId)) {
            return false;
        }

        particleVisualizer.clearSelection(player, selectionId);

        selections.remove(selectionId);

        if (selectionId.equals(activeSelections.get(playerId))) {
            activeSelections.remove(playerId);
        }

        return true;
    }

    public Map<String, Selection> getAllSelections(Player player) {
        return new HashMap<>(playerSelections.getOrDefault(player.getUniqueId(), new HashMap<>()));
    }

    public void setSelectionCallback(Player player, Consumer<Selection> callback) {
        selectionCallbacks.put(player.getUniqueId(), callback);
    }

    public void removeSelectionCallback(Player player) {
        selectionCallbacks.remove(player.getUniqueId());
    }

    public void setVisualizationEnabled(Player player, boolean enabled) {
        UUID playerId = player.getUniqueId();
        visualizationEnabled.put(playerId, enabled);

        if (!enabled) {
            particleVisualizer.clearAll(player);
        } else {
             
            Map<String, Selection> selections = playerSelections.get(playerId);
            if (selections != null) {
                selections.values().stream()
                        .filter(Selection::isComplete)
                        .forEach(selection -> particleVisualizer.showSelection(player, selection));
            }
        }
    }

    public boolean isVisualizationEnabled(Player player) {
        return visualizationEnabled.getOrDefault(
                player.getUniqueId(),
                particleVisualizer.getConfig().isEnabledByDefault()
        );
    }

    public void showSelection(Player player, String selectionId) {
        Optional<Selection> selectionOpt = getSelection(player, selectionId);
        if (selectionOpt.isPresent() && isVisualizationEnabled(player)) {
            particleVisualizer.showSelection(player, selectionOpt.get());
        }
    }

    public void hideSelection(Player player, String selectionId) {
        particleVisualizer.clearSelection(player, selectionId);
    }

    private void checkSelectionComplete(Player player, Selection selection) {
        if (selection.isComplete()) {
             
            SelectionCompleteEvent event = new SelectionCompleteEvent(
                    player, selection, selection.getPos1(), selection.getPos2()
            );
            Bukkit.getPluginManager().callEvent(event);

            Consumer<Selection> callback = selectionCallbacks.get(player.getUniqueId());
            if (callback != null) {
                callback.accept(selection);
            }
        }
    }

    private void updateVisualization(Player player, Selection selection) {
        if (isVisualizationEnabled(player)) {
            particleVisualizer.updateSelection(player, selection);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        particleVisualizer.clearAll(event.getPlayer());
        playerSelections.remove(playerId);
        activeSelections.remove(playerId);
        selectionCallbacks.remove(playerId);
        visualizationEnabled.remove(playerId);
    }

    public void cleanup() {
        particleVisualizer.cleanup();
        playerSelections.clear();
        activeSelections.clear();
        selectionCallbacks.clear();
        visualizationEnabled.clear();
    }
}
