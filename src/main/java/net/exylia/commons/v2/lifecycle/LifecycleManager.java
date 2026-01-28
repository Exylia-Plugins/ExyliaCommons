package net.exylia.commons.v2.lifecycle;

import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.license.z;

import static net.exylia.commons.utils.DebugUtils.*;

@Getter
public class LifecycleManager {
    private final ExyliaPlugin plugin;
    private final SystemBootstrapper bootstrapper;
    private final ShutdownCoordinator shutdownCoordinator;
    private LifecycleStage currentStage;

    public LifecycleManager(ExyliaPlugin plugin) {
        this.plugin = plugin;
        this.bootstrapper = new SystemBootstrapper();
        this.shutdownCoordinator = new ShutdownCoordinator();
        this.currentStage = LifecycleStage.PRE_INIT;
    }

    public boolean executeLicenseValidation() {
        currentStage = LifecycleStage.LICENSE_VALIDATION;
        logInternalDebug("Starting license validation...");

        try {
            boolean valid = z.a(plugin);
            if (!valid) {
                return false;
            }
            return true;

        } catch (Exception e) {
            logInternalError("License validation failed: " + e.getMessage());
            logInternalError("You need support? Join our Discord: https://discord.exylia.net/");
            return false;
        }
    }

    public void executeBootstrap() {
        currentStage = LifecycleStage.CORE_INIT;
        logInternalDebug("Starting bootstrap process...");

        try {
            logInternalDebug("Initializing core systems...");
            bootstrapper.initializeCoreSystemsAsync(plugin);

            logInternalDebug("Checking optional dependencies...");
            bootstrapper.checkOptionalDependencies();

            logInternalDebug("Bootstrap completed successfully");
        } catch (Exception e) {
            logInternalError("Bootstrap failed: " + e.getMessage());
            throw new RuntimeException("Failed to bootstrap plugin", e);
        }
    }

    public void executePluginEnable() {
        currentStage = LifecycleStage.PLUGIN_INIT;
        logInternalDebug("Starting plugin initialization...");

        try {
            logInternalDebug("Calling plugin-specific enable logic...");
            plugin.callOnExyliaEnable();

            logInternalDebug("Plugin initialization completed, entering POST_INIT stage...");
            currentStage = LifecycleStage.POST_INIT;

            logInternalDebug("Entering RUNNING stage...");
            currentStage = LifecycleStage.RUNNING;
        } catch (Exception e) {
            logInternalError("Error enabling plugin: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to enable plugin", e);
        }
    }

    public void executeShutdown() {
        currentStage = LifecycleStage.PRE_SHUTDOWN;

        try {
            plugin.callOnExyliaDisable();
        } catch (Exception e) {
            logInternalError("Error in plugin disable hook: " + e.getMessage());
        }

        currentStage = LifecycleStage.SHUTDOWN;
        shutdownCoordinator.executeOrderedShutdown(plugin);

        logInternalInfo("Plugin Exylia disabled: " + plugin.getDescription().getName());
    }
}
