package net.exylia.commons.v2.reward.api;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import net.exylia.commons.v2.reward.model.RewardEntry;
import net.exylia.commons.v2.reward.model.RewardType;
import net.exylia.commons.v2.ui.api.MenuAPI;
import net.exylia.commons.v2.ui.model.MenuData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public final class RewardPreviewAPI {

    private RewardPreviewAPI() {}

    public static void open(Player player, ConfigurationSection menuConfig, List<RewardEntry> rewards) {
        open(player, menuConfig, rewards, PlaceholderContext.create());
    }

    public static void open(Player player, ConfigurationSection menuConfig, List<RewardEntry> rewards, PlaceholderContext extraContext) {
        MenuData menuData = MenuAPI.parse(menuConfig);
        menuData.setContext(extraContext.copy().put("reward_count", String.valueOf(rewards.size())));
        menuData.withPaginationData(rewards, entry -> PlaceholderContext.create()
                .put("reward_icon", entry.getResolvedIconMaterial())
                .put("reward_name", entry.getDisplayName())
                .put("reward_chance", formatChance(entry.getChance()))
                .put("reward_type", formatType(entry.getType()))
                .put("reward_has_condition", entry.getCondition() != null ? "{success}Yes" : "{error}No")
                .put("reward_has_permission", entry.getPermission() != null ? "{success}Yes" : "{error}No"));

        MenuAPI.open(player, menuData);
    }

    private static String formatChance(double chance) {
        String formatted = chance == Math.floor(chance) ? String.valueOf((int) chance) : String.valueOf(chance);
        return formatted + "%";
    }

    private static String formatType(RewardType type) {
        return switch (type) {
            case COMMAND -> "{warning}Command";
            case ITEM -> "{info}Item";
            case MESSAGE -> "{letters}Message";
        };
    }
}
