package net.exylia.commons.v2.formatter.price;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompactSuffix {
    TRILLION("T", 1_000_000_000_000.0),
    BILLION("B", 1_000_000_000.0),
    MILLION("M", 1_000_000.0),
    THOUSAND("K", 1_000.0),
    NONE("", 1.0);

    private final String suffix;
    private final double divisor;

    public static CompactSuffix fromValue(double value) {
        double absValue = Math.abs(value);

        if (absValue >= TRILLION.divisor) {
            return TRILLION;
        } else if (absValue >= BILLION.divisor) {
            return BILLION;
        } else if (absValue >= MILLION.divisor) {
            return MILLION;
        } else if (absValue >= THOUSAND.divisor) {
            return THOUSAND;
        } else {
            return NONE;
        }
    }
}
