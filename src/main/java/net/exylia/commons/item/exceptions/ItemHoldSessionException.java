package net.exylia.commons.item.exceptions;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

@Getter
public class ItemHoldSessionException extends RuntimeException {

    public ItemHoldSessionException(String message) {
        super(message);
    }

    public ItemHoldSessionException(String message, Throwable cause) {
        super(message, cause);
    }

}
