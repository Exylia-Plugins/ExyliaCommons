package net.exylia.commons;

import com.hapangama.SunLicenseAPI;
import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.async.SchedulerManager;
import net.exylia.commons.config.ConfigBase;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.config.ConfigurationSystem;
import net.exylia.commons.config.base.MainConfigBase;
import net.exylia.commons.config.base.MessagesBase;
import net.exylia.commons.utils.ColorUtils;
import net.exylia.commons.utils.DateFormatter;
import net.exylia.commons.utils.TimeFormatter;
import net.exylia.commons.v2.lifecycle.LifecycleManager;
import net.exylia.commons.v2.reload.api.ReloadAPI;
import net.exylia.commons.v2.reload.api.ReloadContext;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public abstract class ExyliaPlugin extends JavaPlugin {

    private static boolean initialized = false;
    private static final Set<ExyliaPlugin> registeredPlugins = new HashSet<>();
    @Getter
    private static ExyliaPlugin instance;

    private BukkitAudiences adventure;
    private ConfigurationSystem configSystem;
    private LifecycleManager lifecycleManager;

    @Getter
    @Setter
    private SunLicenseAPI sunLicenseAPI;

    public abstract int getProductID();

    @Override
    public final void onEnable() {
        onPreExyliaEnable();

        lifecycleManager = new LifecycleManager(this);

        if (!lifecycleManager.executeLicenseValidation()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            net.exylia.commons.utils.DebugUtils.logInternalDebug("Creating BukkitAudiences...");
            this.adventure = BukkitAudiences.create(this);
            registeredPlugins.add(this);

            if (!initialized) {
                instance = this;
                initialized = true;
            }

            lifecycleManager.executeBootstrap();

//            initializeConfigurationSystem();

            net.exylia.commons.utils.DebugUtils.logInternalDebug("Initializing ReloadAPI...");
            ReloadAPI.initialize(this);

            net.exylia.commons.utils.DebugUtils.logInternalDebug("Scheduling plugin enable on main thread...");
            SchedulerManager.getInstance().runTask(() -> lifecycleManager.executePluginEnable());

        } catch (Exception e) {
            net.exylia.commons.utils.DebugUtils.logInternalError("Critical error during plugin initialization", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public final void onDisable() {
        registeredPlugins.remove(this);

        if (configSystem != null) {
            configSystem.shutdown();
        }

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        if (lifecycleManager != null) {
            lifecycleManager.executeShutdown();
        }

        if (registeredPlugins.isEmpty()) {
            initialized = false;
        }
    }

    public void initializeConfigurationSystem() {
        try {
            configSystem = new ConfigurationSystem(this);

            Class<? extends ConfigBase>[] pluginConfigClasses = getConfigurationClasses();
            List<Class<? extends ConfigBase>> allConfigClasses = new ArrayList<>();

            allConfigClasses.add(MainConfigBase.class);
            allConfigClasses.add(MessagesBase.class);

            if (pluginConfigClasses != null && pluginConfigClasses.length > 0) {
                for (Class<? extends ConfigBase> pluginClass : pluginConfigClasses) {
                    if (MainConfigBase.class.isAssignableFrom(pluginClass) && !pluginClass.equals(MainConfigBase.class)) {
                        allConfigClasses.removeIf(cls -> cls.equals(MainConfigBase.class));
                    }
                    if (MessagesBase.class.isAssignableFrom(pluginClass) && !pluginClass.equals(MessagesBase.class)) {
                        allConfigClasses.removeIf(cls -> cls.equals(MessagesBase.class));
                    }
                    allConfigClasses.add(pluginClass);
                }
            }

            Class<? extends ConfigBase>[] finalConfigClasses = allConfigClasses.toArray(new Class[0]);

            configSystem.initialize(finalConfigClasses);
            setupConfigurationListeners();

            ConfigManager.init(configSystem, finalConfigClasses);
            TimeFormatter.init();
            DateFormatter.init();

            ColorUtils.initializePresets(this, getCustomColorPresets());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setupConfigurationListeners() {
    }

    protected void onPreExyliaEnable() {
    }

    protected abstract void onExyliaEnable();

    protected abstract void onExyliaDisable();

    protected Class<? extends ConfigBase>[] getConfigurationClasses() {
        return new Class[0];
    }

    protected Map<String, String> getCustomColorPresets() {
        return new LinkedHashMap<>();
    }

    protected void onReload(ReloadContext context) {
    }

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
            throw new IllegalStateException("Attempted to access Adventure when the plugin was disabled!");
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
