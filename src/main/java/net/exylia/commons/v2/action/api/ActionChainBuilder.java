package net.exylia.commons.v2.action.api;

import net.exylia.commons.v2.action.core.ActionManager;
import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionMetadata;
import net.exylia.commons.v2.action.model.ChainedAction;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ActionChainBuilder {
    private final String id;
    private JavaPlugin owner;
    private final List<String> actionIds = new ArrayList<>();
    private String namespace;
    private String description = "";

    public ActionChainBuilder(String id) {
        this.id = id;
        this.owner = null;
        this.namespace = null;
    }

    public ActionChainBuilder(String id, JavaPlugin owner) {
        this.id = id;
        this.owner = owner;
        this.namespace = owner.getName().toLowerCase();
    }

    public ActionChainBuilder plugin(JavaPlugin plugin) {
        this.owner = plugin;
        if (this.namespace == null) {
            this.namespace = plugin.getName().toLowerCase();
        }
        return this;
    }

    public ActionChainBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ActionChainBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ActionChainBuilder then(String actionId) {
        actionIds.add(actionId);
        return this;
    }

    public ActionChainBuilder thenAll(String... actionIds) {
        for (String actionId : actionIds) {
            this.actionIds.add(actionId);
        }
        return this;
    }

    public CompletableFuture<Action> buildAsync() {
        validateBuilder();

        return CompletableFuture.supplyAsync(() -> {
            List<Action> actions = new ArrayList<>();

            for (String actionId : actionIds) {
                Action action = ActionManager.getInstance().getRegistry().get(actionId)
                        .orElseThrow(() -> new ActionException.ActionNotFoundException(actionId));
                actions.add(action);
            }

            ActionMetadata metadata = ActionMetadata.builder()
                    .id(id)
                    .namespace(namespace)
                    .description(description)
                    .owner(owner)
                    .build();

            Action chain = new ChainedAction(metadata, actions);

            ActionManager.getInstance().registerAction(chain);

            return chain;
        });
    }

    public Action build() {
        return buildAsync().join();
    }

    private void validateBuilder() {
        if (id == null || id.isEmpty()) {
            throw new IllegalStateException("Chain ID cannot be null or empty");
        }

        if (owner == null) {
            owner = ActionManager.getInstance().getPlugin();
            if (namespace == null) {
                namespace = owner.getName().toLowerCase();
            }
        }

        if (actionIds.isEmpty()) {
            throw new IllegalStateException("Chain must contain at least one action");
        }
    }
}
