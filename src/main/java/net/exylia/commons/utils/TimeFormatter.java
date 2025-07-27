package net.exylia.commons.utils;

import net.exylia.commons.config.base.MainConfigBase;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeFormatter {

    // Instancia singleton estática
    public static final TimeFormatter timeFormatter = new TimeFormatter();

    // Constantes de tiempo en milisegundos
    private static final long MILLISECOND = 1;
    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;

    // Configuración dinámica (se carga desde MainConfigBase)
    private static String zeroText;
    private static boolean showMilliseconds;
    private static boolean compactMode;
    private static int precision;
    private static String language;

    // Constructor privado para singleton
    private TimeFormatter() {
        init();
    }

    /**
     * Inicializa la configuración desde MainConfigBase
     */
    public static void init() {
        reload();
    }

    /**
     * Recarga la configuración desde MainConfigBase
     */
    public static void reload() {
        zeroText = MainConfigBase.timeFormatterZeroText();
        showMilliseconds = MainConfigBase.timeFormatterShowMilliseconds();
        compactMode = MainConfigBase.timeFormatterCompactMode();
        precision = MainConfigBase.timeFormatterPrecision();
        language = MainConfigBase.timeFormatterLanguage();
    }

    /**
     * Formateo automático inteligente con detección de unidades
     */
    public String format(Object input) {
        long millis = parseInputIntelligent(input);

        // Manejar casos especiales
        if (millis <= 0) {
            return zeroText;
        }

        return formatDuration(millis);
    }

    /**
     * Formateo con texto personalizado para cero/negativo
     */
    public String format(Object input, String zeroText) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return zeroText;
        }

        return formatDuration(millis);
    }

    /**
     * Formateo en estilo reloj: HH:MM:SS o MM:SS
     */
    public String formatClock(Object input) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return "00:00";
        }

        return formatAsClockTime(millis);
    }

    /**
     * Formateo en estilo reloj con formato personalizado
     */
    public String formatClock(Object input, ClockFormat format) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return format == ClockFormat.HH_MM_SS ? "00:00:00" : "00:00";
        }

        return formatAsClockTime(millis, format);
    }

    /**
     * Formateo compacto (sin espacios)
     */
    public String formatCompact(Object input) {
        TimeFormatter formatter = this.copy();
        compactMode = true;

        long millis = parseInputIntelligent(input);
        if (millis <= 0) {
            return zeroText;
        }

        return formatter.formatDuration(millis);
    }

    /**
     * Formateo con precisión específica para milisegundos
     */
    public String formatWithPrecision(Object input, int decimalPlaces) {
        TimeFormatter formatter = this.copy();
        precision = decimalPlaces;

        return formatter.format(input);
    }

    /**
     * Formateo sin milisegundos
     */
    public String formatNoMillis(Object input) {
        TimeFormatter formatter = this.copy();
        showMilliseconds = false;

        return formatter.format(input);
    }

    /**
     * Formateo verbal en español
     */
    public String formatVerbal(Object input) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return "sin tiempo";
        }

        return formatVerbalDuration(millis);
    }

    /**
     * Formateo para mayor unidad significativa
     */
    public String formatLargestUnit(Object input) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return zeroText;
        }

        return formatLargestSignificantUnit(millis);
    }

    /**
     * Formateo aproximado (solo las dos unidades más grandes)
     */
    public String formatApproximate(Object input) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return zeroText;
        }

        return formatApproximateDuration(millis);
    }

    /**
     * Convierte a unidad específica
     */
    public String formatAsUnit(Object input, TimeUnit unit) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return "0 " + getUnitName(unit);
        }

        return convertToUnit(millis, unit);
    }

    /**
     * Obtiene componentes de tiempo por separado
     */
    public TimeComponents getComponents(Object input) {
        long millis = parseInputIntelligent(input);
        return new TimeComponents(millis);
    }

    // ================= MÉTODOS INTERNOS =================

    /**
     * Parseador inteligente que detecta automáticamente la unidad basándose en el valor
     */
    private long parseInputIntelligent(Object input) {
        if (input == null) {
            return 0;
        }

        if (input instanceof String) {
            return parseStringDuration((String) input);
        }

        if (input instanceof Number) {
            double value = ((Number) input).doubleValue();

            // Si es exactamente 0, devolver 0
            if (value == 0) {
                return 0;
            }

            return detectTimeUnit(value);
        }

        return 0;
    }

    /**
     * Detecta automáticamente la unidad de tiempo basándose en el valor numérico
     */
    private long detectTimeUnit(double value) {
        double absValue = Math.abs(value);

        // Casos especiales para valores muy pequeños (probablemente decimales de segundos)
        if (absValue < 1.0 && absValue > 0) {
            // Es un decimal menor a 1, probablemente segundos decimales
            return (long) (value * SECOND);
        }

        // Para valores enteros pequeños (1-120), asumir segundos
        if (absValue <= 120 && value == Math.floor(value)) {
            return (long) (value * SECOND);
        }

        // Para valores entre 121-7200, podrían ser segundos o minutos
        if (absValue <= 7200) {
            // Si es menor a 3600, probablemente segundos
            if (absValue <= 3600) {
                return (long) (value * SECOND);
            }
            // Entre 3600-7200, podría ser segundos (1-2 horas) o minutos (60-120 min)
            // Asumimos segundos para mantener consistencia
            return (long) (value * SECOND);
        }

        // Para valores grandes (típicos de System.currentTimeMillis() o timestamps)
        // Si el valor es mayor a 86400000 (1 día en millis), probablemente ya son milisegundos
        if (absValue > 86400000) {
            return (long) value;
        }

        // Para valores medianos (7200-86400000), necesitamos más heurística
        if (absValue <= 86400) {
            // Hasta 86400 podría ser segundos (1 día = 86400 segundos)
            return (long) (value * SECOND);
        }

        // Para valores entre 86400-86400000, es ambiguo, pero asumimos milisegundos
        // ya que es más común trabajar con milisegundos en sistemas
        return (long) value;
    }

    /**
     * Método de parseado original para compatibilidad hacia atrás
     */
    private long parseInput(Object input) {
        if (input == null) {
            return 0;
        }

        if (input instanceof Number) {
            return ((Number) input).longValue();
        }

        if (input instanceof String) {
            return parseStringDuration((String) input);
        }

        return 0;
    }

    private long parseStringDuration(String duration) {
        // Parsear strings como "1h 30m", "90s", etc.
        duration = duration.toLowerCase().replaceAll("\\s+", "");
        long totalMillis = 0;

        // Patrones para extraer números y unidades
        String[] patterns = {"(\\d+(?:\\.\\d+)?)y", "(\\d+(?:\\.\\d+)?)mo", "(\\d+(?:\\.\\d+)?)w",
                "(\\d+(?:\\.\\d+)?)d", "(\\d+(?:\\.\\d+)?)h", "(\\d+(?:\\.\\d+)?)m(?!s)",
                "(\\d+(?:\\.\\d+)?)s", "(\\d+(?:\\.\\d+)?)ms"};
        long[] multipliers = {YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, MILLISECOND};

        for (int i = 0; i < patterns.length; i++) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patterns[i]);
            java.util.regex.Matcher matcher = pattern.matcher(duration);
            if (matcher.find()) {
                double value = Double.parseDouble(matcher.group(1));
                totalMillis += (long) (value * multipliers[i]);
            }
        }

        return totalMillis;
    }

    private String formatDuration(long millis) {
        List<String> parts = new ArrayList<>();

        // Años
        if (millis >= YEAR) {
            long years = millis / YEAR;
            parts.add(years + ("y"));
            millis %= YEAR;
        }

        // Meses
        if (millis >= MONTH) {
            long months = millis / MONTH;
            parts.add(months + ("mo"));
            millis %= MONTH;
        }

        // Semanas
        if (millis >= WEEK) {
            long weeks = millis / WEEK;
            parts.add(weeks + ("w"));
            millis %= WEEK;
        }

        // Días
        if (millis >= DAY) {
            long days = millis / DAY;
            parts.add(days + ("d"));
            millis %= DAY;
        }

        // Horas
        if (millis >= HOUR) {
            long hours = millis / HOUR;
            parts.add(hours + ("h"));
            millis %= HOUR;
        }

        // Minutos
        if (millis >= MINUTE) {
            long minutes = millis / MINUTE;
            parts.add(minutes + ("m"));
            millis %= MINUTE;
        }

        // Segundos (con milisegundos opcionales)
        if (millis >= SECOND || parts.isEmpty()) {
            if (showMilliseconds && millis % SECOND != 0) {
                double seconds = millis / 1000.0;
                DecimalFormat df = new DecimalFormat("0." + "0".repeat(precision));
                String secondsStr = df.format(seconds);
                parts.add(secondsStr + ("s"));
            } else {
                long seconds = (millis + 500) / SECOND; // redondear
                parts.add(seconds + ("s"));
            }
        } else if (showMilliseconds && millis > 0) {
            // Solo milisegundos
            parts.add(millis + ("ms"));
        }

        return String.join(compactMode ? "" : " ", parts);
    }

    private String formatAsClockTime(long millis) {
        return formatAsClockTime(millis, ClockFormat.AUTO);
    }

    private String formatAsClockTime(long millis, ClockFormat format) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        switch (format) {
            case HH_MM_SS:
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            case MM_SS:
                return String.format("%02d:%02d", (hours * 60) + minutes, seconds);
            case AUTO:
            default:
                if (hours > 0) {
                    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
                } else {
                    return String.format("%02d:%02d", minutes, seconds);
                }
        }
    }

    private String formatVerbalDuration(long millis) {
        List<String> parts = new ArrayList<>();
        boolean isSpanish = "es".equalsIgnoreCase(language) || "spanish".equalsIgnoreCase(language);

        if (millis >= YEAR) {
            long years = millis / YEAR;
            if (isSpanish) {
                parts.add(years + " año" + (years != 1 ? "s" : ""));
            } else {
                parts.add(years + " year" + (years != 1 ? "s" : ""));
            }
            millis %= YEAR;
        }

        if (millis >= MONTH) {
            long months = millis / MONTH;
            if (isSpanish) {
                parts.add(months + " mes" + (months != 1 ? "es" : ""));
            } else {
                parts.add(months + " month" + (months != 1 ? "s" : ""));
            }
            millis %= MONTH;
        }

        if (millis >= WEEK) {
            long weeks = millis / WEEK;
            if (isSpanish) {
                parts.add(weeks + " semana" + (weeks != 1 ? "s" : ""));
            } else {
                parts.add(weeks + " week" + (weeks != 1 ? "s" : ""));
            }
            millis %= WEEK;
        }

        if (millis >= DAY) {
            long days = millis / DAY;
            if (isSpanish) {
                parts.add(days + " día" + (days != 1 ? "s" : ""));
            } else {
                parts.add(days + " day" + (days != 1 ? "s" : ""));
            }
            millis %= DAY;
        }

        if (millis >= HOUR) {
            long hours = millis / HOUR;
            if (isSpanish) {
                parts.add(hours + " hora" + (hours != 1 ? "s" : ""));
            } else {
                parts.add(hours + " hour" + (hours != 1 ? "s" : ""));
            }
            millis %= HOUR;
        }

        if (millis >= MINUTE) {
            long minutes = millis / MINUTE;
            if (isSpanish) {
                parts.add(minutes + " minuto" + (minutes != 1 ? "s" : ""));
            } else {
                parts.add(minutes + " minute" + (minutes != 1 ? "s" : ""));
            }
            millis %= MINUTE;
        }

        if (millis >= SECOND || parts.isEmpty()) {
            long seconds = (millis + 500) / SECOND;
            if (isSpanish) {
                parts.add(seconds + " segundo" + (seconds != 1 ? "s" : ""));
            } else {
                parts.add(seconds + " second" + (seconds != 1 ? "s" : ""));
            }
        }

        if (parts.size() > 1) {
            String last = parts.remove(parts.size() - 1);
            String connector = isSpanish ? " y " : " and ";
            return String.join(", ", parts) + connector + last;
        }

        return parts.get(0);
    }

    private String formatLargestSignificantUnit(long millis) {
        if (millis >= YEAR) {
            double years = millis / (double) YEAR;
            return String.format("%.1fy", years);
        } else if (millis >= MONTH) {
            double months = millis / (double) MONTH;
            return String.format("%.1fmo", months);
        } else if (millis >= WEEK) {
            double weeks = millis / (double) WEEK;
            return String.format("%.1fw", weeks);
        } else if (millis >= DAY) {
            double days = millis / (double) DAY;
            return String.format("%.1fd", days);
        } else if (millis >= HOUR) {
            double hours = millis / (double) HOUR;
            return String.format("%.1fh", hours);
        } else if (millis >= MINUTE) {
            double minutes = millis / (double) MINUTE;
            return String.format("%.1fm", minutes);
        } else if (millis >= SECOND) {
            double seconds = millis / (double) SECOND;
            return String.format("%.1fs", seconds);
        } else {
            return millis + "ms";
        }
    }

    private String formatApproximateDuration(long millis) {
        List<String> parts = new ArrayList<>();

        // Solo mostrar las dos unidades más significativas
        if (millis >= YEAR) {
            long years = millis / YEAR;
            parts.add(years + "y");
            millis %= YEAR;
            if (millis >= MONTH) {
                long months = millis / MONTH;
                parts.add(months + "mo");
            }
        } else if (millis >= MONTH) {
            long months = millis / MONTH;
            parts.add(months + "mo");
            millis %= MONTH;
            if (millis >= DAY) {
                long days = millis / DAY;
                parts.add(days + "d");
            }
        } else if (millis >= DAY) {
            long days = millis / DAY;
            parts.add(days + "d");
            millis %= DAY;
            if (millis >= HOUR) {
                long hours = millis / HOUR;
                parts.add(hours + "h");
            }
        } else if (millis >= HOUR) {
            long hours = millis / HOUR;
            parts.add(hours + "h");
            millis %= HOUR;
            if (millis >= MINUTE) {
                long minutes = millis / MINUTE;
                parts.add(minutes + "m");
            }
        } else if (millis >= MINUTE) {
            long minutes = millis / MINUTE;
            parts.add(minutes + "m");
            millis %= MINUTE;
            if (millis >= SECOND) {
                long seconds = millis / SECOND;
                parts.add(seconds + "s");
            }
        } else {
            long seconds = millis / SECOND;
            parts.add(seconds + "s");
        }

        return String.join(" ", parts);
    }

    private String convertToUnit(long millis, TimeUnit unit) {
        double value;
        String unitName = getUnitName(unit);

        switch (unit) {
            case NANOSECONDS:
                value = millis * 1_000_000.0;
                break;
            case MICROSECONDS:
                value = millis * 1_000.0;
                break;
            case MILLISECONDS:
                value = millis;
                break;
            case SECONDS:
                value = millis / 1000.0;
                break;
            case MINUTES:
                value = millis / (60.0 * 1000);
                break;
            case HOURS:
                value = millis / (60.0 * 60 * 1000);
                break;
            case DAYS:
                value = millis / (24.0 * 60 * 60 * 1000);
                break;
            default:
                value = millis;
                unitName = "ms";
        }

        if (value == (long) value) {
            return String.format("%.0f %s", value, unitName);
        } else {
            return String.format("%.2f %s", value, unitName);
        }
    }

    private String getUnitName(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> "ns";
            case MICROSECONDS -> "μs";
            case MILLISECONDS -> "ms";
            case SECONDS -> "s";
            case MINUTES -> "min";
            case HOURS -> "h";
            case DAYS -> "días";
            default -> "unidad";
        };
    }

    private TimeFormatter copy() {
        TimeFormatter copy = new TimeFormatter();
        zeroText = zeroText;
        showMilliseconds = showMilliseconds;
        compactMode = compactMode;
        precision = precision;
        return copy;
    }

    // ================= CLASES AUXILIARES =================

    public enum ClockFormat {
        AUTO,       // Automático: MM:SS o HH:MM:SS según corresponda
        HH_MM_SS,   // Siempre HH:MM:SS
        MM_SS       // Siempre MM:SS (suma horas a minutos)
    }

    public static class TimeComponents {
        public final long years, months, weeks, days, hours, minutes, seconds, milliseconds;
        public final long totalMilliseconds;

        TimeComponents(long millis) {
            this.totalMilliseconds = millis;

            long remaining = millis;
            this.years = remaining / YEAR;
            remaining %= YEAR;

            this.months = remaining / MONTH;
            remaining %= MONTH;

            this.weeks = remaining / WEEK;
            remaining %= WEEK;

            this.days = remaining / DAY;
            remaining %= DAY;

            this.hours = remaining / HOUR;
            remaining %= HOUR;

            this.minutes = remaining / MINUTE;
            remaining %= MINUTE;

            this.seconds = remaining / SECOND;
            this.milliseconds = remaining % SECOND;
        }

        @Override
        public String toString() {
            return String.format("TimeComponents{%dy %dmo %dw %dd %dh %dm %ds %dms}",
                    years, months, weeks, days, hours, minutes, seconds, milliseconds);
        }
    }

    // ================= MÉTODOS ESTÁTICOS DE CONVENIENCIA =================

    public static String formatMillis(long millis) {
        return timeFormatter.format(millis);
    }

    public static String formatSeconds(long seconds) {
        return timeFormatter.format(seconds * 1000);
    }

    public static String formatMinutes(long minutes) {
        return timeFormatter.format(minutes * 60 * 1000);
    }
}