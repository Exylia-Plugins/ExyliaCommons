package net.exylia.commons.v2.action.pipeline;

import net.exylia.commons.v2.action.exception.ActionException;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionResult;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ActionPipeline {
    private static final String RESULT_KEY = "__pipeline_result__";
    private final Map<PipelineStage, List<Middleware>> middlewares;

    public ActionPipeline() {
        this.middlewares = new EnumMap<>(PipelineStage.class);
        for (PipelineStage stage : PipelineStage.values()) {
            middlewares.put(stage, new CopyOnWriteArrayList<>());
        }
    }

    public void registerMiddleware(PipelineStage stage, Middleware middleware) {
        List<Middleware> stageMiddlewares = middlewares.get(stage);
        stageMiddlewares.add(middleware);
        ((CopyOnWriteArrayList<Middleware>) stageMiddlewares).sort(Comparator.comparing(Middleware::getPriority));
    }

    public void unregisterMiddleware(PipelineStage stage, Middleware middleware) {
        middlewares.get(stage).remove(middleware);
    }

    public ActionResult execute(Action action, ActionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            DebugAPI.logLibDebug(DebugCategory.ACTION, "Pipeline: PRE_VALIDATE for " + action.getMetadata().getFullId());
            executeStage(PipelineStage.PRE_VALIDATE, action, context);

            DebugAPI.logLibDebug(DebugCategory.ACTION, "Pipeline: Checking canExecute() for " + action.getMetadata().getFullId());
            if (!action.canExecute(context)) {
                throw new ActionException.ActionValidationException("Action canExecute() returned false");
            }

            DebugAPI.logLibDebug(DebugCategory.ACTION, "Pipeline: PRE_EXECUTE for " + action.getMetadata().getFullId());
            executeStage(PipelineStage.PRE_EXECUTE, action, context);

            DebugAPI.logLibDebug(DebugCategory.ACTION, "Pipeline: Calling action.execute() for " + action.getMetadata().getFullId());
            ActionResult result = action.execute(context).join();
            DebugAPI.logLibDebug(DebugCategory.ACTION, "Pipeline: action.execute() returned for " + action.getMetadata().getFullId() + " - success: " + result.isSuccess());

            context.getData().put(RESULT_KEY, result);
            executeStage(PipelineStage.POST_EXECUTE, action, context);

            return result;

        } catch (Exception ex) {
            DebugAPI.logLibError(DebugCategory.ACTION, "Pipeline: Exception during execution of " + action.getMetadata().getFullId(), ex);
            context.getData().put(RESULT_KEY, ActionResult.failure(ex));
            executeStage(PipelineStage.ERROR, action, context);
            return ActionResult.failure(ex);
        }
    }

    public static ActionResult getResultFromContext(ActionContext context) {
        return (ActionResult) context.getData().get(RESULT_KEY);
    }

    private void executeStage(PipelineStage stage, Action action, ActionContext context) {
        for (Middleware middleware : middlewares.get(stage)) {
            if (middleware.isEnabled()) {
                middleware.execute(action, context);
            }
        }
    }

    public List<Middleware> getMiddlewares(PipelineStage stage) {
        return Collections.unmodifiableList(middlewares.get(stage));
    }

    public void clearMiddlewares(PipelineStage stage) {
        middlewares.get(stage).clear();
    }

    public void clearAll() {
        for (PipelineStage stage : PipelineStage.values()) {
            middlewares.get(stage).clear();
        }
    }
}
