package net.exylia.commons;

import lombok.Getter;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.lifecycle.LifecycleManager;
import net.exylia.commons.v2.reload.api.ReloadAPI;
import net.exylia.commons.v2.reload.api.ReloadContext;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public abstract class ExyliaPlugin extends JavaPlugin {

    private static boolean initialized = false;
    private static final Set<ExyliaPlugin> registeredPlugins = new HashSet<>();

    @Getter
    private static ExyliaPlugin instance;

    private BukkitAudiences adventure;
    private LifecycleManager lifecycleManager;

    @Override
    public final void onEnable() {
        onPreExyliaEnable();

        lifecycleManager = new LifecycleManager(this);

        try {
            this.adventure = BukkitAudiences.create(this);
            registeredPlugins.add(this);

            if (!initialized) {
                instance = this;
                initialized = true;
            }

            lifecycleManager.executeBootstrap();
            ReloadAPI.initialize(this);
            lifecycleManager.executePluginEnable();

        } catch (Exception e) {
            DebugAPI.logLibError("Critical error during plugin enable", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public final void onDisable() {
        registeredPlugins.remove(this);

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        if (lifecycleManager != null) {
            lifecycleManager.executeShutdown();
        }

        if (registeredPlugins.isEmpty()) {
            initialized = false;
            TaskAPI.shutdown();
        }
    }

    protected void onPreExyliaEnable() {}

    protected abstract void onExyliaEnable();

    protected abstract void onExyliaDisable();

    protected void onReload(ReloadContext context) {}

    public final void callOnExyliaEnable() {
        onExyliaEnable();
    }

    public final void callOnExyliaDisable() {
        onExyliaDisable();
    }

    public final void callOnReload(ReloadContext context) {
        onReload(context);
    }

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Adventure is not available");
        }
        return this.adventure;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExyliaPlugin> T getExyliaPlugin(Class<T> pluginClass) {
        for (ExyliaPlugin plugin : registeredPlugins) {
            if (pluginClass.isInstance(plugin)) {
                return (T) plugin;
            }
        }
        return null;
    }

    public static boolean isPlaceholderAPIEnabled() {
        return Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }
}
