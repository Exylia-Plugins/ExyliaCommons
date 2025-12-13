package net.exylia.commons.v2.formatter.api;

import net.exylia.commons.async.AsyncAPI;
import net.exylia.commons.v2.formatter.core.FormatterRegistry;
import net.exylia.commons.v2.formatter.date.DateFormatter;
import net.exylia.commons.v2.formatter.price.PriceFormatter;
import net.exylia.commons.v2.formatter.time.TimeFormatter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncFormatterAPI {
    private static volatile AsyncFormatterAPI instance;

    private AsyncFormatterAPI() {}

    public static AsyncFormatterAPI getInstance() {
        if (instance == null) {
            synchronized (AsyncFormatterAPI.class) {
                if (instance == null) {
                    instance = new AsyncFormatterAPI();
                }
            }
        }
        return instance;
    }

    public CompletableFuture<List<String>> formatTimeBatch(List<Object> inputs) {
        return AsyncAPI.compute(() -> {
            TimeFormatter formatter = FormatterRegistry.getTimeFormatter();
            return formatter.formatBatch(inputs);
        });
    }

    public CompletableFuture<List<String>> formatDateBatch(List<Object> inputs) {
        return AsyncAPI.compute(() -> {
            DateFormatter formatter = FormatterRegistry.getDateFormatter();
            return formatter.formatBatch(inputs);
        });
    }

    public CompletableFuture<List<String>> formatPriceBatch(List<Object> inputs) {
        return AsyncAPI.compute(() -> {
            PriceFormatter formatter = FormatterRegistry.getPriceFormatter();
            return formatter.formatBatch(inputs);
        });
    }

    public CompletableFuture<List<String>> formatTimeBatchWithPattern(List<Object> inputs, String pattern) {
        return AsyncAPI.compute(() -> {
            TimeFormatter formatter = FormatterRegistry.getTimeFormatter();
            return inputs.stream()
                .map(input -> formatter.format(input, pattern))
                .collect(java.util.stream.Collectors.toList());
        });
    }

    public CompletableFuture<List<String>> formatDateBatchWithPattern(List<Object> inputs, String pattern) {
        return AsyncAPI.compute(() -> {
            DateFormatter formatter = FormatterRegistry.getDateFormatter();
            return inputs.stream()
                .map(input -> formatter.format(input, pattern))
                .collect(java.util.stream.Collectors.toList());
        });
    }

    public CompletableFuture<Map<String, String>> formatMixed(Map<String, FormatterType> formatTasks) {
        return AsyncAPI.compute(() -> {
            Map<String, String> results = new ConcurrentHashMap<>();
            TimeFormatter timeFormatter = FormatterRegistry.getTimeFormatter();
            DateFormatter dateFormatter = FormatterRegistry.getDateFormatter();
            PriceFormatter priceFormatter = FormatterRegistry.getPriceFormatter();

            formatTasks.entrySet().parallelStream().forEach(entry -> {
                String key = entry.getKey();
                FormatterType type = entry.getValue();

                String result = switch (type.getType()) {
                    case "time" -> timeFormatter.format(type.getValue());
                    case "date" -> dateFormatter.format(type.getValue());
                    case "price" -> priceFormatter.format(type.getValue());
                    default -> String.valueOf(type.getValue());
                };

                results.put(key, result);
            });

            return results;
        });
    }

    public static class FormatterType {
        private final String type;
        private final Object value;

        private FormatterType(String type, Object value) {
            this.type = type;
            this.value = value;
        }

        public static FormatterType time(Object value) {
            return new FormatterType("time", value);
        }

        public static FormatterType date(Object value) {
            return new FormatterType("date", value);
        }

        public static FormatterType price(Object value) {
            return new FormatterType("price", value);
        }

        public String getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }
}
