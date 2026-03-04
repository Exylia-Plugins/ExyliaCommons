package net.exylia.commons.v2.formatter.date;

import lombok.Getter;
import net.exylia.commons.v2.formatter.core.AbstractFormatter;
import net.exylia.commons.v2.formatter.core.FormatterException;
import net.exylia.commons.v2.formatter.cache.FormatterCache;
import net.exylia.commons.v2.formatter.time.TimeFormatter;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Getter
public class DateFormatter extends AbstractFormatter<Object, String> {
    private final DateFormatterConfig config;
    private final TimeFormatter timeFormatter;

    DateFormatter(FormatterCache cache, DateFormatterConfig config) {
        super(cache);
        this.config = config;
        this.timeFormatter = TimeFormatter.builder().cache(cache).build();
    }

    public static DateFormatterBuilder builder() {
        return new DateFormatterBuilder();
    }

    @Override
    public String format(Object input) {
        long startTime = System.nanoTime();
        LocalDateTime dateTime = parseInput(input);

        String pattern = config.isUseIsoDefault() ? DatePattern.ISO.getPattern() : config.getDefaultPattern();
        String cacheKey = getCacheKey(input, "default");
        String cachedResult = cache.getResultCache().getIfPresent(cacheKey);

        if (cachedResult != null) {
            stats.recordFormat(System.nanoTime() - startTime, true);
            return cachedResult;
        }

        DateTimeFormatter formatter = cache.getDateTimeFormatter(pattern);
        String result = dateTime.format(formatter);
        cache.getResultCache().put(cacheKey, result);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    @Override
    public String format(Object input, String customPattern) {
        long startTime = System.nanoTime();
        LocalDateTime dateTime = parseInput(input);

        String cacheKey = getCacheKey(input, customPattern);
        String cachedResult = cache.getResultCache().getIfPresent(cacheKey);

        if (cachedResult != null) {
            stats.recordFormat(System.nanoTime() - startTime, true);
            return cachedResult;
        }

        DateTimeFormatter formatter = cache.getDateTimeFormatter(customPattern);
        String result = dateTime.format(formatter);
        cache.getResultCache().put(cacheKey, result);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatDate(Object input) {
        long startTime = System.nanoTime();
        LocalDateTime dateTime = parseInput(input);

        String cacheKey = getCacheKey(input, "date");
        String cachedResult = cache.getResultCache().getIfPresent(cacheKey);

        if (cachedResult != null) {
            stats.recordFormat(System.nanoTime() - startTime, true);
            return cachedResult;
        }

        DateTimeFormatter formatter = cache.getDateTimeFormatter(config.getDatePattern());
        String result = dateTime.format(formatter);
        cache.getResultCache().put(cacheKey, result);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatTime(Object input) {
        long startTime = System.nanoTime();
        LocalDateTime dateTime = parseInput(input);

        String cacheKey = getCacheKey(input, "time");
        String cachedResult = cache.getResultCache().getIfPresent(cacheKey);

        if (cachedResult != null) {
            stats.recordFormat(System.nanoTime() - startTime, true);
            return cachedResult;
        }

        DateTimeFormatter formatter = cache.getDateTimeFormatter(config.getTimePattern());
        String result = dateTime.format(formatter);
        cache.getResultCache().put(cacheKey, result);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatISO(Object input) {
        long startTime = System.nanoTime();
        LocalDateTime dateTime = parseInput(input);

        String cacheKey = getCacheKey(input, "iso");
        String cachedResult = cache.getResultCache().getIfPresent(cacheKey);

        if (cachedResult != null) {
            stats.recordFormat(System.nanoTime() - startTime, true);
            return cachedResult;
        }

        DateTimeFormatter formatter = cache.getDateTimeFormatter(DatePattern.ISO.getPattern());
        String result = dateTime.format(formatter);
        cache.getResultCache().put(cacheKey, result);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatRelative(Object input) {
        return formatRelativeFrom(input, LocalDateTime.now());
    }

    public String formatRelativeFrom(Object input, Object fromDate) {
        long startTime = System.nanoTime();
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime fromDateTime = parseInput(fromDate);

        String result = calculateRelativeTime(inputDateTime, fromDateTime);
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatRelativeCompact(Object input) {
        long startTime = System.nanoTime();
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();

        long millis = Math.abs(Duration.between(inputDateTime, now).toMillis());
        String timeStr = timeFormatter.formatCompact(millis);
        boolean isPast = inputDateTime.isBefore(now);

        String result = isPast ? timeStr + " ago" : "in " + timeStr;
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public String formatRelativeVerbose(Object input) {
        long startTime = System.nanoTime();
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();

        long millis = Math.abs(Duration.between(inputDateTime, now).toMillis());
        String timeStr = timeFormatter.formatVerbal(millis);
        boolean isPast = inputDateTime.isBefore(now);

        String result = isPast ? timeStr + " ago" : "in " + timeStr;
        stats.recordFormat(System.nanoTime() - startTime, false);
        return result;
    }

    public long getDifference(Object input1, Object input2, ChronoUnit unit) {
        LocalDateTime date1 = parseInput(input1);
        LocalDateTime date2 = parseInput(input2);
        return unit.between(date1, date2);
    }

    public String getDifferenceFormatted(Object input1, Object input2) {
        LocalDateTime date1 = parseInput(input1);
        LocalDateTime date2 = parseInput(input2);

        long millis = Math.abs(Duration.between(date1, date2).toMillis());
        return timeFormatter.format(millis);
    }

    public boolean isPast(Object input) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.isBefore(LocalDateTime.now());
    }

    public boolean isFuture(Object input) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.isAfter(LocalDateTime.now());
    }

    public boolean isToday(Object input) {
        LocalDateTime dateTime = parseInput(input);
        LocalDate today = LocalDate.now();
        return dateTime.toLocalDate().equals(today);
    }

    public boolean isWithin(Object input, long millis) {
        LocalDateTime dateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();
        long difference = Math.abs(Duration.between(dateTime, now).toMillis());
        return difference <= millis;
    }

    @Override
    protected String getCacheKey(Object input, String pattern) {
        if (input instanceof LocalDateTime) {
            LocalDateTime dt = (LocalDateTime) input;
            return "date_" + pattern + "_" + dt.toEpochSecond(ZoneOffset.UTC);
        }
        return "date_" + pattern + "_" + input.toString();
    }

    private LocalDateTime parseInput(Object input) {
        if (input == null) {
            throw new FormatterException("Input cannot be null");
        }

        if (input instanceof LocalDateTime) {
            return (LocalDateTime) input;
        }

        if (input instanceof LocalDate) {
            return ((LocalDate) input).atStartOfDay();
        }

        if (input instanceof Instant) {
            return LocalDateTime.ofInstant((Instant) input, ZoneId.systemDefault());
        }

        if (input instanceof ZonedDateTime) {
            return ((ZonedDateTime) input).toLocalDateTime();
        }

        if (input instanceof Date) {
            return LocalDateTime.ofInstant(((Date) input).toInstant(), ZoneId.systemDefault());
        }

        if (input instanceof Timestamp) {
            return ((Timestamp) input).toLocalDateTime();
        }

        if (input instanceof Number) {
            long value = ((Number) input).longValue();
            if (value > 10000000000L) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
            } else {
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneId.systemDefault());
            }
        }

        if (input instanceof String) {
            return parseStringInput((String) input);
        }

        throw new FormatterException("Unsupported input type: " + input.getClass().getName());
    }

    private LocalDateTime parseStringInput(String dateString) {
        for (DatePattern pattern : DatePattern.values()) {
            try {
                DateTimeFormatter formatter = cache.getDateTimeFormatter(pattern.getPattern());
                return LocalDateTime.parse(dateString, formatter);
            } catch (Exception ignored) {
            }
        }

        try {
            DateTimeFormatter formatter = cache.getDateTimeFormatter(config.getDefaultPattern());
            return LocalDateTime.parse(dateString, formatter);
        } catch (Exception e) {
            throw new FormatterException("Unable to parse date string: " + dateString, e);
        }
    }

    private String calculateRelativeTime(LocalDateTime inputDateTime, LocalDateTime fromDateTime) {
        long millis = Duration.between(fromDateTime, inputDateTime).toMillis();
        boolean isPast = millis < 0;
        millis = Math.abs(millis);

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        String result;
        if (years > 0) {
            result = years + " year" + (years > 1 ? "s" : "");
        } else if (months > 0) {
            result = months + " month" + (months > 1 ? "s" : "");
        } else if (weeks > 0) {
            result = weeks + " week" + (weeks > 1 ? "s" : "");
        } else if (days > 0) {
            result = days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            result = hours + " hour" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            result = minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            result = seconds + " second" + (seconds != 1 ? "s" : "");
        }

        return isPast ? result + " ago" : "in " + result;
    }
}
