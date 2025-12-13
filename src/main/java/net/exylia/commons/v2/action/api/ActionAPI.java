package net.exylia.commons.v2.action.api;

import net.exylia.commons.v2.action.core.ActionManager;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionResult;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ActionAPI {

    private ActionAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(JavaPlugin plugin) {
        ActionManager.initialize(plugin);
    }

    public static boolean isInitialized() {
        return ActionManager.isInitialized();
    }

    public static ActionBuilder create(String id) {
        return new ActionBuilder(id);
    }

    public static ActionBuilder create(String id, JavaPlugin owner) {
        return new ActionBuilder(id, owner);
    }

    public static ActionChainBuilder createChain(String id) {
        return new ActionChainBuilder(id);
    }

    public static ActionChainBuilder createChain(String id, JavaPlugin owner) {
        return new ActionChainBuilder(id, owner);
    }

    public static CompletableFuture<ActionResult> executeAsync(String actionString, ActionContext context) {
        return ActionManager.getInstance().executeActionAsync(actionString, context);
    }

    public static ActionResult execute(String actionString, ActionContext context) {
        return ActionManager.getInstance().executeAction(actionString, context);
    }

    public static CompletableFuture<Void> registerAsync(Action action) {
        return ActionManager.getInstance().registerActionAsync(action);
    }

    public static void register(Action action) {
        ActionManager.getInstance().registerAction(action);
    }

    public static Optional<Action> get(String id) {
        return ActionManager.getInstance().getRegistry().get(id);
    }

    public static Collection<Action> getAll() {
        return ActionManager.getInstance().getRegistry().getAll();
    }

    public static Collection<Action> getAllByNamespace(String namespace) {
        return ActionManager.getInstance().getRegistry().getAllByNamespace(namespace);
    }

    public static void unregister(String actionId, JavaPlugin owner) {
        ActionManager.getInstance().unregisterAction(actionId, owner);
    }

    public static void unregisterAll(JavaPlugin owner) {
        ActionManager.getInstance().unregisterPluginActions(owner);
    }

    public static void reload() {
        ActionManager.getInstance().reload();
    }

    public static void shutdown() {
        ActionManager.getInstance().shutdown();
    }

    public static ActionStats getStats() {
        ActionManager manager = ActionManager.getInstance();

        Map<String, Integer> actionsByNamespace = new HashMap<>();
        for (String namespace : manager.getRegistry().getNamespaces()) {
            actionsByNamespace.put(namespace, manager.getRegistry().getAllByNamespace(namespace).size());
        }

        return ActionStats.builder()
                .totalActions(manager.getRegisteredActionsCount())
                .totalNamespaces(manager.getRegistry().getNamespaces().size())
                .actionsByNamespace(actionsByNamespace)
                .cacheStats(manager.getCacheStats())
                .cooldownCacheSize(manager.getCacheManager().getCooldownCache().size())
                .rateLimitCacheSize(manager.getCacheManager().getRateLimitCache().size())
                .executionCacheSize(manager.getCacheManager().getExecutionCache().size())
                .auditLogSize(manager.getAuditLogger().size())
                .auditEnabled(manager.getAuditLogger().isEnabled())
                .build();
    }

    public static ActionManager getManager() {
        return ActionManager.getInstance();
    }
}
