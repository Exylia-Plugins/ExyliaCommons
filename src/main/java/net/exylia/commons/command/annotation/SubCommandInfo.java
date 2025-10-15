package net.exylia.commons.command.annotation;

public record SubCommandInfo(String name, String usage, String permission, boolean playerOnly, String[] aliases,
                             int order) {

    public boolean matches(String input) {
        if (name.equalsIgnoreCase(input)) {
            return true;
        }

        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) {
                return true;
            }
        }

        return false;
    }
}
