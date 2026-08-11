package net.exylia.commons.v2.chat.config;

import net.exylia.commons.v2.config.schema.Comment;
import net.exylia.commons.v2.config.schema.ConfigSchema;
import net.exylia.commons.v2.config.schema.ConfigSection;
import net.exylia.commons.v2.config.schema.ConfigValue;
@ConfigSchema(file = "config", strict = true, version = "1.1")
public class ChatInputDefaults {

    @ConfigSection("chat-input")
    public static class ChatInput {

        @ConfigSection("chat-fallback")
        public static class ChatFallback {

            @ConfigValue("cancel-word")
            public static String CANCEL_WORD = "cancel";

            @ConfigValue("cancel-message")
            public static String CANCEL_MESSAGE = "&cInput cancelled.";

            @ConfigValue("invalid-message")
            public static String INVALID_MESSAGE = "&cInvalid input, try again.";

            @ConfigValue("show-title")
            public static boolean SHOW_TITLE = true;

            @ConfigValue("subtitle-text")
            public static String SUBTITLE_TEXT = "&7Type in chat or &ccancel&7 to cancel";
        }

        @ConfigSection("ui-fallback")
        public static class UIFallback {

            @ConfigValue("yes-label")
            public static String YES_LABEL = "&aYes";

            @ConfigValue("no-label")
            public static String NO_LABEL = "&cNo";

            @ConfigValue("cancel-label")
            public static String CANCEL_LABEL = "&cCancel";

            @ConfigValue("yes-material")
            public static String YES_MATERIAL = "LIME_CONCRETE";

            @ConfigValue("no-material")
            public static String NO_MATERIAL = "RED_CONCRETE";

            @ConfigValue("cancel-material")
            public static String CANCEL_MATERIAL = "RED_CONCRETE";

            @ConfigValue("filler-material")
            public static String FILLER_MATERIAL = "GRAY_STAINED_GLASS_PANE";
        }

        @ConfigSection("dialog")
        public static class Dialog {

            @ConfigValue("cancel-label")
            public static String CANCEL_LABEL = "Cancel";

            @ConfigValue("submit-label")
            public static String SUBMIT_LABEL = "Submit";

            @ConfigValue("search-label")
            @Comment("Label of the search field shown above a searchable option list.")
            public static String SEARCH_LABEL = "{letters}Search";

            @ConfigValue("search-button")
            @Comment("Button that applies whatever was typed in the search field.")
            public static String SEARCH_BUTTON = "{info}\uD83D\uDD0D Search";

            @ConfigValue("clear-search-button")
            @Comment("Button that clears the active search filter.")
            public static String CLEAR_SEARCH_BUTTON = "{error}\u2716 Clear";

            @ConfigValue("previous-button")
            @Comment("Previous-page button in a paged option list.")
            public static String PREVIOUS_BUTTON = "{secondary}\u2190 Previous";

            @ConfigValue("next-button")
            @Comment("Next-page button in a paged option list.")
            public static String NEXT_BUTTON = "{secondary}Next \u2192";

            @ConfigValue("page-line")
            @Comment("Page indicator line. Supports %page% and %total%.")
            public static String PAGE_LINE = "{muted}Page {highlight}%page% {muted}of {highlight}%total%";

            @ConfigValue("filter-line")
            @Comment("Active-filter line. Supports %query% and %matches%.")
            public static String FILTER_LINE = "{muted}Filter: {highlight}%query% {muted}({highlight}%matches% {muted}matches)";

            @ConfigValue("no-matches-line")
            @Comment("Shown when a search returns nothing.")
            public static String NO_MATCHES_LINE = "{error}No matches. Try a different search.";
        }

        @ConfigSection("bedrock")
        public static class Bedrock {

            @ConfigValue("prefix")
            public static String PREFIX = "*";
        }
    }
}
