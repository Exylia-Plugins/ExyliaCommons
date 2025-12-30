package net.exylia.commons.v2.items.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Set;

@Getter
@Builder
public class ClickAction {

    @Singular
    private final Set<ClickTypeGroup> clickTypes;
    private final String action;

    public boolean matchesClick(ClickTypeGroup clickType) {
        if (clickTypes == null || clickTypes.isEmpty()) {
            return true;
        }

        if (clickTypes.contains(ClickTypeGroup.ANY)) {
            return true;
        }

        return clickTypes.contains(clickType);
    }
}
