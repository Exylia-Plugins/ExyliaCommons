package net.exylia.commons.v2.debug.core;

import com.github.lalyos.jfiglet.FigletFont;
import net.exylia.commons.async.AsyncExecutor;
import net.exylia.commons.utils.AnsiComponentLogger;
import net.exylia.commons.v2.debug.config.DebugConfig;
import net.exylia.commons.v2.debug.formatter.DebugFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class DebugManager {
    private static volatile DebugManager instance;

    private String pluginPrefix = "";
    private static final String LIBRARY_PREFIX = "<#696969>[<#c995fc>ExyliaLib<#696969>] ";

    private DebugManager() {
    }

    public static DebugManager getInstance() {
        if (instance == null) {
            synchronized (DebugManager.class) {
                if (instance == null) {
                    instance = new DebugManager();
                }
            }
        }
        return instance;
    }

    public void init(JavaPlugin plugin) {
        this.pluginPrefix = "<#696969>[<gradient:#aa76de:#8a51c4:#aa76de>" + plugin.getName() + "</gradient><#696969>] ";
        sendPluginMOTD(plugin);
    }

    public void sendPluginMOTD(JavaPlugin plugin) {
        try {
            String asciiArt = FigletFont.convertOneLine(plugin.getName());

            String[] lines = asciiArt.split("\n");
            Bukkit.getLogger().info("");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    logLibrary(DebugType.INFO, null, "<#8a51c4>" + line, null);
                }
            }
            logLibrary(DebugType.INFO, null, "", null);
            logLibrary(DebugType.INFO, null, "Version: v" + plugin.getDescription().getVersion(), null);
            logLibrary(DebugType.INFO, null, "Powered by Exylia - https://discord.exylia.net", null);
            Bukkit.getLogger().info("");
        } catch (Exception e) {
            logLibrary(DebugType.INFO, null, "<#8a51c4>========== " + plugin.getName().toUpperCase() + " ==========<reset>", null);
        }
    }

    public void logPlugin(DebugType type, DebugCategory category, String message, Throwable throwable) {
        DebugConfig config = DebugConfig.getInstance();

        if (type == DebugType.DEBUG) {
            if (!config.isEnabled(DebugLevel.PLUGIN_ONLY)) {
                return;
            }

            if (category != null && !config.isCategoryAllowed(category)) {
                return;
            }
        }

        Component formatted = DebugFormatter.format(
                DebugSource.PLUGIN,
                type,
                category,
                message,
                pluginPrefix,
                config
        );

        sendMessage(formatted, throwable, config.isAsyncLogging());
    }

    public void logLibrary(DebugType type, DebugCategory category, String message, Throwable throwable) {
        DebugConfig config = DebugConfig.getInstance();

        if (type == DebugType.DEBUG) {
            if (!config.isEnabled(DebugLevel.LIBRARY_ONLY)) {
                return;
            }

            if (category != null && !config.isCategoryAllowed(category)) {
                return;
            }
        }

        Component formatted = DebugFormatter.format(
                DebugSource.LIBRARY,
                type,
                category,
                message,
                LIBRARY_PREFIX,
                config
        );

        sendMessage(formatted, throwable, config.isAsyncLogging());
    }

    private void sendMessage(Component formatted, Throwable throwable, boolean async) {
        Runnable task = () -> {
            Bukkit.getLogger().info(AnsiComponentLogger.convertHexColors(formatted));
            if (throwable != null) {
                throwable.printStackTrace();
            }
        };

        if (async) {
            AsyncExecutor.getInstance().runAsync(task, false);
        } else {
            task.run();
        }
    }
}
