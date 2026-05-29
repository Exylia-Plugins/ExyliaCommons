package net.exylia.commons.v2.chat.api;

import net.exylia.commons.v2.chat.core.ChatInputManager;
import net.exylia.commons.v2.chat.request.BooleanInputRequest;
import net.exylia.commons.v2.chat.request.ConfirmationRequest;
import net.exylia.commons.v2.chat.request.MultiNumberInputRequest;
import net.exylia.commons.v2.chat.request.NumberFieldDef;
import net.exylia.commons.v2.chat.request.NumberInputRequest;
import net.exylia.commons.v2.chat.request.SingleOptionRequest;
import net.exylia.commons.v2.chat.request.TextInputRequest;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class ChatInputAPI {

    private ChatInputAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void init(Plugin plugin) {
        ChatInputManager.init(plugin);
    }

    public static void shutdown() {
        ChatInputManager.shutdown();
    }

    public static void cancel(Player player) {
        ChatInputManager.getInstance().cancelSession(player);
    }

    public static boolean hasActiveSession(Player player) {
        return ChatInputManager.getInstance().hasActiveSession(player);
    }

    public static TextBuilder text(Player player, String prompt) {
        return new TextBuilder(player, prompt);
    }

    public static IntegerBuilder integer(Player player, String prompt) {
        return new IntegerBuilder(player, prompt);
    }

    public static DecimalBuilder decimal(Player player, String prompt) {
        return new DecimalBuilder(player, prompt);
    }

    public static BoolBuilder bool(Player player, String prompt) {
        return new BoolBuilder(player, prompt);
    }

    public static OptionBuilder option(Player player, String prompt) {
        return new OptionBuilder(player, prompt);
    }

    public static ConfirmBuilder confirm(Player player, String prompt) {
        return new ConfirmBuilder(player, prompt);
    }

    public static MultiNumberBuilder numbers(Player player, String prompt) {
        return new MultiNumberBuilder(player, prompt);
    }

    public static IdBuilder id(Player player, String prompt) {
        return new IdBuilder(player, prompt);
    }

    public static final class TextBuilder {
        private final TextInputRequest request;

        private TextBuilder(Player player, String prompt) {
            this.request = new TextInputRequest(player, prompt);
        }

        public TextBuilder maxLength(int maxLength) {
            request.setMaxLength(maxLength);
            return this;
        }

        public TextBuilder validator(Predicate<String> validator) {
            request.setValidator(validator);
            return this;
        }

        public TextBuilder onResponse(Consumer<String> callback) {
            request.setOnResponse(callback);
            return this;
        }

        public TextBuilder onCancel(Runnable callback) {
            request.setOnCancel(callback);
            return this;
        }

        public TextBuilder forceChat() {
            request.setForceChat(true);
            return this;
        }

        public TextBuilder forceTitle(String titleText) {
            request.setTitleText(titleText);
            return this;
        }

        public void ask() {
            ChatInputManager.getInstance().submit(request.getPlayer(), request);
        }
    }

    public static final class IntegerBuilder {
        private final NumberInputRequest request;

        private IntegerBuilder(Player player, String prompt) {
            this.request = new NumberInputRequest(player, prompt, false);
        }

        public IntegerBuilder range(long min, long max) {
            request.setMin(min);
            request.setMax(max);
            return this;
        }

        public IntegerBuilder validator(Predicate<Long> validator) {
            request.setValidator(n -> validator.test(n.longValue()));
            return this;
        }

        public IntegerBuilder onResponse(Consumer<Number> callback) {
            request.setOnResponse(callback);
            return this;
        }

        public IntegerBuilder onCancel(Runnable callback) {
            request.setOnCancel(callback);
            return this;
        }

        public IntegerBuilder forceTitle(String titleText) {
            request.setTitleText(titleText);
            return this;
        }

        public void ask() {
            ChatInputManager.getInstance().submit(request.getPlayer(), request);
        }
    }

    public static final class DecimalBuilder {
        private final NumberInputRequest request;

        private DecimalBuilder(Player player, String prompt) {
            this.request = new NumberInputRequest(player, prompt, true);
        }

        public DecimalBuilder range(double min, double max) {
            request.setMin(min);
            request.setMax(max);
            return this;
        }

        public DecimalBuilder validator(Predicate<Double> validator) {
            request.setValidator(n -> validator.test(n.doubleValue()));
            return this;
        }

        public DecimalBuilder onResponse(Consumer<Number> callback) {
            request.setOnResponse(callback);
            return this;
        }

        public DecimalBuilder onCancel(Runnable callback) {
            request.setOnCancel(callback);
            return this;
        }

        public DecimalBuilder forceTitle(String titleText) {
            request.setTitleText(titleText);
            return this;
        }

        public void ask() {
            ChatInputManager.getInstance().submit(request.getPlayer(), request);
        }
    }

    public static final class BoolBuilder {
        private final BooleanInputRequest request;

        private BoolBuilder(Player player, String prompt) {
            this.request = new BooleanInputRequest(player, prompt);
        }

        public BoolBuilder onResponse(Consumer<Boolean> callback) {
            request.setOnResponse(callback);
            return this;
        }

        public BoolBuilder onCancel(Runnable callback) {
            request.setOnCancel(callback);
            return this;
        }

        public void ask() {
            ChatInputManager.getInstance().submit(request.getPlayer(), request);
        }
    }

    public static final class OptionBuilder {
        private final SingleOptionRequest request;

        private OptionBuilder(Player player, String prompt) {
            this.request = new SingleOptionRequest(player, prompt);
        }

        public OptionBuilder option(String key, String label) {
            request.addOption(key, label);
            return this;
        }

        public OptionBuilder onResponse(Consumer<String> callback) {
            request.setOnResponse(callback);
            return this;
        }

        public OptionBuilder onCancel(Runnable callback) {
            request.setOnCancel(callback);
            return this;
        }

        public OptionBuilder columns(int columns) {
            request.setColumns(Math.max(1, columns));
            return this;
        }

        public void ask() {
            ChatInputManager.getInstance().submit(request.getPlayer(), request);
        }
    }

    public static final class ConfirmBuilder {
        private final ConfirmationRequest request;

        private ConfirmBuilder(Player player, String prompt) {
            this.request = new ConfirmationRequest(player, prompt);
        }

        public ConfirmBuilder onConfirm(Runnable callback) {
            request.setOnConfirm(callback);
            return this;
        }

        public ConfirmBuilder onDeny(Runnable callback) {
            request.setOnDeny(callback);
            return this;
        }

        public ConfirmBuilder onCancel(Runnable callback) {
            request.setOnCancel(callback);
            return this;
        }

        public void ask() {
            ChatInputManager.getInstance().submit(request.getPlayer(), request);
        }
    }

    public static final class MultiNumberBuilder {
        private final MultiNumberInputRequest request;

        private MultiNumberBuilder(Player player, String prompt) {
            this.request = new MultiNumberInputRequest(player, prompt);
        }

        public MultiNumberBuilder field(String key, String label, long min, long max) {
            request.addField(new NumberFieldDef(key, label, min, max, false));
            return this;
        }

        public MultiNumberBuilder fieldDecimal(String key, String label, double min, double max) {
            request.addField(new NumberFieldDef(key, label, min, max, true));
            return this;
        }

        public MultiNumberBuilder onResponse(Consumer<Map<String, Number>> callback) {
            request.setOnResponse(callback);
            return this;
        }

        public MultiNumberBuilder onCancel(Runnable callback) {
            request.setOnCancel(callback);
            return this;
        }

        public void ask() {
            ChatInputManager.getInstance().submit(request.getPlayer(), request);
        }
    }

    public static final class IdBuilder {
        private static final Pattern SPACES = Pattern.compile("\\s+");
        private static final Pattern INVALID = Pattern.compile("[^a-z0-9_\\-]");
        private static final Pattern TRIM_EDGES = Pattern.compile("^[_\\-]+|[_\\-]+$");

        private final TextInputRequest request;

        private IdBuilder(Player player, String prompt) {
            this.request = new TextInputRequest(player, prompt);
            this.request.setMaxLength(32);
            this.request.setTransformer(input -> {
                String result = SPACES.matcher(input.toLowerCase()).replaceAll("_");
                result = INVALID.matcher(result).replaceAll("");
                return TRIM_EDGES.matcher(result).replaceAll("");
            });
            this.request.setValidator(s -> !s.isEmpty());
        }

        public IdBuilder maxLength(int maxLength) {
            request.setMaxLength(maxLength);
            return this;
        }

        public IdBuilder validator(Predicate<String> validator) {
            request.setValidator(s -> !s.isEmpty() && validator.test(s));
            return this;
        }

        public IdBuilder onResponse(Consumer<String> callback) {
            request.setOnResponse(callback);
            return this;
        }

        public IdBuilder onCancel(Runnable callback) {
            request.setOnCancel(callback);
            return this;
        }

        public IdBuilder forceChat() {
            request.setForceChat(true);
            return this;
        }

        public IdBuilder forceTitle(String titleText) {
            request.setTitleText(titleText);
            return this;
        }

        public void ask() {
            ChatInputManager.getInstance().submit(request.getPlayer(), request);
        }
    }
}
