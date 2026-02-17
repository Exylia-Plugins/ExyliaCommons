package net.exylia.commons.v2.wizard.config;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WizardConfig {
    @Builder.Default
    private boolean showTitle = true;
    @Builder.Default
    private boolean showActionBar = true;
    @Builder.Default
    private String titleText = "{warning}⚡ Wizard Active";
    @Builder.Default
    private String subtitleText = "{info}Use SHIFT + CLICK";
    @Builder.Default
    private String actionBarText = "{info}Wizard in progress...";
    @Builder.Default
    private int fadeIn = 0;
    @Builder.Default
    private int stay = 100;
    @Builder.Default
    private int fadeOut = 0;
    @Builder.Default
    private boolean closeInventory = false;
    @Builder.Default
    private long interactionCooldown = 50L;

    public static WizardConfig defaults() {
        return WizardConfig.builder().build();
    }

    public WizardConfig withTitle(String title, String subtitle) {
        return WizardConfig.builder()
                .showTitle(this.showTitle)
                .showActionBar(this.showActionBar)
                .titleText(title)
                .subtitleText(subtitle)
                .actionBarText(this.actionBarText)
                .fadeIn(this.fadeIn)
                .stay(this.stay)
                .fadeOut(this.fadeOut)
                .closeInventory(this.closeInventory)
                .interactionCooldown(this.interactionCooldown)
                .build();
    }

    public WizardConfig withActionBar(String text) {
        return WizardConfig.builder()
                .showTitle(this.showTitle)
                .showActionBar(this.showActionBar)
                .titleText(this.titleText)
                .subtitleText(this.subtitleText)
                .actionBarText(text)
                .fadeIn(this.fadeIn)
                .stay(this.stay)
                .fadeOut(this.fadeOut)
                .closeInventory(this.closeInventory)
                .interactionCooldown(this.interactionCooldown)
                .build();
    }
}
