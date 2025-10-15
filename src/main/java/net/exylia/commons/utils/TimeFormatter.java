package net.exylia.commons.utils;

import net.exylia.commons.config.base.MainConfigBase;
import net.exylia.commons.configSimple.Configs;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeFormatter {

    public static final TimeFormatter timeFormatter = new TimeFormatter();

    private static final long MILLISECOND = 1;
    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;

    private static String zeroText;
    private static boolean showMilliseconds;
    private static boolean compactMode;
    private static int precision;
    private static String language;
    private static boolean forceShowZeroDecimals;

    private TimeFormatter() {
        init();
    }

    public static void init() {
        reload();
    }

    public static void reload() {
        zeroText = Configs.string("time-formatter.zero-text");
        showMilliseconds = Configs.bool("time-formatter.show-milliseconds");
        compactMode = Configs.bool("time-formatter.compact-mode");
        precision = Configs.integer("time-formatter.precision");
        language = Configs.string("time-formatter.language");
        forceShowZeroDecimals = Configs.bool("time-formatter.force-show-zero-decimals");
    }

    public String format(Object input) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return zeroText;
        }

        return formatDuration(millis);
    }

    public String format(Object input, String zeroText) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return zeroText;
        }

        return formatDuration(millis);
    }

    public String formatClock(Object input) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return "00:00";
        }

        return formatAsClockTime(millis);
    }

    public String formatClock(Object input, ClockFormat format) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return format == ClockFormat.HH_MM_SS ? "00:00:00" : "00:00";
        }

        return formatAsClockTime(millis, format);
    }

    public String formatCompact(Object input) {
        TimeFormatter formatter = this.copy();
        compactMode = true;

        long millis = parseInputIntelligent(input);
        if (millis <= 0) {
            return zeroText;
        }

        return formatter.formatDuration(millis);
    }

    public String formatWithPrecision(Object input, int decimalPlaces) {
        TimeFormatter formatter = this.copy();
        precision = decimalPlaces;

        return formatter.format(input);
    }

    public String formatNoMillis(Object input) {
        TimeFormatter formatter = this.copy();
        showMilliseconds = false;

        return formatter.format(input);
    }

    public String formatVerbal(Object input) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return "sin tiempo";
        }

        return formatVerbalDuration(millis);
    }

    public String formatLargestUnit(Object input) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return zeroText;
        }

        return formatLargestSignificantUnit(millis);
    }

    public String formatApproximate(Object input) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return zeroText;
        }

        return formatApproximateDuration(millis);
    }

    public String formatAsUnit(Object input, TimeUnit unit) {
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            return "0 " + getUnitName(unit);
        }

        return convertToUnit(millis, unit);
    }

    public TimeComponents getComponents(Object input) {
        long millis = parseInputIntelligent(input);
        return new TimeComponents(millis);
    }

    private long parseInputIntelligent(Object input) {
        if (input == null) {
            return 0;
        }

        if (input instanceof String) {
            return parseStringDuration((String) input);
        }

        if (input instanceof Long) {
            return (Long) input;
        }

        if (input instanceof Integer) {
            return ((Integer) input).longValue() * SECOND;
        }

        if (input instanceof Double || input instanceof Float) {
            double seconds = ((Number) input).doubleValue();
            return (long) (seconds * SECOND);
        }

        return 0;
    }

    private long detectTimeUnit(double value) {
        double absValue = Math.abs(value);

        if (absValue < 1.0 && absValue > 0) {
            return (long) (value * SECOND);
        }

        if (absValue <= 120 && value == Math.floor(value)) {
            return (long) (value * SECOND);
        }

        if (absValue <= 7200) {
            if (absValue <= 3600) {
                return (long) (value * SECOND);
            }
            return (long) (value * SECOND);
        }

        if (absValue > 86400000) {
            return (long) value;
        }

        if (absValue <= 86400) {
            return (long) (value * SECOND);
        }

        return (long) value;
    }

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
        duration = duration.toLowerCase().replaceAll("\\s+", "");
        long totalMillis = 0;

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
        boolean hasLargerUnits = millis >= MINUTE;

        if (millis >= YEAR) {
            long years = millis / YEAR;
            parts.add(years + ("y"));
            millis %= YEAR;
        }

        if (millis >= MONTH) {
            long months = millis / MONTH;
            parts.add(months + ("mo"));
            millis %= MONTH;
        }

        if (millis >= WEEK) {
            long weeks = millis / WEEK;
            parts.add(weeks + ("w"));
            millis %= WEEK;
        }

        if (millis >= DAY) {
            long days = millis / DAY;
            parts.add(days + ("d"));
            millis %= DAY;
        }

        if (millis >= HOUR) {
            long hours = millis / HOUR;
            parts.add(hours + ("h"));
            millis %= HOUR;
        }

        if (millis >= MINUTE) {
            long minutes = millis / MINUTE;
            parts.add(minutes + ("m"));
            millis %= MINUTE;
        }

        if (millis >= SECOND || parts.isEmpty()) {
            if (showMilliseconds && (millis % SECOND != 0 || forceShowZeroDecimals) && !hasLargerUnits) {
                double seconds = millis / 1000.0;
                DecimalFormat df = new DecimalFormat("0." + "0".repeat(precision));
                String secondsStr = df.format(seconds);
                parts.add(secondsStr + ("s"));
            } else {
                long seconds = (millis + 500) / SECOND;
                parts.add(seconds + ("s"));
            }
        } else if (showMilliseconds && millis > 0 && !hasLargerUnits) {
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
            long minutes = millis / MINUTE;
            return minutes + "m";
        } else if (millis >= SECOND) {
            double seconds = millis / (double) SECOND;
            return String.format("%.1fs", seconds);
        } else {
            return millis + "ms";
        }
    }

    private String formatApproximateDuration(long millis) {
        List<String> parts = new ArrayList<>();

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
        return copy;
    }

    public enum ClockFormat {
        AUTO,
        HH_MM_SS,
        MM_SS
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
