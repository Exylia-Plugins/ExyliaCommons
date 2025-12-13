package net.exylia.commons.v2.action.migration;

import lombok.RequiredArgsConstructor;
import net.exylia.commons.actions.ActionContext;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionMetadata;
import net.exylia.commons.v2.action.model.ActionResult;
import net.exylia.commons.v2.action.parser.ArgumentToken;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class ActionV1Adapter implements Action {
    private final ActionMetadata metadata;
    private final BiConsumer<ActionContext, String[]> legacyHandler;

    @Override
    public CompletableFuture<ActionResult> execute(net.exylia.commons.v2.action.model.ActionContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ActionContext legacyContext = MigrationHelper.convertContextV2ToV1(context);

                String[] args = context.getArguments().getTokens().stream()
                        .map(ArgumentToken::getValue)
                        .toArray(String[]::new);

                legacyHandler.accept(legacyContext, args);

                return ActionResult.success();
            } catch (Exception e) {
                return ActionResult.failure(e);
            }
        });
    }

    @Override
    public ActionMetadata getMetadata() {
        return metadata;
    }
}
