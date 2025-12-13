package net.exylia.commons.v2.action.migration;

import net.exylia.commons.actions.ActionContext;
import net.exylia.commons.actions.ActionSource;

import java.util.HashMap;

public class MigrationHelper {

    public static ActionContext convertContextV2ToV1(net.exylia.commons.v2.action.model.ActionContext v2Context) {
        ActionSource v1Source = convertSourceV2ToV1(v2Context.getSource());

        ActionContext v1Context = new ActionContext(v2Context.getPlayer(), v1Source);

        for (var entry : v2Context.getData().entrySet()) {
            v1Context = v1Context.withData(entry.getKey(), entry.getValue());
        }

        return v1Context;
    }

    public static net.exylia.commons.v2.action.model.ActionContext convertContextV1ToV2(ActionContext v1Context) {
        net.exylia.commons.v2.action.model.ActionSource v2Source = convertSourceV1ToV2(v1Context.getSource());

        return net.exylia.commons.v2.action.model.ActionContext.builder()
                .player(v1Context.getPlayer())
                .source(v2Source)
                .data(new HashMap<>())
                .build();
    }

    public static ActionSource convertSourceV2ToV1(net.exylia.commons.v2.action.model.ActionSource v2Source) {
        switch (v2Source) {
            case MENU:
                return ActionSource.MENU;
            case ITEM_CLICK:
                return ActionSource.ITEM_CLICK;
            case ITEM_USE:
                return ActionSource.ITEM_USE;
            case COMMAND:
                return ActionSource.COMMAND;
            case NPC:
                return ActionSource.NPC;
            case INVENTORY_CLICK:
                return ActionSource.INVENTORY_CLICK;
            case CUSTOM:
            case CHAT:
            case REGION:
            case HOLOGRAM:
            default:
                return ActionSource.CUSTOM;
        }
    }

    public static net.exylia.commons.v2.action.model.ActionSource convertSourceV1ToV2(ActionSource v1Source) {
        switch (v1Source) {
            case MENU:
                return net.exylia.commons.v2.action.model.ActionSource.MENU;
            case ITEM_CLICK:
                return net.exylia.commons.v2.action.model.ActionSource.ITEM_CLICK;
            case ITEM_USE:
                return net.exylia.commons.v2.action.model.ActionSource.ITEM_USE;
            case COMMAND:
                return net.exylia.commons.v2.action.model.ActionSource.COMMAND;
            case NPC:
                return net.exylia.commons.v2.action.model.ActionSource.NPC;
            case INVENTORY_CLICK:
                return net.exylia.commons.v2.action.model.ActionSource.INVENTORY_CLICK;
            case CUSTOM:
            default:
                return net.exylia.commons.v2.action.model.ActionSource.CUSTOM;
        }
    }
}
