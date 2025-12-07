package net.exylia.commons.v2.formatter.date;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DatePattern {
    ISO("yyyy-MM-dd'T'HH:mm:ss"),
    ISO_DATE("yyyy-MM-dd"),
    ISO_TIME("HH:mm:ss"),
    EU_DATETIME("dd/MM/yyyy HH:mm:ss"),
    EU_DATE("dd/MM/yyyy"),
    EU_TIME("HH:mm:ss"),
    US_DATETIME("MM/dd/yyyy HH:mm:ss"),
    US_DATE("MM/dd/yyyy"),
    US_TIME("hh:mm:ss a"),
    COMPACT_DATETIME("yyyyMMddHHmmss"),
    HUMAN_READABLE("MMMM dd, yyyy HH:mm:ss"),
    HUMAN_DATE("MMMM dd, yyyy"),
    SHORT_DATETIME("dd/MM/yy HH:mm"),
    TIMESTAMP("yyyy-MM-dd HH:mm:ss");

    private final String pattern;
}
