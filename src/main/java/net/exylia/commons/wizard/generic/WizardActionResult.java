package net.exylia.commons.wizard.generic;

import lombok.Getter;
import net.exylia.commons.config.components.ActionBarConfig;
import net.exylia.commons.config.components.TitleConfig;
import net.exylia.commons.placeholders.ExyliaContext;

/**
 * Result of a wizard action
 */
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

    /**
     * Continue wizard without changes
     */
    public static WizardActionResult continueWizard() {
        return new WizardActionResult(WizardActionType.CONTINUE, null, null, null, null);
    }

    /**
     * Continue wizard and update title
     */
    public static WizardActionResult continueWizard(TitleConfig titleConfig) {
        return new WizardActionResult(WizardActionType.CONTINUE, null, titleConfig, null, null);
    }

    /**
     * Continue wizard and update action bar
     */
    public static WizardActionResult continueWizard(ActionBarConfig actionBarConfig) {
        return new WizardActionResult(WizardActionType.CONTINUE, null, null, actionBarConfig, null);
    }

    /**
     * Continue wizard and update both title and action bar
     */
    public static WizardActionResult continueWizard(TitleConfig titleConfig, ActionBarConfig actionBarConfig) {
        return new WizardActionResult(WizardActionType.CONTINUE, null, titleConfig, actionBarConfig, null);
    }

    /**
     * Complete wizard with result
     */
    public static WizardActionResult complete(Object value) {
        return new WizardActionResult(WizardActionType.COMPLETE, value, null, null, null);
    }

    /**
     * Cancel the wizard
     */
    public static WizardActionResult cancel() {
        return new WizardActionResult(WizardActionType.CANCEL, null, null, null, null);
    }

    public enum WizardActionType {
        CONTINUE,   // Keep wizard active
        COMPLETE,   // Complete wizard with result
        CANCEL      // Cancel the wizard
    }
}