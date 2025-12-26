package net.exylia.commons.v2.snapshot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SnapshotMetadata {

    private long createdAt;
    private long lastAccessedAt;
    private String createdBy;
    private String tag;
    private Map<String, String> customData;

    public static SnapshotMetadata createDefault() {
        long now = System.currentTimeMillis();
        return new SnapshotMetadata(
                now,
                now,
                "system",
                null,
                new HashMap<>()
        );
    }

    public void updateAccessTime() {
        this.lastAccessedAt = System.currentTimeMillis();
    }
}
