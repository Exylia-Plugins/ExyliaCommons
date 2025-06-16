package net.exylia.commons.wizard.impl;

import net.exylia.commons.wizard.LocationWizard;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Wizard para selección de bloques (área rectangular)
 * Click izquierdo = primera ubicación
 * Click derecho = segunda ubicación
 */
public class BlockSelectionWizard implements LocationWizard {

    private final UUID wizardId;
    private final Player player;
    private final List<Location> selectedLocations;
    private boolean completed;
    private boolean cancelled;

    private Consumer<List<Location>> completeCallback;
    private Runnable cancelCallback;
    private Consumer<WizardState> updateCallback;

    public BlockSelectionWizard(Player player) {
        this.wizardId = UUID.randomUUID();
        this.player = player;
        this.selectedLocations = new ArrayList<>();
        this.completed = false;
        this.cancelled = false;
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
        return WizardType.BLOCK_SELECTION;
    }

    @Override
    public boolean processClick(Location location, ClickType clickType) {
        if (completed || cancelled) {
            return false;
        }

        // Validar que la ubicación sea un bloque válido (no aire)
        if (location.getBlock().getType().isAir()) {
            player.sendMessage("§c¡No puedes seleccionar aire! Haz click en un bloque sólido.");
            return true; // Consumir el evento pero no procesar la selección
        }

        switch (clickType) {
            case LEFT_CLICK:
                // Primera ubicación
                if (selectedLocations.isEmpty()) {
                    selectedLocations.add(location.clone());
                    player.sendMessage("§a¡Primera posición seleccionada! Haz click derecho para la segunda posición.");
                    updateState();
                    return true;
                } else if (selectedLocations.size() == 1) {
                    // Reemplazar primera ubicación
                    selectedLocations.set(0, location.clone());
                    player.sendMessage("§a¡Primera posición actualizada! Haz click derecho para la segunda posición.");
                    updateState();
                    return true;
                }
                break;

            case RIGHT_CLICK:
                // Segunda ubicación
                if (selectedLocations.size() >= 1) {
                    if (selectedLocations.size() == 2) {
                        selectedLocations.set(1, location.clone());
                    } else {
                        selectedLocations.add(location.clone());
                    }

                    // Mostrar información del área seleccionada
                    Location pos1 = selectedLocations.get(0);
                    Location pos2 = selectedLocations.get(1);
                    int sizeX = Math.abs(pos2.getBlockX() - pos1.getBlockX()) + 1;
                    int sizeY = Math.abs(pos2.getBlockY() - pos1.getBlockY()) + 1;
                    int sizeZ = Math.abs(pos2.getBlockZ() - pos1.getBlockZ()) + 1;
                    int totalBlocks = sizeX * sizeY * sizeZ;

                    player.sendMessage("§a¡Selección completada!");
                    player.sendMessage("§eÁrea seleccionada: " + sizeX + "x" + sizeY + "x" + sizeZ + " (" + totalBlocks + " bloques)");

                    // Completar wizard
                    completed = true;
                    updateState();

                    if (completeCallback != null) {
                        completeCallback.accept(new ArrayList<>(selectedLocations));
                    }
                    return true;
                } else {
                    player.sendMessage("§c¡Primero debes seleccionar la primera posición con click izquierdo!");
                    return true;
                }
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

        if (currentStep == 0) {
            nextAction = "LEFT_CLICK_FIRST_BLOCK";
        } else if (currentStep == 1) {
            nextAction = "RIGHT_CLICK_SECOND_BLOCK";
        } else {
            nextAction = "COMPLETED";
        }

        return new WizardState(currentStep, 2, getCurrentLocations(), nextAction);
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
}