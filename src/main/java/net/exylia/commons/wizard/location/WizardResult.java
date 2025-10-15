package net.exylia.commons.wizard.location;

import lombok.Getter;

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

    public static WizardResult continueWizard() {
        return new WizardResult(WizardResultType.CONTINUE, null, null);
    }

    public static WizardResult continueWizard(String message) {
        return new WizardResult(WizardResultType.CONTINUE, null, message);
    }

    public static WizardResult complete(Object value) {
        return new WizardResult(WizardResultType.COMPLETE, value, null);
    }

    public static WizardResult complete(Object value, String message) {
        return new WizardResult(WizardResultType.COMPLETE, value, message);
    }

    public static WizardResult cancel() {
        return new WizardResult(WizardResultType.CANCEL, null, null);
    }

    public static WizardResult cancel(String message) {
        return new WizardResult(WizardResultType.CANCEL, null, message);
    }

    public enum WizardResultType {
        CONTINUE,    
        COMPLETE,    
        CANCEL       
    }
}
