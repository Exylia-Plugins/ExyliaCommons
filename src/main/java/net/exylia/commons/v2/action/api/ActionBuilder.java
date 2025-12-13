package net.exylia.commons.v2.action.api;

import net.exylia.commons.v2.action.core.ActionManager;
import net.exylia.commons.v2.action.model.*;
import net.exylia.commons.v2.action.parser.ParsedArguments;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class ActionBuilder {
    private final String id;
    private JavaPlugin owner;
    private String namespace;
    private String description = "";
    private String category = "general";
    private ExecutionMode mode = ExecutionMode.SYNC;
    private ActionPriority priority = ActionPriority.NORMAL;
    private String permission;
    private long cooldownMillis = 0;
    private int rateLimit = 0;
    private boolean auditable = false;
    private Set<String> aliases = new HashSet<>();
    private BiConsumer<ActionContext, ParsedArguments> handler;

    public ActionBuilder(String id) {
        this.id = id;
        this.owner = null;
        this.namespace = null;
    }

    public ActionBuilder(String id, JavaPlugin owner) {
        this.id = id;
        this.owner = owner;
        this.namespace = owner.getName().toLowerCase();
    }

    public ActionBuilder plugin(JavaPlugin plugin) {
        this.owner = plugin;
        if (this.namespace == null) {
            this.namespace = plugin.getName().toLowerCase();
        }
        return this;
    }

    public ActionBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ActionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ActionBuilder category(String category) {
        this.category = category;
        return this;
    }

    public ActionBuilder async() {
        this.mode = ExecutionMode.ASYNC;
        return this;
    }

    public ActionBuilder sync() {
        this.mode = ExecutionMode.SYNC;
        return this;
    }

    public ActionBuilder mode(ExecutionMode mode) {
        this.mode = mode;
        return this;
    }

    public ActionBuilder permission(String permission) {
        this.permission = permission;
        return this;
    }

    public ActionBuilder cooldown(long duration, TimeUnit unit) {
        this.cooldownMillis = unit.toMillis(duration);
        return this;
    }

    public ActionBuilder cooldownMillis(long millis) {
        this.cooldownMillis = millis;
        return this;
    }

    public ActionBuilder rateLimit(int requestsPerMinute) {
        this.rateLimit = requestsPerMinute;
        return this;
    }

    public ActionBuilder auditable() {
        this.auditable = true;
        return this;
    }

    public ActionBuilder auditable(boolean auditable) {
        this.auditable = auditable;
        return this;
    }

    public ActionBuilder priority(ActionPriority priority) {
        this.priority = priority;
        return this;
    }

    public ActionBuilder alias(String alias) {
        this.aliases.add(alias);
        return this;
    }

    public ActionBuilder aliases(String... aliases) {
        for (String alias : aliases) {
            this.aliases.add(alias);
        }
        return this;
    }

    public ActionBuilder handler(BiConsumer<ActionContext, ParsedArguments> handler) {
        this.handler = handler;
        return this;
    }

    public CompletableFuture<Action> buildAsync() {
        validateBuilder();

        ActionMetadata metadata = ActionMetadata.builder()
                .id(id)
                .namespace(namespace)
                .description(description)
                .category(category)
                .owner(owner)
                .executionMode(mode)
                .priority(priority)
                .permission(permission)
                .cooldownMillis(cooldownMillis)
                .rateLimit(rateLimit)
                .auditable(auditable)
                .aliases(aliases)
                .build();

        Action action = mode == ExecutionMode.ASYNC
                ? new AsyncAction(metadata, handler)
                : new SyncAction(metadata, handler);

        return ActionManager.getInstance().registerActionAsync(action)
                .thenApply(v -> action);
    }

    public Action build() {
        return buildAsync().join();
    }

    private void validateBuilder() {
        if (id == null || id.isEmpty()) {
            throw new IllegalStateException("Action ID cannot be null or empty");
        }

        if (owner == null) {
            owner = ActionManager.getInstance().getPlugin();
            if (namespace == null) {
                namespace = owner.getName().toLowerCase();
            }
        }

        if (handler == null) {
            throw new IllegalStateException("Handler cannot be null");
        }
    }
}
