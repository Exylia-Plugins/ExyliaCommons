package net.exylia.commons.utils;

import com.github.lalyos.jfiglet.FigletFont;
import lombok.Getter;
import net.exylia.commons.ExyliaPlugin;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

import static net.exylia.commons.utils.AnsiComponentLogger.convertHexColors;

/**
 * Utilidades para mostrar mensajes de depuración en la consola
 */
public class DebugUtils {

    public static final String PURPLE = "\u001B[35m";
    public static final String RESET = "\u001B[0m";

    @Getter
    private static String prefix = "";

    public static void init(ExyliaPlugin plugin) {
        prefix = "<#696969>[<gradient:#aa76de:#8a51c4:#aa76de>" + plugin.getName() + "</gradient><#696969>] ";
        sendPluginMOTD(plugin.getName());
    }

    public static void logDebug(Boolean enabled, String message){
        if (!enabled) return;
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
    
    public static void logInternalDebug(Boolean enabled, String message){
        if (!enabled) return;
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse("<#696969>[<gradient:#b48fd9:#F2C6DE:#b48fd9>ExyliaCommons</gradient><#696969>] <#e7cfff>[DEBUG] " + message)));
    }
    
    public static void logInternalError(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse("<#696969>[<gradient:#b48fd9:#F2C6DE:#b48fd9>ExyliaCommons</gradient><#696969>] <#b36476>[ERROR] " + message)));
    }
    
    public static void logInternalWarn(String message){ 
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse("<#696969>[<gradient:#b48fd9:#F2C6DE:#b48fd9>ExyliaCommons</gradient><#696969>] <#ffd2a8>[WARN] " + message)));
    }
    
    public static void logInternalInfo(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse("<#696969>[<gradient:#b48fd9:#F2C6DE:#b48fd9>ExyliaCommons</gradient><#696969>] <#7db7ff>[INFO] " + message)));
    }
    
    public static void logInternalSuccess(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse("<#696969>[<gradient:#b48fd9:#F2C6DE:#b48fd9>ExyliaCommons</gradient><#696969>] <#a1ffc3>[SUCCESS] " + message)));
    }

    public static void sendPluginMOTD(String pluginName) {
        try {
            String asciiArt = FigletFont.convertOneLine(pluginName);

            String[] lines = asciiArt.split("\n");
            Bukkit.getLogger().info("");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    log("<#8a51c4>" + line);
                }
            }
            log("");
            log("Powered by Exylia - https://discord.exylia.net");
            Bukkit.getLogger().info("");
        } catch (Exception e) {
            // Fallback simple
            log("<#8a51c4>========== " + pluginName.toUpperCase() + " ==========<reset>");
        }
    }
}