package net.exylia.commons.wizard.generic;

import lombok.Getter;

/**
 * Result of a wizard action
 */
@Getter
public class WizardActionResult {

    private final WizardActionType type;
    private final Object value;
    private final WizardDisplayConfig displayConfig;

    private WizardActionResult(WizardActionType type, Object value, WizardDisplayConfig displayConfig) {
        this.type = type;
        this.value = value;
        this.displayConfig = displayConfig;
    }

    /**
     * Continue wizard without changes
     */
    public static WizardActionResult continueWizard() {
        return new WizardActionResult(WizardActionType.CONTINUE, null, null);
    }

    /**
     * Continue wizard and update display
     */
    public static WizardActionResult continueWizard(WizardDisplayConfig displayConfig) {
        return new WizardActionResult(WizardActionType.CONTINUE, null, displayConfig);
    }

    /**
     * Complete wizard with result
     */
    public static WizardActionResult complete(Object value) {
        return new WizardActionResult(WizardActionType.COMPLETE, value, null);
    }

    /**
     * Cancel the wizard
     */
    public static WizardActionResult cancel() {
        return new WizardActionResult(WizardActionType.CANCEL, null, null);
    }

    public enum WizardActionType {
        CONTINUE,   // Keep wizard active
        COMPLETE,   // Complete wizard with result
        CANCEL      // Cancel the wizard
    }
}