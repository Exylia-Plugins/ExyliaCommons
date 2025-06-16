package net.exylia.commons.wizard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Interfaz base para todos los tipos de wizards de ubicación
 */
public interface LocationWizard {

    /**
     * Obtiene el ID único del wizard
     */
    UUID getWizardId();

    /**
     * Obtiene el jugador asociado al wizard
     */
    Player getPlayer();

    /**
     * Obtiene el tipo de wizard
     */
    WizardType getType();

    /**
     * Procesa un click del jugador
     * @param location La ubicación donde se hizo click
     * @param clickType El tipo de click (LEFT_CLICK, RIGHT_CLICK, SHIFT_LEFT_CLICK, etc.)
     * @return true si el click fue procesado, false si debe continuar el evento normal
     */
    boolean processClick(Location location, ClickType clickType);

    /**
     * Verifica si el wizard está completado
     */
    boolean isCompleted();

    /**
     * Verifica si el wizard está cancelado
     */
    boolean isCancelled();

    /**
     * Cancela el wizard
     */
    void cancel();

    /**
     * Obtiene el progreso actual del wizard (ubicaciones seleccionadas hasta ahora)
     */
    List<Location> getCurrentLocations();

    /**
     * Obtiene información sobre el estado actual del wizard
     */
    WizardState getState();

    /**
     * Callback que se ejecuta cuando el wizard se completa exitosamente
     */
    void onComplete(Consumer<List<Location>> callback);

    /**
     * Callback que se ejecuta cuando el wizard es cancelado
     */
    void onCancel(Runnable callback);

    /**
     * Callback que se ejecuta en cada actualización del wizard
     */
    void onUpdate(Consumer<WizardState> callback);

    /**
     * Limpia los recursos del wizard
     */
    void cleanup();

    /**
     * Enum para los tipos de wizard disponibles
     */
    enum WizardType {
        BLOCK_SELECTION,    // Selección de 2 bloques (área)
        POSITION_SELECTION  // Selección de N posiciones específicas
    }

    /**
     * Enum para los tipos de click
     */
    enum ClickType {
        LEFT_CLICK,
        RIGHT_CLICK,
        SHIFT_LEFT_CLICK,
        SHIFT_RIGHT_CLICK
    }

    /**
     * Clase que representa el estado actual del wizard
     */
    class WizardState {
        private final int currentStep;
        private final int totalSteps;
        private final List<Location> selectedLocations;
        private final String nextAction;

        public WizardState(int currentStep, int totalSteps, List<Location> selectedLocations, String nextAction) {
            this.currentStep = currentStep;
            this.totalSteps = totalSteps;
            this.selectedLocations = selectedLocations;
            this.nextAction = nextAction;
        }

        public int getCurrentStep() { return currentStep; }
        public int getTotalSteps() { return totalSteps; }
        public List<Location> getSelectedLocations() { return selectedLocations; }
        public String getNextAction() { return nextAction; }
        public boolean isCompleted() { return currentStep >= totalSteps; }
        public double getProgress() { return totalSteps > 0 ? (double) currentStep / totalSteps : 0.0; }
    }
}