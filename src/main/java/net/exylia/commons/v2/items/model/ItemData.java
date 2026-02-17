package net.exylia.commons.v2.items.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.exylia.commons.v2.items.config.ArmorTrimConfig;
import net.exylia.commons.v2.items.config.LeatherArmorConfig;
import net.exylia.commons.v2.items.config.PotionConfig;
import net.exylia.commons.v2.items.config.SlotConfig;
import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ItemData {

    @Builder.Default
    private ItemStack itemStack = null;

    @Builder.Default
    private Supplier<ItemStack> itemStackSupplier = null;

    @Builder.Default
    private String rawMaterial = "STONE";

    @Builder.Default
    private String rawAmount = null;

    @Builder.Default
    private String rawName = null;

    @Builder.Default
    private String rawDisplayName = null;

    @Builder.Default
    private List<String> rawLore = new ArrayList<>();

    @Builder.Default
    private Supplier<List<String>> loreDynamicSupplier = null;

    @Builder.Default
    private Map<String, Integer> rawEnchantments = new HashMap<>();

    @Builder.Default
    private PotionConfig potionConfig = null;

    @Builder.Default
    private ArmorTrimConfig armorTrimConfig = null;

    @Builder.Default
    private LeatherArmorConfig leatherArmorConfig = null;

    @Builder.Default
    private boolean glowing = false;

    @Builder.Default
    private boolean hideAttributes = false;

    @Builder.Default
    private boolean hideTooltip = false;

    @Builder.Default
    private String rawItemModel = null;

    @Builder.Default
    private String rawTooltipStyle = null;

    @Builder.Default
    private List<String> clickSounds = new ArrayList<>();

    @Builder.Default
    private PlaceholderContext context = PlaceholderContext.create();

    @Builder.Default
    private long updateInterval = 20L;

    @Builder.Default
    private boolean dynamicUpdate = false;

    @Builder.Default
    private List<String> rawAttributes = new ArrayList<>();

    @Builder.Default
    private Map<String, String> customNBT = new HashMap<>();

    @Builder.Default
    private boolean unbreakable = false;

    @Builder.Default
    private int maxStackSize = -1;

    @Builder.Default
    private SlotConfig slotConfig = null;

    @Builder.Default
    private List<ClickAction> actions = new ArrayList<>();

    @Builder.Default
    private List<ClickCommand> commands = new ArrayList<>();

    @Builder.Default
    private boolean requiresTarget = false;

    @Builder.Default
    private String templateKey = null;

    public ItemData copy() {
        return ItemData.builder()
                .itemStack(this.itemStack != null ? this.itemStack.clone() : null)
                .itemStackSupplier(this.itemStackSupplier)
                .rawMaterial(this.rawMaterial)
                .rawAmount(this.rawAmount)
                .rawName(this.rawName)
                .rawDisplayName(this.rawDisplayName)
                .rawLore(new ArrayList<>(this.rawLore))
                .loreDynamicSupplier(this.loreDynamicSupplier)
                .rawEnchantments(new HashMap<>(this.rawEnchantments))
                .potionConfig(this.potionConfig)
                .armorTrimConfig(this.armorTrimConfig)
                .leatherArmorConfig(this.leatherArmorConfig)
                .glowing(this.glowing)
                .hideAttributes(this.hideAttributes)
                .hideTooltip(this.hideTooltip)
                .rawItemModel(this.rawItemModel)
                .rawTooltipStyle(this.rawTooltipStyle)
                .clickSounds(new ArrayList<>(this.clickSounds))
                .context(this.context.copy())
                .updateInterval(this.updateInterval)
                .dynamicUpdate(this.dynamicUpdate)
                .rawAttributes(new ArrayList<>(this.rawAttributes))
                .customNBT(new HashMap<>(this.customNBT))
                .unbreakable(this.unbreakable)
                .maxStackSize(this.maxStackSize)
                .slotConfig(this.slotConfig)
                .actions(new ArrayList<>(this.actions))
                .commands(new ArrayList<>(this.commands))
                .requiresTarget(this.requiresTarget)
                .templateKey(this.templateKey)
                .build();
    }
}
