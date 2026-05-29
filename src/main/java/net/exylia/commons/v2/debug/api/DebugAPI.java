package net.exylia.commons.v2.debug.api;

import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.debug.core.DebugManager;
import net.exylia.commons.v2.debug.core.DebugType;
import org.bukkit.plugin.java.JavaPlugin;

public class DebugAPI {

    public static void initialize(JavaPlugin plugin) {
        DebugManager.getInstance().init(plugin);
    }

    public static void init(JavaPlugin plugin) {
        DebugManager.getInstance().init(plugin);
    }

    public static void logPluginDebug(String message) {
        DebugManager.getInstance().logPlugin(DebugType.DEBUG, null, message, null);
    }

    public static void logPluginDebug(DebugCategory category, String message) {
        DebugManager.getInstance().logPlugin(DebugType.DEBUG, category, message, null);
    }

    public static void logPluginError(String message) {
        DebugManager.getInstance().logPlugin(DebugType.ERROR, null, message, null);
    }

    public static void logPluginError(String message, Throwable throwable) {
        DebugManager.getInstance().logPlugin(DebugType.ERROR, null, message, throwable);
    }

    public static void logPluginError(DebugCategory category, String message) {
        DebugManager.getInstance().logPlugin(DebugType.ERROR, category, message, null);
    }

    public static void logPluginError(DebugCategory category, String message, Throwable throwable) {
        DebugManager.getInstance().logPlugin(DebugType.ERROR, category, message, throwable);
    }

    public static void logPluginWarn(String message) {
        DebugManager.getInstance().logPlugin(DebugType.WARN, null, message, null);
    }

    public static void logPluginWarn(String message, Throwable throwable) {
        DebugManager.getInstance().logPlugin(DebugType.WARN, null, message, throwable);
    }

    public static void logPluginWarn(DebugCategory category, String message) {
        DebugManager.getInstance().logPlugin(DebugType.WARN, category, message, null);
    }

    public static void logPluginInfo(String message) {
        DebugManager.getInstance().logPlugin(DebugType.INFO, null, message, null);
    }

    public static void logPluginInfo(DebugCategory category, String message) {
        DebugManager.getInstance().logPlugin(DebugType.INFO, category, message, null);
    }

    public static void logPluginSuccess(String message) {
        DebugManager.getInstance().logPlugin(DebugType.SUCCESS, null, message, null);
    }

    public static void logPluginSuccess(DebugCategory category, String message) {
        DebugManager.getInstance().logPlugin(DebugType.SUCCESS, category, message, null);
    }

    public static void logLibDebug(String message) {
        DebugManager.getInstance().logLibrary(DebugType.DEBUG, null, message, null);
    }

    public static void logLibDebug(DebugCategory category, String message) {
        DebugManager.getInstance().logLibrary(DebugType.DEBUG, category, message, null);
    }

    public static boolean isLibDebugEnabled() {
        return DebugManager.getInstance().isLibDebugEnabled();
    }

    public static void logLibError(String message) {
        DebugManager.getInstance().logLibrary(DebugType.ERROR, null, message, null);
    }

    public static void logLibError(String message, Throwable throwable) {
        DebugManager.getInstance().logLibrary(DebugType.ERROR, null, message, throwable);
    }

    public static void logLibError(DebugCategory category, String message) {
        DebugManager.getInstance().logLibrary(DebugType.ERROR, category, message, null);
    }

    public static void logLibError(DebugCategory category, String message, Throwable throwable) {
        DebugManager.getInstance().logLibrary(DebugType.ERROR, category, message, throwable);
    }

    public static void logLibWarn(String message) {
        DebugManager.getInstance().logLibrary(DebugType.WARN, null, message, null);
    }

    public static void logLibWarn(DebugCategory category, String message) {
        DebugManager.getInstance().logLibrary(DebugType.WARN, category, message, null);
    }

    public static void logLibInfo(String message) {
        DebugManager.getInstance().logLibrary(DebugType.INFO, null, message, null);
    }

    public static void logLibInfo(DebugCategory category, String message) {
        DebugManager.getInstance().logLibrary(DebugType.INFO, category, message, null);
    }

    public static void logLibSuccess(String message) {
        DebugManager.getInstance().logLibrary(DebugType.SUCCESS, null, message, null);
    }

    public static void logLibSuccess(DebugCategory category, String message) {
        DebugManager.getInstance().logLibrary(DebugType.SUCCESS, category, message, null);
    }

    public static void sendPluginMOTD(JavaPlugin plugin) {
        DebugManager.getInstance().sendPluginMOTD(plugin);
    }
}
