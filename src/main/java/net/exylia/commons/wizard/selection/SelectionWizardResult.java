package net.exylia.commons.wizard.selection;

import lombok.Getter;

/**
 * Result of selection wizard completion
 */
@Getter
public class SelectionWizardResult {

    private final SelectionWizardResultType type;
    private final Object value;
    private final String message;

    private SelectionWizardResult(SelectionWizardResultType type, Object value, String message) {
        this.type = type;
        this.value = value;
        this.message = message;
    }

    /**
     * Continue wizard - more selections needed
     */
    public static SelectionWizardResult continueWizard() {
        return new SelectionWizardResult(SelectionWizardResultType.CONTINUE, null, null);
    }

    /**
     * Continue with message
     */
    public static SelectionWizardResult continueWizard(String message) {
        return new SelectionWizardResult(SelectionWizardResultType.CONTINUE, null, message);
    }

    /**
     * Complete wizard with result
     */
    public static SelectionWizardResult complete(Object value) {
        return new SelectionWizardResult(SelectionWizardResultType.COMPLETE, value, null);
    }

    /**
     * Complete with result and message
     */
    public static SelectionWizardResult complete(Object value, String message) {
        return new SelectionWizardResult(SelectionWizardResultType.COMPLETE, value, message);
    }

    /**
     * Cancel the wizard
     */
    public static SelectionWizardResult cancel() {
        return new SelectionWizardResult(SelectionWizardResultType.CANCEL, null, null);
    }

    /**
     * Cancel with message
     */
    public static SelectionWizardResult cancel(String message) {
        return new SelectionWizardResult(SelectionWizardResultType.CANCEL, null, message);
    }

    public enum SelectionWizardResultType {
        CONTINUE,   // Keep selecting more areas
        COMPLETE,   // Wizard completed successfully
        CANCEL      // Cancel the wizard
    }
}