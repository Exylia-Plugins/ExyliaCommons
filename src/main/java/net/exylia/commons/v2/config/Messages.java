package net.exylia.commons.v2.config;

import net.exylia.commons.v2.placeholders.api.Placeholders;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.visual.api.ColorAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Messages {
    private static Config messagesConfig;
    private static String globalPrefix = "";
    private static final Map<String, Config> fileCache = new ConcurrentHashMap<>();

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
        fileCache.values().forEach(Config::reload);
    }

    public static MessageBuilder message(String path) {
        return new MessageBuilder(null, path);
    }

    public static MessageBuilder message(String filePath, String path) {
        return new MessageBuilder(filePath, path);
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

    public static String get(String filePath, String path) {
        return message(filePath, path).raw();
    }

    public static String get(String filePath, String path, Object... replacements) {
        return message(filePath, path).replace(replacements).raw();
    }

    public static String get(String filePath, String path, PlaceholderContext context) {
        return message(filePath, path).context(context).raw();
    }

    public static List<String> getList(String path) {
        return message(path).rawList();
    }

    public static List<String> getList(String path, Object... replacements) {
        return message(path).replace(replacements).rawList();
    }

    public static List<String> getList(String path, PlaceholderContext context) {
        return message(path).context(context).rawList();
    }

    public static List<String> getList(String filePath, String path) {
        return message(filePath, path).rawList();
    }

    public static List<String> getList(String filePath, String path, Object... replacements) {
        return message(filePath, path).replace(replacements).rawList();
    }

    public static List<String> getList(String filePath, String path, PlaceholderContext context) {
        return message(filePath, path).context(context).rawList();
    }

    public static Object getAny(String path) {
        return getAny(null, path);
    }

    public static Object getAny(String filePath, String path) {
        Config config = getConfigFile(filePath);
        Object value = config.raw().get(path);
        if (value instanceof List) {
            return value;
        }
        return config.string(path);
    }

    public static Component getComponent(String path) {
        return message(path).build();
    }

    public static Component getComponent(String path, Object... replacements) {
        return message(path).replace(replacements).build();
    }

    public static Component getComponent(String filePath, String path) {
        return message(filePath, path).build();
    }

    public static Component getComponent(String filePath, String path, Object... replacements) {
        return message(filePath, path).replace(replacements).build();
    }

    private static Config getConfigFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return messagesConfig;
        }
        return fileCache.computeIfAbsent(filePath, path -> Configs.get(path));
    }

    public static class MessageBuilder {
        private final String filePath;
        private final String path;
        private PlaceholderContext context = PlaceholderContext.create();
        private Player player;
        private final Map<String, Object> replacements = new HashMap<>();
        private boolean usePrefix = true;
        private String customPrefix;

        MessageBuilder(String filePath, String path) {
            this.filePath = filePath;
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
            Config config = getConfigFile(filePath);
            String message = config.string(path, "{error}" + path + " not found");

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
            Config config = getConfigFile(filePath);
            String message = config.string(path, "{error}" + path + " not found");

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

        public List<String> rawList() {
            Config config = getConfigFile(filePath);
            List<String> messages = config.stringList(path);

            if (messages.isEmpty()) {
                return List.of("{error}" + path + " not found");
            }

            PlaceholderContext ctx = context;
            if (player != null) {
                ctx = ctx.copy().withPlayer(player);
            }

            final PlaceholderContext finalCtx = ctx;
            return messages.stream()
                    .map(message -> {
                        if (usePrefix) {
                            if (customPrefix != null) {
                                message = customPrefix + message;
                            } else {
                                message = message.replace("%prefix%", globalPrefix);
                            }
                        }

                        message = Placeholders.process(message, player, finalCtx);

                        for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                            if (!(entry.getValue() instanceof Component)) {
                                message = message.replace(entry.getKey(), entry.getValue().toString());
                            }
                        }

                        return message;
                    })
                    .collect(Collectors.toList());
        }
    }
}
