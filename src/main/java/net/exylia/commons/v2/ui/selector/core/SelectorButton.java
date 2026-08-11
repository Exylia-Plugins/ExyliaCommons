package net.exylia.commons.v2.ui.selector.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.items.model.ClickAction;
import net.exylia.commons.v2.items.model.ClickTypeGroup;
import net.exylia.commons.v2.items.model.ItemData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * A plugin-defined button injected into a built-in selector menu, letting consumers extend an
 * editor UI without forking it.
 *
 * <p>The handler receives the live editor session, so it can mutate the in-progress data. Nothing
 * is persisted until the player saves, which keeps destructive buttons (bulk imports, presets)
 * fully revertible through the menu's own cancel button.
 *
 * @param <S> the session type of the selector the button belongs to
 */
@Getter
@Builder
public class SelectorButton<S> {

    private final String key;

    private final int slot;

    @Builder.Default
    private final String material = "PAPER";

    private final String displayName;

    @Singular("lore")
    private final List<String> lore;

    @Builder.Default
    private final boolean glowing = false;

    @Builder.Default
    private final ClickTypeGroup clickType = ClickTypeGroup.ANY;

    private final BiConsumer<Player, S> onClick;

    /** Reopens the selector menu after the handler runs, so mutations are reflected instantly. */
    @Builder.Default
    private final boolean refreshOnClick = true;

    public ItemData toItemData(String action) {
        return ItemData.builder()
                .rawMaterial(material)
                .rawDisplayName(displayName)
                .rawLore(lore)
                .glowing(glowing)
                .slotConfig(SlotConfig.single(slot))
                .actions(List.of(ClickAction.builder()
                        .clickType(clickType)
                        .action(action)
                        .build()))
                .build();
    }

    public void run(Player player, S session) {
        if (onClick != null) onClick.accept(player, session);
    }
}
