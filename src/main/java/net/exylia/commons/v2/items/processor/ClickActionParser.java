package net.exylia.commons.v2.items.processor;

import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.debug.core.DebugCategory;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickCommand;
import net.exylia.commons.v2.items.model.ClickTypeGroup;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClickActionParser {

    private static final Pattern CLICK_PREFIX_PATTERN = Pattern.compile("^([a-zA-Z_,]+):\\s*(.+)$");

    public static ClickAction parseAction(String actionString) {
        if (actionString == null || actionString.isEmpty()) {
            return null;
        }

        Matcher matcher = CLICK_PREFIX_PATTERN.matcher(actionString.trim());

        if (!matcher.matches()) {
            DebugAPI.logLibDebug(DebugCategory.ITEMS,
                "No click prefix found in action '" + actionString + "', treating as ANY click");
            return ClickAction.builder()
                .clickType(ClickTypeGroup.ANY)
                .action(actionString.trim())
                .build();
        }

        String clickTypesStr = matcher.group(1);
        String action = matcher.group(2);

        if (!isValidClickPrefix(clickTypesStr)) {
            DebugAPI.logLibDebug(DebugCategory.ITEMS,
                "Prefix '" + clickTypesStr + "' is not a valid click type, treating action as: " + action);
            return ClickAction.builder()
                .clickType(ClickTypeGroup.ANY)
                .action(actionString.trim())
                .build();
        }

        Set<ClickTypeGroup> clickTypes = parseClickTypes(clickTypesStr);

        if (clickTypes.isEmpty()) {
            DebugAPI.logLibWarn(DebugCategory.ITEMS,
                "Invalid click types '" + clickTypesStr + "' in action '" + actionString + "', treating as ANY");
            clickTypes.add(ClickTypeGroup.ANY);
        }

        DebugAPI.logLibDebug(DebugCategory.ITEMS,
            "Parsed action: types=" + clickTypes + ", action=" + action);

        return ClickAction.builder()
            .clickTypes(clickTypes)
            .action(action)
            .build();
    }

    public static ClickCommand parseCommand(String commandString) {
        if (commandString == null || commandString.isEmpty()) {
            return null;
        }

        Matcher matcher = CLICK_PREFIX_PATTERN.matcher(commandString.trim());

        if (!matcher.matches()) {
            DebugAPI.logLibDebug(DebugCategory.ITEMS,
                "No click prefix found in command '" + commandString + "', treating as ANY click");
            return ClickCommand.builder()
                .clickType(ClickTypeGroup.ANY)
                .command(commandString.trim())
                .build();
        }

        String clickTypesStr = matcher.group(1);
        String command = matcher.group(2);

        if (!isValidClickPrefix(clickTypesStr)) {
            DebugAPI.logLibDebug(DebugCategory.ITEMS,
                "Prefix '" + clickTypesStr + "' is not a valid click type, treating command as: " + command);
            return ClickCommand.builder()
                .clickType(ClickTypeGroup.ANY)
                .command(commandString.trim())
                .build();
        }

        Set<ClickTypeGroup> clickTypes = parseClickTypes(clickTypesStr);

        if (clickTypes.isEmpty()) {
            DebugAPI.logLibWarn(DebugCategory.ITEMS,
                "Invalid click types '" + clickTypesStr + "' in command '" + commandString + "', treating as ANY");
            clickTypes.add(ClickTypeGroup.ANY);
        }

        DebugAPI.logLibDebug(DebugCategory.ITEMS,
            "Parsed command: types=" + clickTypes + ", command=" + command);

        return ClickCommand.builder()
            .clickTypes(clickTypes)
            .command(command)
            .build();
    }

    private static boolean isValidClickPrefix(String prefix) {
        String[] parts = prefix.split(",");
        for (String part : parts) {
            if (ClickTypeGroup.isValid(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private static Set<ClickTypeGroup> parseClickTypes(String clickTypesStr) {
        Set<ClickTypeGroup> result = new HashSet<>();

        String[] parts = clickTypesStr.split(",");
        for (String part : parts) {
            String trimmed = part.trim().toLowerCase();
            ClickTypeGroup type = ClickTypeGroup.fromPrefix(trimmed);

            if (type != null) {
                result.add(type);
            } else {
                DebugAPI.logLibWarn(DebugCategory.ITEMS,
                    "Unknown click type '" + trimmed + "', skipping");
            }
        }

        return result;
    }

    public static List<ClickAction> parseActions(List<String> actionStrings) {
        if (actionStrings == null || actionStrings.isEmpty()) {
            return Collections.emptyList();
        }

        List<ClickAction> actions = new ArrayList<>();
        for (String actionString : actionStrings) {
            ClickAction action = parseAction(actionString);
            if (action != null) {
                actions.add(action);
            }
        }

        return actions;
    }

    public static List<ClickCommand> parseCommands(List<String> commandStrings) {
        if (commandStrings == null || commandStrings.isEmpty()) {
            return Collections.emptyList();
        }

        List<ClickCommand> commands = new ArrayList<>();
        for (String commandString : commandStrings) {
            ClickCommand command = parseCommand(commandString);
            if (command != null) {
                commands.add(command);
            }
        }

        return commands;
    }
}
