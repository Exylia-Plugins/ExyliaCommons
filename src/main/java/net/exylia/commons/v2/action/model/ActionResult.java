package net.exylia.commons.v2.action.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ActionResult {
    private final boolean success;
    private final String message;
    private final Throwable error;
    private final Map<String, Object> metadata;
    private final long executionTimeMillis;

    public static ActionResult success() {
        return new ActionResult(true, "Success", null, new HashMap<>(), 0);
    }

    public static ActionResult success(String message) {
        return new ActionResult(true, message, null, new HashMap<>(), 0);
    }

    public static ActionResult success(String message, long executionTime) {
        return new ActionResult(true, message, null, new HashMap<>(), executionTime);
    }

    public static ActionResult failure(Throwable error) {
        return new ActionResult(false, error.getMessage(), error, new HashMap<>(), 0);
    }

    public static ActionResult failure(String message) {
        return new ActionResult(false, message, null, new HashMap<>(), 0);
    }

    public static ActionResult cancelled(String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cancelled", true);
        return new ActionResult(false, reason, null, metadata, 0);
    }

    public boolean isCancelled() {
        return metadata.containsKey("cancelled") && (boolean) metadata.get("cancelled");
    }
}
