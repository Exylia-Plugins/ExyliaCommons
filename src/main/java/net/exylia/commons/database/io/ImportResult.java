package net.exylia.commons.database.io;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Setter
@Getter
public class ImportResult {
    private boolean success;
    private int totalImported;
    private long duration;
    private List<String> importedTables;
    private List<String> skippedTables;
    private List<String> errors;
    private ExportMetadata sourceMetadata;
}