package net.exylia.commons.v2.action.core;

import net.exylia.commons.v2.action.model.*;
import net.exylia.commons.v2.action.parser.ParsedArguments;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class ActionFactory {

    public Action createSync(String id, JavaPlugin owner, BiConsumer<ActionContext, ParsedArguments> handler) {
        ActionMetadata metadata = ActionMetadata.builder()
                .id(id)
                .namespace(owner.getName().toLowerCase())
                .owner(owner)
                .executionMode(ExecutionMode.SYNC)
                .build();

        return new SyncAction(metadata, handler);
    }

    public Action createAsync(String id, JavaPlugin owner, BiConsumer<ActionContext, ParsedArguments> handler) {
        ActionMetadata metadata = ActionMetadata.builder()
                .id(id)
                .namespace(owner.getName().toLowerCase())
                .owner(owner)
                .executionMode(ExecutionMode.ASYNC)
                .build();

        return new AsyncAction(metadata, handler);
    }

    public Action createChain(String id, JavaPlugin owner, List<Action> actions) {
        ActionMetadata metadata = ActionMetadata.builder()
                .id(id)
                .namespace(owner.getName().toLowerCase())
                .owner(owner)
                .build();

        return new ChainedAction(metadata, actions);
    }

    public Action createConditional(String id, JavaPlugin owner, Predicate<ActionContext> condition, Action action) {
        ActionMetadata metadata = ActionMetadata.builder()
                .id(id)
                .namespace(owner.getName().toLowerCase())
                .owner(owner)
                .build();

        return new ConditionalAction(metadata, condition, action);
    }

    public Action fromMetadata(ActionMetadata metadata, BiConsumer<ActionContext, ParsedArguments> handler) {
        if (metadata.getExecutionMode() == ExecutionMode.ASYNC) {
            return new AsyncAction(metadata, handler);
        }
        return new SyncAction(metadata, handler);
    }
}
