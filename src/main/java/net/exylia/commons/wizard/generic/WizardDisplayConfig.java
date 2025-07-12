package net.exylia.commons.wizard.generic;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for wizard display elements (titles and action bars)
 */
@Getter
@Builder
public class WizardDisplayConfig {

    private final TitleConfig titleConfig;
    private final ActionBarConfig actionBarConfig;

    /**
     * Create config with only title
     */
    public static WizardDisplayConfig title(String title) {
        return WizardDisplayConfig.builder()
                .titleConfig(TitleConfig.builder()
                        .id("generic_wizard")
                        .title(title)
                        .build())
                .build();
    }

    /**
     * Create config with title and subtitle
     */
    public static WizardDisplayConfig title(String title, String subtitle) {
        return WizardDisplayConfig.builder()
                .titleConfig(TitleConfig.builder()
                        .id("generic_wizard")
                        .title(title)
                        .subtitle(subtitle)
                        .build())
                .build();
    }

    /**
     * Create config with only action bar
     */
    public static WizardDisplayConfig actionBar(String text) {
        return WizardDisplayConfig.builder()
                .actionBarConfig(ActionBarConfig.builder()
                        .id("generic_wizard")
                        .text(text)
                        .build())
                .build();
    }

    /**
     * Create config with both title and action bar
     */
    public static WizardDisplayConfig both(String title, String subtitle, String actionBarText) {
        return WizardDisplayConfig.builder()
                .titleConfig(TitleConfig.builder()
                        .id("generic_wizard")
                        .title(title)
                        .subtitle(subtitle)
                        .build())
                .actionBarConfig(ActionBarConfig.builder()
                        .id("generic_wizard")
                        .text(actionBarText)
                        .build())
                .build();
    }

    @Getter
    @Builder
    public static class TitleConfig {
        private final String id;
        private final String title;
        private final String subtitle;
    }

    @Getter
    @Builder
    public static class ActionBarConfig {
        private final String id;
        private final String text;
    }
}