package net.exylia.commons.v2.visual.config;

import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class MessageConfig extends VisualConfig {
    private final List<String> messages;
    private final boolean centered;
    private final boolean broadcast;
    private final Collection<Player> recipients;
    private final Collection<Player> excluded;
    private final Predicate<Player> filter;
    private final MessageType type;

    public MessageConfig(
            List<String> messages,
            boolean centered,
            boolean broadcast,
            Collection<Player> recipients,
            Collection<Player> excluded,
            Predicate<Player> filter,
            MessageType type
    ) {
        this.messages = messages;
        this.centered = centered;
        this.broadcast = broadcast;
        this.recipients = recipients;
        this.excluded = excluded;
        this.filter = filter;
        this.type = type;
    }

    public List<String> getMessages() {
        return new ArrayList<>(messages);
    }

    public boolean isCentered() {
        return centered;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public Collection<Player> getRecipients() {
        return recipients;
    }

    public Collection<Player> getExcluded() {
        return excluded;
    }

    public Predicate<Player> getFilter() {
        return filter;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();

        if (messages == null || messages.isEmpty()) {
            errors.add("At least one message must be provided");
        }

        if (!broadcast && recipients == null && filter == null) {
            errors.add("Must specify recipients, filter, or broadcast mode");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    public enum MessageType {
        CHAT,
        SYSTEM,
        ANNOUNCEMENT
    }
}
