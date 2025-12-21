package net.exylia.commons.v2.ui.config;

import lombok.Builder;
import lombok.Getter;
import net.exylia.commons.v2.ui.model.ClickAction;
import net.exylia.commons.v2.ui.sound.SoundConfig;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MenuItemConfig {
    private final String id;
    private final String rawMaterial;
    private final String rawName;
    @Builder.Default
    private final String rawAmount = "1";
    @Builder.Default
    private final List<String> rawLore = new ArrayList<>();

    @Builder.Default
    private final Map<ClickType, List<ClickAction>> clickActions = new HashMap<>();
    @Builder.Default
    private final Map<String, Integer> enchantments = new HashMap<>();

    private final String potionType;
    private final String armorTrimPattern;
    private final String armorTrimMaterial;
    private final String leatherArmorColor;
    private final String itemModel;
    @Builder.Default
    private final List<String> attributes = new ArrayList<>();
    @Builder.Default
    private final Map<String, String> customNBT = new HashMap<>();

    @Builder.Default
    private final boolean glowing = false;
    @Builder.Default
    private final boolean hideAttributes = false;
    @Builder.Default
    private final boolean dynamicUpdate = false;
    @Builder.Default
    private final long updateInterval = 20L;

    private final SoundConfig clickSound;

    private final int slot;
    @Builder.Default
    private final List<Integer> slots = new ArrayList<>();

    private final String customItemId;
    private final String skullTexture;
    private final String skullOwner;
}
