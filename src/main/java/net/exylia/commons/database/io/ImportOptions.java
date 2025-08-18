package net.exylia.commons.database.io;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
public class ImportOptions {
    private String filePath;
    private Set<Class<?>> entityClasses;
    private boolean compressed = false;
    private boolean clearBeforeImport = false;
    private boolean useUpsert = true;
    private boolean continueOnError = true;
    private int batchSize = 100;
    private boolean validateBeforeImport = true;

    public ImportOptions() {}

}