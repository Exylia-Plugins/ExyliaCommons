package net.exylia.commons.v2.database.transfer.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferResult {

    private final boolean success;
    private final int tablesProcessed;
    private final int rowsProcessed;
    private final long durationMs;
    private final String outputPath;
    private final String error;
}
