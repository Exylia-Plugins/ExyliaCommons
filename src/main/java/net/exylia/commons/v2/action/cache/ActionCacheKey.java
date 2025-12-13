package net.exylia.commons.v2.action.cache;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.UUID;

@Value
@EqualsAndHashCode
public class ActionCacheKey {
    UUID playerId;
    String actionId;
}
