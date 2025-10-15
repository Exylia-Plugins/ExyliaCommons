package net.exylia.commons.database.io;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
public class ExportOptions {
    private String fileName;
    private Set<Class<?>> entityClasses;
    private boolean compressed = true;
    private boolean includeEmptyTables = false;
    private boolean continueOnError = true;
    private boolean includeMetadata = true;

    public ExportOptions() {}

}
