package net.exylia.commons.v2.database.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Indexes.class)
public @interface Index {
    String name();

    String[] fields();

    /** Index key directions matching {@link #fields()}; positive is ascending, negative descending. */
    int[] directions() default {};

    /** Include the persisted entity primary key as the final tie-breaker key. */
    boolean includePrimaryKey() default false;
}
