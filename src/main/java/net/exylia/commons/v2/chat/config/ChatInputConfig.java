package net.exylia.commons.v2.chat.config;

import lombok.Builder;
import lombok.Getter;

import java.util.function.Predicate;

@Getter
@Builder(toBuilder = true)
public class ChatInputConfig {

    @Builder.Default
    private String prompt = null;

    @Builder.Default
    private int timeout = 30;

    @Builder.Default
    private String cancelWord = "cancel";

    @Builder.Default
    private Predicate<String> validator = null;

    @Builder.Default
    private String invalidMessage = "&cInvalid input, try again.";

    @Builder.Default
    private String cancelMessage = "&cInput cancelled.";

    @Builder.Default
    private String timeoutMessage = "&cTime expired.";

    @Builder.Default
    private Runnable onCancel = null;

    @Builder.Default
    private Runnable onTimeout = null;

    @Builder.Default
    private boolean closeInventory = true;

    @Builder.Default
    private boolean showTitle = true;

    @Builder.Default
    private String titleText = "&e&lType in chat";

    @Builder.Default
    private String subtitleText = "&7Time: %time%s";
}
