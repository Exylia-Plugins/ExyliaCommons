package net.exylia.commons.v2.yaml.config;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@Builder
public class YamlConfig {

    @Builder.Default
    private final Path baseDir = Paths.get("data");

    @Builder.Default
    private final boolean backupEnabled = true;

    @Builder.Default
    private final int maxBackupsPerEntity = 5;

    @Builder.Default
    private final boolean cacheEnabled = true;

    @Builder.Default
    private final long cacheTtlMinutes = 30;

    @Builder.Default
    private final int cacheMaxEntries = 1000;

    @Builder.Default
    private final boolean cacheRecordStats = false;

    @Builder.Default
    private final boolean validateOnLoad = true;

    @Builder.Default
    private final boolean autoCreateDirectories = true;
}
