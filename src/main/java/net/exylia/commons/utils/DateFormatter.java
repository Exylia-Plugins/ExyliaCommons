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

    /**
     * Método principal de formateo - detecta automáticamente el tipo de entrada
     */
    public String format(Object input) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.format(defaultFormatter);
    }

    /**
     * Formateo con patrón personalizado
     */
    public String format(Object input, String pattern) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Formateo solo fecha
     */
    public String formatDate(Object input) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.format(dateOnlyFormatter);
    }

    /**
     * Formateo solo hora
     */
    public String formatTime(Object input) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.format(timeOnlyFormatter);
    }

    /**
     * Formateo ISO
     */
    public String formatISO(Object input) {
        LocalDateTime dateTime = parseInput(input);
        return dateTime.format(isoFormatter);
    }

    /**
     * Formateo relativo inteligente usando TimeFormatter - "2m 30s ago", "in 1h 15m", etc.
     */
    public String formatRelative(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();

        return calculateRelativeTime(inputDateTime, now);
    }

    /**
     * Formateo relativo desde una fecha específica
     */
    public String formatRelativeFrom(Object input, Object fromDate) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime fromDateTime = parseInput(fromDate);

        return calculateRelativeTime(inputDateTime, fromDateTime);
    }

    /**
     * Formateo relativo compacto (sin sufijos ago/in)
     */
    public String formatRelativeCompact(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();

        long diffMillis = Math.abs(Duration.between(now, inputDateTime).toMillis());
        return TimeFormatter.timeFormatter.formatCompact(diffMillis);
    }

    /**
     * Formateo relativo usando TimeFormatter en modo verbal
     */
    public String formatRelativeVerbose(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();

        long diffMillis = Duration.between(now, inputDateTime).toMillis();
        boolean isFuture = inputDateTime.isAfter(now);

        return TimeFormatter.timeFormatter.formatVerbal(Math.abs(diffMillis));
    }

    /**
     * Obtiene la diferencia en una unidad específica
     */
    public long getDifference(Object input1, Object input2, ChronoUnit unit) {
        LocalDateTime dt1 = parseInput(input1);
        LocalDateTime dt2 = parseInput(input2);

        return unit.between(dt1, dt2);
    }

    /**
     * Obtiene la diferencia como duración formateada
     */
    public String getDifferenceFormatted(Object input1, Object input2) {
        LocalDateTime dt1 = parseInput(input1);
        LocalDateTime dt2 = parseInput(input2);

        long diffMillis = Math.abs(Duration.between(dt1, dt2).toMillis());
        return TimeFormatter.timeFormatter.format(diffMillis);
    }

    /**
     * Verifica si una fecha es pasada
     */
    public boolean isPast(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        return inputDateTime.isBefore(LocalDateTime.now());
    }

    /**
     * Verifica si una fecha es futura
     */
    public boolean isFuture(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        return inputDateTime.isAfter(LocalDateTime.now());
    }

    /**
     * Verifica si una fecha es hoy
     */
    public boolean isToday(Object input) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDate inputDate = inputDateTime.toLocalDate();
        return inputDate.equals(LocalDate.now());
    }

    /**
     * Verifica si una fecha está dentro de un rango de tiempo
     */
    public boolean isWithin(Object input, long amount, ChronoUnit unit) {
        LocalDateTime inputDateTime = parseInput(input);
        LocalDateTime now = LocalDateTime.now();

        long diffMillis = Math.abs(Duration.between(now, inputDateTime).toMillis());
        long rangeMillis = unit.getDuration().toMillis() * amount;

        return diffMillis <= rangeMillis;
    }

    /**
     * Parser inteligente que maneja múltiples tipos de entrada
     */
    private LocalDateTime parseInput(Object input) {
        if (input == null) {
            throw new IllegalArgumentException("La entrada no puede ser null");
        }

        // LocalDateTime directo
        if (input instanceof LocalDateTime) {
            return (LocalDateTime) input;
        }

        // LocalDate
        if (input instanceof LocalDate) {
            return ((LocalDate) input).atStartOfDay();
        }

        // Instant
        if (input instanceof Instant) {
            return LocalDateTime.ofInstant((Instant) input, ZoneId.systemDefault());
        }

        // ZonedDateTime
        if (input instanceof ZonedDateTime) {
            return ((ZonedDateTime) input).toLocalDateTime();
        }

        // java.util.Date
        if (input instanceof java.util.Date) {
            return LocalDateTime.ofInstant(((java.util.Date) input).toInstant(), ZoneId.systemDefault());
        }

        // java.sql.Timestamp
        if (input instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) input).toLocalDateTime();
        }

        // Números (timestamps)
        if (input instanceof Number) {
            return parseNumericInput((Number) input);
        }

        // String
        if (input instanceof String) {
            return parseStringInput((String) input);
        }

        throw new IllegalArgumentException("Tipo no soportado: " + input.getClass().getSimpleName());
    }

    /**
     * Parsea entradas numéricas (timestamps)
     */
    private LocalDateTime parseNumericInput(Number number) {
        long value = number.longValue();

        // Detectar si son segundos o milisegundos
        // Si el número es menor que el timestamp de año 3000 en segundos, asumimos segundos
        if (value < 32503680000L) { // 01/01/3000 en segundos
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneId.systemDefault());
        } else {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
        }
    }

    /**
     * Parsea entradas de string con múltiples formatos
     */
    private LocalDateTime parseStringInput(String dateString) {
        dateString = dateString.trim();

        // Intentar diferentes formatos comunes
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

                // Si es solo fecha, agregar tiempo por defecto
                if (pattern.equals("yyyy-MM-dd") || pattern.equals("dd/MM/yyyy") || pattern.equals("dd-MM-yyyy")) {
                    LocalDate date = LocalDate.parse(dateString, formatter);
                    return date.atStartOfDay();
                }

                // Si es solo tiempo, agregar fecha de hoy
                if (pattern.equals("HH:mm:ss") || pattern.equals("HH:mm")) {
                    LocalTime time = LocalTime.parse(dateString, formatter);
                    return LocalDate.now().atTime(time);
                }

                return LocalDateTime.parse(dateString, formatter);
            } catch (Exception ignored) {
                // Continuar con el siguiente formato
            }
        }

        // Intentar como ISO
        try {
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}

        throw new IllegalArgumentException("No se pudo parsear la fecha: " + dateString);
    }

    /**
     * Calcula el tiempo relativo entre dos fechas usando TimeFormatter
     */
    private String calculateRelativeTime(LocalDateTime target, LocalDateTime reference) {
        long diffMillis = Duration.between(reference, target).toMillis();
        return TimeFormatter.timeFormatter.format((int) Math.abs(diffMillis) / 1000);
    }

    // Métodos de conveniencia estáticos adicionales
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