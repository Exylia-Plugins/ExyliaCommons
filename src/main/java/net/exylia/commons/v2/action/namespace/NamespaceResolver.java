package net.exylia.commons.v2.action.namespace;

public class NamespaceResolver {

    public static String extractNamespace(String fullId) {
        if (fullId == null || !fullId.contains(":")) {
            return null;
        }

        int colonIndex = fullId.indexOf(':');
        return fullId.substring(0, colonIndex);
    }

    public static String extractActionId(String fullId) {
        if (fullId == null || !fullId.contains(":")) {
            return fullId;
        }

        int colonIndex = fullId.indexOf(':');
        return fullId.substring(colonIndex + 1);
    }

    public static String buildFullId(String namespace, String actionId) {
        if (namespace == null || namespace.isEmpty()) {
            return actionId;
        }
        return namespace + ":" + actionId;
    }

    public static boolean hasNamespace(String fullId) {
        return fullId != null && fullId.contains(":");
    }
}
