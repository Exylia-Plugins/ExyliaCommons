package net.exylia.commons.v2.formatter.time;

import lombok.Getter;

@Getter
public class TimeComponents {
    private final long years;
    private final long months;
    private final long weeks;
    private final long days;
    private final long hours;
    private final long minutes;
    private final long seconds;
    private final long millis;

    public TimeComponents(long totalMillis) {
        long remaining = totalMillis;

        this.years = remaining / TimeUnit.YEAR.getMillis();
        remaining %= TimeUnit.YEAR.getMillis();

        this.months = remaining / TimeUnit.MONTH.getMillis();
        remaining %= TimeUnit.MONTH.getMillis();

        this.weeks = remaining / TimeUnit.WEEK.getMillis();
        remaining %= TimeUnit.WEEK.getMillis();

        this.days = remaining / TimeUnit.DAY.getMillis();
        remaining %= TimeUnit.DAY.getMillis();

        this.hours = remaining / TimeUnit.HOUR.getMillis();
        remaining %= TimeUnit.HOUR.getMillis();

        this.minutes = remaining / TimeUnit.MINUTE.getMillis();
        remaining %= TimeUnit.MINUTE.getMillis();

        this.seconds = remaining / TimeUnit.SECOND.getMillis();
        this.millis = remaining % TimeUnit.SECOND.getMillis();
    }

    public static TimeComponents fromMillis(long millis) {
        return new TimeComponents(millis);
    }
}
