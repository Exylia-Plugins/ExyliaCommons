package net.exylia.commons.database.io;

import lombok.Getter;
import lombok.Setter;

@Deprecated
@Setter
@Getter
public class ExportMetadata {
    private long timestamp;
    private String databaseType;
    private String pluginName;
    private String pluginVersion;
    private String serverVersion;
    private String exportVersion;
    private ExportOptions options;

}
