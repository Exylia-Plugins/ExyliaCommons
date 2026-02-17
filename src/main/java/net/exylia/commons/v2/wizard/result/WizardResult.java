package net.exylia.commons.v2.wizard.result;

import lombok.Getter;
import net.exylia.commons.v2.wizard.config.WizardConfig;
import net.exylia.commons.v2.wizard.model.ResultType;

@Getter
public class WizardResult<T> {
    private final ResultType type;
    private final T value;
    private final WizardConfig displayConfig;

    private WizardResult(ResultType type, T value, WizardConfig displayConfig) {
        this.type = type;
        this.value = value;
        this.displayConfig = displayConfig;
    }

    public static <T> WizardResult<T> continueWizard() {
        return new WizardResult<>(ResultType.CONTINUE, null, null);
    }

    public static <T> WizardResult<T> continueWith(WizardConfig config) {
        return new WizardResult<>(ResultType.CONTINUE, null, config);
    }

    public static <T> WizardResult<T> continueWith(String title, String subtitle) {
        WizardConfig config = WizardConfig.builder()
                .titleText(title)
                .subtitleText(subtitle)
                .build();
        return new WizardResult<>(ResultType.CONTINUE, null, config);
    }

    public static <T> WizardResult<T> complete(T value) {
        return new WizardResult<>(ResultType.COMPLETE, value, null);
    }

    public static <T> WizardResult<T> cancel() {
        return new WizardResult<>(ResultType.CANCEL, null, null);
    }

    public boolean shouldContinue() {
        return type == ResultType.CONTINUE;
    }

    public boolean isComplete() {
        return type == ResultType.COMPLETE;
    }

    public boolean isCancelled() {
        return type == ResultType.CANCEL;
    }

    public boolean hasDisplayConfig() {
        return displayConfig != null;
    }
}
