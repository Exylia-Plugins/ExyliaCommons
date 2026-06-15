package net.exylia.commons.v2.ui.selector.impl.effect.action;

import net.exylia.commons.v2.action.api.ActionAPI;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectEditorRegistry;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectEditorSession;
import net.exylia.commons.v2.ui.selector.impl.effect.PotionEffectSelector;
import net.exylia.commons.v2.ui.selector.impl.effect.menu.PotionEffectListMenu;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class PotionEffectEditorActionRegistrar {

    private static final String NS = "commons";

    private PotionEffectEditorActionRegistrar() {}

    public static void register(JavaPlugin plugin) {
        if (ActionAPI.get(NS + ":effect_add").isPresent()) return;

        ActionAPI.create("effect_add", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    PotionEffectEditorSession session = PotionEffectEditorRegistry.getInstance().get(player);
                    if (session == null) return;
                    PotionEffectSelector.of(player)
                            .onSelect((p, result) -> {
                                session.addEffect(result);
                                PotionEffectListMenu.open(p, session);
                            })
                            .onCancel(() -> PotionEffectListMenu.open(player, session))
                            .open();
                })
                .build();

        ActionAPI.create("effect_delete", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    PotionEffectEditorSession session = PotionEffectEditorRegistry.getInstance().get(player);
                    if (session == null) return;
                    int index = args.getInt(0, -1);
                    session.removeEffect(index);
                    PotionEffectListMenu.open(player, session);
                })
                .build();

        ActionAPI.create("effect_save", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    PotionEffectEditorSession session = PotionEffectEditorRegistry.getInstance().get(player);
                    if (session == null) return;
                    PotionEffectEditorRegistry.getInstance().remove(player);
                    if (session.getOnSave() != null) {
                        session.getOnSave().accept(player, List.copyOf(session.getEffects()));
                    }
                })
                .build();

        ActionAPI.create("effect_cancel", plugin).namespace(NS)
                .handler((ctx, args) -> {
                    Player player = ctx.getPlayer();
                    PotionEffectEditorSession session = PotionEffectEditorRegistry.getInstance().get(player);
                    if (session == null) return;
                    PotionEffectEditorRegistry.getInstance().remove(player);
                    if (session.getOnCancel() != null) {
                        session.getOnCancel().run();
                    }
                })
                .build();
    }
}
