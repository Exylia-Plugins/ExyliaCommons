package net.exylia.commons.v2.debug.formatter;

import net.exylia.commons.v2.visual.color.ColorProcessor;
import net.exylia.commons.v2.debug.config.DebugDefaults;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.debug.core.DebugSource;
import net.exylia.commons.v2.debug.core.DebugType;
import net.kyori.adventure.text.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DebugFormatter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static Component format(
            DebugSource source,
            DebugType type,
            DebugCategory category,
            String message,
            String prefix
    ) {
        StringBuilder sb = new StringBuilder();

        if (DebugDefaults.Debug.SHOW_TIMESTAMPS) {
            sb.append("<#696969>[")
                    .append(LocalTime.now().format(TIME_FORMATTER))
                    .append("] ");
        }

        sb.append(prefix);

        String color = source == DebugSource.PLUGIN
                ? type.getPluginColor()
                : type.getLibraryColor();

        sb.append(color)
                .append("[")
                .append(type.getLabel())
                .append("] ");

        if (category != null) {
            sb.append("<#8a8a8a>[")
                    .append(category.getName())
                    .append("] ");
        }

        if (DebugDefaults.Debug.SHOW_CLASS_NAMES) {
            String className = getCallerClassName();
            if (className != null) {
                sb.append("<#696969>[")
                        .append(className)
                        .append("] ");
            }
        }

        sb.append(color)
                .append(message);

        return ColorProcessor.parseToComponent(sb.toString());
    }

    private static String getCallerClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (!className.startsWith("net.exylia.commons.v2.debug")) {
                String fullClassName = stackTrace[i].getClassName();
                return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
            }
        }
        return null;
    }
}
