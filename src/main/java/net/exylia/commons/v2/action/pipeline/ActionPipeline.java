package net.exylia.commons.v2.action.pipeline;

import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionResult;

import java.util.*;

public class ActionPipeline {
    private final Map<PipelineStage, List<Middleware>> middlewares;

    public ActionPipeline() {
        this.middlewares = new EnumMap<>(PipelineStage.class);
        for (PipelineStage stage : PipelineStage.values()) {
            middlewares.put(stage, new ArrayList<>());
        }
    }

    public void registerMiddleware(PipelineStage stage, Middleware middleware) {
        List<Middleware> stageMiddlewares = middlewares.get(stage);
        stageMiddlewares.add(middleware);
        stageMiddlewares.sort(Comparator.comparing(Middleware::getPriority));
    }

    public void unregisterMiddleware(PipelineStage stage, Middleware middleware) {
        middlewares.get(stage).remove(middleware);
    }

    public ActionResult execute(Action action, ActionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            executeStage(PipelineStage.PRE_VALIDATE, action, context);
            executeStage(PipelineStage.PRE_EXECUTE, action, context);

            ActionResult result = action.execute(context).join();

            executeStage(PipelineStage.POST_EXECUTE, action, context);

            return result;

        } catch (Exception ex) {
            executeStage(PipelineStage.ERROR, action, context);
            long executionTime = System.currentTimeMillis() - startTime;
            return ActionResult.failure(ex);
        }
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
