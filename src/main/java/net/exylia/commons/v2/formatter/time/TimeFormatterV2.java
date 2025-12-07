package net.exylia.commons.v2.formatter.time;

import lombok.Getter;
import net.exylia.commons.v2.formatter.core.AbstractFormatter;
import net.exylia.commons.v2.formatter.core.FormatterException;
import net.exylia.commons.v2.formatter.cache.FormatterCache;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class TimeFormatterV2 extends AbstractFormatter<Object, String> {
    private final TimeFormatterConfig config;

    TimeFormatterV2(FormatterCache cache, TimeFormatterConfig config) {
        super(cache);
        this.config = config;
    }

    public static TimeFormatterBuilder builder() {
        return new TimeFormatterBuilder();
    }

    @Override
    public String format(Object input) {
        long startTime = System.nanoTime();
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            stats.recordFormat(System.nanoTime() - startTime, false);
            return config.getZeroText();
        }

        String result = formatDuration(millis);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    @Override
    public String format(Object input, String customPattern) {
        return format(input);
    }

    public String formatClock(Object input) {
        return formatClock(input, ClockFormat.AUTO);
    }

    public String formatClock(Object input, ClockFormat format) {
        long startTime = System.nanoTime();
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            stats.recordFormat(System.nanoTime() - startTime, false);
            return format.getDefaultZero();
        }

        ClockFormat actualFormat = format == ClockFormat.AUTO ? ClockFormat.detect(millis) : format;
        String result = formatAsClockTime(millis, actualFormat);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatCompact(Object input) {
        long startTime = System.nanoTime();
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            stats.recordFormat(System.nanoTime() - startTime, false);
            return config.getZeroText();
        }

        String result = formatDurationCompact(millis);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatVerbal(Object input) {
        long startTime = System.nanoTime();
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            stats.recordFormat(System.nanoTime() - startTime, false);
            return config.getLanguage().equals("es") ? "sin tiempo" : "no time";
        }

        String result = formatVerbalDuration(millis);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatLargestUnit(Object input) {
        long startTime = System.nanoTime();
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            stats.recordFormat(System.nanoTime() - startTime, false);
            return config.getZeroText();
        }

        String result = formatLargestSignificantUnit(millis);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatApproximate(Object input) {
        long startTime = System.nanoTime();
        long millis = parseInputIntelligent(input);

        if (millis <= 0) {
            stats.recordFormat(System.nanoTime() - startTime, false);
            return config.getZeroText();
        }

        String result = formatApproximateDuration(millis);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatWithPrecision(Object input, int decimalPlaces) {
        TimeFormatterConfig customConfig = TimeFormatterConfig.builder()
            .zeroText(config.getZeroText())
            .showMilliseconds(config.isShowMilliseconds())
            .compactMode(config.isCompactMode())
            .precision(decimalPlaces)
            .language(config.getLanguage())
            .forceShowZeroDecimals(config.isForceShowZeroDecimals())
            .decimalThresholdMillis(config.getDecimalThresholdMillis())
            .showDecimalsWhenUnderThreshold(config.isShowDecimalsWhenUnderThreshold())
            .build();

        TimeFormatterV2 customFormatter = new TimeFormatterV2(cache, customConfig);
        return customFormatter.format(input);
    }

    public TimeComponents getComponents(Object input) {
        long millis = parseInputIntelligent(input);
        return TimeComponents.fromMillis(millis);
    }

    @Override
    protected String getCacheKey(Object input, String pattern) {
        return "time_" + pattern + "_" + input.toString();
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
            return ((Integer) input).longValue() * TimeUnit.SECOND.getMillis();
        }

        if (input instanceof Double || input instanceof Float) {
            double seconds = ((Number) input).doubleValue();
            return (long) (seconds * TimeUnit.SECOND.getMillis());
        }

        return 0;
    }

    private long parseStringDuration(String duration) {
        duration = duration.toLowerCase().replaceAll("\\s+", "");
        long totalMillis = 0;

        String[] patternStrings = {
            "(\\d+(?:\\.\\d+)?)y",
            "(\\d+(?:\\.\\d+)?)mo",
            "(\\d+(?:\\.\\d+)?)w",
            "(\\d+(?:\\.\\d+)?)d",
            "(\\d+(?:\\.\\d+)?)h",
            "(\\d+(?:\\.\\d+)?)m(?!s)",
            "(\\d+(?:\\.\\d+)?)s",
            "(\\d+(?:\\.\\d+)?)ms"
        };

        TimeUnit[] units = {
            TimeUnit.YEAR, TimeUnit.MONTH, TimeUnit.WEEK, TimeUnit.DAY,
            TimeUnit.HOUR, TimeUnit.MINUTE, TimeUnit.SECOND, TimeUnit.MILLISECOND
        };

        for (int i = 0; i < patternStrings.length; i++) {
            Pattern pattern = cache.getPattern(patternStrings[i]);
            Matcher matcher = pattern.matcher(duration);
            if (matcher.find()) {
                double value = Double.parseDouble(matcher.group(1));
                totalMillis += (long) (value * units[i].getMillis());
            }
        }

        return totalMillis;
    }

    private String formatDuration(long millis) {
        List<String> parts = new ArrayList<>();
        long remaining = millis;
        long remainingBeforeMinutes = remaining;

        if (remaining >= TimeUnit.YEAR.getMillis()) {
            long years = remaining / TimeUnit.YEAR.getMillis();
            parts.add(years + "y");
            remaining %= TimeUnit.YEAR.getMillis();
        }

        if (remaining >= TimeUnit.MONTH.getMillis()) {
            long months = remaining / TimeUnit.MONTH.getMillis();
            parts.add(months + "mo");
            remaining %= TimeUnit.MONTH.getMillis();
        }

        if (remaining >= TimeUnit.WEEK.getMillis()) {
            long weeks = remaining / TimeUnit.WEEK.getMillis();
            parts.add(weeks + "w");
            remaining %= TimeUnit.WEEK.getMillis();
        }

        if (remaining >= TimeUnit.DAY.getMillis()) {
            long days = remaining / TimeUnit.DAY.getMillis();
            parts.add(days + "d");
            remaining %= TimeUnit.DAY.getMillis();
        }

        if (remaining >= TimeUnit.HOUR.getMillis()) {
            long hours = remaining / TimeUnit.HOUR.getMillis();
            parts.add(hours + "h");
            remaining %= TimeUnit.HOUR.getMillis();
        }

        remainingBeforeMinutes = remaining;

        if (remaining >= TimeUnit.MINUTE.getMillis()) {
            long minutes = remaining / TimeUnit.MINUTE.getMillis();
            parts.add(minutes + "m");
            remaining %= TimeUnit.MINUTE.getMillis();
        }

        if (remaining >= TimeUnit.SECOND.getMillis() || parts.isEmpty()) {
            boolean shouldShowDecimals = shouldShowDecimals(remainingBeforeMinutes);

            if (shouldShowDecimals && (remaining % TimeUnit.SECOND.getMillis() != 0 || config.isForceShowZeroDecimals())) {
                double seconds = remaining / 1000.0;
                DecimalFormat df = cache.getDecimalFormat(config.getPrecision(), config.isForceShowZeroDecimals());
                String secondsStr = df.format(seconds);
                parts.add(secondsStr + "s");
            } else {
                long seconds = remaining / TimeUnit.SECOND.getMillis();
                if (remaining % TimeUnit.SECOND.getMillis() >= 500) {
                    seconds++;
                }
                if (seconds > 0 || parts.isEmpty()) {
                    parts.add(seconds + "s");
                }
            }
        } else if (config.isShowMilliseconds() && remaining > 0) {
            parts.add(remaining + "ms");
        }

        return String.join(" ", parts);
    }

    private boolean shouldShowDecimals(long remainingMillisBeforeMinutes) {
        if (!config.isShowMilliseconds()) {
            return false;
        }

        if (config.isShowDecimalsWhenUnderThreshold() && config.getDecimalThresholdMillis() > 0) {
            return remainingMillisBeforeMinutes < config.getDecimalThresholdMillis();
        }

        return remainingMillisBeforeMinutes < TimeUnit.MINUTE.getMillis();
    }

    private String formatDurationCompact(long millis) {
        long remaining = millis;

        if (remaining >= TimeUnit.YEAR.getMillis()) {
            double years = remaining / (double) TimeUnit.YEAR.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(years) + "y";
        }

        if (remaining >= TimeUnit.MONTH.getMillis()) {
            double months = remaining / (double) TimeUnit.MONTH.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(months) + "mo";
        }

        if (remaining >= TimeUnit.WEEK.getMillis()) {
            double weeks = remaining / (double) TimeUnit.WEEK.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(weeks) + "w";
        }

        if (remaining >= TimeUnit.DAY.getMillis()) {
            double days = remaining / (double) TimeUnit.DAY.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(days) + "d";
        }

        if (remaining >= TimeUnit.HOUR.getMillis()) {
            double hours = remaining / (double) TimeUnit.HOUR.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(hours) + "h";
        }

        if (remaining >= TimeUnit.MINUTE.getMillis()) {
            double minutes = remaining / (double) TimeUnit.MINUTE.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(minutes) + "m";
        }

        if (remaining >= TimeUnit.SECOND.getMillis()) {
            double seconds = remaining / (double) TimeUnit.SECOND.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(seconds) + "s";
        }

        return remaining + "ms";
    }

    private String formatAsClockTime(long millis, ClockFormat format) {
        long totalSeconds = millis / TimeUnit.SECOND.getMillis();

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        switch (format) {
            case HH_MM_SS:
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            case MM_SS:
                long totalMinutes = totalSeconds / 60;
                return String.format("%02d:%02d", totalMinutes, seconds);
            case SS:
                return String.format("%02d", totalSeconds);
            default:
                return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private String formatVerbalDuration(long millis) {
        TimeComponents components = new TimeComponents(millis);
        List<String> parts = new ArrayList<>();

        if (components.getYears() > 0) {
            parts.add(components.getYears() + " " + TimeUnit.YEAR.getName(components.getYears()));
        }
        if (components.getMonths() > 0) {
            parts.add(components.getMonths() + " " + TimeUnit.MONTH.getName(components.getMonths()));
        }
        if (components.getWeeks() > 0) {
            parts.add(components.getWeeks() + " " + TimeUnit.WEEK.getName(components.getWeeks()));
        }
        if (components.getDays() > 0) {
            parts.add(components.getDays() + " " + TimeUnit.DAY.getName(components.getDays()));
        }
        if (components.getHours() > 0) {
            parts.add(components.getHours() + " " + TimeUnit.HOUR.getName(components.getHours()));
        }
        if (components.getMinutes() > 0) {
            parts.add(components.getMinutes() + " " + TimeUnit.MINUTE.getName(components.getMinutes()));
        }
        if (components.getSeconds() > 0 || parts.isEmpty()) {
            parts.add(components.getSeconds() + " " + TimeUnit.SECOND.getName(components.getSeconds()));
        }

        if (parts.size() > 1) {
            String last = parts.remove(parts.size() - 1);
            String separator = config.getLanguage().equals("es") ? " y " : " and ";
            return String.join(", ", parts) + separator + last;
        }

        return parts.isEmpty() ? config.getZeroText() : parts.get(0);
    }

    private String formatLargestSignificantUnit(long millis) {
        TimeComponents components = new TimeComponents(millis);

        if (components.getYears() > 0) {
            double years = millis / (double) TimeUnit.YEAR.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(years) + "y";
        }

        if (components.getMonths() > 0) {
            double months = millis / (double) TimeUnit.MONTH.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(months) + "mo";
        }

        if (components.getWeeks() > 0) {
            double weeks = millis / (double) TimeUnit.WEEK.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(weeks) + "w";
        }

        if (components.getDays() > 0) {
            double days = millis / (double) TimeUnit.DAY.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(days) + "d";
        }

        if (components.getHours() > 0) {
            double hours = millis / (double) TimeUnit.HOUR.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(hours) + "h";
        }

        if (components.getMinutes() > 0) {
            double minutes = millis / (double) TimeUnit.MINUTE.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(minutes) + "m";
        }

        if (components.getSeconds() > 0) {
            double seconds = millis / (double) TimeUnit.SECOND.getMillis();
            DecimalFormat df = cache.getDecimalFormat(1, false);
            return df.format(seconds) + "s";
        }

        return millis + "ms";
    }

    private String formatApproximateDuration(long millis) {
        TimeComponents components = new TimeComponents(millis);
        List<String> parts = new ArrayList<>();

        if (components.getYears() > 0) {
            parts.add(components.getYears() + "y");
            if (components.getMonths() > 0) {
                parts.add(components.getMonths() + "mo");
            }
        } else if (components.getMonths() > 0) {
            parts.add(components.getMonths() + "mo");
            if (components.getWeeks() > 0) {
                parts.add(components.getWeeks() + "w");
            }
        } else if (components.getWeeks() > 0) {
            parts.add(components.getWeeks() + "w");
            if (components.getDays() > 0) {
                parts.add(components.getDays() + "d");
            }
        } else if (components.getDays() > 0) {
            parts.add(components.getDays() + "d");
            if (components.getHours() > 0) {
                parts.add(components.getHours() + "h");
            }
        } else if (components.getHours() > 0) {
            parts.add(components.getHours() + "h");
            if (components.getMinutes() > 0) {
                parts.add(components.getMinutes() + "m");
            }
        } else if (components.getMinutes() > 0) {
            parts.add(components.getMinutes() + "m");
            if (components.getSeconds() > 0) {
                parts.add(components.getSeconds() + "s");
            }
        } else {
            parts.add(components.getSeconds() + "s");
        }

        return String.join(" ", parts);
    }
}
