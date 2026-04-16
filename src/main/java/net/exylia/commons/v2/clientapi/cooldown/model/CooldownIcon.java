package net.exylia.commons.v2.clientapi.cooldown.model;

public abstract class CooldownIcon {

    public static CooldownIcon item(String itemName) {
        return new ItemCooldownIcon(itemName);
    }

    public static CooldownIcon resource(String resourceLocation, int size) {
        return new ResourceCooldownIcon(resourceLocation, size);
    }

    public static final class ItemCooldownIcon extends CooldownIcon {
        private final String itemName;

        ItemCooldownIcon(String itemName) {
            this.itemName = itemName;
        }

        public String getItemName() {
            return itemName;
        }
    }

    public static final class ResourceCooldownIcon extends CooldownIcon {
        private final String resourceLocation;
        private final int size;

        ResourceCooldownIcon(String resourceLocation, int size) {
            this.resourceLocation = resourceLocation;
            this.size = size;
        }

        public String getResourceLocation() {
            return resourceLocation;
        }

        public int getSize() {
            return size;
        }
    }
}
