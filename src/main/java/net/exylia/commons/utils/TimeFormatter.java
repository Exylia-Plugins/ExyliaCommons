package net.exylia.commons.utils;

import lombok.Getter;
import net.exylia.commons.config.base.MainConfigBase;

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
    private String minValueText = null;
    private double minThreshold = 0.0;

    // Constructores
    public TimeFormatter() {}

    public TimeFormatter(Format defaultFormat) {
        this.defaultFormat = defaultFormat;
    }

    @Getter
    public static TimeFormatter timeFormatter;

    public static void init() {
        timeFormatter = new TimeFormatter().setDefaultFormat(TimeFormatter.Format.valueOf(MainConfigBase.timeFormat())).showZeroValues(MainConfigBase.timeShowZeroValues());
    }

    public static void reload() {
        timeFormatter.setDefaultFormat(TimeFormatter.Format.valueOf(MainConfigBase.timeFormat())).showZeroValues(MainConfigBase.timeShowZeroValues());
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

    /**
     * Establece el texto a mostrar cuando el tiempo es menor o igual al umbral mínimo
     * @param text Texto a mostrar (ej: "AHORA", "Recién", "0s")
     * @param threshold Umbral en segundos (por defecto 0.0)
     * @return this para encadenamiento fluido
     */
    public TimeFormatter whenMin(String text, double threshold) {
        this.minValueText = text;
        this.minThreshold = threshold;
        return this;
    }

    /**
     * Establece el texto a mostrar cuando el tiempo es 0
     * @param text Texto a mostrar (ej: "AHORA", "Recién", "0s")
     * @return this para encadenamiento fluido
     */
    public TimeFormatter whenMin(String text) {
        return whenMin(text, 0.0);
    }

    /**
     * Limpia la configuración de valor mínimo
     * @return this para encadenamiento fluido
     */
    public TimeFormatter clearMin() {
        this.minValueText = null;
        this.minThreshold = 0.0;
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
        // Verificar si debe mostrar el texto de valor mínimo
        if (minValueText != null && timeInSeconds <= minThreshold) {
            return minValueText;
        }

        return switch (format) {
            case DIGITAL -> formatDigital(timeInSeconds);
            case APPROXIMATE -> formatApproximate(timeInSeconds);
            default -> formatHumanReadable(timeInSeconds);
        };
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

        if (seconds > 0 || result.isEmpty() || showZeroValues) {
            if (hours == 0 && minutes == 0) {
                double secondsWithDecimals = totalSeconds % 60;
                DecimalFormat df = new DecimalFormat("#.#");
                result.append(df.format(secondsWithDecimals)).append("s");
            } else {
                result.append(seconds).append("s");
            }
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

    // Métodos utilitarios estáticos con valor mínimo
    public static String quickFormatWithMin(long seconds, String minText) {
        return new TimeFormatter().whenMin(minText).format(seconds);
    }

    public static String quickFormatWithMin(int seconds, String minText) {
        return new TimeFormatter().whenMin(minText).format(seconds);
    }

    public static String quickFormatWithMin(double seconds, String minText) {
        return new TimeFormatter().whenMin(minText).format(seconds);
    }

    public static String quickDigitalWithMin(long seconds, String minText) {
        return new TimeFormatter().whenMin(minText).format(seconds, Format.DIGITAL);
    }

    public static String quickDigitalWithMin(int seconds, String minText) {
        return new TimeFormatter().whenMin(minText).format(seconds, Format.DIGITAL);
    }

    public static String quickDigitalWithMin(double seconds, String minText) {
        return new TimeFormatter().whenMin(minText).format(seconds, Format.DIGITAL);
    }
}