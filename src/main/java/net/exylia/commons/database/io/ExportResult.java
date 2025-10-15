package net.exylia.commons.database.io;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExportResult {
    private boolean success;
    private String filePath;
    private int totalTables;
    private int totalEntities;
    private long fileSize;
    private long duration;
    private String error;

}
