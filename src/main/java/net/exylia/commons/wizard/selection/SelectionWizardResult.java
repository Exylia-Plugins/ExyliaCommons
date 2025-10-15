package net.exylia.commons.wizard.selection;

import lombok.Getter;

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

    public static SelectionWizardResult continueWizard() {
        return new SelectionWizardResult(SelectionWizardResultType.CONTINUE, null, null);
    }

    public static SelectionWizardResult continueWizard(String message) {
        return new SelectionWizardResult(SelectionWizardResultType.CONTINUE, null, message);
    }

    public static SelectionWizardResult complete(Object value) {
        return new SelectionWizardResult(SelectionWizardResultType.COMPLETE, value, null);
    }

    public static SelectionWizardResult complete(Object value, String message) {
        return new SelectionWizardResult(SelectionWizardResultType.COMPLETE, value, message);
    }

    public static SelectionWizardResult cancel() {
        return new SelectionWizardResult(SelectionWizardResultType.CANCEL, null, null);
    }

    public static SelectionWizardResult cancel(String message) {
        return new SelectionWizardResult(SelectionWizardResultType.CANCEL, null, message);
    }

    public enum SelectionWizardResultType {
        CONTINUE,    
        COMPLETE,    
        CANCEL       
    }
}
