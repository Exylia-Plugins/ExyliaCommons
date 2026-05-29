package net.exylia.commons.v2.chat.request;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@Getter
@Setter
public class ConfirmationRequest extends InputRequest<Boolean> {

    private Runnable onConfirm;
    private Runnable onDeny;

    public ConfirmationRequest(Player player, String prompt) {
        super(player, prompt);
    }

    @Override
    public void accept(Boolean value) {
        if (Boolean.TRUE.equals(value)) {
            if (onConfirm != null) onConfirm.run();
        } else {
            if (onDeny != null) onDeny.run();
        }
    }
}
