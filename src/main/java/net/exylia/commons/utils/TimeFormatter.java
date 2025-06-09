package net.exylia.commons.utils;

import java.text.DecimalFormat;

/**
 * Formateador de tiempo que acepta long, int o double y retorna diferentes formatos
 */
public class TimeFormatter {

    public enum Format {
        HUMAN_READABLE,  // 4m 3s
        DIGITAL,         // 04:03
        APPROXIMATE      // 4 (aproximado)
    }

    private Format defaultFormat = Format.HUMAN_READABLE;
    private boolean showZeroValues = false;
    private boolean showMilliseconds = false;

    // Constructores
    public TimeFormatter() {}

    public TimeFormatter(Format defaultFormat) {
        this.defaultFormat = defaultFormat;
    }

    // Métodos de configuración
    public TimeFormatter setDefaultFormat(Format format) {
        this.defaultFormat = format;
        return this;
    }

    public TimeFormatter showZeroValues(boolean show) {
        this.showZeroValues = show;
        return this;
    }

    public TimeFormatter showMilliseconds(boolean show) {
        this.showMilliseconds = show;
        return this;
    }

    // Métodos principales de formateo
    public String format(long timeInSeconds) {
        return format(timeInSeconds, defaultFormat);
    }

    public String format(int timeInSeconds) {
        return format((long) timeInSeconds, defaultFormat);
    }

    public String format(double timeInSeconds) {
        return format(timeInSeconds, defaultFormat);
    }

    public String format(long timeInSeconds, Format format) {
        return format((double) timeInSeconds, format);
    }

    public String format(int timeInSeconds, Format format) {
        return format((double) timeInSeconds, format);
    }

    public String format(double timeInSeconds, Format format) {
        switch (format) {
            case HUMAN_READABLE:
                return formatHumanReadable(timeInSeconds);
            case DIGITAL:
                return formatDigital(timeInSeconds);
            case APPROXIMATE:
                return formatApproximate(timeInSeconds);
            default:
                return formatHumanReadable(timeInSeconds);
        }
    }

    // Formateo legible por humanos: 4m 3s
    private String formatHumanReadable(double totalSeconds) {
        int hours = (int) (totalSeconds / 3600);
        int minutes = (int) ((totalSeconds % 3600) / 60);
        int seconds = (int) (totalSeconds % 60);
        int milliseconds = (int) ((totalSeconds - Math.floor(totalSeconds)) * 1000);

        StringBuilder result = new StringBuilder();

        if (hours > 0 || showZeroValues) {
            result.append(hours).append("h ");
        }

        if (minutes > 0 || (showZeroValues && hours > 0)) {
            result.append(minutes).append("m ");
        }

        if (seconds > 0 || result.length() == 0 || showZeroValues) {
            result.append(seconds).append("s");
        }

        if (showMilliseconds && milliseconds > 0) {
            result.append(" ").append(milliseconds).append("ms");
        }

        return result.toString().trim();
    }

    // Formateo digital: 04:03
    private String formatDigital(double totalSeconds) {
        int hours = (int) (totalSeconds / 3600);
        int minutes = (int) ((totalSeconds % 3600) / 60);
        int seconds = (int) (totalSeconds % 60);
        int milliseconds = (int) ((totalSeconds - Math.floor(totalSeconds)) * 1000);

        StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(String.format("%02d:", hours));
        }

        result.append(String.format("%02d:%02d", minutes, seconds));

        if (showMilliseconds) {
            result.append(String.format(".%03d", milliseconds));
        }

        return result.toString();
    }

    // Formateo aproximado: 4 (aproximado)
    private String formatApproximate(double totalSeconds) {
        if (totalSeconds < 60) {
            return Math.round(totalSeconds) + "s";
        } else if (totalSeconds < 3600) {
            return Math.round(totalSeconds / 60.0) + "m";
        } else if (totalSeconds < 86400) {
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(totalSeconds / 3600.0) + "h";
        } else {
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(totalSeconds / 86400.0) + "d";
        }
    }

    // Métodos utilitarios estáticos
    public static String quickFormat(long seconds) {
        return new TimeFormatter().format(seconds);
    }

    public static String quickFormat(int seconds) {
        return new TimeFormatter().format(seconds);
    }

    public static String quickFormat(double seconds) {
        return new TimeFormatter().format(seconds);
    }

    public static String quickDigital(long seconds) {
        return new TimeFormatter().format(seconds, Format.DIGITAL);
    }

    public static String quickDigital(int seconds) {
        return new TimeFormatter().format(seconds, Format.DIGITAL);
    }

    public static String quickDigital(double seconds) {
        return new TimeFormatter().format(seconds, Format.DIGITAL);
    }

    public static String quickApproximate(long seconds) {
        return new TimeFormatter().format(seconds, Format.APPROXIMATE);
    }

    public static String quickApproximate(int seconds) {
        return new TimeFormatter().format(seconds, Format.APPROXIMATE);
    }

    public static String quickApproximate(double seconds) {
        return new TimeFormatter().format(seconds, Format.APPROXIMATE);
    }
}