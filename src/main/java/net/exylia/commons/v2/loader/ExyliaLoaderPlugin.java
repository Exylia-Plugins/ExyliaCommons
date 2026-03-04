package net.exylia.commons.v2.loader;

import com.lukittu.loader.LoaderPlugin;
import com.lukittu.loader.entry.ILukittuLoader;
import com.lukittu.loader.spigot.SpigotLukittuLoader;
import com.lukittu.loader.util.PlatformHelper;
import lombok.Getter;
import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.lifecycle.ShutdownCoordinator;
import net.exylia.commons.v2.lifecycle.SystemBootstrapper;
import net.exylia.commons.v2.reload.api.ReloadAPI;
import net.exylia.commons.v2.reload.api.ReloadContext;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public abstract class ExyliaLoaderPlugin implements LoaderPlugin {

    private static boolean initialized = false;
    private static final Set<ExyliaLoaderPlugin> activePlugins = new HashSet<>();

    @Getter
    private static ExyliaLoaderPlugin instance;

    @Getter
    private JavaPlugin plugin;
    @Getter
    private SpigotLukittuLoader loader;

    private BukkitAudiences audiences;
    private SystemBootstrapper bootstrapper;
    private ShutdownCoordinator shutdownCoordinator;

    @Override
    public void start(ILukittuLoader lukittuLoader) {
        loader = PlatformHelper.cast(lukittuLoader, SpigotLukittuLoader.class);
        plugin = loader.getPlugin();

        Configs.init(plugin, getClass().getClassLoader());
        TaskAPI.initialize(plugin);

        bootstrapper = new SystemBootstrapper();
        shutdownCoordinator = new ShutdownCoordinator();

        DebugAPI.logLibDebug("Calling onPreExyliaEnable...");
        onPreExyliaEnable();

        try {
            audiences = BukkitAudiences.create(plugin);
            activePlugins.add(this);

            if (!initialized) {
                instance = this;
                initialized = true;
            }

            DebugAPI.logLibDebug("Executing bootstrap...");
            bootstrapper.initializeCoreSystemsAsync(plugin);
            bootstrapper.checkOptionalDependencies();

            ReloadAPI.initialize(plugin);

            DebugAPI.logLibDebug("Calling onExyliaEnable...");
            onExyliaEnable();
            DebugAPI.logLibInfo("Plugin enabled: " + plugin.getDescription().getName());

        } catch (Exception e) {
            DebugAPI.logLibError("Error enabling plugin: " + e.getMessage(), e);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    @Override
    public void shutdown(ILukittuLoader lukittuLoader) {
        activePlugins.remove(this);

        try {
            onExyliaDisable();
        } catch (Exception e) {
            DebugAPI.logLibError("Error in disable: " + e.getMessage());
        }

        if (audiences != null) {
            audiences.close();
            audiences = null;
        }

        if (shutdownCoordinator != null) {
            shutdownCoordinator.executeOrderedShutdown(plugin);
        }

        if (activePlugins.isEmpty()) {
            initialized = false;
        }

        DebugAPI.logLibInfo("Plugin disabled: " + plugin.getDescription().getName());
    }

    protected void onPreExyliaEnable() {}
    protected abstract void onExyliaEnable();
    protected abstract void onExyliaDisable();
    protected void onReload(ReloadContext context) {}

    public final void callOnReload(ReloadContext context) {
        onReload(context);
    }

    public static void callOnReloadForActivePlugins(ReloadContext context) {
        for (ExyliaLoaderPlugin plugin : activePlugins) {
            try {
                plugin.callOnReload(context);
            } catch (Exception ignored) {
            }
        }
    }

    public BukkitAudiences adventure() {
        if (audiences == null) throw new IllegalStateException("Adventure not available");
        return audiences;
    }

    public Logger getLogger() {
        return plugin.getLogger();
    }

    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
        }

        File outFile = new File(plugin.getDataFolder(), resourcePath);
        File outDir = outFile.getParentFile();

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        if (!outFile.exists() || replace) {
            try (OutputStream out = new FileOutputStream(outFile)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (IOException ex) {
                getLogger().severe("Could not save " + outFile.getName() + " to " + outFile);
            } finally {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }
        return getClass().getClassLoader().getResourceAsStream(filename);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExyliaLoaderPlugin> T getExyliaPlugin(Class<T> pluginClass) {
        for (ExyliaLoaderPlugin p : activePlugins) {
            if (pluginClass.isInstance(p)) return (T) p;
        }
        return null;
    }
}
