package net.exylia.commons.item.exceptions;

public class ItemConfigurationException extends ItemException {

    private final String itemId;
    private final String configurationKey;

    public ItemConfigurationException(String itemId, String message) {
        super(String.format("Configuration error for item '%s': %s", itemId, message));
        this.itemId = itemId;
        this.configurationKey = null;
    }

    public ItemConfigurationException(String itemId, String configurationKey, String message) {
        super(String.format("Configuration error for item '%s' at key '%s': %s", itemId, configurationKey, message));
        this.itemId = itemId;
        this.configurationKey = configurationKey;
    }

    public ItemConfigurationException(String itemId, String message, Throwable cause) {
        super(String.format("Configuration error for item '%s': %s", itemId, message), cause);
        this.itemId = itemId;
        this.configurationKey = null;
    }

    public String getItemId() {
        return itemId;
    }

    public String getConfigurationKey() {
        return configurationKey;
    }
}
