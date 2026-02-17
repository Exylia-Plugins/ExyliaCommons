package net.exylia.commons.v2.wizard.session;

import lombok.Getter;
import net.exylia.commons.v2.wizard.config.WizardConfig;
import net.exylia.commons.v2.wizard.handler.InteractionHandler;
import net.exylia.commons.v2.wizard.model.InteractionType;
import net.exylia.commons.v2.wizard.model.WizardType;
import net.exylia.commons.v2.wizard.result.WizardResult;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

@Getter
public class InteractionSession<T> extends WizardSession<T> {
    private final InteractionHandler<T> handler;

    public InteractionSession(Player player, WizardConfig config, InteractionHandler<T> handler) {
        super(player, config);
        this.handler = handler;
    }

    @Override
    public WizardType getType() {
        return WizardType.INTERACTION;
    }

    @Override
    public void handleInteraction(PlayerInteractEvent event) {
        if (!player.isSneaking()) return;

        InteractionType type = toInteractionType(event.getAction());
        if (type == null) return;

        event.setCancelled(true);
        WizardResult<T> result = handler.onInteraction(player, type, event);
        processResult(result);
    }

    private InteractionType toInteractionType(Action action) {
        return switch (action) {
            case LEFT_CLICK_BLOCK, LEFT_CLICK_AIR -> InteractionType.LEFT_CLICK;
            case RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR -> InteractionType.RIGHT_CLICK;
            default -> null;
        };
    }

    private void processResult(WizardResult<T> result) {
        switch (result.getType()) {
            case COMPLETE -> complete(result.getValue());
            case CANCEL -> cancel();
            case CONTINUE -> {}
        }
    }
}
