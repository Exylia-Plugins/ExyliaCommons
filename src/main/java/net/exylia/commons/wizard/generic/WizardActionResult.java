package net.exylia.commons.wizard.generic;

import lombok.Getter;
import net.exylia.commons.config.components.ActionBarConfig;
import net.exylia.commons.config.components.TitleConfig;
import net.exylia.commons.placeholders.ExyliaContext;

@Getter
public class WizardActionResult {

    private final WizardActionType type;
    private final Object value;
    private final TitleConfig titleConfig;
    private final ActionBarConfig actionBarConfig;
    private final ExyliaContext context;

    private WizardActionResult(WizardActionType type, Object value, TitleConfig titleConfig, ActionBarConfig actionBarConfig, ExyliaContext context) {
        this.type = type;
        this.value = value;
        this.titleConfig = titleConfig;
        this.actionBarConfig = actionBarConfig;
        this.context = context;
    }

    public static WizardActionResult continueWizard() {
        return new WizardActionResult(WizardActionType.CONTINUE, null, null, null, null);
    }

    public static WizardActionResult continueWizard(TitleConfig titleConfig) {
        return new WizardActionResult(WizardActionType.CONTINUE, null, titleConfig, null, null);
    }

    public static WizardActionResult continueWizard(ActionBarConfig actionBarConfig) {
        return new WizardActionResult(WizardActionType.CONTINUE, null, null, actionBarConfig, null);
    }

    public static WizardActionResult continueWizard(TitleConfig titleConfig, ActionBarConfig actionBarConfig) {
        return new WizardActionResult(WizardActionType.CONTINUE, null, titleConfig, actionBarConfig, null);
    }

    public static WizardActionResult complete(Object value) {
        return new WizardActionResult(WizardActionType.COMPLETE, value, null, null, null);
    }

    public static WizardActionResult cancel() {
        return new WizardActionResult(WizardActionType.CANCEL, null, null, null, null);
    }

    public enum WizardActionType {
        CONTINUE,    
        COMPLETE,    
        CANCEL       
    }
}
