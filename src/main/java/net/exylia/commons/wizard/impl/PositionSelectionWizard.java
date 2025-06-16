package net.exylia.commons.wizard.impl;

import net.exylia.commons.wizard.LocationWizard;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Wizard para selección de posiciones específicas
 * SHIFT + CLICK IZQUIERDO = agregar ubicación
 * Se debe especificar el número de ubicaciones requeridas
 */
public class PositionSelectionWizard implements LocationWizard {

    private final UUID wizardId;
    private final Player player;
    private final int requiredPositions;
    private final List<Location> selectedLocations;
    private boolean completed;
    private boolean cancelled;
    private boolean allowDuplicates;

    private Consumer<List<Location>> completeCallback;
    private Runnable cancelCallback;
    private Consumer<WizardState> updateCallback;

    public PositionSelectionWizard(Player player, int requiredPositions) {
        this(player, requiredPositions, false);
    }

    public PositionSelectionWizard(Player player, int requiredPositions, boolean allowDuplicates) {
        this.wizardId = UUID.randomUUID();
        this.player = player;
        this.requiredPositions = Math.max(1, requiredPositions);
        this.selectedLocations = new ArrayList<>();
        this.completed = false;
        this.cancelled = false;
        this.allowDuplicates = allowDuplicates;
    }

    @Override
    public UUID getWizardId() {
        return wizardId;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public WizardType getType() {
        return WizardType.POSITION_SELECTION;
    }

    @Override
    public boolean processClick(Location location, ClickType clickType) {
        if (completed || cancelled) {
            return false;
        }

        Player player = getPlayer();

        if (clickType == ClickType.SHIFT_LEFT_CLICK) {
            if (!allowDuplicates && containsLocation(location)) {
                return true;
            }

            selectedLocations.add(location.clone());
            updateState();

            // Verificar si se completó
            if (selectedLocations.size() >= requiredPositions) {
                completed = true;
                if (completeCallback != null) {
                    completeCallback.accept(new ArrayList<>(selectedLocations));
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        if (!completed && !cancelled) {
            cancelled = true;
            if (cancelCallback != null) {
                cancelCallback.run();
            }
        }
    }

    @Override
    public List<Location> getCurrentLocations() {
        return new ArrayList<>(selectedLocations);
    }

    @Override
    public WizardState getState() {
        int currentStep = selectedLocations.size();
        String nextAction;

        if (currentStep < requiredPositions) {
            nextAction = "SHIFT_LEFT_CLICK_POSITION_" + (currentStep + 1);
        } else {
            nextAction = "COMPLETED";
        }

        return new WizardState(currentStep, requiredPositions, getCurrentLocations(), nextAction);
    }

    @Override
    public void onComplete(Consumer<List<Location>> callback) {
        this.completeCallback = callback;
    }

    @Override
    public void onCancel(Runnable callback) {
        this.cancelCallback = callback;
    }

    @Override
    public void onUpdate(Consumer<WizardState> callback) {
        this.updateCallback = callback;
    }

    @Override
    public void cleanup() {
        selectedLocations.clear();
        completeCallback = null;
        cancelCallback = null;
        updateCallback = null;
    }

    private void updateState() {
        if (updateCallback != null) {
            updateCallback.accept(getState());
        }
    }

    /**
     * Verifica si una ubicación ya existe en la lista
     */
    private boolean containsLocation(Location location) {
        for (Location existing : selectedLocations) {
            if (isSameBlock(existing, location)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compara si dos ubicaciones son el mismo bloque
     */
    private boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getWorld().equals(loc2.getWorld()) &&
                loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }

    /**
     * Permite agregar posiciones manualmente (útil para completar programáticamente)
     */
    public void addPosition(Location location) {
        if (!completed && !cancelled && selectedLocations.size() < requiredPositions) {
            if (!allowDuplicates && containsLocation(location)) {
                return;
            }

            selectedLocations.add(location.clone());
            updateState();

            if (selectedLocations.size() >= requiredPositions) {
                completed = true;
                if (completeCallback != null) {
                    completeCallback.accept(new ArrayList<>(selectedLocations));
                }
            }
        }
    }

    /**
     * Permite remover la última posición agregada
     */
    public boolean removeLastPosition() {
        if (!completed && !cancelled && !selectedLocations.isEmpty()) {
            selectedLocations.remove(selectedLocations.size() - 1);
            updateState();
            return true;
        }
        return false;
    }

    /**
     * Obtiene el número de posiciones requeridas
     */
    public int getRequiredPositions() {
        return requiredPositions;
    }

    /**
     * Verifica si se permiten ubicaciones duplicadas
     */
    public boolean isAllowDuplicates() {
        return allowDuplicates;
    }
}