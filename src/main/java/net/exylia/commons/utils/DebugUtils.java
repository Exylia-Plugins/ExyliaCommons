package net.exylia.commons.utils;

import com.github.lalyos.jfiglet.FigletFont;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.v2.config.Configs;
import net.exylia.commons.v2.debug.api.DebugAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import static net.exylia.commons.utils.AnsiComponentLogger.convertHexColors;

public class DebugUtils {
    private static String prefix = "";
    private static final String internalPrefix = "<#696969>[<#c995fc>ExyliaLib<#696969>] ";

    public static void init(JavaPlugin plugin) {
        prefix = "<#696969>[<gradient:#aa76de:#8a51c4:#aa76de>" + plugin.getName() + "</gradient><#696969>] ";
        DebugAPI.init(plugin);
    }

    @Deprecated
    public static void logDebug(String message){
        DebugAPI.logLibDebug(message);
    }

    @Deprecated
    public static void logError(String message){
        DebugAPI.logLibError(message);
    }

    @Deprecated
    public static void logError(String message, Throwable throwable){
        DebugAPI.logLibError(message, throwable);
    }

    @Deprecated
    public static void logWarn(String message){
        DebugAPI.logLibWarn(message);
    }

    @Deprecated
    public static void logInfo(String message){
        DebugAPI.logLibInfo(message);
    }

    @Deprecated
    public static void logSuccess(String message){
        DebugAPI.logLibSuccess(message);
    }

    @Deprecated
    public static void log(String message){
        DebugAPI.logLibInfo(message);
    }
    
    @Deprecated
    public static void logInternalDebug(String message){
        DebugAPI.logLibDebug(message);
    }

    @Deprecated
    public static void logInternalError(String message){
        DebugAPI.logLibError(message);
    }

    @Deprecated
    public static void logInternalError(String message, Throwable throwable){
        DebugAPI.logLibError(message, throwable);
    }

    @Deprecated
    public static void logInternalWarn(String message){
        DebugAPI.logLibWarn(message);
    }

    @Deprecated
    public static void logInternalInfo(String message){
        DebugAPI.logLibInfo(message);
    }

    @Deprecated
    public static void logInternalSuccess(String message){
        DebugAPI.logLibSuccess(message);
    }

    @Deprecated
    public static void logInternal(String message){
        DebugAPI.logLibInfo(message);
    }

    @Deprecated
    public static void sendPluginMOTD(JavaPlugin plugin) {
        DebugAPI.sendPluginMOTD(plugin);
    }
}
