package net.exylia.commons.v2.action.namespace;

public class NamespaceValidator {

    private static final String NAMESPACE_PATTERN = "^[a-z0-9_]+$";
    private static final String ACTION_ID_PATTERN = "^[a-z0-9_]+$";

    public static boolean isValidNamespace(String namespace) {
        if (namespace == null || namespace.isEmpty()) {
            return false;
        }
        return namespace.matches(NAMESPACE_PATTERN);
    }

    public static boolean isValidActionId(String actionId) {
        if (actionId == null || actionId.isEmpty()) {
            return false;
        }
        return actionId.matches(ACTION_ID_PATTERN);
    }

    public static boolean isValidFullId(String fullId) {
        if (fullId == null || fullId.isEmpty()) {
            return false;
        }

        if (!fullId.contains(":")) {
            return isValidActionId(fullId);
        }

        String[] parts = fullId.split(":", 2);
        if (parts.length != 2) {
            return false;
        }

        return isValidNamespace(parts[0]) && isValidActionId(parts[1]);
    }
}
