package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.visual.config.MessageConfig;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class MessageBuilder extends VisualBuilder<MessageConfig, MessageBuilder> {
    private List<String> messages = new ArrayList<>();
    private boolean centered = false;
    private boolean broadcast = false;
    private Collection<Player> recipients = null;
    private Collection<Player> excluded = null;
    private Predicate<Player> filter = null;
    private MessageConfig.MessageType type = MessageConfig.MessageType.CHAT;

    private MessageBuilder() {
    }

    public static MessageBuilder create() {
        return new MessageBuilder();
    }

    public MessageBuilder message(String message) {
        this.messages.add(message);
        return this;
    }

    public MessageBuilder messages(List<String> messages) {
        this.messages.addAll(messages);
        return this;
    }

    public MessageBuilder messages(String... messages) {
        this.messages.addAll(List.of(messages));
        return this;
    }

    public MessageBuilder centered() {
        this.centered = true;
        return this;
    }

    public MessageBuilder centered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public MessageBuilder broadcast() {
        this.broadcast = true;
        return this;
    }

    public MessageBuilder to(Player player) {
        if (this.recipients == null) {
            this.recipients = new ArrayList<>();
        }
        this.recipients.add(player);
        return this;
    }

    public MessageBuilder to(Collection<Player> players) {
        if (this.recipients == null) {
            this.recipients = new ArrayList<>();
        }
        this.recipients.addAll(players);
        return this;
    }

    public MessageBuilder excluding(Player player) {
        if (this.excluded == null) {
            this.excluded = new ArrayList<>();
        }
        this.excluded.add(player);
        return this;
    }

    public MessageBuilder excluding(Collection<Player> players) {
        if (this.excluded == null) {
            this.excluded = new ArrayList<>();
        }
        this.excluded.addAll(players);
        return this;
    }

    public MessageBuilder filter(Predicate<Player> filter) {
        this.filter = filter;
        return this;
    }

    public MessageBuilder type(MessageConfig.MessageType type) {
        this.type = type;
        return this;
    }

    @Override
    protected ValidationResult validateInternal() {
        List<String> errors = new ArrayList<>();

        if (messages.isEmpty()) {
            errors.add("At least one message must be provided");
        }

        if (!broadcast && recipients == null && filter == null) {
            errors.add("Must specify recipients, filter, or broadcast mode");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    protected MessageConfig buildInternal() {
        return new MessageConfig(
                messages,
                centered,
                broadcast,
                recipients,
                excluded,
                filter,
                type
        );
    }
}
