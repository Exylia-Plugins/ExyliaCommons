package net.exylia.commons.utils;

import net.exylia.commons.configSimple.Configs;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateFormatter {
    public static final DateFormatter dateFormatter = new DateFormatter();
    private static DateTimeFormatter defaultFormatter;
    private static DateTimeFormatter dateOnlyFormatter;
    private static DateTimeFormatter timeOnlyFormatter;
    private static DateTimeFormatter isoFormatter;
    private DateFormatter() {
        init();
    }
    public static void init() {
        reload();
    }
    public static void reload() {
        String defaultPattern = Configs.string("date-formatter.default-pattern");
        String datePattern = Configs.string("date-formatter.date-pattern");
        String timePattern = Configs.string("date-formatter.time-pattern");
        boolean useIsoDefault = Configs.bool("date-formatter.use-iso-default");

        try {
            defaultFormatter = DateTimeFormatter.ofPattern(defaultPattern);
        } catch (Exception e) {
            defaultFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        }

        try {
            dateOnlyFormatter = DateTimeFormatter.ofPattern(datePattern);
        } catch (Exception e) {
            dateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        }

        try {
            timeOnlyFormatter = DateTimeFormatter.ofPattern(timePattern);
        } catch (Exception e) {
            timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        }

        isoFormatter = useIsoDefault ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : defaultFormatter;
    }

    public String format(Object input) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.format(defaultFormatter);
    }

    public String format(Object input, String pattern) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public String formatDate(Object input) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.format(dateOnlyFormatter);
    }

    public String formatTime(Object input) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.format(timeOnlyFormatter);
    }

    public String formatISO(Object input) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.format(isoFormatter);
    }

    public String formatRelative(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();

        return calculateRelativeTime(inputDateTime, now);
    }

    public String formatRelativeFrom(Object input, Object fromDate) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime fromDateTime = parseInput(fromDate);

        return calculateRelativeTime(inputDateTime, fromDateTime);
    }

    public String formatRelativeCompact(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();

        long diffMillis = Math.abs(Duration.between(now, inputDateTime).toMillis());
        return TimeFormatter.timeFormatter.formatCompact(diffMillis);
    }

    public String formatRelativeVerbose(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();

        long diffMillis = Duration.between(now, inputDateTime).toMillis();
        boolean isFuture = inputDateTime.isAfter(now);

        return TimeFormatter.timeFormatter.formatVerbal(Math.abs(diffMillis));
    }

    public long getDifference(Object input1, Object input2, ChronoUnit unit) {
        LocalDateTime dt1 = parseInput(input1);
        LocalDateTime dt2 = parseInput(input2);

        return unit.between(dt1, dt2);
    }

    public String getDifferenceFormatted(Object input1, Object input2) {
        LocalDateTime dt1 = parseInput(input1);
        LocalDateTime dt2 = parseInput(input2);

        long diffMillis = Math.abs(Duration.between(dt1, dt2).toMillis());
        return TimeFormatter.timeFormatter.format(diffMillis);
    }

    public boolean isPast(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        return inputDateTime.isBefore(LocalDateTime.now());
    }

    public boolean isFuture(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        return inputDateTime.isAfter(LocalDateTime.now());
    }

    public boolean isToday(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDate inputDate = inputDateTime.toLocalDate();
        return inputDate.equals(LocalDate.now());
    }

    public boolean isWithin(Object input, long amount, ChronoUnit unit) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();

        long diffMillis = Math.abs(Duration.between(now, inputDateTime).toMillis());
        long rangeMillis = unit.getDuration().toMillis() * amount;

        return diffMillis <= rangeMillis;
    }

    private LocalDateTime parseInput(Object input) {
        if (input == null) {
            throw new IllegalArgumentException("La entrada no puede ser null");
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

        if (input instanceof java.util.Date) {
            return LocalDateTime.ofInstant(((java.util.Date) input).toInstant(), ZoneId.systemDefault());
        }

        if (input instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) input).toLocalDateTime();
        }

        if (input instanceof Number) {
            return parseNumericInput((Number) input);
        }

        if (input instanceof String) {
            return parseStringInput((String) input);
        }

        throw new IllegalArgumentException("Tipo no soportado: " + input.getClass().getSimpleName());
    }

    private LocalDateTime parseNumericInput(Number number) {
        long value = number.longValue();

        if (value < 32503680000L) {  
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneId.systemDefault());
        } else {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
        }
    }

    private LocalDateTime parseStringInput(String dateString) {
        dateString = dateString.trim();

        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy HH:mm:ss",
                "dd-MM-yyyy HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "HH:mm:ss",
                "HH:mm"
        };

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

                if (pattern.equals("yyyy-MM-dd") || pattern.equals("dd/MM/yyyy") || pattern.equals("dd-MM-yyyy")) {
                    LocalDate date = LocalDate.parse(dateString, formatter);
                    return date.atStartOfDay();
                }

                if (pattern.equals("HH:mm:ss") || pattern.equals("HH:mm")) {
                    LocalTime time = LocalTime.parse(dateString, formatter);
                    return LocalDate.now().atTime(time);
                }

                return LocalDateTime.parse(dateString, formatter);
            } catch (Exception ignored) {
                 
            }
        }

        try {
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}

        throw new IllegalArgumentException("No se pudo parsear la fecha: " + dateString);
    }

    private String calculateRelativeTime(LocalDateTime target, LocalDateTime reference) {
        long diffMillis = Duration.between(reference, target).toMillis();
        return TimeFormatter.timeFormatter.format((int) Math.abs(diffMillis) / 1000);
    }

    public static String formatNow() {
        return dateFormatter.format(LocalDateTime.now());
    }

    public static String formatNow(String pattern) {
        return dateFormatter.format(LocalDateTime.now(), pattern);
    }

    public static String formatNowRelative() {
        return "now";
    }

    public static String ago(Object input) {
        return dateFormatter.formatRelative(input);
    }

    public static String between(Object start, Object end) {
        return dateFormatter.getDifferenceFormatted(start, end);
    }
}
