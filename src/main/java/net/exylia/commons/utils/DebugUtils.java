package net.exylia.commons.utils;

import com.github.lalyos.jfiglet.FigletFont;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.configSimple.Configs;
import org.bukkit.Bukkit;

import static net.exylia.commons.utils.AnsiComponentLogger.convertHexColors;

public class DebugUtils {
    private static String prefix = "";
    private static final String internalPrefix = "<#696969>[<#c995fc>ExyliaLib<#696969>] ";

    public static void init(ExyliaPlugin plugin) {
        prefix = "<#696969>[<gradient:#aa76de:#8a51c4:#aa76de>" + plugin.getName() + "</gradient><#696969>] ";
        sendPluginMOTD(plugin);
    }

    public static void logDebug(String message){
        if (!Configs.debug()) return;
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#e7cfff>[DEBUG] " + message)));
    }

    public static void logError(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#a33b53>[ERROR] " + message)));
    }

    public static void logWarn(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#ffc58f>[WARN] " + message)));
    }

    public static void logInfo(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#59a4ff>[INFO] " + message)));
    }

    public static void logSuccess(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#8fffc1>[SUCCESS] " + message)));
    }

    public static void log(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#e7cfff> " + message)));
    }
    
    public static void logInternalDebug(String message){
        if (!Configs.debug()) return;
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(internalPrefix + "<#e7cfff>[DEBUG] " + message)));
    }
    
    public static void logInternalError(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(internalPrefix + "<#b36476>[ERROR] " + message)));
    }
    
    public static void logInternalWarn(String message){ 
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(internalPrefix + "<#ffd2a8>[WARN] " + message)));
    }
    
    public static void logInternalInfo(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(internalPrefix + "<#7db7ff>[INFO] " + message)));
    }
    
    public static void logInternalSuccess(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(internalPrefix + "<#a1ffc3>[SUCCESS] " + message)));
    }

    public static void logInternal(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(internalPrefix + "<#e7cfff> " + message)));
    }

    public static void sendPluginMOTD(ExyliaPlugin plugin) {
        try {
            String asciiArt = FigletFont.convertOneLine(plugin.getName());

            String[] lines = asciiArt.split("\n");
            Bukkit.getLogger().info("");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    logInternal("<#8a51c4>" + line);
                }
            }
            logInternal("");
            logInternal("Version: v" + plugin.getDescription().getVersion());
            logInternal("Powered by Exylia - https://discord.exylia.net");
            Bukkit.getLogger().info("");
        } catch (Exception e) {
             
            logInternal("<#8a51c4>========== " + plugin.getName().toUpperCase() + " ==========<reset>");
        }
    }
}
