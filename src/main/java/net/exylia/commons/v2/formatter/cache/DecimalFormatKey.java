package net.exylia.commons.v2.formatter.cache;

import lombok.Value;

@Value
public class DecimalFormatKey {
    int precision;
    boolean forceZero;
    String decimalSeparator;
    String thousandSeparator;
}
