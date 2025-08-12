package net.exylia.commons.utils.code;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntRange {
    private int min;
    private int max;

    public IntRange() {
        this(1, 1);
    }

    public IntRange(int min, int max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public IntRange(int value) {
        this(value, value);
    }

    public int getRandomValue() {
        if (min == max) return min;
        return min + (int)(Math.random() * (max - min + 1));
    }

    public boolean isRange() {
        return min != max;
    }

    @Override
    public String toString() {
        return min == max ? String.valueOf(min) : min + "-" + max;
    }
}