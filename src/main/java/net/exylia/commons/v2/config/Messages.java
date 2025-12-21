package net.exylia.commons.v2.config;

import net.exylia.commons.v2.placeholders.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class Messages {
    private static Config messagesConfig;
    private static String globalPrefix = "";

    static void init(Config config) {
        messagesConfig = config;
        loadGlobalPrefix();
    }

    private static void loadGlobalPrefix() {
        if (messagesConfig != null) {
            globalPrefix = messagesConfig.string("prefix", "");
        }
    }

    public static void reload() {
        if (messagesConfig != null) {
            messagesConfig.reload();
            loadGlobalPrefix();
        }
    }

    public static MessageBuilder message(String path) {
        return new MessageBuilder(path);
    }

    public static String get(String path) {
        return message(path).raw();
    }

    public static String get(String path, Object... replacements) {
        return message(path).replace(replacements).raw();
    }

    public static String get(String path, PlaceholderContext context) {
        return message(path).context(context).raw();
    }

    public static Component getComponent(String path) {
        return message(path).build();
    }

    public static Component getComponent(String path, Object... replacements) {
        return message(path).replace(replacements).build();
    }

    public static class MessageBuilder {
        private final String path;
        private PlaceholderContext context = PlaceholderContext.create();
        private Player player;
        private final Map<String, Object> replacements = new HashMap<>();
        private boolean usePrefix = true;
        private String customPrefix;

        MessageBuilder(String path) {
            this.path = path;
        }

        public MessageBuilder context(PlaceholderContext context) {
            this.context = context != null ? context : PlaceholderContext.create();
            return this;
        }

        public MessageBuilder with(Object object) {
            this.context.with(object);
            return this;
        }

        public MessageBuilder with(Object... objects) {
            for (Object obj : objects) {
                this.context.with(obj);
            }
            return this;
        }

        public MessageBuilder put(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public MessageBuilder player(Player player) {
            this.player = player;
            return this;
        }

        public MessageBuilder replace(String placeholder, Object value) {
            replacements.put(placeholder, value);
            return this;
        }

        public MessageBuilder replace(Object... args) {
            for (int i = 0; i < args.length - 1; i += 2) {
                replacements.put(args[i].toString(), args[i + 1]);
            }
            return this;
        }

        public MessageBuilder replace(Map<String, Object> replacements) {
            this.replacements.putAll(replacements);
            return this;
        }

        public MessageBuilder noPrefix() {
            this.usePrefix = false;
            return this;
        }

        public MessageBuilder prefix(String prefix) {
            this.customPrefix = prefix;
            return this;
        }

        public Component build() {
            String message = messagesConfig.string(path, "{error}" + path + " not found");

            if (usePrefix) {
                if (customPrefix != null) {
                    message = customPrefix + message;
                } else {
                    message = message.replace("%prefix%", globalPrefix);
                }
            }

            PlaceholderContext ctx = context;
            if (player != null) {
                ctx = ctx.copy().withPlayer(player);
            }
            message = Placeholders.process(message, player, ctx);

            for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                if (!(entry.getValue() instanceof Component)) {
                    message = message.replace(entry.getKey(), entry.getValue().toString());
                }
            }

            Component component = ColorAPI.parse(message);

            for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                if (entry.getValue() instanceof Component) {
                    component = component.replaceText(TextReplacementConfig.builder()
                            .match(entry.getKey())
                            .replacement((Component) entry.getValue())
                            .build());
                }
            }

            return component;
        }

        public String asString() {
            return build().toString();
        }

        public String raw() {
            String message = messagesConfig.string(path, "{error}" + path + " not found");

            if (usePrefix) {
                if (customPrefix != null) {
                    message = customPrefix + message;
                } else {
                    message = message.replace("%prefix%", globalPrefix);
                }
            }

            PlaceholderContext ctx = context;
            if (player != null) {
                ctx = ctx.copy().withPlayer(player);
            }
            message = Placeholders.process(message, player, ctx);

            for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                if (!(entry.getValue() instanceof Component)) {
                    message = message.replace(entry.getKey(), entry.getValue().toString());
                }
            }

            return message;
        }
    }
}
