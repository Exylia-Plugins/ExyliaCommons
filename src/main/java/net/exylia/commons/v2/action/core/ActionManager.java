package net.exylia.commons.v2.action.core;

import lombok.Getter;
import net.exylia.commons.v2.action.audit.AuditLogger;
import net.exylia.commons.v2.action.cache.ActionCacheManager;
import net.exylia.commons.v2.action.cache.ActionCacheStats;
import net.exylia.commons.v2.action.cooldown.CooldownManager;
import net.exylia.commons.v2.action.model.Action;
import net.exylia.commons.v2.action.model.ActionContext;
import net.exylia.commons.v2.action.model.ActionResult;
import net.exylia.commons.v2.action.permission.BasicPermissionProvider;
import net.exylia.commons.v2.action.permission.PermissionProvider;
import net.exylia.commons.v2.action.permission.VaultPermissionProvider;
import net.exylia.commons.v2.action.pipeline.*;
import net.exylia.commons.v2.action.ratelimit.RateLimiter;
import net.exylia.commons.v2.action.ratelimit.TokenBucketRateLimiter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public class ActionManager {
    private static ActionManager instance;
    private static final Object LOCK = new Object();

    @Getter
    private final JavaPlugin plugin;
    @Getter
    private final ActionRegistry registry;
    @Getter
    private final ActionExecutor executor;
    @Getter
    private final ActionFactory factory;
    @Getter
    private final ActionCacheManager cacheManager;
    @Getter
    private final ActionPipeline pipeline;
    @Getter
    private final CooldownManager cooldownManager;
    @Getter
    private final AuditLogger auditLogger;
    private final RateLimiter rateLimiter;
    private final PermissionProvider permissionProvider;

    private ActionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.registry = new ActionRegistry();
        this.cacheManager = new ActionCacheManager();
        this.cooldownManager = new CooldownManager(cacheManager.getCooldownCache());
        this.rateLimiter = new TokenBucketRateLimiter(cacheManager.getRateLimitCache());
        this.auditLogger = new AuditLogger();
        this.factory = new ActionFactory();
        this.pipeline = new ActionPipeline();
        this.executor = new ActionExecutor(registry, pipeline, cacheManager, cooldownManager, auditLogger);

        this.permissionProvider = initializePermissionProvider();

        initializeDefaultMiddlewares();

    }

    public static void initialize(JavaPlugin plugin) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new ActionManager(plugin);
            }
        }
    }

    public static ActionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ActionManager not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private PermissionProvider initializePermissionProvider() {
        VaultPermissionProvider vaultProvider = new VaultPermissionProvider();
        if (vaultProvider.isEnabled()) {
            return vaultProvider;
        }
        return new BasicPermissionProvider();
    }

    private void initializeDefaultMiddlewares() {
        pipeline.registerMiddleware(PipelineStage.PRE_VALIDATE, new NamespaceMiddleware());
        pipeline.registerMiddleware(PipelineStage.PRE_VALIDATE, new ValidationMiddleware());

        pipeline.registerMiddleware(PipelineStage.PRE_EXECUTE, new PermissionMiddleware(permissionProvider));
        pipeline.registerMiddleware(PipelineStage.PRE_EXECUTE, new CooldownMiddleware(cooldownManager));
        pipeline.registerMiddleware(PipelineStage.PRE_EXECUTE, new RateLimitMiddleware(rateLimiter));

        pipeline.registerMiddleware(PipelineStage.POST_EXECUTE, new CooldownApplyMiddleware(cooldownManager));
        pipeline.registerMiddleware(PipelineStage.POST_EXECUTE, new LoggingMiddleware(auditLogger));

        DebugAPI.logLibDebug(DebugCategory.ACTION, "Default middlewares initialized");
    }

    public CompletableFuture<Void> registerActionAsync(Action action) {
        return CompletableFuture.runAsync(() -> {
            registry.register(action);
        });
    }

    public void registerAction(Action action) {
        registry.register(action);
    }

    public CompletableFuture<ActionResult> executeActionAsync(String actionString, ActionContext context) {
        return executor.executeAsync(actionString, context);
    }

    public ActionResult executeAction(String actionString, ActionContext context) {
        return executor.executeSync(actionString, context);
    }

    public void unregisterAction(String actionId, JavaPlugin owner) {
        registry.unregister(actionId, owner);
    }

    public void unregisterPluginActions(JavaPlugin owner) {
        registry.unregisterByOwner(owner);
    }

    public void clearAll() {
        registry.clear();
        cacheManager.invalidateAll();
        auditLogger.clear();
        DebugAPI.logLibInfo(DebugCategory.ACTION, "ActionManager cleared");
    }

    public void clearCache() {
        cacheManager.invalidateAll();
        auditLogger.clear();
        DebugAPI.logLibInfo(DebugCategory.ACTION, "ActionManager cache cleared");
    }

    public void reload() {
        cacheManager.invalidateAll();
        DebugAPI.logLibInfo(DebugCategory.ACTION, "ActionManager reloaded");
    }

    public void shutdown() {
        registry.clear();
        cacheManager.invalidateAll();
        auditLogger.clear();
        pipeline.clearAll();
        DebugAPI.logLibInfo(DebugCategory.ACTION, "ActionManager shutdown");
    }

    public ActionCacheStats getCacheStats() {
        return cacheManager.getStats();
    }

    public int getRegisteredActionsCount() {
        return registry.size();
    }
}
