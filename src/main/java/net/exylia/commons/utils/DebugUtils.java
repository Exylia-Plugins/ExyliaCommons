package net.exylia.commons.utils;

import org.bukkit.Bukkit;

import static net.exylia.commons.utils.AnsiComponentLogger.convertHexColors;

/**
 * Utilidades para mostrar mensajes de depuración en la consola
 */
public class DebugUtils {

    public static final String PURPLE = "\u001B[35m";
    public static final String RESET = "\u001B[0m";

    private static String prefix = "<#a89ab5>[<gradient:#aa76de:#8a51c4:#aa76de>ExyliaCommons</gradient><#a89ab5>] ";

    /**
     * Establece un prefijo personalizado para los mensajes
     * @param pluginName Nombre del plugin para el prefijo
     */
    public static void setPrefix(String pluginName) {
        prefix = "<#a89ab5>[<gradient:#aa76de:#8a51c4:#aa76de>" + pluginName + "</gradient><#a89ab5>] ";
    }

    /**
     * Obtiene el prefijo actual
     * @return El prefijo actual
     */
    public static String getPrefix() {
        return prefix;
    }

    /**
     * Registra un mensaje de depuración si está habilitado
     * @param enabled Si la depuración está habilitada
     * @param message Mensaje a registrar
     */
    public static void logDebug(Boolean enabled, String message){
        if (!enabled) return;
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#e7cfff>[DEBUG] " + message)));
    }

    /**
     * Registra un mensaje de error
     * @param message Mensaje de error
     */
    public static void logError(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#a33b53>[ERROR] " + message)));
    }

    /**
     * Registra un mensaje de advertencia
     * @param message Mensaje de advertencia
     */
    public static void logWarn(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#ffc58f>[WARN] " + message)));
    }

    /**
     * Registra un mensaje informativo
     * @param message Mensaje informativo
     */
    public static void logInfo(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#59a4ff>[INFO] " + message)));
    }

    /**
     * Registra un mensaje informativo
     * @param message Mensaje informativo
     */
    public static void logSuccess(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + "<#8fffc1>[SUCCESS] " + message)));
    }

    /**
     * Registra un mensaje con el prefijo actual
     * @param message Mensaje a registrar
     */
    public static void log(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.parse(prefix + message)));
    }

    /**
     * Muestra un MOTD genérico de Exylia
     */
    public static void sendMOTD() {
        log("<#8a51c4> ______            _ _       ______               _    __      _____  <reset>");
        log("<#8a51c4> |  ____|          | (_)     |  ____|             | |   \\ \\    / /__ \\ <reset>");
        log("<#8a51c4> | |__  __  ___   _| |_  __ _| |____   _____ _ __ | |_ __\\ \\  / /   ) |<reset>");
        log("<#8a51c4> |  __| \\ \\/ / | | | | |/ _` |  __\\ \\ / / _ \\ '_ \\| __/ __\\ \\/ /   / / <reset>");
        log("<#8a51c4> | |____ >  <| |_| | | | (_| | |___\\ V /  __/ | | | |_\\__ \\\\  /   / /_ <reset>");
        log("<#8a51c4> |______/_/\\_\\\\__, |_|_|\\__,_|______\\_/ \\___|_| |_|\\__|___/ \\/   |____|<reset>");
        log("<#8a51c4>               __/ |                                                   <reset>");
        log("<#8a51c4>              |___/                                                    <reset>");
    }

    /**
     * Muestra un MOTD personalizado para un plugin específico
     * @param pluginName Nombre del plugin para mostrar en el MOTD
     */
    public static void sendPluginMOTD(String pluginName) {
        log("<#8a51c4> ______            _ _       " + pluginName + " <reset>");
        log("<#8a51c4> |  ____|          | (_)      <reset>");
        log("<#8a51c4> | |__  __  ___   _| |_  __ _ <reset>");
        log("<#8a51c4> |  __| \\ \\/ / | | | | |/ _` |<reset>");
        log("<#8a51c4> | |____ >  <| |_| | | | (_| |<reset>");
        log("<#8a51c4> |______/_/\\_\\\\__, |_|_|\\__,_|<reset>");
        log("<#8a51c4>               __/ |          <reset>");
        log("<#8a51c4>              |___/           <reset>");
    }
}