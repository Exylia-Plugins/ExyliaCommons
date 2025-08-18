package net.exylia.commons.database.io;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class DatabaseExport {
    private ExportMetadata metadata;
    private Map<String, EntityExport> data;

}