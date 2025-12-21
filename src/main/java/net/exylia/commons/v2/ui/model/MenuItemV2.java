package net.exylia.commons.v2.ui.model;

import lombok.Getter;
import lombok.Setter;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.ui.adapter.MenuItemAdapter;
import net.exylia.commons.v2.ui.config.MenuItemConfig;
import net.exylia.commons.v2.ui.sound.SoundConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Getter
@Setter
public class MenuItemV2 {
    private final String id;
    private final MenuItemAdapter itemAdapter;

    private final Map<ClickType, List<ClickAction>> clickActions = new HashMap<>();
    private long cooldown = 0L;
    private String permission;
    private SoundConfig clickSound;

    private MenuItemV2(String id, MenuItemAdapter adapter) {
        this.id = id;
        this.itemAdapter = adapter;
    }

    public static MenuItemV2 fromConfig(MenuItemConfig config) {
        MenuItemAdapter adapter = MenuItemAdapter.fromConfig(config);
        MenuItemV2 item = new MenuItemV2(config.getId(), adapter);

        config.getClickActions().forEach((clickType, actions) -> {
            item.clickActions.put(clickType, new ArrayList<>(actions));
        });

        item.clickSound = config.getClickSound();

        return item;
    }

    public static MenuItemV2 create(Material material) {
        MenuItemConfig config = MenuItemConfig.builder()
                .id(UUID.randomUUID().toString())
                .rawMaterial(material.name())
                .build();
        return fromConfig(config);
    }

    public static MenuItemV2 create(String materialName) {
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            return create(material);
        } catch (IllegalArgumentException e) {
            return create(Material.STONE);
        }
    }

    @Deprecated
    public MenuItemV2 setName(String name) {
        return this;
    }

    @Deprecated
    public MenuItemV2 setLore(String... lore) {
        return this;
    }

    @Deprecated
    public MenuItemV2 setLore(List<String> lore) {
        return this;
    }

    @Deprecated
    public MenuItemV2 setLoreDynamic(Supplier<List<String>> supplier) {
        return this;
    }

    @Deprecated
    public MenuItemV2 setAmount(int amount) {
        return this;
    }

    public MenuItemV2 addAction(ClickType clickType, ClickAction action) {
        clickActions.computeIfAbsent(clickType, k -> new ArrayList<>()).add(action);
        return this;
    }

    public MenuItemV2 addAction(ClickType clickType, String actionString) {
        ClickAction action = ClickAction.builder()
                .actionString(actionString)
                .async(true)
                .build();
        addAction(clickType, action);
        return this;
    }

    @Deprecated
    public MenuItemV2 addEnchantment(String enchantmentName, int level) {
        return this;
    }

    @Deprecated
    public MenuItemV2 setGlowing(boolean glowing) {
        return this;
    }

    @Deprecated
    public MenuItemV2 setHideAttributes(boolean hideAttributes) {
        return this;
    }

    @Deprecated
    public MenuItemV2 setHideEnchants(boolean hideEnchants) {
        return this;
    }

    @Deprecated
    public MenuItemV2 setDynamicUpdate(boolean dynamicUpdate) {
        return this;
    }

    public MenuItemV2 setClickSound(SoundConfig sound) {
        this.clickSound = sound;
        return this;
    }

    @Deprecated
    public MenuItemV2 withContext(PlaceholderContext context) {
        return this;
    }

    public CompletableFuture<ItemStack> buildAsync(Player player, PlaceholderContext menuContext) {
        return itemAdapter.buildAsync(player, menuContext != null ? menuContext : PlaceholderContext.create());
    }

    public ItemStack build(Player player, PlaceholderContext menuContext) {
        return itemAdapter.build(player, menuContext != null ? menuContext : PlaceholderContext.create());
    }

    public void executeActions(Player player, ClickType clickType, MenuContext menuContext) {
        List<ClickAction> actions = clickActions.get(clickType);
        if (actions == null || actions.isEmpty()) {
            return;
        }

        if (clickSound != null) {
            clickSound.play(player);
        }

        for (ClickAction action : actions) {
            if (action.isAsync()) {
                action.executeAsync(player, menuContext);
            } else {
                action.execute(player, menuContext);
            }
        }
    }

    public MenuItemV2 clone() {
        MenuItemV2 cloned = new MenuItemV2(UUID.randomUUID().toString(), this.itemAdapter);
        cloned.clickActions.putAll(this.clickActions);
        cloned.cooldown = this.cooldown;
        cloned.permission = this.permission;
        cloned.clickSound = this.clickSound;
        return cloned;
    }

    public boolean isDynamicUpdate() {
        return itemAdapter != null && itemAdapter.getItemData().isDynamicUpdate();
    }

    public Supplier<List<String>> getLoreDynamicSupplier() {
        return itemAdapter != null ? itemAdapter.getItemData().getLoreDynamicSupplier() : null;
    }

    public String getRawName() {
        return itemAdapter != null ? itemAdapter.getItemData().getRawName() : null;
    }

    public List<String> getRawLore() {
        return itemAdapter != null ? itemAdapter.getItemData().getRawLore() : null;
    }
}
