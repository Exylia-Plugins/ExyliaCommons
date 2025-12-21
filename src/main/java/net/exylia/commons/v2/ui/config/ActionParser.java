package net.exylia.commons.v2.ui.config;

import net.exylia.commons.v2.ui.model.ClickAction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.ClickType;

import java.util.*;

public class ActionParser {

    public static Map<ClickType, List<ClickAction>> parseActions(ConfigurationSection section) {
        Map<ClickType, List<ClickAction>> actions = new HashMap<>();

        if (section == null) {
            return actions;
        }

        for (String clickTypeStr : section.getKeys(false)) {
            ClickType clickType = parseClickType(clickTypeStr);
            if (clickType == null) {
                continue;
            }

            List<ClickAction> clickActions = new ArrayList<>();

            if (section.isList(clickTypeStr)) {
                List<?> actionList = section.getList(clickTypeStr);
                if (actionList != null) {
                    for (Object obj : actionList) {
                        if (obj instanceof String) {
                            clickActions.add(parseActionString((String) obj));
                        } else if (obj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> actionMap = (Map<String, Object>) obj;
                            clickActions.add(parseActionMap(actionMap));
                        }
                    }
                }
            } else if (section.isConfigurationSection(clickTypeStr)) {
                ConfigurationSection actionConfig = section.getConfigurationSection(clickTypeStr);
                clickActions.add(parseActionConfig(actionConfig));
            } else if (section.isString(clickTypeStr)) {
                String actionString = section.getString(clickTypeStr);
                if (actionString != null) {
                    clickActions.add(parseActionString(actionString));
                }
            }

            if (!clickActions.isEmpty()) {
                actions.put(clickType, clickActions);
            }
        }

        return actions;
    }

    private static ClickAction parseActionString(String actionString) {
        return ClickAction.builder()
            .actionString(actionString)
            .async(true)
            .build();
    }

    private static ClickAction parseActionConfig(ConfigurationSection config) {
        String actionString = config.getString("action");
        if (actionString == null) {
            actionString = config.getString("command");
        }
        if (actionString == null) {
            actionString = "ui:none";
        }

        return ClickAction.builder()
            .actionString(actionString)
            .cooldown(config.getLong("cooldown", 0))
            .permission(config.getString("permission"))
            .closeMenu(config.getBoolean("close_menu", false))
            .refreshMenu(config.getBoolean("refresh_menu", false))
            .async(config.getBoolean("async", true))
            .build();
    }

    private static ClickAction parseActionMap(Map<String, Object> map) {
        String actionString = (String) map.getOrDefault("action", map.get("command"));
        if (actionString == null) {
            actionString = "ui:none";
        }

        Object cooldownObj = map.get("cooldown");
        long cooldown = 0;
        if (cooldownObj instanceof Number) {
            cooldown = ((Number) cooldownObj).longValue();
        }

        return ClickAction.builder()
            .actionString(actionString)
            .cooldown(cooldown)
            .permission((String) map.get("permission"))
            .closeMenu(Boolean.TRUE.equals(map.get("close_menu")))
            .refreshMenu(Boolean.TRUE.equals(map.get("refresh_menu")))
            .async(!Boolean.FALSE.equals(map.get("async")))
            .build();
    }

    public static ClickType parseClickType(String str) {
        if (str == null) {
            return null;
        }

        try {
            return ClickType.valueOf(str.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return switch (str.toLowerCase()) {
                case "left", "left_click" -> ClickType.LEFT;
                case "right", "right_click" -> ClickType.RIGHT;
                case "shift", "shift_click" -> ClickType.SHIFT_LEFT;
                case "shift_right", "shift_right_click" -> ClickType.SHIFT_RIGHT;
                case "middle", "middle_click" -> ClickType.MIDDLE;
                case "drop", "drop_key" -> ClickType.DROP;
                case "all", "any" -> ClickType.LEFT;
                default -> null;
            };
        }
    }
}
