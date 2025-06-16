package net.exylia.commons.wizard;

import net.exylia.commons.wizard.LocationWizard;
import net.exylia.commons.wizard.impl.BlockSelectionWizard;
import net.exylia.commons.wizard.impl.PositionSelectionWizard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manager principal para manejar todos los wizards de ubicación
 */
public class LocationWizardManager implements Listener {

    private static LocationWizardManager instance;
    private final Map<UUID, LocationWizard> activeWizards;
    private final Map<UUID, Long> wizardTimeouts;
    public final JavaPlugin plugin; // Hacer público para acceso desde ArenaSetupWizard

    // Configuración
    private static final long DEFAULT_TIMEOUT = 300000; // 5 minutos en milisegundos

    private LocationWizardManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.activeWizards = new ConcurrentHashMap<>();
        this.wizardTimeouts = new ConcurrentHashMap<>();
    }

    public static void initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new LocationWizardManager(plugin);
            Bukkit.getPluginManager().registerEvents(instance, plugin);

            // Tarea para limpiar wizards expirados
            Bukkit.getScheduler().runTaskTimer(plugin, instance::cleanupExpiredWizards, 20L * 60L, 20L * 60L);
        }
    }

    public static LocationWizardManager getInstance() {
        return instance;
    }

    /**
     * Crea un wizard de selección de bloques
     */
    public BlockSelectionWizard createBlockSelectionWizard(Player player) {
        cancelActiveWizard(player);

        BlockSelectionWizard wizard = new BlockSelectionWizard(player);
        registerWizard(wizard);
        return wizard;
    }

    /**
     * Crea un wizard de selección de posiciones
     */
    public PositionSelectionWizard createPositionSelectionWizard(Player player, int requiredPositions) {
        return createPositionSelectionWizard(player, requiredPositions, false);
    }

    /**
     * Crea un wizard de selección de posiciones con opción de duplicados
     */
    public PositionSelectionWizard createPositionSelectionWizard(Player player, int requiredPositions, boolean allowDuplicates) {
        cancelActiveWizard(player);

        PositionSelectionWizard wizard = new PositionSelectionWizard(player, requiredPositions, allowDuplicates);
        registerWizard(wizard);
        return wizard;
    }

    /**
     * Obtiene el wizard activo de un jugador
     */
    public LocationWizard getActiveWizard(Player player) {
        return activeWizards.get(player.getUniqueId());
    }

    /**
     * Verifica si un jugador tiene un wizard activo
     */
    public boolean hasActiveWizard(Player player) {
        return activeWizards.containsKey(player.getUniqueId());
    }

    /**
     * Cancela el wizard activo de un jugador
     */
    public boolean cancelActiveWizard(Player player) {
        LocationWizard wizard = activeWizards.get(player.getUniqueId());
        if (wizard != null) {
            wizard.cancel();
            unregisterWizard(wizard);
            return true;
        }
        return false;
    }

    /**
     * Cancela todos los wizards activos
     */
    public void cancelAllWizards() {
        activeWizards.values().forEach(wizard -> {
            wizard.cancel();
            wizard.cleanup();
        });
        activeWizards.clear();
        wizardTimeouts.clear();
    }

    /**
     * Obtiene todos los wizards activos
     */
    public Map<UUID, LocationWizard> getActiveWizards() {
        return new HashMap<>(activeWizards);
    }

    private void registerWizard(LocationWizard wizard) {
        activeWizards.put(wizard.getPlayer().getUniqueId(), wizard);
        wizardTimeouts.put(wizard.getPlayer().getUniqueId(), System.currentTimeMillis() + DEFAULT_TIMEOUT);

        // Auto-cleanup cuando se complete o cancele
        wizard.onComplete(locations -> unregisterWizard(wizard));
        wizard.onCancel(() -> unregisterWizard(wizard));
    }

    private void unregisterWizard(LocationWizard wizard) {
        UUID playerId = wizard.getPlayer().getUniqueId();
        activeWizards.remove(playerId);
        wizardTimeouts.remove(playerId);
        wizard.cleanup();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        LocationWizard wizard = activeWizards.get(player.getUniqueId());

        if (wizard == null) {
            return;
        }

        // Debug: mostrar información del evento
        boolean isSneaking = player.isSneaking();
        Action action = event.getAction();

        // Determinar el tipo de click
        LocationWizard.ClickType clickType = getClickType(action, isSneaking);
        if (clickType == null) {
            return;
        }

        // Obtener la ubicación del click
        Location location = null;

        // Para BlockSelectionWizard, solo permitir clicks en bloques, no en aire
        if (wizard.getType() == LocationWizard.WizardType.BLOCK_SELECTION) {
            if (event.getClickedBlock() != null) {
                location = event.getClickedBlock().getLocation();
            } else {
                event.setCancelled(true);
                return;
            }
        } else {
            // Para otros tipos de wizard (como PositionSelection), permitir aire
            if (event.getClickedBlock() != null) {
                location = event.getClickedBlock().getLocation();
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR) {
                location = player.getLocation();
            }
        }

        if (location == null) {
            return;
        }

        boolean handled = wizard.processClick(location, clickType);

        if (handled) {
            event.setCancelled(true);

            // Si el wizard se completó o canceló, limpiarlo
            if (wizard.isCompleted() || wizard.isCancelled()) {
                unregisterWizard(wizard);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpiar wizard cuando el jugador se desconecte
        LocationWizard wizard = activeWizards.get(event.getPlayer().getUniqueId());
        if (wizard != null) {
            wizard.cancel();
            unregisterWizard(wizard);
        }
    }

    private LocationWizard.ClickType getClickType(Action action, boolean sneaking) {
        // Debug más detallado
        Player debugPlayer = null;
        for (LocationWizard wizard : activeWizards.values()) {
            debugPlayer = wizard.getPlayer();
            break;
        }

        switch (action) {
            case LEFT_CLICK_BLOCK:
            case LEFT_CLICK_AIR:
                if (sneaking) {
                    return LocationWizard.ClickType.SHIFT_LEFT_CLICK;
                } else {
                    return LocationWizard.ClickType.LEFT_CLICK;
                }
            case RIGHT_CLICK_BLOCK:
            case RIGHT_CLICK_AIR:
                if (sneaking) {
                    return LocationWizard.ClickType.SHIFT_RIGHT_CLICK;
                } else {
                    return LocationWizard.ClickType.RIGHT_CLICK;
                }
            default:
                return null;
        }
    }

    private void cleanupExpiredWizards() {
        long currentTime = System.currentTimeMillis();
        wizardTimeouts.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue()) {
                LocationWizard wizard = activeWizards.get(entry.getKey());
                if (wizard != null) {
                    wizard.cancel();
                    unregisterWizard(wizard);
                }
                return true;
            }
            return false;
        });
    }

    public void cleanup() {
        cancelAllWizards();
    }

    /**
     * Builder para crear wizards con configuración personalizada
     */
    public static class WizardBuilder {
        private Player player;
        private LocationWizard.WizardType type;
        private int requiredPositions = 1;
        private boolean allowDuplicates = false;
        private Consumer<LocationWizard.WizardState> updateCallback;
        private Consumer<java.util.List<Location>> completeCallback;
        private Runnable cancelCallback;

        public WizardBuilder(Player player) {
            this.player = player;
        }

        public WizardBuilder blockSelection() {
            this.type = LocationWizard.WizardType.BLOCK_SELECTION;
            return this;
        }

        public WizardBuilder positionSelection(int positions) {
            this.type = LocationWizard.WizardType.POSITION_SELECTION;
            this.requiredPositions = positions;
            return this;
        }

        public WizardBuilder allowDuplicates(boolean allow) {
            this.allowDuplicates = allow;
            return this;
        }

        public WizardBuilder onUpdate(Consumer<LocationWizard.WizardState> callback) {
            this.updateCallback = callback;
            return this;
        }

        public WizardBuilder onComplete(Consumer<java.util.List<Location>> callback) {
            this.completeCallback = callback;
            return this;
        }

        public WizardBuilder onCancel(Runnable callback) {
            this.cancelCallback = callback;
            return this;
        }

        public LocationWizard build() {
            if (instance == null) {
                throw new IllegalStateException("LocationWizardManager not initialized!");
            }

            LocationWizard wizard;

            switch (type) {
                case BLOCK_SELECTION:
                    wizard = instance.createBlockSelectionWizard(player);
                    break;
                case POSITION_SELECTION:
                    wizard = instance.createPositionSelectionWizard(player, requiredPositions, allowDuplicates);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid wizard type: " + type);
            }

            if (updateCallback != null) {
                wizard.onUpdate(updateCallback);
            }
            if (completeCallback != null) {
                wizard.onComplete(completeCallback);
            }
            if (cancelCallback != null) {
                wizard.onCancel(cancelCallback);
            }

            return wizard;
        }
    }

    /**
     * Crea un builder para configurar wizards
     */
    public static WizardBuilder builder(Player player) {
        return new WizardBuilder(player);
    }
}