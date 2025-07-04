package net.exylia.commons.selection;

import net.exylia.commons.selection.events.SelectionCompleteEvent;
import net.exylia.commons.selection.events.SelectionCreateEvent;
import net.exylia.commons.selection.listeners.WandListener;
import net.exylia.commons.selection.model.Selection;
import net.exylia.commons.selection.model.SelectionType;
import net.exylia.commons.selection.model.WandConfig;
import net.exylia.commons.selection.wand.WandFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manager principal para el sistema de selecciones
 */
public class SelectionManager {
    private static SelectionManager instance;
    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, Selection>> playerSelections;
    private final Map<UUID, String> activeSelections;
    private final Map<UUID, Consumer<Selection>> selectionCallbacks;
    private final WandFactory wandFactory;

    private SelectionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerSelections = new ConcurrentHashMap<>();
        this.activeSelections = new ConcurrentHashMap<>();
        this.selectionCallbacks = new ConcurrentHashMap<>();
        this.wandFactory = new WandFactory(plugin);

        // Registrar listeners
        plugin.getServer().getPluginManager().registerEvents(new WandListener(plugin, wandFactory), plugin);
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new SelectionManager(plugin);
        }
    }

    public static SelectionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SelectionManager no ha sido inicializado");
        }
        return instance;
    }

    // ===== MÉTODOS DE WAND =====

    /**
     * Crea una wand con configuración por defecto
     */
    public ItemStack createWand() {
        return wandFactory.createWand();
    }

    /**
     * Crea una wand con ID de selección específico
     */
    public ItemStack createWand(String selectionId) {
        return wandFactory.createWand(selectionId);
    }

    /**
     * Crea una wand con configuración personalizada
     */
    public ItemStack createWand(WandConfig config) {
        return wandFactory.createWand(config);
    }

    /**
     * Verifica si un item es una wand
     */
    public boolean isWand(ItemStack item) {
        return wandFactory.isWand(item);
    }

    /**
     * Obtiene la factory de wands
     */
    public WandFactory getWandFactory() {
        return wandFactory;
    }

    // ===== MÉTODOS DE SELECCIÓN =====

    /**
     * Crea una nueva selección para un jugador
     */
    public Selection createSelection(Player player, String selectionId, SelectionType type) {
        UUID playerId = player.getUniqueId();
        Selection selection = new Selection(playerId, selectionId, type);

        // Disparar evento
        SelectionCreateEvent event = new SelectionCreateEvent(player, selection);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return null;
        }

        // Guardar selección
        playerSelections.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(selectionId, selection);

        // Establecer como activa
        activeSelections.put(playerId, selectionId);

        return selection;
    }

    /**
     * Establece el primer punto de una selección
     */
    public boolean setPos1(Player player, Location location) {
        return setPos1(player, getActiveSelectionId(player), location);
    }

    /**
     * Establece el primer punto de una selección específica
     */
    public boolean setPos1(Player player, String selectionId, Location location) {
        Optional<Selection> selectionOpt = getSelection(player, selectionId);
        if (!selectionOpt.isPresent()) {
            return false;
        }

        Selection selection = selectionOpt.get();
        selection.setPos1(location);

        checkSelectionComplete(player, selection);
        return true;
    }

    /**
     * Establece el segundo punto de una selección
     */
    public boolean setPos2(Player player, Location location) {
        return setPos2(player, getActiveSelectionId(player), location);
    }

    /**
     * Establece el segundo punto de una selección específica
     */
    public boolean setPos2(Player player, String selectionId, Location location) {
        Optional<Selection> selectionOpt = getSelection(player, selectionId);
        if (!selectionOpt.isPresent()) {
            return false;
        }

        Selection selection = selectionOpt.get();
        selection.setPos2(location);

        checkSelectionComplete(player, selection);
        return true;
    }

    /**
     * Obtiene una selección específica de un jugador
     */
    public Optional<Selection> getSelection(Player player, String selectionId) {
        UUID playerId = player.getUniqueId();
        return Optional.ofNullable(
                playerSelections.getOrDefault(playerId, new HashMap<>()).get(selectionId)
        );
    }

    /**
     * Obtiene la selección activa de un jugador
     */
    public Optional<Selection> getActiveSelection(Player player) {
        String activeId = getActiveSelectionId(player);
        if (activeId == null) {
            return Optional.empty();
        }
        return getSelection(player, activeId);
    }

    /**
     * Obtiene el ID de la selección activa
     */
    public String getActiveSelectionId(Player player) {
        return activeSelections.get(player.getUniqueId());
    }

    /**
     * Establece una selección como activa
     */
    public boolean setActiveSelection(Player player, String selectionId) {
        if (getSelection(player, selectionId).isPresent()) {
            activeSelections.put(player.getUniqueId(), selectionId);
            return true;
        }
        return false;
    }

    /**
     * Limpia todas las selecciones de un jugador
     */
    public void clearSelections(Player player) {
        UUID playerId = player.getUniqueId();
        playerSelections.remove(playerId);
        activeSelections.remove(playerId);
        selectionCallbacks.remove(playerId);
    }

    /**
     * Limpia una selección específica
     */
    public boolean clearSelection(Player player, String selectionId) {
        UUID playerId = player.getUniqueId();
        Map<String, Selection> selections = playerSelections.get(playerId);

        if (selections == null || !selections.containsKey(selectionId)) {
            return false;
        }

        selections.remove(selectionId);

        // Si era la selección activa, limpiar
        if (selectionId.equals(activeSelections.get(playerId))) {
            activeSelections.remove(playerId);
        }

        return true;
    }

    /**
     * Obtiene todas las selecciones de un jugador
     */
    public Map<String, Selection> getAllSelections(Player player) {
        return new HashMap<>(playerSelections.getOrDefault(player.getUniqueId(), new HashMap<>()));
    }

    /**
     * Registra un callback para cuando se complete una selección
     */
    public void setSelectionCallback(Player player, Consumer<Selection> callback) {
        selectionCallbacks.put(player.getUniqueId(), callback);
    }

    /**
     * Remueve el callback de selección
     */
    public void removeSelectionCallback(Player player) {
        selectionCallbacks.remove(player.getUniqueId());
    }

    // ===== MÉTODOS PRIVADOS =====

    private void checkSelectionComplete(Player player, Selection selection) {
        if (selection.isComplete()) {
            // Disparar evento
            SelectionCompleteEvent event = new SelectionCompleteEvent(
                    player, selection, selection.getPos1(), selection.getPos2()
            );
            Bukkit.getPluginManager().callEvent(event);

            // Ejecutar callback si existe
            Consumer<Selection> callback = selectionCallbacks.get(player.getUniqueId());
            if (callback != null) {
                callback.accept(selection);
            }
        }
    }

    /**
     * Limpia datos de jugadores desconectados
     */
    public void cleanup() {
        playerSelections.clear();
        activeSelections.clear();
        selectionCallbacks.clear();
    }
}