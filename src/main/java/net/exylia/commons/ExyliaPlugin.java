package net.exylia.commons;

import com.hapangama.SunLicenseAPI;
import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.tasks.api.TaskAPI;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class ExyliaPlugin extends JavaPlugin {

    private static boolean _0xI = false;
    private static final Set<ExyliaPlugin> _0xR = new HashSet<>();
    @Getter
    private static ExyliaPlugin instance;

    private BukkitAudiences _0xA;
    private ConfigurationSystem _0xCS;
    private LifecycleManager _0xL;
    private volatile int _0xV = 0;

    @Getter @Setter
    private SunLicenseAPI x;

    private static final int[] _0xK = {0x45, 0x78, 0x79, 0x6C, 0x69, 0x61};

    public abstract int getProductID();
    public String getLukittuProductId() { return null; }

    @Override
    public final void onEnable() {
        onPreExyliaEnable();

        _0xL = new LifecycleManager(this);
        _0xV = 0;

        TaskAPI.initialize(this);

        CompletableFuture<Boolean> _0xLV = TaskAPI.async(() -> _0xL._879nd_()).thenApply(_0xRS -> _0xRS.getValue().orElse(false));

        boolean _0xVL;
        try { _0xVL = _0xLV.get(); } catch (InterruptedException | ExecutionException _0xE) {
            net.exylia.commons.utils.DebugUtils.logInternalError(_0xF1(0x56,0x61,0x6C,0x69,0x64,0x61,0x74,0x69,0x6F,0x6E,0x20,0x69,0x6E,0x74,0x65,0x72,0x72,0x75,0x70,0x74,0x65,0x64), _0xE);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!_0xVL || !_0xL._0xH()) { _0xV = 0; getServer().getPluginManager().disablePlugin(this); return; }
        _0xV = _0xL._0xG() ^ (_0xK[0] + _0xK[5]);

        try {
            this._0xA = BukkitAudiences.create(this);
            _0xR.add(this);

            if (!_0xI) { instance = this; _0xI = true; }

            if (!_0xC1()) { getServer().getPluginManager().disablePlugin(this); return; }
            _0xL.executeBootstrap();

            ReloadAPI.initialize(this);

            _0xL.executePluginEnable();

        } catch (Exception _0xE) {
            net.exylia.commons.utils.DebugUtils.logInternalError(_0xF1(0x43,0x72,0x69,0x74,0x69,0x63,0x61,0x6C,0x20,0x65,0x72,0x72,0x6F,0x72), _0xE);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private boolean _0xC1() { return _0xV != 0 && _0xL._0xH() && (_0xV & 0xF0) != 0; }
    private String _0xF1(int... _0xI) { char[] _0xC = new char[_0xI.length]; for (int _0xJ = 0; _0xJ < _0xI.length; _0xJ++) _0xC[_0xJ] = (char) _0xI[_0xJ]; return new String(_0xC); }

    @Override
    public final void onDisable() {
        _0xR.remove(this);
        if (_0xCS != null) _0xCS.shutdown();
        if (this._0xA != null) { this._0xA.close(); this._0xA = null; }
        if (_0xL != null) _0xL.executeShutdown();
        _0xV = 0;
        if (_0xR.isEmpty()) { _0xI = false; TaskAPI.shutdown(); }
    }

    public void initializeConfigurationSystem() {
        if (!_0xC1()) return;
        try {
            _0xCS = new ConfigurationSystem(this);
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
            _0xCS.initialize(finalConfigClasses);
            setupConfigurationListeners();
            ConfigManager.init(_0xCS, finalConfigClasses);
            TimeFormatter.init();
            DateFormatter.init();
            ColorUtils.initializePresets(this, getCustomColorPresets());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void setupConfigurationListeners() {}
    protected void onPreExyliaEnable() {}
    protected abstract void onExyliaEnable();
    protected abstract void onExyliaDisable();
    protected Class<? extends ConfigBase>[] getConfigurationClasses() { return new Class[0]; }
    protected Map<String, String> getCustomColorPresets() { return new LinkedHashMap<>(); }
    protected void onReload(ReloadContext context) {}

    public final void callOnExyliaEnable() { if (_0xC1()) onExyliaEnable(); }
    public final void callOnExyliaDisable() { onExyliaDisable(); }
    public final void callOnReload(ReloadContext context) { if (_0xC1()) onReload(context); }

    public BukkitAudiences adventure() {
        if (this._0xA == null) throw new IllegalStateException(_0xF1(0x41,0x64,0x76,0x65,0x6E,0x74,0x75,0x72,0x65,0x20,0x6E,0x6F,0x74,0x20,0x61,0x76,0x61,0x69,0x6C,0x61,0x62,0x6C,0x65));
        return this._0xA;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExyliaPlugin> T getExyliaPlugin(Class<T> pluginClass) {
        for (ExyliaPlugin plugin : _0xR) { if (pluginClass.isInstance(plugin)) return (T) plugin; }
        return null;
    }

    public static boolean isPlaceholderAPIEnabled() { return Bukkit.getServer().getPluginManager().isPluginEnabled(_0xF2(0x50,0x6C,0x61,0x63,0x65,0x68,0x6F,0x6C,0x64,0x65,0x72,0x41,0x50,0x49)); }
    private static String _0xF2(int... _0xI) { char[] _0xC = new char[_0xI.length]; for (int _0xJ = 0; _0xJ < _0xI.length; _0xJ++) _0xC[_0xJ] = (char) _0xI[_0xJ]; return new String(_0xC); }
}
