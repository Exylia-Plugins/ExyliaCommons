package net.exylia.commons.wizard.location;

import lombok.Getter;

/**
 * Result of wizard location selection
 */
@Getter
public class WizardResult {

    private final WizardResultType type;
    private final Object value;
    private final String message;

    private WizardResult(WizardResultType type, Object value, String message) {
        this.type = type;
        this.value = value;
        this.message = message;
    }

    /**
     * Continue wizard - more positions needed
     */
    public static WizardResult continueWizard() {
        return new WizardResult(WizardResultType.CONTINUE, null, null);
    }

    /**
     * Continue with message
     */
    public static WizardResult continueWizard(String message) {
        return new WizardResult(WizardResultType.CONTINUE, null, message);
    }

    /**
     * Complete wizard with result
     */
    public static WizardResult complete(Object value) {
        return new WizardResult(WizardResultType.COMPLETE, value, null);
    }

    /**
     * Complete with result and message
     */
    public static WizardResult complete(Object value, String message) {
        return new WizardResult(WizardResultType.COMPLETE, value, message);
    }

    /**
     * Cancel the wizard
     */
    public static WizardResult cancel() {
        return new WizardResult(WizardResultType.CANCEL, null, null);
    }

    /**
     * Cancel with message
     */
    public static WizardResult cancel(String message) {
        return new WizardResult(WizardResultType.CANCEL, null, message);
    }

    public enum WizardResultType {
        CONTINUE,   // Keep selecting more positions
        COMPLETE,   // Wizard completed successfully
        CANCEL      // Cancel the wizard
    }
}